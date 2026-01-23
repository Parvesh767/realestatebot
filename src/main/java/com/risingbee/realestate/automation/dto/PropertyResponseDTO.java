package com.risingbee.realestate.automation.dto;


import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyResponseDTO {

    private Long id;
    private String title;
    private String bhk;

    /** Canonical location (for filters / logic) */
    private String cityCode;
    private String localityCode;

    /** Optional display string (NOT used for logic) */
    private String displayLocation;

    private Integer price;
    private String description;
    private String mapLink;

    private List<String> photos;
    private boolean active;
}


