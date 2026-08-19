package com.risingbee.realestate.location.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.risingbee.realestate.location.domain.Locality;

public interface LocalityRepository extends JpaRepository<Locality, Long> {

//    @Query("""
//        SELECT l FROM Locality l
//        WHERE LOWER(:text) LIKE CONCAT('%', l.normalizedName, '%')
//        ORDER BY LENGTH(l.normalizedName) DESC
//    """)
//    Optional<Locality> findBestMatch(String text);
    
    

//	Optional<City> findByCode(String localityCode);

//	Optional<ResolvedLocation> findNameByCode(String localityCode);
//	
    Optional<Locality> findByCode(String code);
    
    
    
    List<Locality>  findByNormalizedName(String name);
    
    
    
    

    @Query("""
    	    SELECT l
    	    FROM Locality l
    	    WHERE LOWER(:input) LIKE LOWER(CONCAT('%', l.normalizedName, '%'))
    	""")
    	List<Locality> findBestMatches(@Param("input") String input);


}
