package com.risingbee.realestate.automation.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "broker_upi_handles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BrokerUpiHandle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "upi_id", nullable = false)
    private String upiId;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}