package com.kodality.termx.uam.auth.service;

import com.kodality.termx.core.auth.SessionInfo;
import com.kodality.termx.uam.user.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Singleton
public class JwtService {
    
    @Value("${jwt.secret}")
    private String secretKey;
    
    @Value("${jwt.expiration:86400000}") // Default: 24 hours in milliseconds
    private long jwtExpiration;
    
    @Value("${jwt.refresh-expiration:604800000}") // Default: 7 days in milliseconds
    private long refreshExpiration;
    
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    public String generateToken(User user, Set<String> privileges) {
        return generateToken(new HashMap<>(), user, privileges);
    }
    
    public String generateToken(Map<String, Object> extraClaims, User user, Set<String> privileges) {
        extraClaims.put("privileges", privileges);
        extraClaims.put("email", user.getEmail());
        
        if (user.getFirstName() != null) {
            extraClaims.put("firstName", user.getFirstName());
        }
        
        if (user.getLastName() != null) {
            extraClaims.put("lastName", user.getLastName());
        }
        
        return buildToken(extraClaims, user.getUsername(), jwtExpiration);
    }
    
    public String generateRefreshToken(User user) {
        return buildToken(new HashMap<>(), user.getUsername(), refreshExpiration);
    }
    
    private String buildToken(Map<String, Object> extraClaims, String subject, long expiration) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    public boolean isTokenValid(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username)) && !isTokenExpired(token);
    }
    
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    public SessionInfo createSessionInfo(String token, String username, Set<String> privileges) {
        return new SessionInfo()
                .setToken(token)
                .setUsername(username)
                .setPrivileges(privileges);
    }
}
