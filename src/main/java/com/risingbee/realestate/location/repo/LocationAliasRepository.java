package com.risingbee.realestate.location.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.risingbee.realestate.location.domain.LocationAlias;

public interface LocationAliasRepository extends JpaRepository<LocationAlias, Long> {

//    @Query("""
//        SELECT a FROM LocationAlias a
//        WHERE LOWER(:text) LIKE CONCAT('%', LOWER(a.alias), '%')
//        ORDER BY LENGTH(a.alias) DESC
//    """)
//    Optional<LocationAlias> findBestMatch(String text);
    
    
//    List<LocationAlias> findBestMatches(String name);
    
    

	@Query("""
		    SELECT la
		    FROM LocationAlias la
		    WHERE 
		        LOWER(:input) LIKE LOWER(CONCAT('%', la.alias, '%'))
		        OR LOWER(la.alias) LIKE LOWER(CONCAT('%', :input, '%'))
		""")
		List<LocationAlias> findBestMatches(@Param("input") String input);
}
