package com.risingbee.realestate.location.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.risingbee.realestate.location.domain.LocationAlias;

public interface LocationAliasRepository extends JpaRepository<LocationAlias, Long> {

    @Query("""
        SELECT a FROM LocationAlias a
        WHERE LOWER(:text) LIKE CONCAT('%', LOWER(a.alias), '%')
        ORDER BY LENGTH(a.alias) DESC
    """)
    Optional<LocationAlias> findBestMatch(String text);
}
