package com.risingbee.realestate.automation.actor.domain;

import com.risingbee.realestate.automation.actor.enums.ActorType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
    name = "accounts",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "phone")
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActorType type; // USER / OWNER / BROKER

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 100)
    private String name;

    // 🔮 Future-ready, but not forced now
    // @Enumerated(EnumType.STRING)
    // private OnboardingStatus onboardingStatus;

    public Account(ActorType type, String phone) {
        this.type = type;
        this.phone = phone;
    }
}
