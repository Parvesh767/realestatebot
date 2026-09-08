package com.risingbee.realestate.automation.tenancy.domain;

import com.risingbee.realestate.automation.tenancy.enums.InspectionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tenancy_inspections")
@Getter
@Setter
@NoArgsConstructor
public class InspectionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenancy_contract_id", nullable = false)
    private TenancyContract tenancyContract;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InspectionType type; // MOVE_IN or MOVE_OUT

    private Double electricityMeterReading;
    private String meterPhotoUrl;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> roomPhotos = new ArrayList<>();

    @Column(columnDefinition = "text")
    private String notes;

    @Column(nullable = false)
    private Long recordedByAccountId;

    private boolean acknowledgedByOtherParty = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}