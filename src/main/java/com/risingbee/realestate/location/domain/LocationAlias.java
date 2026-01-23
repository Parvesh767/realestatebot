package com.risingbee.realestate.location.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "location_aliases",
    indexes = {
        @Index(name = "idx_alias_value", columnList = "alias")
    }
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class LocationAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "sec 56", "sector-56", "golf"
    @Column(nullable = false)
    private String alias;

    @ManyToOne(optional = false)
    @JoinColumn(name = "locality_id")
    private Locality locality;
}
