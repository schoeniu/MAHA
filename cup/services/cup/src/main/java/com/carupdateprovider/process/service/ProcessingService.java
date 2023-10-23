package com.carupdateprovider.process.service;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for executing the processing time for a given time.
 */
@Service
@Slf4j
public class ProcessingService {

    /**
     * Executing the processing time for a given time by sleeping.
     *
     * @param time time to sleep.
     */
    public void simulateProcessingTime(final int time) {
        try {
            Thread.sleep(time);
            log.debug("Processing sleep " + time);
        } catch (InterruptedException e) {
            log.error("Processing sleep interrupted.", e);
            throw new RuntimeException(e);
        }
    }
}
