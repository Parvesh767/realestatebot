package com.risingbee.realestate.automation.mapper;

import com.risingbee.realestate.automation.domain.Property;
import com.risingbee.realestate.automation.dto.PropertyRequestDTO;
import com.risingbee.realestate.automation.dto.PropertyResponseDTO;

public class PropertyMapper {

    public static Property toEntity(PropertyRequestDTO dto, Long ownerAccountId) { //
        Long priceLong = dto.getPrice() != null ? dto.getPrice().longValue() : 0L;
        Long depositLong = dto.getSecurityDeposit() != null ? dto.getSecurityDeposit().longValue() : null;

        Property property = new Property(
                ownerAccountId,
                resolveTitle(dto),
                dto.getBhk(),
                priceLong,
                dto.getCityCode(),
                dto.getLocalityCode(),
                dto.getPropertyType(),
                dto.getFurnishing()
        );

        property.updateDetails(
                property.getTitle(),
                property.getPrice(),
                dto.getDescription(),
                dto.getMapLink(),
                dto.getVideoTourUrl(),
                depositLong,
                dto.getFloorNumber(),
                dto.getFurnishing(),
                dto.getPropertyType()
        );

        if (dto.getAmenities() != null && !dto.getAmenities().isEmpty()) {
            property.updateAmenities(dto.getAmenities());
        }

        if (dto.getPhotos() != null && !dto.getPhotos().isEmpty()) {
            property.addPhotos(dto.getPhotos());
        }

        if (Boolean.FALSE.equals(dto.getActive())) {
            property.deactivate();
        }

        return property;
    }

    public static PropertyResponseDTO toDTO(Property p) {
        String displayLoc = (p.getLocalityCode() != null && !p.getLocalityCode().isBlank())
                ? p.getLocalityCode()
                : p.getCityCode();

        return new PropertyResponseDTO(
                p.getId(),
                p.getOwnerAccountId(),
                p.getTitle(),
                p.getBhk(),
                p.getPrice(),
                p.getSecurityDeposit(),
                p.getFloorNumber(),
                p.getPropertyType(),
                p.getFurnishing(),
                p.getCityCode(),
                p.getLocalityCode(),
                displayLoc,
                p.getDescription(),
                p.getMapLink(),
                p.getVideoTourUrl(),
                p.getPhotos(),
                p.getAmenities(),
                p.isActive(),
                p.getCreatedAt()
        );
    }

    public static PropertyRequestDTO toRequestDTO(PropertyResponseDTO res) {
        PropertyRequestDTO dto = new PropertyRequestDTO();
        dto.setTitle(res.title());
        dto.setBhk(res.bhk());
        dto.setPrice(res.price());
        dto.setSecurityDeposit(res.securityDeposit());
        dto.setFloorNumber(res.floorNumber());
        dto.setPropertyType(res.propertyType());
        dto.setFurnishing(res.furnishing());
        dto.setCityCode(res.cityCode());
        dto.setLocalityCode(res.localityCode());
        dto.setArea(res.localityCode());
        dto.setDescription(res.description());
        dto.setMapLink(res.mapLink());
        dto.setVideoTourUrl(res.videoTourUrl());
        dto.setAmenities(res.amenities());
        dto.setPhotos(res.photos());
        dto.setActive(res.active());
        return dto;
    }

    public static void updateEntity(Property property, PropertyRequestDTO dto) {
        Long priceLong = dto.getPrice() != null ? dto.getPrice().longValue() : property.getPrice();
        Long depositLong = dto.getSecurityDeposit() != null ? dto.getSecurityDeposit().longValue() : property.getSecurityDeposit();

        property.updateDetails(
                dto.getTitle() != null ? dto.getTitle() : property.getTitle(),
                priceLong,
                dto.getDescription() != null ? dto.getDescription() : property.getDescription(),
                dto.getMapLink() != null ? dto.getMapLink() : property.getMapLink(),
                dto.getVideoTourUrl() != null ? dto.getVideoTourUrl() : property.getVideoTourUrl(),
                depositLong,
                dto.getFloorNumber() != null ? dto.getFloorNumber() : property.getFloorNumber(),
                dto.getFurnishing() != null ? dto.getFurnishing() : property.getFurnishing(),
                dto.getPropertyType() != null ? dto.getPropertyType() : property.getPropertyType()
        );

        if (dto.getAmenities() != null) {
            property.updateAmenities(dto.getAmenities());
        }

        if (dto.getCityCode() != null) {
            property.updateLocation(dto.getCityCode(), dto.getLocalityCode());
        }

        if (dto.getActive() != null) {
            if (dto.getActive()) {
                property.activate();
            } else {
                property.deactivate();
            }
        }
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
}