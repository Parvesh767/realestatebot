package com.risingbee.realestate.automation.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PropertyRequestDTO {

    @NotBlank
    private String title;

    @NotBlank
    private String area;

    @NotNull
    private Integer price;

    @NotBlank
    private String bhk;

    private String description;
    private String mapLink;

    // comma-separated photo URLs for now
    private String photosCsv;
}

