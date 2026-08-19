package com.risingbee.realestate.profile.dto;

public enum ProfileStage {
    ANONYMOUS,        // no account
    ROLE_PENDING,     // role not selected
    BASIC_INFO,       // name/email missing
    COMPLETED         // ready
}
