package com.risingbee.realestate.automation.reputation.dto;

import com.risingbee.realestate.automation.reputation.enums.TargetType;

public record RatingSummaryDTO(
        TargetType targetType,
        Long targetId,
        Double averageRating,
        Long reviewCount,
        String trustBadge
) {}