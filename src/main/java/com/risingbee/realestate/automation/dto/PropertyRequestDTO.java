package com.risingbee.realestate.automation.dto;

import java.util.List;
import com.risingbee.realestate.automation.domain.enums.FurnishingType;
import com.risingbee.realestate.automation.domain.enums.PropertyType;
import lombok.Data;

@Data
public class PropertyRequestDTO {
    private String title;
    private String bhk;
    private Long price;
    private Long securityDeposit;
    private Integer floorNumber;
    private Integer totalFloors;
    private PropertyType propertyType;
    private FurnishingType furnishing;
    private String area; // Raw input passed to LocationResolver
    private String cityCode;
    private String localityCode;
    private String description;
    private String mapLink;
    private String videoTourUrl;
    private List<String> amenities;
    private List<String> photos; // Existing URLs / paths
    private Boolean active = true;
}