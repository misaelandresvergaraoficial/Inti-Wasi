package com.intiwasi.backend.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.*;

class SessionRegistryTest {
    private final TestClock clock = new TestClock();
    private final SessionRegistry sessions = new SessionRegistry(clock);

    @Test
    void logoutRevocaSoloSuSesionYReiniciarPierdeElRegistro() {
        String first = open();
        String second = open();
        assertNotEquals(first, second);
        sessions.revoke(first);
        assertFalse(sessions.isActive(first, "admin.prueba@intiwasi.pe"));
        assertTrue(sessions.isActive(second, "admin.prueba@intiwasi.pe"));
        assertFalse(new SessionRegistry(clock).isActive(second, "admin.prueba@intiwasi.pe"));
    }

    @Test
    void revocarUsuarioInvalidaTodasSusSesionesYConservaLasDemas() {
        String first = open();
        String second = open();
        String other = sessions.open("operador.prueba@intiwasi.pe", clock.instant().plus(Duration.ofHours(8)));
        sessions.revokeUser("admin.prueba@intiwasi.pe");
        assertFalse(sessions.isActive(first, "admin.prueba@intiwasi.pe"));
        assertFalse(sessions.isActive(second, "admin.prueba@intiwasi.pe"));
        assertTrue(sessions.isActive(other, "operador.prueba@intiwasi.pe"));
    }

    @Test
    void consultarNoRenuevaInactividadYActividadTardiaNoResucitaSesion() {
        String id = open();
        clock.advance(Duration.ofMinutes(15));
        assertTrue(sessions.isActive(id, "admin.prueba@intiwasi.pe"));
        clock.advance(Duration.ofSeconds(30));
        assertFalse(sessions.isActive(id, "admin.prueba@intiwasi.pe"));
        assertFalse(sessions.activity(id, "admin.prueba@intiwasi.pe"));
    }

    @Test
    void confirmarDuranteAvisoRenuevaInactividadSinExtenderOchoHoras() {
        String id = open();
        clock.advance(Duration.ofMinutes(15).plusSeconds(29));
        assertTrue(sessions.activity(id, "admin.prueba@intiwasi.pe"));
        clock.advance(Duration.ofMinutes(14));
        assertTrue(sessions.isActive(id, "admin.prueba@intiwasi.pe"));
        for (int i = 0; i < 31; i++) {
            assertTrue(sessions.activity(id, "admin.prueba@intiwasi.pe"));
            clock.advance(Duration.ofMinutes(14));
        }
        assertTrue(sessions.activity(id, "admin.prueba@intiwasi.pe"));
        clock.advance(Duration.ofMinutes(14));
        assertTrue(sessions.activity(id, "admin.prueba@intiwasi.pe"));
        clock.set(Instant.parse("2026-10-31T20:00:00Z"));
        assertFalse(sessions.activity(id, "admin.prueba@intiwasi.pe"));
    }

    @Test
    void unaAutenticacionIniciadaAntesDeRevocarNoPuedeAbrirSesionDespues() {
        long version = sessions.version("admin.prueba@intiwasi.pe");
        sessions.revokeUser("admin.prueba@intiwasi.pe");
        assertThrows(BadCredentialsException.class, () -> sessions.open("admin.prueba@intiwasi.pe",
                clock.instant().plus(Duration.ofHours(8)), version));
        assertDoesNotThrow(this::open);
    }

    private String open() {
        return sessions.open("admin.prueba@intiwasi.pe", clock.instant().plus(Duration.ofHours(8)));
    }

    static final class TestClock extends Clock {
        private Instant now = Instant.parse("2026-10-31T12:00:00Z");
        void advance(Duration duration) { now = now.plus(duration); }
        void set(Instant instant) { now = instant; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
