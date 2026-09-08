package com.risingbee.realestate.automation.domain;

import com.risingbee.realestate.automation.domain.enums.FurnishingType;
import com.risingbee.realestate.automation.domain.enums.PropertyType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "properties")
@Getter
@Setter
@Access(AccessType.FIELD)
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ownerAccountId;

    // Assigned broker details for direct alerts and credit matching
    private Long assignedBrokerAccountId;
    private String assignedBrokerPhone;

    private String title;
    private String bhk;

    @Column(nullable = false)
    private Long price;

    private Long securityDeposit;
    
    @Column(name = "floor_number")
    private Integer floorNumber;
    private Integer totalFloors;

    // Default Utility Rates for Sub-meter Rent Generation
    private Double ratePerUnit = 8.5;
    private Long fixedMaintenance = 0L;
    private Long fixedWater = 0L;

    @Enumerated(EnumType.STRING)
    private PropertyType propertyType = PropertyType.APARTMENT;

    @Enumerated(EnumType.STRING)
    private FurnishingType furnishing = FurnishingType.SEMI_FURNISHED;

    @Column(columnDefinition = "text")
    private String description;

    private String mapLink;
    private String videoTourUrl;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> photos = new ArrayList<>();

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> amenities = new ArrayList<>();

    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(length = 32, nullable = false)
    private String cityCode;

    @Column(length = 128)
    private String localityCode;

    /* ----------------- JPA Requirement ----------------- */
    public Property() {}

    /* ----------------- Canonical Constructor ----------------- */
    public Property(
            Long ownerAccountId,
            String title,
            String bhk,
            Long price,
            String cityCode,
            String localityCode,
            PropertyType propertyType,
            FurnishingType furnishing
    ) {
        this.ownerAccountId = ownerAccountId;
        this.title = title;
        this.bhk = bhk;
        this.price = (price != null) ? price : 0L;
        this.cityCode = cityCode;
        this.localityCode = localityCode;
        this.propertyType = (propertyType != null) ? propertyType : PropertyType.APARTMENT;
        this.furnishing = (furnishing != null) ? furnishing : FurnishingType.SEMI_FURNISHED;
        this.createdAt = Instant.now();
        this.active = true;
    }

    // Helper alias for locality compatibility
    public String getArea() {
        return this.localityCode;
    }

    public void setArea(String area) {
        this.localityCode = area;
    }

    public void updateLocation(String cityCode, String localityCode) {
        this.cityCode = cityCode;
        this.localityCode = localityCode;
    }

    public void addPhotos(List<String> newPhotos) {
        if (newPhotos != null && !newPhotos.isEmpty()) {
            this.photos.addAll(newPhotos);
        }
    }

    public void removePhotos(List<String> removePhotos) {
        if (removePhotos != null && !removePhotos.isEmpty()) {
            this.photos.removeAll(removePhotos);
        }
    }

    public void updateAmenities(List<String> amenities) {
        this.amenities = (amenities != null) ? new ArrayList<>(amenities) : new ArrayList<>();
    }

    public void updateDetails(
            String title,
            Long price,
            String description,
            String mapLink,
            String videoTourUrl,
            Long securityDeposit,
            Integer floorNumber,
            FurnishingType furnishing,
            PropertyType propertyType
    ) {
        if (title != null) this.title = title;
        if (price != null) this.price = price;
        if (description != null) this.description = description;
        if (mapLink != null) this.mapLink = mapLink;
        if (videoTourUrl != null) this.videoTourUrl = videoTourUrl;
        if (securityDeposit != null) this.securityDeposit = securityDeposit;
        if (floorNumber != null) this.floorNumber = floorNumber;
        if (furnishing != null) this.furnishing = furnishing;
        if (propertyType != null) this.propertyType = propertyType;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }
}