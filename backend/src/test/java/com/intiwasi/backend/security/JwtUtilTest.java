package com.intiwasi.backend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {
    @Test
    void unaClaveNuevaRechazaLaFirmaAnteriorAunqueLaSesionSigaRegistrada() {
        byte[] anterior = new byte[64];
        byte[] nueva = new byte[64];
        SecureRandom random = new SecureRandom();
        random.nextBytes(anterior);
        random.nextBytes(nueva);
        var clock = new SessionRegistryTest.TestClock();
        var sessions = new SessionRegistry(clock);
        var oldJwt = new JwtUtil(Base64.getEncoder().encodeToString(anterior), 60000, clock, sessions);
        var newJwt = new JwtUtil(Base64.getEncoder().encodeToString(nueva), 60000, clock, sessions);
        String email = "rotacion.prueba@intiwasi.pe";
        String previous = oldJwt.generarToken(email);
        assertTrue(oldJwt.validarToken(previous, email));
        assertThrows(io.jsonwebtoken.security.SignatureException.class, () -> newJwt.extraerCorreo(previous));
        String current = newJwt.generarToken(email);
        assertTrue(newJwt.validarToken(current, email));
        assertThrows(io.jsonwebtoken.security.SignatureException.class, () -> oldJwt.extraerCorreo(current));
    }

    @Test
    void limitaElJwtAOchoHorasYRechazaFirmasValidasSinSesionRegistrada() {
        byte[] bytes = new byte[64]; new SecureRandom().nextBytes(bytes);
        String secret = Base64.getEncoder().encodeToString(bytes);
        var key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        var clock = new SessionRegistryTest.TestClock();
        var sessions = new SessionRegistry(clock);
        var jwt = new JwtUtil(secret, Duration.ofHours(12).toMillis(), clock, sessions);
        String email = "operador.prueba@intiwasi.pe";
        String token = jwt.generarToken(email);
        var claims = Jwts.parser().verifyWith(key).clock(() -> Date.from(clock.instant())).build()
                .parseSignedClaims(token).getPayload();
        assertEquals(Duration.ofHours(8).toMillis(), claims.getExpiration().getTime() - claims.getIssuedAt().getTime());
        assertTrue(jwt.validarToken(token, email));
        String legacy = Jwts.builder().subject(email).expiration(claims.getExpiration()).signWith(key).compact();
        assertFalse(jwt.validarToken(legacy, email));
    }
}
