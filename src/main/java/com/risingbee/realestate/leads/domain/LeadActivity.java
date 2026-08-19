package com.risingbee.realestate.leads.domain;

import java.time.Instant;

import com.risingbee.realestate.leads.enums.ActivityType;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;


@Entity
@Table(name = "lead_activities")
@Getter
public class LeadActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long leadId;

    private Long accountId;

    @Enumerated(EnumType.STRING)
    private ActivityType type;

    private String message;

    private Instant createdAt;

    protected LeadActivity() {}

    public LeadActivity(
        Long leadId,
        Long accountId,
        ActivityType type,
        String message
    ) {
        this.leadId = leadId;
        this.accountId = accountId;
        this.type = type;
        this.message = message;
        this.createdAt = Instant.now();
    }
}
