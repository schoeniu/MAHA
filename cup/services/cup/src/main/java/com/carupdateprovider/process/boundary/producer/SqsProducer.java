package com.carupdateprovider.process.boundary.producer;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.carupdateprovider.process.model.Queue;
import com.carupdateprovider.process.model.RoutingMessage;
import com.carupdateprovider.process.model.Status;
import com.carupdateprovider.process.observability.MetricManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * SqsProducer which sends a {@link RoutingMessage} to SQS queues.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SqsProducer {

    @Value("${aws.sqs.client.endpoint}")
    private String endpoint;
    @Value("${aws.sqs.client.account}")
    private String account;

    private final MetricManager metricManager;
    private final SqsClient sqsClient;

    /**
     * Sends a {@link RoutingMessage} to SQS queues.
     * If the status of the message is populated, an additional message is sent to the HISTORY queue.
     *
     * @param message RoutingMessage to send
     */
    public void send(final RoutingMessage message) {
        long start = System.currentTimeMillis();
        message.setTime(new Date());
        final String messageString = messageAsString(message);
        sendTo(messageString, message.getQueue());
        metricManager.incProduced(message.getQueue());
        log.info("Sent message: " + messageString);
        long historyStart = System.currentTimeMillis();
        if (message.getStatus() != null && message.getStatus() != Status.NONE) {
            message.setQueue(Queue.HISTORY);
            final String historyMessageString = messageAsString(message);
            sendTo(historyMessageString, Queue.HISTORY);
            metricManager.incProduced(Queue.HISTORY);
            log.info("Sent message to history: " + historyMessageString);
        }
        long duration = System.currentTimeMillis() - start;
        long durationHistory = System.currentTimeMillis() - historyStart;
        log.info("Session {} send time details: normal: {}, history: {}",
                 message.getSessionId(),
                 duration,
                 durationHistory);
    }

    /**
     * Wrapper method for the SQSClient to send a message.
     *
     * @param body  message body to send
     * @param queue queue to send the message to
     */
    private void sendTo(final String body, final Queue queue) {
        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                                                                  .queueUrl(account + queue.name())
                                                                  .messageBody(body)
                                                                  .build();
        sqsClient.sendMessage(sendMessageRequest);
    }

    /**
     * Maps a {@link RoutingMessage} to its JSON string.
     *
     * @param message message to map
     * @return JSON string created from message
     */
    private String messageAsString(final RoutingMessage message) {
        try {
            return new ObjectMapper().writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Invalid Message format: " + message.toString());
        }
    }

}
