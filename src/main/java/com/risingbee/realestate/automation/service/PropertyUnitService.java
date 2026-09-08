package com.risingbee.realestate.automation.service;

import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.risingbee.realestate.automation.domain.Property;
import com.risingbee.realestate.automation.domain.PropertyUnit;
import com.risingbee.realestate.automation.repo.PropertyRepository;
import com.risingbee.realestate.automation.repo.PropertyUnitRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PropertyUnitService {

    private final PropertyRepository propertyRepository;
    private final PropertyUnitRepository unitRepository;

    @Transactional
    public void generateFloorMatrix(Long propertyId, int totalFloors, long baseRent, long deposit) {
        Property parentProperty = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Parent property not found: " + propertyId));

        String[] floorLabels = {"Ground Floor", "1st Floor", "2nd Floor", "3rd Floor", "4th Floor", "5th Floor", "Penthouse"};

        for (int i = 0; i < totalFloors; i++) {
            String floorName = (i < floorLabels.length) ? floorLabels[i] : (i + "th Floor");

            PropertyUnit unit = new PropertyUnit();
            unit.setPropertyId(parentProperty.getId());
            unit.setUnitName(parentProperty.getTitle() + " — " + floorName);
            unit.setFloorNumber(floorName);
            unit.setRentInr(baseRent);
            unit.setSecurityDepositInr(deposit);
            unit.setStatus("VACANT");

            unitRepository.save(unit);
        }
    }
    
    
    @Transactional
    public void generateAdvancedMatrix(Long propertyId, int totalFloors, int unitsPerFloor, long baseRent, long deposit) {
        Property parentProperty = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Parent property not found"));

        String[] floorLabels = {"Ground", "1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "9th", "10th"};

        for (int f = 0; f < totalFloors; f++) {
            String floorName = (f < floorLabels.length) ? floorLabels[f] + " Floor" : (f + "th Floor");

            for (int u = 1; u <= unitsPerFloor; u++) {
                // Generates unit numbers like "101", "102"... "201", "202"
                String unitCode = String.format("%d%02d", (f + 1), u); 

                PropertyUnit unit = new PropertyUnit();
                unit.setPropertyId(parentProperty.getId());
                unit.setFloorNumber(floorName);
                unit.setUnitNumber("Unit " + unitCode);
                unit.setRentInr(baseRent);
                unit.setSecurityDepositInr(deposit);
                unit.setStatus("VACANT");

                unitRepository.save(unit);
            }
        }
    }
    
    
    @Transactional
    public void generateCustomMatrix(Long propertyId, int totalFloors, String unitsDistribution, long baseRent, long deposit) {
        // 1. Fetch parent to inherit its attributes
        Property parent = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Parent property not found"));

        String[] floorLabels = {"Ground", "1st", "2nd", "3rd", "4th", "5th"};
        String[] distributionArray = (unitsDistribution != null && !unitsDistribution.isBlank()) 
                ? unitsDistribution.split(",") 
                : new String[0];

        for (int f = 0; f < totalFloors; f++) {
            String floorName = (f < floorLabels.length) ? floorLabels[f] + " Floor" : (f + "th Floor");
            
            int unitsOnThisFloor = 1; 
            if (f < distributionArray.length) {
                try { unitsOnThisFloor = Integer.parseInt(distributionArray[f].trim()); } catch (Exception e) {}
            } else if (distributionArray.length > 0) {
                 unitsOnThisFloor = Integer.parseInt(distributionArray[distributionArray.length - 1].trim());
            }

            for (int u = 1; u <= unitsOnThisFloor; u++) {
                String unitCode = String.format("%d%02d", (f + 1), u); 

                PropertyUnit unit = new PropertyUnit();
                unit.setPropertyId(parent.getId());
                unit.setFloorNumber(floorName);
                unit.setUnitNumber("Unit " + unitCode);
                unit.setUnitName(parent.getTitle() + " — " + floorName + " — Unit " + unitCode);
                unit.setRentInr(baseRent);
                unit.setSecurityDepositInr(deposit);
                unit.setStatus("VACANT");

                // 2. AUTO-FILL: Inherit from Parent
                unit.setFurnishing(parent.getFurnishing());
                if (parent.getAmenities() != null) {
                    unit.setAmenities(new ArrayList<>(parent.getAmenities())); 
                }

                unitRepository.save(unit);
            }
        }
    }
    
    
}