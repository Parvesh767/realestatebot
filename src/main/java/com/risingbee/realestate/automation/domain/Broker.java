package com.risingbee.realestate.automation.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import com.risingbee.realestate.enums.*;
import com.risingbee.realestate.automation.actor.*;


@Entity
@Table(name = "broker")
@Getter
public class Broker {

    @Id
    private Long id; // SAME as Account ID

    private String phone;

    @Enumerated(EnumType.STRING)
    private BrokerStatus status;

    protected Broker() {}

    public Broker(Long accountId, String phone) {
        this.id = accountId;
        this.phone = phone;
        this.status = BrokerStatus.ACTIVE;
    }

    public static Broker createFromActor(Actor actor) {
        if (actor.internalId() == null) {
            throw new IllegalStateException("Actor has no account identity");
        }
        return new Broker(actor.internalId(), actor.externalId());
    }

    public void activate() {
        this.status = BrokerStatus.ACTIVE;
    }

    public void suspend() {
        this.status = BrokerStatus.SUSPENDED;
    }
}
