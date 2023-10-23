package com.schoeniu.maha.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import com.schoeniu.maha.config.properties.ScalingConfigProperties;
import com.schoeniu.maha.config.properties.ScalingConfigProperties.ConsumerServiceConfig;
import com.schoeniu.maha.config.properties.ScalingConfigProperties.ProducerQueueConfig;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for calculating rates of message consumption
 */
@Slf4j
@AllArgsConstructor
@Service
public class RateService {

    private final ScalingConfigProperties scalingConfig;

    /**
     * Calculates recursively the rates how many messages services will need to consume.
     *
     * @param queue      origin queue name
     * @param originRate rate how fast the consumption from the origin queue is
     * @return map with queue names as key and rate to consume as value
     */
    public Map<String, Float> calcFollowUpQueueRates(final String queue, final float originRate) {
        Map<String, Float> result = new HashMap<>();
        if (scalingConfig.getQueuesConsumedFrom()
                         .containsKey(queue)) {
            calcFollowUpQueueRatesRecursive(scalingConfig.getQueuesConsumedFrom()
                                                         .get(queue), originRate, result);
        }
        return result;
    }

    private void calcFollowUpQueueRatesRecursive(final ConsumerServiceConfig consumer,
                                                 final float rateToConsume,
                                                 final Map<String, Float> result) {
        if (MapUtils.isEmpty(consumer.getQueuesProducedTo())) {
            return;
        }
        for (Map.Entry<String, ProducerQueueConfig> entry : consumer.getQueuesProducedTo()
                                                                    .entrySet()) {
            String producedQueue = entry.getKey();
            float producedToRate = entry.getValue()
                                        .getRelativeProductionRate() * rateToConsume;
            result.put(producedQueue, producedToRate + result.getOrDefault(entry.getKey(), 0F));
            if (scalingConfig.getQueuesConsumedFrom()
                             .containsKey(producedQueue)) {
                calcFollowUpQueueRatesRecursive(scalingConfig.getQueuesConsumedFrom()
                                                             .get(producedQueue), producedToRate, result);
            }
        }
    }

    /**
     * Get configured consumption rate of a service on a queue
     *
     * @param queue   queue name to consume from
     * @param service service name which consumes
     * @return configured consumption rate
     */
    public float getConsumptionRate(final String queue, final String service) {
        ConsumerServiceConfig config = scalingConfig.getQueuesConsumedFrom()
                                                    .get(queue);
        assert config != null && config.getServiceName()
                                       .equals(service);
        return config.getConsumptionRate();
    }

}
