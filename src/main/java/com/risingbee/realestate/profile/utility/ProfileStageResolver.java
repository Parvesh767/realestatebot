package com.risingbee.realestate.profile.utility;

import com.risingbee.realestate.automation.actor.domain.Account;
import com.risingbee.realestate.profile.dto.ProfileStage;

public class ProfileStageResolver {

    public static ProfileStage resolve(Account account) {


        if (account.getType() == null) {
            return ProfileStage.ROLE_PENDING;
        }

        boolean missingName =
            account.getName() == null
            || account.getName().isBlank();

        boolean missingEmail =
            account.getEmail() == null
            || account.getEmail().isBlank();

        if (missingName || missingEmail) {
            return ProfileStage.BASIC_INFO;
        }

        
        
        return ProfileStage.COMPLETED;
    }

    public static String nextStep(ProfileStage stage) {
        return switch (stage) {
            case ANONYMOUS -> "LOGIN";
            case ROLE_PENDING -> "SELECT_ROLE";
            case BASIC_INFO -> "ENTER_DETAILS";
            case COMPLETED -> "DASHBOARD";
        };
    }
    
    
}
