package com.risingbee.realestate.automation.reputation.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.repo.AccountRepository;
import com.risingbee.realestate.automation.exception.AccessDeniedException;
import com.risingbee.realestate.automation.exception.ResourceNotFoundException;
import com.risingbee.realestate.automation.reputation.domain.Review;
import com.risingbee.realestate.automation.reputation.dto.RatingSummaryDTO;
import com.risingbee.realestate.automation.reputation.dto.SubmitReviewRequestDTO;
import com.risingbee.realestate.automation.reputation.enums.ReviewStatus;
import com.risingbee.realestate.automation.reputation.enums.TargetType;
import com.risingbee.realestate.automation.reputation.repo.ReviewRepository;
import com.risingbee.realestate.automation.service.WhatsAppSender;
import com.risingbee.realestate.automation.tenancy.domain.TenancyContract;
import com.risingbee.realestate.automation.tenancy.repo.TenancyContractRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReputationService {

    private final ReviewRepository reviewRepository;
    private final TenancyContractRepository contractRepository;
    private final AccountRepository accountRepository;
    private final WhatsAppSender whatsAppSender;

    /**
     * Submit a rating (Double-Blind: Stays hidden until counterparty submits)
     */
    public Review submitTenancyReview(Actor actor, SubmitReviewRequestDTO req) {
        Long authorId = actor.internalId();
        TenancyContract contract = contractRepository.findById(req.getTenancyContractId())
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found"));

        if (!contract.getOwnerAccountId().equals(authorId) && !contract.getTenantAccountId().equals(authorId)) {
            throw new AccessDeniedException("You are not a participant in this tenancy contract");
        }

        // Validate duplicates
        reviewRepository.findByTenancyContractIdAndAuthorAccountIdAndTargetType(
                contract.getId(), authorId, req.getTargetType()
        ).ifPresent(existing -> {
            throw new IllegalStateException("You have already submitted a review for this contract");
        });

        Review review = new Review();
        review.setTenancyContractId(contract.getId());
        review.setAuthorAccountId(authorId);
        review.setTargetType(req.getTargetType());
        review.setTargetId(req.getTargetId());
        review.setOverallRating(Math.max(1, Math.min(5, req.getOverallRating())));
        review.setAspect1Rating(req.getAspect1Rating());
        review.setAspect2Rating(req.getAspect2Rating());
        review.setAspect3Rating(req.getAspect3Rating());
        review.setFeedbackText(req.getFeedbackText());
        review.setStatus(ReviewStatus.BLIND_HELD);

        reviewRepository.save(review);
        log.info("Review recorded in BLIND_HELD mode by account #{} for contract #{}", authorId, contract.getId());

        // Check if both parties have submitted -> Publish both simultaneously
        checkAndPublishBlindPair(contract);

        return review;
    }

    /**
     * Compute real-time verified star average (e.g. 4.8 / 5.0 across 3 tenancies)
     */
    @Transactional(readOnly = true)
    public RatingSummaryDTO getRatingSummary(TargetType targetType, Long targetId) {
        List<Object[]> res = reviewRepository.computeAggregatedScore(targetType, targetId);
        if (res.isEmpty() || res.get(0) == null) {
            return new RatingSummaryDTO(targetType, targetId, 0.0, 0L, "UNRATED");
        }

        Double avg = (Double) res.get(0)[0];
        Long count = (Long) res.get(0)[1];

        // Format to 1 decimal place
        double roundedScore = Math.round(avg * 10.0) / 10.0;
        String badge = count >= 2 && roundedScore >= 4.5 ? "VERIFIED_SUPER_TENANT" : (count > 0 ? "REVIEWED" : "NEW");

        return new RatingSummaryDTO(targetType, targetId, roundedScore, count, badge);
    }

    private void checkAndPublishBlindPair(TenancyContract contract) {
        List<Review> contractReviews = reviewRepository.findByTenancyContractId(contract.getId());

        boolean hasTenantReview = contractReviews.stream().anyMatch(r -> r.getTargetType() == TargetType.TENANT);
        boolean hasOwnerOrPropertyReview = contractReviews.stream().anyMatch(r -> r.getTargetType() == TargetType.OWNER || r.getTargetType() == TargetType.PROPERTY);

        if (hasTenantReview && hasOwnerOrPropertyReview) {
            // Both submitted! Release reviews publicly
            contractReviews.forEach(Review::publish);
            reviewRepository.saveAll(contractReviews);
            log.info("Both parties submitted. Reviews for contract #{} are now PUBLISHED live!", contract.getId());

            // Notify parties
            whatsAppSender.sendTextMessage(contract.getTenantPhone(), "⭐ *Reviews are now Live!* Your landlord left feedback on your tenancy profile.");
            whatsAppSender.sendTextMessage(contract.getOwnerPhone(), "⭐ *Reviews are now Live!* Your tenant submitted their review for your property.");
        }
    }
}