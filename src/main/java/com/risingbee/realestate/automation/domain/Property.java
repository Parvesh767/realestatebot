package com.risingbee.realestate.automation.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "properties")
@Getter
@Access(AccessType.FIELD)
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fingerprint", unique = true)
    private String fingerprint;

    /* ======================
       Ownership (actor-centric)
       ====================== */

    @Column(name = "owner_account_id", nullable = false, updatable = false)
    private Long ownerAccountId;

    /* ======================
       Property details
       ====================== */

    private String title;
    private String bhk;
    private Long price; // 🔑 Upgraded to Long for INR Crore/Lakh handling

    @Column(columnDefinition = "text")
    private String description;

    private String mapLink;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> photos = new ArrayList<>();

    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /* ======================
       Location (canonical truth)
       ====================== */

    @Column(length = 32, nullable = false)
    private String cityCode;

    @Column(length = 128)
    private String localityCode;

    protected Property() {}

    public Property(
            Long ownerAccountId,
            String title,
            String bhk,
            Long price,
            String cityCode,
            String localityCode
    ) {
        this.ownerAccountId = ownerAccountId;
        this.title = title;
        this.bhk = bhk;
        this.price = price;
        this.cityCode = cityCode;
        this.localityCode = localityCode;
        this.createdAt = Instant.now();
        this.active = true;
    }

    public void updateDetails(
            String title,
            Long price,
            String description,
            String mapLink
    ) {
        if (title != null) this.title = title;
        if (price != null) this.price = price;
        if (description != null) this.description = description;
        if (mapLink != null) this.mapLink = mapLink;
    }

    public void updateLocation(
            String cityCode,
            String localityCode
    ) {
        this.cityCode = cityCode;
        this.localityCode = localityCode;
    }

    public void addPhotos(List<String> newPhotos) {
        if (newPhotos != null) {
            this.photos.addAll(newPhotos);
        }
    }

    public void removePhotos(List<String> removePhotos) {
        if (removePhotos != null) {
            this.photos.removeAll(removePhotos);
        }
    }

    public void deactivate() {
        this.active = false;
    }
}