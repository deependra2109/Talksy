package com.deependra.talksy.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;


    public String generateToken(String username) {
        Date now = new Date();
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(now)
            .setExpiration(new Date(now.getTime() + jwtExpirationMs))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }



    @Nullable
    public Claims parseClaimsOrNull(String token) {
        try {
            return parseClaims(token);
        } catch (ExpiredJwtException ex) {
            log.warn("JWT expired");
        } catch (UnsupportedJwtException ex) {
            log.warn("JWT unsupported");
        } catch (MalformedJwtException ex) {
            log.warn("JWT malformed");
        } catch (SignatureException ex) {
            log.warn("JWT invalid signature");
        } catch (IllegalArgumentException ex) {
            log.warn("JWT is null or empty");
        }
        return null;
    }

    public boolean isTokenStructurallyValid(String token) {
        return parseClaimsOrNull(token) != null;
    }


    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }



    public boolean isTokenValid(String token, String username) {
        try {
            Claims claims = parseClaims(token);
            String subject = claims.getSubject();
            boolean notExpired = claims.getExpiration().after(new Date());
            return subject != null && subject.equals(username) && notExpired;
        } catch (Exception ex) {
            log.warn("JWT validation failed for user '{}': {}", username, ex.getMessage());
            return false;
        }
    }


    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
