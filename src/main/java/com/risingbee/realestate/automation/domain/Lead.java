package com.risingbee.realestate.automation.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.risingbee.realestate.leads.enums.LeadSource;
import com.risingbee.realestate.leads.enums.LeadStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "leads")
@Getter
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String phoneNumber;
    private String name;

    // 🔑 Upgraded to Long to prevent Integer overflow on Lakhs/Crores
    private Long minBudget;
    private Long maxBudget;
    private String bhk;

    private String cityCode;
    private String localityCode;

    @Column(columnDefinition = "text")
    private String rawMessage;

    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    private LeadStatus status;

    private Instant nextFollowUpAt;

    @Column(length = 2000)
    private String notes;

    private Boolean archived = false;

    private Instant updatedAt;

    @Enumerated(EnumType.STRING)
    private LeadSource source;

    private Boolean qualified;

    @Column(name = "owner_account_id", nullable = false)
    private Long ownerAccountId;

    /* ====================================
       💰 DAY 4 MONETIZATION & PAYWALL
       ==================================== */
    @Column(nullable = false)
    private Boolean unlocked = false;

    private Instant unlockedAt;

    /* ====================================
       MESSAGE ID TRACKING
       ==================================== */
    @Column(name = "confirmed_message_ids_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> confirmedMessageIds = new ArrayList<>();

    protected Lead() {}

    public Lead(
        String phoneNumber,
        Long ownerAccountId,
        String bhk,
        Long minBudget,
        Long maxBudget,
        String cityCode,
        String localityCode,
        String rawMessage
    ) {
        this.status = LeadStatus.NEW;
        this.phoneNumber = phoneNumber;
        this.ownerAccountId = ownerAccountId;
        this.bhk = bhk;
        this.minBudget = minBudget;
        this.maxBudget = maxBudget;
        this.cityCode = cityCode;
        this.localityCode = localityCode;
        this.rawMessage = rawMessage;
        this.qualified = true;
        this.unlocked = false; // Default: Phone number masked until unlocked
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /* ------------------------------------
       DOMAIN MUTATIONS & MONETIZATION
       ------------------------------------ */

    public void unlock() {
        this.unlocked = true;
        this.unlockedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public boolean markYesProcessed(String messageId) {
        if (confirmedMessageIds.contains(messageId)) {
            return false;
        }
        confirmedMessageIds.add(messageId);
        return true;
    }

    public void updateStatus(LeadStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    /**
     * Safe status updater from frontend string values.
     */
    public void updateStatusFromString(String statusStr) {
        if (statusStr == null || statusStr.isBlank()) {
            return;
        }
        String cleanStatus = statusStr.trim().toUpperCase();
        try {
            this.status = LeadStatus.valueOf(cleanStatus);
            this.updatedAt = Instant.now();
        } catch (IllegalArgumentException e) {
            // Flexible match for composite statuses (e.g. SITE_VISIT -> SITE_VISIT_SCHEDULED)
            for (LeadStatus s : LeadStatus.values()) {
                if (s.name().startsWith(cleanStatus) || cleanStatus.startsWith(s.name())) {
                    this.status = s;
                    this.updatedAt = Instant.now();
                    return;
                }
            }
            throw new IllegalArgumentException("Invalid LeadStatus value: " + statusStr);
        }
    }

    public void scheduleFollowUp(Instant followUpAt) {
        this.nextFollowUpAt = followUpAt;
        this.updatedAt = Instant.now();
    }

    public void updateNotes(String notes) {
        this.notes = notes;
        this.updatedAt = Instant.now();
    }
}