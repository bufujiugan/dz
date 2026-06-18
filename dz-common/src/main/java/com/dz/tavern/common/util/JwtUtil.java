package com.dz.tavern.common.util;

import com.dz.tavern.common.context.AuthPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class JwtUtil {
    private final SecretKey secretKey;

    public JwtUtil(@Value("${security.jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(Long id, String subject, String type,
                              Set<String> permissions, Duration duration) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .claims(Map.of(
                        "id", id,
                        "type", type,
                        "permissions", permissions == null ? Set.of() : permissions))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(duration)))
                .signWith(secretKey)
                .compact();
    }

    public AuthPrincipal parseToken(String token) {
        Claims claims = Jwts.parser().verifyWith(secretKey).build()
                .parseSignedClaims(token).getPayload();
        Long id = claims.get("id", Number.class).longValue();
        String type = claims.get("type", String.class);
        Object permissionValue = claims.get("permissions");
        Set<String> permissions = new HashSet<>();
        if (permissionValue instanceof List<?> permissionList) {
            permissionList.forEach(item -> permissions.add(String.valueOf(item)));
        }
        return new AuthPrincipal(id, claims.getSubject(), type, permissions);
    }
}
