package com.risingbee.realestate.automation.dto;

import java.time.Instant;
import java.util.List;
import com.risingbee.realestate.automation.domain.enums.FurnishingType;
import com.risingbee.realestate.automation.domain.enums.PropertyType;

public record PropertyResponseDTO(
    Long id,
    Long ownerAccountId,
    String title,
    String bhk,
    Long price,
    Long securityDeposit,
    Integer floorNumber,
    PropertyType propertyType,
    FurnishingType furnishing,
    String cityCode,
    String localityCode,
    String displayLocation,
    String description,
    String mapLink,
    String videoTourUrl,
    List<String> photos,
    List<String> amenities,
    boolean active,
    Instant createdAt
) {}