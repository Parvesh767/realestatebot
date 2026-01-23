package com.risingbee.realestate.security;

import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

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

    
    
//        public static void main(String[] args) {
//            SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
//            String base64 = Base64.getEncoder().encodeToString(key.getEncoded());
//            System.out.println(base64);
//        }
    
    
    private static final long EXPIRY_MS = 7 * 24 * 60 * 60 * 1000; // 7 days

    public String generateToken(Long brokerId, String phone) {

        return Jwts.builder()
                .setSubject(brokerId.toString())
                .claim("phone", phone)
                .claim("role", "BROKER")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRY_MS))
                .signWith(Keys.hmacShaKeyFor(SECRET), SignatureAlgorithm.HS256)
                .compact();
    }
    
    public Claims parseToken(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

}

