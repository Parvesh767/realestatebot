package com.risingbee.realestate.automation.dto;

import java.time.Instant;

/**
 * DTO for delivering lead details over REST APIs.
 * Supports masking phone numbers for locked leads.
 */
public record LeadResponseDTO(
    Long id,
    String phoneNumber,
    String bhk,
    Long minBudget,
    Long maxBudget,
    String cityCode,
    String localityCode,
    Boolean unlocked,
    Instant createdAt
) {}