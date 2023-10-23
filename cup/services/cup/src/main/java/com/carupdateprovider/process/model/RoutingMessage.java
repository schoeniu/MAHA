package com.carupdateprovider.process.model;

import java.util.Date;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RoutingMessage sent through SQS between services.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoutingMessage {

    private UUID sessionId;
    private String vin;
    private int processingTime;
    private Queue queue;
    private Queue futureTarget;
    private Status status;
    private Date time;

    /**
     * Utility function to continue a session by providing a RoutingMessageBuilder with the same session
     * and VIN as the RoutingMessage instance the method is called on.
     *
     * @return RoutingMessageBuilder
     */
    public RoutingMessageBuilder continueSession() {
        return RoutingMessage.builder()
                             .sessionId(this.sessionId)
                             .vin(this.vin);
    }
}
