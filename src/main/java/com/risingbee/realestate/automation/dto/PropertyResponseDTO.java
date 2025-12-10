package com.risingbee.realestate.automation.dto;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PropertyResponseDTO {

    private Long id;
    private String title;
    private String area;
    private Integer price;
    private String bhk;
    private String description;
    private String mapLink;
    private String photosCsv;
    private boolean active;
}

