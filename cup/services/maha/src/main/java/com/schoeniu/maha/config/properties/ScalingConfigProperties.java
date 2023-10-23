package com.schoeniu.maha.config.properties;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ScalingConfigProperties for defining scaling strategy settings and service relationships.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "scaling-config")
public class ScalingConfigProperties {

    private StrategyConfig strategy;
    private Map<String, ConsumerServiceConfig> queuesConsumedFrom;

    @Data
    @NoArgsConstructor
    public static class StrategyConfig {

        private boolean exportMetrics;
        private boolean scalingEnabled;
        private boolean followUpScalingEnabled;
        private float queueDecreasePerMinute;
        private int downScaleStabilizationSeconds;
        private int maxNumberOfPods;
    }

    @Data
    @NoArgsConstructor
    public static class ConsumerServiceConfig {

        private String serviceName;
        private float consumptionRate;
        private Map<String, ProducerQueueConfig> queuesProducedTo;
    }

    @Data
    @NoArgsConstructor
    public static class ProducerQueueConfig {

        private float relativeProductionRate;
    }

}

