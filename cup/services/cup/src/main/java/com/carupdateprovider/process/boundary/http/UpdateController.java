package com.carupdateprovider.process.boundary.http;

import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.carupdateprovider.process.boundary.producer.SqsProducer;
import com.carupdateprovider.process.model.Queue;
import com.carupdateprovider.process.model.RoutingMessage;
import com.carupdateprovider.process.model.Status;
import com.carupdateprovider.process.service.history.HistoryService;

import lombok.RequiredArgsConstructor;

/**
 * HTTP interface for triggering an update session.
 * Only active in the ext-request-proxy service.
 */
@ConditionalOnProperty(prefix = "application", name = "ext-request-proxy", havingValue = "true")
@RestController
@RequiredArgsConstructor
@RequestMapping("request")
public class UpdateController {

    @Value("${aws.sqs.processingTime}")
    private int processingTime;

    private final SqsProducer producer;
    private final HistoryService historyService;

    /**
     * Get endpoint for triggering an update session.
     * Sends a message to the EXT_REQUEST SQS queue and persists the created session to the DB.
     *
     * @param vin 17 character vehicle identification number to start the update for
     * @return response message including the created session id for the VIN
     */
    @GetMapping("/{vin}")
    public String trigger(@PathVariable String vin) {
        if (vin.length() != 17) {
            throw new IllegalArgumentException("VIN must be 17 characters long.");
        }
        UUID id = UUID.randomUUID();
        RoutingMessage message = RoutingMessage.builder()
                                               .sessionId(id)
                                               .vin(vin)
                                               .processingTime(processingTime)
                                               .time(new Date())
                                               .queue(Queue.EXT_REQUEST)
                                               .build();
        producer.send(message);
        message.setStatus(Status.REQUESTED);
        historyService.storeHistoryStatus(message);

        return "Session " + id + " for VIN " + vin + " requested.";
    }

}
