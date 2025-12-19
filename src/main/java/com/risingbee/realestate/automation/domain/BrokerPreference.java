package com.risingbee.realestate.automation.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "broker_preferences")
@Getter @Setter
public class BrokerPreference {

    @Id
    private Long brokerId;

    private Integer minBudget;
    private Integer maxBudget;
}
