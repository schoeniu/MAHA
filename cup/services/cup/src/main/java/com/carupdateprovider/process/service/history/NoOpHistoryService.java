package com.carupdateprovider.process.service.history;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import com.carupdateprovider.process.model.RoutingMessage;

/**
 * Dummy implementation which is loaded when {@link DefaultHistoryService} is not loaded.
 * See javadoc of {@link DefaultHistoryService}.
 */
@ConditionalOnMissingBean(DefaultHistoryService.class)
@Service
public class NoOpHistoryService implements HistoryService {

    /**
     * {@link HistoryService} dummy implementation which should never be called.
     * Calling it indicates a configuration or programming error and will throw an exception.
     *
     * @param message RoutingMessage is being ignored.
     */
    @Override
    public void storeHistoryStatus(final RoutingMessage message) {
        throw new IllegalStateException(
                "NoOpHistoryService#storeHistoryStatus should not be called. Please check you config properties.");
    }
}
