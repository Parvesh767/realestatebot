package com.risingbee.realestate.automation.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.domain.enums.FurnishingType;
import com.risingbee.realestate.automation.domain.enums.PropertyType;
import com.risingbee.realestate.automation.dto.PropertyRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyBatchService {

    private final PropertyService propertyService;

    // 1. Generate the Pre-Structured Template
    public String getCsvTemplate() {
        return "title,bhk,price,securityDeposit,cityCode,localityCode,propertyType,furnishing,amenities,photos_urls_comma_separated\n" +
               "Luxury 2BHK,2BHK,25000,50000,GUR,SEC49,APARTMENT,SEMI_FURNISHED,LIFT;PARKING,http://img1.jpg;http://img2.jpg\n";
    }

    // 2. Parse and Process Uploads in Chunks[cite: 1]
    public BatchResult processCsvUpload(Actor actor, MultipartFile file) {
        int successCount = 0;
        List<String> errors = new ArrayList<>();

        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVReader csvReader = new CSVReader(reader)) {

            String[] header = csvReader.readNext(); // Skip header
            String[] row;
            int rowIndex = 2; // Real data starts at row 2

            while ((row = csvReader.readNext()) != null) {
                try {
                    // Map row to DTO
                    PropertyRequestDTO dto = new PropertyRequestDTO();
                    dto.setTitle(row[0]);
                    dto.setBhk(row[1]);
                    dto.setPrice(Long.parseLong(row[2]));
                    dto.setSecurityDeposit(Long.parseLong(row[3]));
                    dto.setCityCode(row[4]);
                    dto.setLocalityCode(row[5]);
                    dto.setPropertyType(PropertyType.valueOf(row[6]));
                    dto.setFurnishing(FurnishingType.valueOf(row[7]));
                    
                    if (row[8] != null && !row[8].isBlank()) {
                        dto.setAmenities(Arrays.asList(row[8].split(";")));
                    }

                    // Save using existing core service
                    propertyService.create(actor, dto, null); 
                    successCount++;
                } catch (Exception e) {
                    // Isolated Error Reporting: Reject only this row, persist the rest[cite: 1]
                    errors.add("Row " + rowIndex + " failed: " + e.getMessage());
                }
                rowIndex++;
            }
        } catch (Exception e) {
            errors.add("Fatal parsing error: " + e.getMessage());
        }

        return new BatchResult(successCount, errors);
    }

    public record BatchResult(int successCount, List<String> errors) {}
}