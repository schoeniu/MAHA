package com.carupdateprovider.process.observability;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * LifeCycleLogger which logs string at application startup and shutdown.
 * These logs are used to calculate statistics based of the logs.
 */
@Slf4j
@Component
public class LifeCycleLogger {

    /**
     * Logs "ApplicationStarted" on application startup.
     */
    @PostConstruct
    public void onStartup() {
        log.info("ApplicationStarted");
    }

    /**
     * Logs "ApplicationStopped" on application startup.
     */
    @PreDestroy
    public void onShutdown() {
        log.info("ApplicationStopped");
    }
}
