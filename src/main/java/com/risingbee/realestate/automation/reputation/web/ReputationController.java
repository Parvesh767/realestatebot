package com.risingbee.realestate.automation.reputation.web;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.ActorContext;
import com.risingbee.realestate.automation.reputation.dto.RatingSummaryDTO;
import com.risingbee.realestate.automation.reputation.dto.SubmitReviewRequestDTO;
import com.risingbee.realestate.automation.reputation.enums.TargetType;
import com.risingbee.realestate.automation.reputation.service.ReputationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reputation")
@RequiredArgsConstructor
public class ReputationController {

    private final ReputationService reputationService;

    @PostMapping("/reviews")
    public ResponseEntity<?> submitReview(@RequestBody SubmitReviewRequestDTO req) {
        Actor actor = ActorContext.get();
        if (actor == null || actor.internalId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthenticated session"));
        }

        reputationService.submitTenancyReview(actor, req);
        return ResponseEntity.ok(Map.of("message", "Review submitted in double-blind mode."));
    }

    @GetMapping("/scores/{targetType}/{targetId}")
    public ResponseEntity<RatingSummaryDTO> getScore(
            @PathVariable TargetType targetType,
            @PathVariable Long targetId
    ) {
        return ResponseEntity.ok(reputationService.getRatingSummary(targetType, targetId));
    }
}