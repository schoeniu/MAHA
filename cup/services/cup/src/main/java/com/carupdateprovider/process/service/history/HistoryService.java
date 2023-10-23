package com.carupdateprovider.process.service.history;

import com.carupdateprovider.process.model.RoutingMessage;

/**
 * HistoryService interface
 */
public interface HistoryService {

    /**
     * Persists the status of a given {@link RoutingMessage} to the database.
     *
     * @param message RoutingMessage to persist.
     */
    void storeHistoryStatus(final RoutingMessage message);
}
