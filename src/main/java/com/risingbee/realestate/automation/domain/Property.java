package com.risingbee.realestate.automation.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "properties")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;       // "2BHK in Sector 57"
    private Integer price;      // Rent or sale price
    private String bhk;         // "2BHK", "3BHK"

    @Column(length = 2000)
    private String photosJson;  // JSON array of URLs

    @Column(length = 1000)
    private String description;

    private String mapLink;
    private boolean active = true;
    
    
    @ManyToOne
    @JoinColumn(name = "broker_id", nullable = false)
    private Broker broker;
    
    @Column(columnDefinition = "text")
    private String area;
}