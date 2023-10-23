package com.carupdateprovider.process.boundary.consumer;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.carupdateprovider.process.model.RoutingMessage;
import com.carupdateprovider.process.observability.MetricManager;
import com.carupdateprovider.process.service.RoutingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/**
 * SqsConsumer which start on application startup and continuously polls on the configured SQS queues.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SqsConsumer {

    @Value("#{'${aws.sqs.consumers}'.split(',')}")
    private Set<String> queues;

    @Value("${aws.sqs.client.account}")
    private String account;

    private final RoutingService routingService;
    private final MetricManager metricManager;
    private final SqsClient sqsClient;

    private boolean listen = true;
    private ExecutorService executorService = null;

    /**
     * Initialisation method which starts up one listener thread per configured queue.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initListeners() {
        executorService = Executors.newFixedThreadPool(queues.size());
        for (String queue : queues) {
            executorService.submit(() -> listen(queue));
        }
    }

    /**
     * Shutdown hook for graceful shutdown of listeners.
     */
    @PreDestroy
    @EventListener({ ContextStoppedEvent.class, ContextClosedEvent.class })
    public void stopListeners() {
        listen = false;
        log.info("Shutting down listeners...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
            Thread.currentThread()
                  .interrupt();
        }
    }

    /**
     * Continuously polls for messages on a queue and processes them.
     *
     * @param queue queue to listen on
     */
    private void listen(final String queue) {
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                                                                    .queueUrl(account + queue)
                                                                    .maxNumberOfMessages(10)
                                                                    .waitTimeSeconds(20)
                                                                    .build();

        log.info("Starting listener for {}", queue);
        while (listen) {
            log.debug("Sending new receive request on {}", queue);
            ReceiveMessageResponse response = sqsClient.receiveMessage(receiveRequest);
            response.messages()
                    .forEach(message -> {
                        try {
                            processMessage(queue, message);
                        } catch (Exception e) {
                            log.error("Unexpected error while consuming {}", message);
                        }
                    });
        }
        log.info("Stopped listener for {}", queue);
    }

    /**
     * Processes a single message consumed from a queue.
     *
     * @param queue   queue listening on
     * @param message message to be processed
     */
    private void processMessage(final String queue, final Message message) {
        long start = System.currentTimeMillis();
        final String body = message.body();
        log.info("Received message: {}", body);

        final RoutingMessage routingMessage;
        try {
            routingMessage = new ObjectMapper().readValue(body, RoutingMessage.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        long millisWaitedInQueue = new Date().getTime() - routingMessage.getTime()
                                                                        .getTime();
        log.info("Session {} waited {} millis in queue {}",
                 routingMessage.getSessionId(),
                 millisWaitedInQueue,
                 routingMessage.getQueue());
        metricManager.incConsumed(routingMessage.getQueue());

        routingService.route(routingMessage);
        log.info("Pre ack updating status for {}, with status {} to {}.",
                 routingMessage.getSessionId(),
                 routingMessage.getStatus(),
                 routingMessage.getTime());
        acknowledge(queue, message);
        long duration = System.currentTimeMillis() - start;
        log.info("Session {} processing time: {}", routingMessage.getSessionId(), duration);
    }

    /**
     * Acknowledges a message by deleting it from the origination queue.
     *
     * @param queue   listening on
     * @param message to be deleted from queue
     */
    private void acknowledge(final String queue, final Message message) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                                                    .queueUrl(account + queue)
                                                    .receiptHandle(message.receiptHandle())
                                                    .build());
    }
}
