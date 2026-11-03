package com.intiwasi.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtUtil {
    private final SecretKey key;
    private final Duration lifetime;
    private final Clock clock;
    private final SessionRegistry sessions;

    public JwtUtil(@Value("${jwt.secreto}") String secret,
                   @Value("${jwt.expiracion-ms:28800000}") long expirationMs,
                   Clock clock, SessionRegistry sessions) {
        if (expirationMs <= 0) throw new IllegalArgumentException("La duración de sesión debe ser positiva");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.lifetime = Duration.ofMillis(Math.min(expirationMs, Duration.ofHours(8).toMillis()));
        this.clock = clock;
        this.sessions = sessions;
    }

    public String generarToken(String correo) {
        return generarToken(correo, versionSesion(correo));
    }

    public long versionSesion(String correo) { return sessions.version(correo); }

    public String generarToken(String correo, long version) {
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = now.plus(lifetime);
        String id = sessions.open(correo, expiresAt, version);
        return Jwts.builder().id(id).subject(correo).issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt)).signWith(key).compact();
    }

    public String extraerCorreo(String token) { return claims(token).getSubject(); }

    public boolean validarToken(String token, String correo) {
        Claims claims = claims(token);
        return correo.equals(claims.getSubject()) && sessions.isActive(claims.getId(), correo);
    }

    public void cerrarSesion(String token) { sessions.revoke(claims(token).getId()); }

    public boolean registrarActividad(String token) {
        Claims claims = claims(token);
        return sessions.activity(claims.getId(), claims.getSubject());
    }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(key).clock(() -> Date.from(clock.instant())).build()
                .parseSignedClaims(token).getPayload();
    }
}
