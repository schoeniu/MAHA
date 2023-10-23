package com.carupdateprovider.process.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * SQS related configuration.
 */
@Configuration
public class SQSConfig {

    @Value("${aws.sqs.client.region}")
    private String region;

    @Value("${aws.sqs.client.accessKey}")
    private String accessKey;

    @Value("${aws.sqs.client.secretKey}")
    private String secretKey;

    @Value("${aws.sqs.client.endpoint}")
    private String endpoint;

    /**
     * SqsClient bean which allows for interaction with the AWS SDK.
     *
     * @return SqsClient
     */
    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
                        .region(Region.of(region))
                        .endpointOverride(URI.create(endpoint))
                        .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey,
                                                                                                         secretKey)))
                        .build();
    }

}
