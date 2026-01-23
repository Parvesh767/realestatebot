package com.risingbee.realestate.location.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "localities",
    indexes = {
        @Index(name = "idx_locality_city", columnList = "city_id")
    }
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Locality {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Canonical name (Sector 56, Golf Course Road)
    @Column(nullable = false)
    private String name;

    // Normalized lookup key (sector 56, golf course road)
    @Column(nullable = false)
    private String normalizedName;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;
    
    @Column
    private String  code;
}
