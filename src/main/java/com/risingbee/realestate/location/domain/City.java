package com.risingbee.realestate.location.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "cities",
    uniqueConstraints = @UniqueConstraint(columnNames = "code")
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Canonical code used everywhere (GURGAON, NOIDA, DELHI)
    @Column(nullable = false, length = 32)
    private String code;

    // Human readable
    @Column(nullable = false)
    private String name;
}
