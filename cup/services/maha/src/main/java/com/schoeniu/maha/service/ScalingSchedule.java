package com.schoeniu.maha.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.schoeniu.maha.api.K8sApi;
import com.schoeniu.maha.api.SqsApi;
import com.schoeniu.maha.config.properties.ScalingConfigProperties;
import com.schoeniu.maha.config.properties.ScalingConfigProperties.ConsumerServiceConfig;
import com.schoeniu.maha.config.properties.ScalingConfigProperties.StrategyConfig;
import com.schoeniu.maha.observability.MetricManager;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Main ScalingSchedule to continuously run the scaling algorithm
 */
@Slf4j
@AllArgsConstructor
@Service
public class ScalingSchedule {

    private MetricManager metricManager;
    private ScalingConfigProperties scalingConfig;
    private SqsApi sqsApi;
    private K8sApi k8SApi;
    private RateService rateService;

    private Map<String, Date> upscaleTimes;

    /**
     * Schedule method to execute the scaling algorithm
     */
    @Scheduled(cron = "* * * * * *")
    public void schedule() {
        log.info("================= Start schedule =================");

        final StrategyConfig strategy = scalingConfig.getStrategy();

        //query and export number of messages in queues metrics
        scalingConfig.getQueuesConsumedFrom()
                     .keySet()
                     .forEach(queue -> metricManager.setMessagesInQueueGauge(queue, sqsApi.getNumberOfMessages(queue)));

        //exit if scaling is disabled
        if (!strategy.isScalingEnabled()) {
            log.info("Scaling is disabled.");
            return;
        }

        // calculate origin queue rates which need to be consumed per minute
        final Map<String, Float> originQueueRates = new HashMap<>();
        for (Map.Entry<String, ConsumerServiceConfig> entry : scalingConfig.getQueuesConsumedFrom()
                                                                           .entrySet()) {
            //skip queues without consumer
            if (StringUtils.isBlank(entry.getValue()
                                         .getServiceName())) {
                continue;
            }
            final String queue = entry.getKey();
            if (originQueueRates.containsKey(queue)) {
                throw new IllegalStateException("Queues must not be defined twice");
            }
            float messagesToConsumePerMinute =
                    metricManager.getCurrentMessageInQueue(queue) * strategy.getQueueDecreasePerMinute();
            originQueueRates.put(queue, messagesToConsumePerMinute);
        }
        log.info("Origin rates: {}", originQueueRates);

        // calculate follow-up queue rates if follow-up scaling is enabled
        final Map<String, Float> followUpQueueRates = new HashMap<>();
        if (strategy.isFollowUpScalingEnabled()) {
            originQueueRates.forEach((queue, rate) -> addMap(followUpQueueRates,
                                                             rateService.calcFollowUpQueueRates(queue, rate)));
            log.info("Follow up rates: {}", followUpQueueRates);
        } else {
            log.info("Follow up scaling disabled.");
        }
        // calculate total queue rates
        final Map<String, Float> totalRates = new HashMap<>(originQueueRates);
        addMap(totalRates, followUpQueueRates);
        log.info("Total rates: {}", totalRates);

        // determine number of pods to scale to
        final Map<String, Integer> scalingUpMap = new HashMap<>();
        final Map<String, Integer> scalingDownMap = new HashMap<>();
        final Map<String, Integer> currentPods = k8SApi.getReplicasPerDeployment();
        mapRatesToRequiredPods(totalRates).forEach((service, numberOfRequiredPods) -> {
            int numberOfCurrentPods = currentPods.get(service);

            Date lastTimeConsumerScaled = upscaleTimes.getOrDefault(service, new Date(0));
            long timeSinceLastUpscale = new Date().getTime() - lastTimeConsumerScaled.getTime();
            long waitTimeAfterLastUpscale = strategy.getDownScaleStabilizationSeconds() * 1000L;
            if (numberOfCurrentPods > 1 && numberOfRequiredPods == 0) {
                if (timeSinceLastUpscale >= waitTimeAfterLastUpscale) {
                    putInMap(service, numberOfRequiredPods + 1, scalingDownMap);
                } else {
                    log.info("Ignoring scaling service {} current pods are {}, required are {}, "
                             + "because time since last upscale is {} seconds, which is less than configured {}.",
                             service,
                             numberOfCurrentPods,
                             numberOfRequiredPods,
                             timeSinceLastUpscale / 1000L,
                             waitTimeAfterLastUpscale / 1000L);
                }
            } else if (numberOfCurrentPods < numberOfRequiredPods) {
                putInMap(service, numberOfRequiredPods, scalingUpMap);
            }
        });

        //execute scaling
        scalingUpMap.forEach((service, scale) -> {
            assert scale != null;
            int safeScale = Math.min(scale, strategy.getMaxNumberOfPods());
            k8SApi.scaleDeployment(service, safeScale);
            upscaleTimes.put(service, new Date());
            log.info("{} scaled UP to {}", service, safeScale);
        });
        scalingDownMap.forEach((service, scale) -> {
            assert scale != null;
            k8SApi.scaleDeployment(service, scale);
            log.info("{} scaled DOWN to {}", service, scale);
        });
        log.info("================= End schedule =================\n");
    }

    /**
     * Utility function which puts key and value in a map.
     * If the key already exists the biggest value is put in the map.
     *
     * @param key   key to add
     * @param value value to add
     * @param map   map to add key and value to
     */
    private void putInMap(final String key, final Integer value, final Map<String, Integer> map) {
        int val = value == null ? -1 : value;
        map.put(key, Math.max(val, map.getOrDefault(key, -1)));
    }

    /**
     * Utility function for combining maps
     *
     * @param originMap original map which receives new entries
     * @param mapToAdd  map to add to the origin map
     */
    private void addMap(final Map<String, Float> originMap, final Map<String, Float> mapToAdd) {
        mapToAdd.forEach((key, value) -> originMap.put(key, value + originMap.getOrDefault(key, 0F)));
    }

    /**
     * Maps a map of queues and their rates which need to be consumed to a map of services and how man pods are needed.
     *
     * @param totalRates map of queues and their rates which need to be consumed
     * @return map of services and how man pods are needed
     */
    private Map<String, Integer> mapRatesToRequiredPods(final Map<String, Float> totalRates) {
        Map<String, Integer> result = new HashMap<>();
        totalRates.forEach((queue, rate) -> {
            ConsumerServiceConfig consumerConfig = scalingConfig.getQueuesConsumedFrom()
                                                                .get(queue);
            if (consumerConfig == null || StringUtils.isBlank(consumerConfig.getServiceName())) {
                return;
            }
            String service = consumerConfig.getServiceName();
            int numberOfRequiredPods = calculateNumberOfRequiredPods(queue, service, rate);
            log.info("Service {} requires {} pods for queue {}",
                     service,
                     numberOfRequiredPods < 0.25 ? "0.25" : numberOfRequiredPods,
                     queue);
            result.put(service, Math.max(result.getOrDefault(service, 0), numberOfRequiredPods));
        });
        return result;
    }

    /**
     * Calculates how many pods are needed of a service to consume given rates from a queue.
     *
     * @param queue        queue to consume from
     * @param serviceName  service name which consumes
     * @param requiredRate rate how many messages should be consumed
     * @return number of pods needed
     */
    private int calculateNumberOfRequiredPods(final String queue, final String serviceName, final float requiredRate) {
        float consumptionPerMinute = rateService.getConsumptionRate(queue, serviceName);
        if (consumptionPerMinute < 1) {
            log.warn("Consumption rate for service {} on queue {} is only {}. "
                     + "Check for performance issues. Calculating with rate 1 instead.",
                     serviceName,
                     queue,
                     consumptionPerMinute);
            consumptionPerMinute = 1;
        }
        float requiredPods = requiredRate / consumptionPerMinute;
        if (requiredPods < 0.25) {
            return 0;
        }
        return (int) Math.ceil(requiredRate / consumptionPerMinute);
    }

}
