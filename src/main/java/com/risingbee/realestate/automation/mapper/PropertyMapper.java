package com.risingbee.realestate.automation.mapper;

import com.risingbee.realestate.automation.domain.Broker;
import com.risingbee.realestate.automation.domain.Property;
import com.risingbee.realestate.automation.dto.PropertyRequestDTO;
import com.risingbee.realestate.automation.dto.PropertyResponseDTO;

public class PropertyMapper {

	public static Property toEntity(PropertyRequestDTO dto, Broker broker) {

	    String title = resolveTitle(dto);

	    Property p = Property.builder()
	            .broker(broker)
	            .title(title)
	            .bhk(dto.getBhk())
	            .area(dto.getArea())
	            .city(dto.getCity())
	            .price(dto.getPrice())
	            .description(dto.getDescription())
	            .mapLink(dto.getMapLink())
	            .active(dto.getActive() != null ? dto.getActive() : true)
	            .build();

	    // ✅ apply logic AFTER build
	    if (dto.getPhotos() != null) {
	        p.setPhotos(dto.getPhotos());
	    }

	    return p;
	}

    private static String resolveTitle(PropertyRequestDTO dto) {
        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            return dto.getTitle();
        }
        if (dto.getBhk() != null && dto.getArea() != null) {
            return dto.getBhk() + " in " + dto.getArea();
        }
        if (dto.getBhk() != null) {
            return dto.getBhk();
        }
        return "Property Listing";
    }

    public static PropertyResponseDTO toDTO(Property p) {
        return PropertyResponseDTO.builder()
                .id(p.getId())
                .title(p.getTitle())
                .bhk(p.getBhk())
                .area(p.getArea())
                .city(p.getCity())
                .price(p.getPrice())
                .description(p.getDescription())
                .mapLink(p.getMapLink())
                .photos(p.getPhotos())
                .active(p.isActive())
                .build();
    }
}



