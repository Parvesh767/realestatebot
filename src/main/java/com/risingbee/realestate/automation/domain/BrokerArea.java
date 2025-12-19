package com.risingbee.realestate.automation.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "broker_areas")
@Getter @Setter 
public class BrokerArea {

    @Id
    @GeneratedValue
    private Long id;

    private Long brokerId;
    private String area;
}
