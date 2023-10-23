package com.carupdateprovider.process.service.history;

import java.sql.Timestamp;
import java.time.Instant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.carupdateprovider.process.dao.StatusRepository;
import com.carupdateprovider.process.model.RoutingMessage;
import com.carupdateprovider.process.model.Status;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Default {@link HistoryService} implementation for persisting a status received from {@link RoutingMessage}.
 * Only active in cup-history or ext-request-proxy.
 */
@ConditionalOnExpression("${application.history} or ${application.ext-request-proxy}")
@Slf4j
@AllArgsConstructor
@Service
public class DefaultHistoryService implements HistoryService {

    private StatusRepository statusRepository;

    /**
     * Persists the status of a given {@link RoutingMessage} to the database.
     *
     * @param message RoutingMessage to persist.
     */
    @Transactional
    public void storeHistoryStatus(final RoutingMessage message) {
        Assert.notNull(message.getSessionId(), message + " has no session UUID.");
        Assert.notNull(message.getStatus(), message + " has no status.");
        Assert.notNull(message.getVin(), message + " has no VIN.");
        Assert.notNull(message.getTime(), message + " has no time.");

        store(message);

        log.info("Updated status history for {}, with status {} to {}.",
                 message.getSessionId(),
                 message.getStatus(),
                 message.getTime());
    }

    private void store(final RoutingMessage message) {
        final Status messageStatus = message.getStatus();
        final Timestamp timestamp = new Timestamp(message.getTime()
                                                         .getTime());
        switch (messageStatus) {
        case REQUESTED -> statusRepository.upsertRequested(message.getSessionId(), message.getVin(), timestamp);
        case TRIGGERD -> statusRepository.upsertTriggered(message.getSessionId(), message.getVin(), timestamp);
        case FETCHED -> statusRepository.upsertFetched(message.getSessionId(), message.getVin(), timestamp);
        case UNFETCHABLE -> statusRepository.upsertUnfetchable(message.getSessionId(), message.getVin(), timestamp);
        case CREATED -> statusRepository.upsertCreated(message.getSessionId(), message.getVin(), timestamp);
        case ROLLEDOUT -> statusRepository.upsertRolledOut(message.getSessionId(), message.getVin(), timestamp);
        default ->
                throw new IllegalArgumentException(String.format("Invalid status %s can not be saved.", messageStatus));
        }
        statusRepository.upsertLastUpdated(message.getSessionId(), message.getVin(), Timestamp.from(Instant.now()));
    }

}
