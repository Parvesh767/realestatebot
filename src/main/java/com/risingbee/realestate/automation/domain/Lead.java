package com.risingbee.realestate.automation.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "leads")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String phoneNumber;
    private String name;

    private Integer minBudget;
    private Integer maxBudget;

    private String location;
    private String bhk;

    private String rawMessage;

    private Instant createdAt;
    
    @ManyToOne
    @JoinColumn(name = "broker_id", nullable = false)
    private Broker broker;
}
