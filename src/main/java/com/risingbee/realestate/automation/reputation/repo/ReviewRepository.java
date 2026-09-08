package com.risingbee.realestate.automation.reputation.repo;

import com.risingbee.realestate.automation.reputation.domain.Review;
import com.risingbee.realestate.automation.reputation.enums.ReviewStatus;
import com.risingbee.realestate.automation.reputation.enums.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByTargetTypeAndTargetIdAndStatus(TargetType targetType, Long targetId, ReviewStatus status);

    List<Review> findByTenancyContractId(Long tenancyContractId);

    Optional<Review> findByTenancyContractIdAndAuthorAccountIdAndTargetType(
            Long tenancyContractId,
            Long authorAccountId,
            TargetType targetType
    );

    /**
     * Compute average overall score & total verified review count.
     */
    @Query("""
        SELECT COALESCE(AVG(r.overallRating), 0.0), COUNT(r)
        FROM Review r
        WHERE r.targetType = :targetType
          AND r.targetId = :targetId
          AND r.status = 'PUBLISHED'
    """)
    List<Object[]> computeAggregatedScore(
            @Param("targetType") TargetType targetType,
            @Param("targetId") Long targetId
    );
}