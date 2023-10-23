package com.carupdateprovider.process.model.persistence;

import java.sql.Timestamp;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * JPA mapped object of status history.
 */
@ConditionalOnExpression("${application.history} or ${application.ext-request-proxy}")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "status")
public class StatusEntity {

    @Id
    @Column(name = "uuid")
    private UUID uuid;

    @Column(name = "vin", columnDefinition = "bpchar")
    private String vin;

    @Column(name = "requested")
    private Timestamp requested;

    @Column(name = "triggered")
    private Timestamp triggered;

    @Column(name = "fetched")
    private Timestamp fetched;

    @Column(name = "unfetchable")
    private Timestamp unfetchable;

    @Column(name = "created")
    private Timestamp created;

    @Column(name = "rolled_out")
    private Timestamp rolled_out;

    @Column(name = "last_update")
    private Timestamp last_update;
}
