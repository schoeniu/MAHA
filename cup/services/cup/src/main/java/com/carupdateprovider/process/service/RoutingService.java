package com.carupdateprovider.process.service;

import static com.carupdateprovider.process.model.Queue.CACHE_REQUEST;
import static com.carupdateprovider.process.model.Queue.CACHE_RESPONSE;
import static com.carupdateprovider.process.model.Queue.PROCESSED;
import static com.carupdateprovider.process.model.Queue.ROLLED_OUT;
import static com.carupdateprovider.process.model.Queue.TRIGGER;
import static com.carupdateprovider.process.model.Queue.VEHICLE_DATA_REQUEST;
import static com.carupdateprovider.process.model.Queue.VEHICLE_DATA_RESPONSE;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.carupdateprovider.process.boundary.producer.SqsProducer;
import com.carupdateprovider.process.model.RoutingMessage;
import com.carupdateprovider.process.model.Status;
import com.carupdateprovider.process.service.history.HistoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for consuming a message and producing the corresponding next one.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingService {

    @Value("${aws.sqs.processingTime}")
    private int processingTime;

    private final ProcessingService processingService;
    private final HistoryService historyService;
    private final SqsProducer producer;

    /**
     * Routing method for consuming a message and producing the corresponding next
     * based one where the message came from.
     *
     * @param message message to route.
     */
    public void route(final RoutingMessage message) {

        processingService.simulateProcessingTime(message.getProcessingTime());

        switch (message.getQueue()) {
        case EXT_REQUEST -> producer.send(message.continueSession()
                                                 .processingTime(processingTime)
                                                 .queue(TRIGGER)
                                                 .status(Status.TRIGGERD)
                                                 .build());
        case TRIGGER -> producer.send(message.continueSession()
                                             .processingTime(processingTime)
                                             .queue(CACHE_REQUEST)
                                             .build());
        case CACHE_REQUEST -> producer.send(message.continueSession()
                                                   .processingTime(processingTime)
                                                   .queue(CACHE_RESPONSE)
                                                   .futureTarget(message.getVin()
                                                                        .startsWith("C")
                                                                 ? PROCESSED
                                                                 : VEHICLE_DATA_REQUEST)
                                                   .build());
        case CACHE_RESPONSE -> producer.send(message.continueSession()
                                                    .processingTime(processingTime)
                                                    .queue(message.getFutureTarget())
                                                    .status(message.getFutureTarget() == PROCESSED
                                                            ? Status.FETCHED
                                                            : Status.UNFETCHABLE)
                                                    .build());
        case VEHICLE_DATA_REQUEST -> producer.send(message.continueSession()
                                                          .processingTime(processingTime)
                                                          .queue(VEHICLE_DATA_RESPONSE)
                                                          .build());
        case VEHICLE_DATA_RESPONSE -> producer.send(message.continueSession()
                                                           .processingTime(processingTime)
                                                           .queue(PROCESSED)
                                                           .status(Status.CREATED)
                                                           .build());
        case PROCESSED -> producer.send(message.continueSession()
                                               .processingTime(processingTime)
                                               .queue(ROLLED_OUT)
                                               .status(Status.ROLLEDOUT)
                                               .build());
        case HISTORY -> historyService.storeHistoryStatus(message);
        case ROLLED_OUT -> log.info(message + " is rolled out.");
        default -> throw new IllegalArgumentException(String.format("Routing case %s does not exist.",
                                                                    message.getQueue()));
        }
    }
}

