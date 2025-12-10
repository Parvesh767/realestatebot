package com.risingbee.realestate.automation.mapper;

import com.risingbee.realestate.automation.domain.Property;
import com.risingbee.realestate.automation.dto.PropertyRequestDTO;
import com.risingbee.realestate.automation.dto.PropertyResponseDTO;

public class PropertyMapper {

    public static Property toEntity(PropertyRequestDTO dto) {
        return Property.builder()
                .title(dto.getTitle())
                .area(dto.getArea())
                .price(dto.getPrice())
                .bhk(dto.getBhk())
                .description(dto.getDescription())
                .mapLink(dto.getMapLink())
                .photosJson(dto.getPhotosCsv())
                .active(true)
                .build();
    }

    public static PropertyResponseDTO toDTO(Property p) {
        return PropertyResponseDTO.builder()
                .id(p.getId())
                .title(p.getTitle())
                .area(p.getArea())
                .price(p.getPrice())
                .bhk(p.getBhk())
                .description(p.getDescription())
                .mapLink(p.getMapLink())
                .photosCsv(p.getPhotosJson())
                .active(p.isActive())
                .build();
    }
}

