package com.risingbee.realestate.security;

import org.springframework.stereotype.Service;

import com.risingbee.realestate.automation.domain.Broker;

@Service
public class JwtService {

    public String generateToken(Broker broker) {
        // TEMP: return fake token
        return "JWT_FOR_BROKER_" + broker.getId();
    }
}

