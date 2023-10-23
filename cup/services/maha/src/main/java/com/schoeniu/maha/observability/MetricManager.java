package com.schoeniu.maha.observability;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.schoeniu.maha.util.MutableFloat;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * MetricManager acting as interface for all micrometer/prometheus interactions.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MetricManager {

    private static final String APPLICATION = "application:";
    private static final String MESSAGES_IN_QUEUE = APPLICATION + "number_of_messages_in_queue";

    @Value("${management.metrics.tags.application}")
    private String applicationTag;

    private final MeterRegistry meterRegistry;

    private final Map<String, MutableFloat> gauges = new HashMap<>();

    /**
     * Creates or updates the gauge metric of how many messages are in a queue
     *
     * @param queueName name of the queue the metric to set for
     * @param value     value to set the metric to
     */
    public void setMessagesInQueueGauge(final String queueName, final Number value) {
        final String metricId = createMetricId(queueName);
        if (!gauges.containsKey(metricId)) {
            final MutableFloat metricRef = MutableFloat.of(value);
            Gauge.builder(MESSAGES_IN_QUEUE, metricRef, MutableFloat::floatValue)
                 .strongReference(true)
                 .tags(createTags(queueName))
                 .register(meterRegistry);
            gauges.put(metricId, metricRef);
        } else {
            final MutableFloat metricRef = gauges.get(metricId);
            metricRef.setValue(value);
        }

        log.info("Set metric {} for queue {} to value {}", MESSAGES_IN_QUEUE, queueName, value.floatValue());
    }

    @NotNull
    private String createMetricId(String queueName) {
        return MESSAGES_IN_QUEUE + "_" + queueName;
    }

    private List<Tag> createTags(final String queueName) {
        return List.of(new ImmutableTag("queue", queueName), new ImmutableTag("application", applicationTag));
    }

    /**
     * Gets the current number of messages in a queue from the set gauge metrics.
     * Should only be called in a schedule iteration after gauges were updated with
     * com.schoeniu.maha.observability.MetricManager#setMessagesInQueueGauge(java.lang.String, java.lang.Number)
     *
     * @param queueName name of queue to get the current number of messages for
     * @return number of messages
     */
    public float getCurrentMessageInQueue(final String queueName) {
        return gauges.get(createMetricId(queueName))
                     .floatValue();
    }

}
