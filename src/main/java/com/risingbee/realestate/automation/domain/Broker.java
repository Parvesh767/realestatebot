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
import lombok.Setter;



@Entity
@Table(name = "broker")
@Getter @Setter
public class Broker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String phone;

    @Enumerated(EnumType.STRING)
    private BrokerStatus status;

    @Enumerated(EnumType.STRING)
    private BrokerOnboardingStep onboardingStep;

    // onboarding data
    private String locations;        // comma separated
    private Integer minBudget;
    private Integer maxBudget;
    private String bhkPreference;    // comma separated
    
    @Enumerated(EnumType.STRING)
    private ConversationState conversationState = ConversationState.NEW;


    // getters/setters
}

