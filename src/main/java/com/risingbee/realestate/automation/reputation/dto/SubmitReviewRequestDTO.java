package com.risingbee.realestate.automation.reputation.dto;

import com.risingbee.realestate.automation.reputation.enums.TargetType;
import lombok.Data;

@Data
public class SubmitReviewRequestDTO {
    private Long tenancyContractId;
    private TargetType targetType; // TENANT, OWNER, PROPERTY
    private Long targetId;
    private Integer overallRating; // 1 to 5
    private Integer aspect1Rating;
    private Integer aspect2Rating;
    private Integer aspect3Rating;
    private String feedbackText;
}