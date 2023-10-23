package com.carupdateprovider.process.observability;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.carupdateprovider.process.model.Queue;

import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * MetricManager acting as interface for all micrometer/prometheus interactions.
 */
@Slf4j
@Component
public class MetricManager {

    private static final String APPLICATION = "application:";
    private static final String CONSUMED = APPLICATION + "consumed_from_queue";
    private static final String PRODUCED = APPLICATION + "produced_to_queue";

    @Value("#{'${aws.sqs.consumers}'.split(',')}")
    private String[] consumers;

    @Value("#{'${aws.sqs.producers}'.split(',')}")
    private String[] producers;

    @Value("${management.metrics.tags.application}")
    private String applicationTag;

    /**
     * Increments the metrics for messages consumed from a queue by 1.
     *
     * @param queue queue the message was consumed from.
     */
    public void incConsumed(final Queue queue) {
        Metrics.counter(CONSUMED, createTags(queue.name()))
               .increment();
    }

    /**
     * Increments the metrics for messages produced to a queue by 1.
     *
     * @param queue queue the message was produced to.
     */
    public void incProduced(final Queue queue) {
        Metrics.counter(PRODUCED, createTags(queue.name()))
               .increment();
    }

    private List<Tag> createTags(final String queue) {
        return List.of(new ImmutableTag("queue", queue), new ImmutableTag("application", applicationTag));
    }

    /**
     * Registers production and consumption counter metrics on startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void registerOnStartup() {
        log.debug("Consumers: {}; Producers: {}", consumers, producers);
        for (String consumer : consumers) {
            if (!consumer.isBlank()) {
                Metrics.counter(CONSUMED, createTags(consumer));
                log.debug("Registered metric for consumer {}", consumer);
            }
        }
        for (String producer : producers) {
            if (!producer.isBlank()) {
                Metrics.counter(PRODUCED, createTags(producer));
                log.debug("Registered metric for producer {}", producer);
            }
        }

    }
}

