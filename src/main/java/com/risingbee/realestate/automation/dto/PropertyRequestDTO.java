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
public class PropertyRequestDTO {

    // -------------------
    // Display / optional
    // -------------------
    private String title;
    private String bhk;

    /**
     * Raw user input (deprecated for persistence)
     * Used only for resolution or display
     */
    @Deprecated
    private String area;

    @Deprecated
    private String city;

    // -------------------
    // Canonical location (USED FOR LOGIC)
    // -------------------
    private String cityCode;
    private String localityCode;

    // -------------------
    // Other fields
    // -------------------
    private Integer price;
    private String description;
    private String mapLink;
    private List<String> photos;

    // Boolean wrapper (important)
    private Boolean active;

    // ---------- helpers (optional but recommended)

    public boolean hasResolvedLocation() {
        return cityCode != null;
    }
}

