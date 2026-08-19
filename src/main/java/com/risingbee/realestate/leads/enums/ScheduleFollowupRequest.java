package com.risingbee.realestate.leads.enums;

import java.time.Instant;

public record ScheduleFollowupRequest(
	    Instant followUpAt
	) {}
