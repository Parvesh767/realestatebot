package com.risingbee.realestate.automation.service_hub.domain;

import com.risingbee.realestate.automation.service_hub.enums.ServiceCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "service_catalog")
@Getter
@Setter
@NoArgsConstructor
public class ServiceCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceCategory category;

    @Column(nullable = false)
    private String title; // e.g. "Switchboard & Wiring Repair"

    private String description;

    @Column(nullable = false)
    private Integer creditCost; // e.g. 4 Credits

    @Column(nullable = false)
    private Long fiatPriceInr; // e.g. ₹299

    private Integer estimatedDurationMins = 60;

    private boolean active = true;
    
    @Column(nullable = false)
    private Long vendorPayoutInr = 180L; // Platform payout to contractor
}