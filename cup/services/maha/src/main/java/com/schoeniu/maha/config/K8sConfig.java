package com.schoeniu.maha.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.Config;

/**
 * Kubernetes API configuration
 */
@Configuration
public class K8sConfig {

    @Bean
    ApiClient apiClient() throws IOException {
        return Config.fromCluster();
    }

    @Bean
    public CoreV1Api coreV1Api(final ApiClient apiClient) {
        return new CoreV1Api(apiClient);
    }

    @Bean
    public AppsV1Api appsV1Api(final ApiClient apiClient) {
        return new AppsV1Api(apiClient);
    }

}
