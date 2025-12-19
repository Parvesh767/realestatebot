package com.risingbee.realestate.automation.interfaces;

import com.risingbee.realestate.automation.domain.Broker;

public interface BrokerOnboardingService {
	  void handle(Broker broker, String message);

}
