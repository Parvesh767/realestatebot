package com.risingbee.realestate.leads.domain;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "inquiry_sessions")
@Getter
public class InquirySession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String searcherPhone;

    private String rawMessage;

    private String bhk;

    // 🔑 Upgraded from Integer to Long to align with Property and Lead entities
    private Long minBudget;

    private Long maxBudget;

    private String cityCode;

    private String localityCode;

    private Long matchedPropertyId;

    private Long ownerAccountId;

    private Instant createdAt;

    private Boolean converted;

    protected InquirySession() {}

    public InquirySession(
        String searcherPhone,
        String rawMessage,
        String bhk,
        Long minBudget,
        Long maxBudget,
        String cityCode,
        String localityCode,
        Long matchedPropertyId,
        Long ownerAccountId
    ) {
        this.searcherPhone = searcherPhone;
        this.rawMessage = rawMessage;
        this.bhk = bhk;
        this.minBudget = minBudget;
        this.maxBudget = maxBudget;
        this.cityCode = cityCode;
        this.localityCode = localityCode;
        this.matchedPropertyId = matchedPropertyId;
        this.ownerAccountId = ownerAccountId;
        this.createdAt = Instant.now();
        this.converted = false;
    }

    public void markConverted() {
        this.converted = true;
    }
}