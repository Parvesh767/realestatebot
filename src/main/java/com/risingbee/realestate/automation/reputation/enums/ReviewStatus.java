package com.risingbee.realestate.automation.reputation.enums;

public enum ReviewStatus {
    BLIND_HELD,      // Submitted but hidden until counterparty submits or timeout
    PUBLISHED,       // Live & contributing to public score calculation
    FLAGGED_DISPUTE  // Quarantined due to abusive terms or dispute
}