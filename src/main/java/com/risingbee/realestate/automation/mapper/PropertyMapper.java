package com.risingbee.realestate.automation.mapper;

import com.risingbee.realestate.automation.domain.Property;
import com.risingbee.realestate.automation.dto.PropertyRequestDTO;
import com.risingbee.realestate.automation.dto.PropertyResponseDTO;

public class PropertyMapper {

    /**
     * Create a Property entity from request DTO.
     * Ownership (ownerAccountId) must be supplied explicitly.
     */
    public static Property toEntity(
            PropertyRequestDTO dto,
            Long ownerAccountId
    ) {
        // 🔑 Safe conversion from Integer/Double/Number to Long
        Long priceLong = dto.getPrice() != null ? dto.getPrice().longValue() : null;

        Property property = new Property(
                ownerAccountId,
                resolveTitle(dto),
                dto.getBhk(),
                priceLong,
                dto.getCityCode(),
                dto.getLocalityCode()
        );

        if (dto.getDescription() != null || dto.getMapLink() != null) {
            property.updateDetails(
                    property.getTitle(),
                    property.getPrice(),
                    dto.getDescription(),
                    dto.getMapLink()
            );
        }

        if (dto.getPhotos() != null && !dto.getPhotos().isEmpty()) {
            property.addPhotos(dto.getPhotos());
        }

        if (dto.getActive() != null && !dto.getActive()) {
            property.deactivate();
        }

        return property;
    }

    private static String resolveTitle(PropertyRequestDTO dto) {

        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            return dto.getTitle();
        }

        if (dto.getBhk() != null && dto.getLocalityCode() != null) {
            return dto.getBhk() + " in " + dto.getLocalityCode();
        }

        if (dto.getBhk() != null) {
            return dto.getBhk() + " Property";
        }

        return "Property Listing";
    }

    public static PropertyResponseDTO toDTO(Property p) {

        return PropertyResponseDTO.builder()
                .id(p.getId())
                .title(p.getTitle())
                .bhk(p.getBhk())
                .cityCode(p.getCityCode())
                .localityCode(p.getLocalityCode())
                .displayLocation(
                        p.getLocalityCode() != null
                                ? p.getLocalityCode()
                                : p.getCityCode()
                )
                .price(p.getPrice())
                .description(p.getDescription())
                .mapLink(p.getMapLink())
                .photos(p.getPhotos())
                .active(p.isActive())
                .build();
    }

    /**
     * Update an existing Property from DTO.
     * Caller is responsible for ownership + authorization checks.
     */
    public static void updateEntity(
            Property property,
            PropertyRequestDTO dto
    ) {

        if (
                dto.getTitle() != null ||
                dto.getPrice() != null ||
                dto.getDescription() != null ||
                dto.getMapLink() != null
        ) {
            // 🔑 Safe conversion from Integer/Double/Number to Long
            Long priceLong = dto.getPrice() != null ? dto.getPrice().longValue() : property.getPrice();

            property.updateDetails(
                    dto.getTitle() != null ? dto.getTitle() : property.getTitle(),
                    priceLong,
                    dto.getDescription(),
                    dto.getMapLink()
            );
        }

        if (dto.getCityCode() != null) {
            property.updateLocation(
                    dto.getCityCode(),
                    dto.getLocalityCode()
            );
        }

        if (dto.getActive() != null && !dto.getActive()) {
            property.deactivate();
        }
    }
}