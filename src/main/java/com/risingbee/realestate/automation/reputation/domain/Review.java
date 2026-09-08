package com.risingbee.realestate.automation.reputation.domain;

import com.risingbee.realestate.automation.reputation.enums.ReviewStatus;
import com.risingbee.realestate.automation.reputation.enums.TargetType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "reputation_reviews", indexes = {
    @Index(name = "idx_reviews_target", columnList = "targetType, targetId"),
    @Index(name = "idx_reviews_contract", columnList = "tenancyContractId"),
    @Index(name = "idx_reviews_author", columnList = "authorAccountId")
})
@Getter
@Setter
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tenancyContractId; // Links counterparty reviews for double-blind reveal

    @Column(nullable = false)
    private Long authorAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetType targetType;

    @Column(nullable = false)
    private Long targetId; // ID of the Tenant Account, Owner Account, or Property

    /* ---------------- Sub-aspect ratings (1 to 5 Stars) ---------------- */
    @Column(nullable = false)
    private Integer overallRating; // 1 to 5

    private Integer aspect1Rating; // Tenant: Rent Promptness | Property: Ventilation | Owner: Deposit Refund
    private Integer aspect2Rating; // Tenant: Upkeep & Cleanliness | Property: Maintenance | Owner: Repair Response
    private Integer aspect3Rating; // Tenant: Communication & Rules | Property: Water/Power | Broker: Transparency

    @Column(columnDefinition = "text")
    private String feedbackText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus status = ReviewStatus.BLIND_HELD;

    private Instant publishedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public void publish() {
        this.status = ReviewStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }
}