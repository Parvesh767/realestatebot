package com.risingbee.realestate.automation.domain;


import com.risingbee.realestate.enums.BrokerOnboardingStep;
import com.risingbee.realestate.enums.BrokerStatus;
import com.risingbee.realestate.enums.ConversationState;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;



@Entity
@Table(name = "broker")
@Getter 
public class Broker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String phone;

    @Enumerated(EnumType.STRING)
    private BrokerStatus status;

    @Enumerated(EnumType.STRING)
    private BrokerOnboardingStep onboardingStep;

    // --------------------
    // Preferences (TEMP)
    // --------------------
    // These can later move fully into BrokerPreference
    private Integer minBudget;
    private Integer maxBudget;

//    @Enumerated(EnumType.STRING)
//    private ConversationState conversationState = ConversationState.NEW;

    // JPA requirement
    protected Broker() {}
    public Broker(String phone) {
    	this.phone = phone ;
    }

    // Controlled mutations (optional but recommended)
    public void advanceOnboarding(BrokerOnboardingStep step) {
        this.onboardingStep = step;
    }

    public void activate() {
        this.status = BrokerStatus.ACTIVE;
    }
    
    public void resetOnboarding() {
        this.onboardingStep = BrokerOnboardingStep.START;
        this.status = BrokerStatus.ONBOARDING;
    }

    public void startOnboarding() {
        this.status = BrokerStatus.ONBOARDING;
        this.onboardingStep = BrokerOnboardingStep.START;
    }

    
}

