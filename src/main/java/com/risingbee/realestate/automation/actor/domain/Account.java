package com.risingbee.realestate.automation.actor.domain;

import java.time.LocalDateTime;

import com.risingbee.realestate.automation.actor.enums.ActorType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
    name = "accounts",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "external_id")
    }
)
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 20)
    private ActorType type;

    @Column(nullable = false, unique = true, length = 100)
    private String externalId;   // phone / whatsapp / email

    @Column(length = 100)
    private String name;
    
    String phone;           // primary identifier (login key)
    String email;           // optional but recommended
    

    boolean isVerified;     // OTP verified or not
    boolean isActive;       // blocked/suspended handling

    LocalDateTime createdAt;
    
    boolean profileCompleted;
    boolean profileStage;

    
 // Inside your Account entity class
    @Column(name = "upi_id")
    private String upiId;

    // Getters and Setters
    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }
    
    
 // Inside Account.java entity
    @Column(name = "credits_balance", nullable = false)
    private Integer creditsBalance = 10; // 10 free credits upon onboarding
    
    
    

    public boolean deductCredit(int amount) {
        if (this.creditsBalance >= amount) {
            this.creditsBalance -= amount;
            return true;
        }
        return false;
    }

  
    
    
 // Inside Account.java:
    public void addCredits(int amount) {
        if (this.creditsBalance == null) {
            this.creditsBalance = 0;
        }
        this.creditsBalance += amount;
    }

    public Account(ActorType type, String externalId) {
        this.type = type;
        this.externalId = externalId;
    }
}

