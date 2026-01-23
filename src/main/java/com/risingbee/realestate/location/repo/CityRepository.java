package com.risingbee.realestate.location.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.risingbee.realestate.location.domain.City;

public interface CityRepository extends JpaRepository<City, Long> {

    Optional<City> findByCode(String code);

    @Query("""
        SELECT c FROM City c
        WHERE LOWER(:text) LIKE CONCAT('%', LOWER(c.name), '%')
           OR LOWER(:text) LIKE CONCAT('%', LOWER(c.code), '%')
    """)
    Optional<City> findBestMatch(String text);
}
