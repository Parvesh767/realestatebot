package com.risingbee.realestate.automation.domain;


import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "brokers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Broker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String phone;
    private String companyName;

    @Column(unique = true, nullable = false)
    private String apiKey; // random UUID token for webhook mapping

    private Instant createdAt;
}

