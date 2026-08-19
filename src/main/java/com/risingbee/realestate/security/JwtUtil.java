package com.risingbee.realestate.security;

import java.util.Base64;
import java.util.Date;

import org.springframework.stereotype.Component;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.enums.ActorType;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;



@Component
public class JwtUtil {

//    private static final String SECRET = "super-secret-key-change-later";
    
    private static final byte[] SECRET =
            Base64.getDecoder().decode(
                "ple5o6TYHqL7YQar0excuJXlZwJf0rkZ3nypC7Tkh24"
            );
    
    
    private static final long EXPIRY_MS = 7 * 24 * 60 * 60 * 1000; // 7 days

    public String generateToken(Actor actor) {

        return Jwts.builder()
                .setSubject(actor.internalId().toString())
                .claim("phone", actor.externalId())
                .claim("role", actor.role() != null ? actor.role().name() : null)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRY_MS))
                .signWith(Keys.hmacShaKeyFor(SECRET), SignatureAlgorithm.HS256)
                .compact();
    }
    
    
    
    
    public Actor parseToken(String token) {
    	
    	Claims claims = Jwts.parserBuilder().setSigningKey(Keys.hmacShaKeyFor(SECRET)).build().parseClaimsJws(token).getBody();
    	
    	
    	 Long accountId = Long.valueOf(claims.getSubject());
    	    String phone = claims.get("phone", String.class);
    	    
    	    
    	    String role = claims.get("role", String.class);

    	    ActorType actorType = null;

    	    if (role != null && !role.isBlank()) {
    	        actorType = ActorType.valueOf(role);
    	    }
    	    
    	    return new Actor(actorType ,phone,accountId);


       
    }

}

