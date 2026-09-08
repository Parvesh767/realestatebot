package com.risingbee.realestate.automation.domain;

import com.risingbee.realestate.automation.domain.enums.FurnishingType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "property_units")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PropertyUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private Long propertyId; 

    @Column(name = "unit_name", nullable = false)
    private String unitName; 

    @Column(name = "floor_number", nullable = false)
    private String floorNumber; 
    
    @Column(name = "unit_number")
    private String unitNumber; 

    @Column(name = "rent_inr", nullable = false)
    private Long rentInr;

    @Column(name = "security_deposit_inr")
    private Long securityDepositInr;

    @Column(name = "status", nullable = false)
    private String status = "VACANT"; 

    // --- NEW FIELDS FOR INDEPENDENT UNIT DETAILS ---

    @Enumerated(EnumType.STRING)
    private FurnishingType furnishing;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> amenities = new ArrayList<>();

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> photos = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();
}