package com.schoeniu.maha.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

/**
 * SQS Api
 */
@RequiredArgsConstructor
@Service
public class SqsApi {

    @Value("${aws.sqs.client.account}")
    private String account;

    private final SqsClient sqsClient;

    /**
     * Gets the approximate current number of messages in a queue.
     *
     * @param queueName name of queue to get the number of messages from
     * @return number of messages
     */
    public int getNumberOfMessages(final String queueName) {
        final GetQueueAttributesRequest request = GetQueueAttributesRequest.builder()
                                                                           .queueUrl(account + queueName)
                                                                           .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)
                                                                           .build();
        final GetQueueAttributesResponse response = sqsClient.getQueueAttributes(request);
        return Integer.parseInt(response.attributes()
                                        .get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES));
    }

}
