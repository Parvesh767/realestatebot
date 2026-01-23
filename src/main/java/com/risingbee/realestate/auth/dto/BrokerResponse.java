package com.risingbee.realestate.auth.dto;

import com.risingbee.realestate.automation.domain.Broker;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BrokerResponse {

    private Long id;
//    private String name;
    private String phone;

    public static BrokerResponse from(Broker broker) {
        return new BrokerResponse(
            broker.getId(),
            broker.getPhone()
//            broker.getName(),
        );
    }
}

