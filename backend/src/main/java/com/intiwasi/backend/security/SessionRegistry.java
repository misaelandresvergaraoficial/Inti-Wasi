package com.intiwasi.backend.security;

import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.authentication.BadCredentialsException;

@Component
public class SessionRegistry {
    private static final Duration IDLE_LIMIT = Duration.ofMinutes(15).plusSeconds(30);
    private static final int MAX_SESSIONS = 10_000;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, Long> versions = new ConcurrentHashMap<>();
    private final Clock clock;

    public SessionRegistry(Clock clock) { this.clock = clock; }

    public synchronized String open(String email, Instant expiresAt) {
        return open(email, expiresAt, version(email));
    }

    public long version(String email) {
        return versions.getOrDefault(email.toLowerCase(Locale.ROOT), 0L);
    }

    public synchronized String open(String email, Instant expiresAt, long expectedVersion) {
        if (version(email) != expectedVersion) throw new BadCredentialsException("La cuenta cambió durante el acceso");
        Instant now = clock.instant();
        sessions.entrySet().removeIf(entry -> expired(entry.getValue(), now));
        if (sessions.size() >= MAX_SESSIONS) throw new LoginThrottledException(60);
        String id = UUID.randomUUID().toString();
        sessions.put(id, new Session(email, expiresAt, now));
        return id;
    }

    public boolean isActive(String id, String email) {
        if (id == null) return false;
        Session session = sessions.computeIfPresent(id,
                (key, current) -> expired(current, clock.instant()) ? null : current);
        return session != null && session.email().equals(email);
    }

    public synchronized boolean activity(String id, String email) {
        if (id == null) return false;
        Instant now = clock.instant();
        Session session = sessions.computeIfPresent(id, (key, current) -> {
            if (expired(current, now)) return null;
            if (!current.email().equals(email)) return current;
            return new Session(current.email(), current.expiresAt(), now);
        });
        return session != null && session.email().equals(email);
    }

    public void revoke(String id) { if (id != null) sessions.remove(id); }

    public synchronized void revokeUser(String email) {
        versions.merge(email.toLowerCase(Locale.ROOT), 1L, Long::sum);
        sessions.entrySet().removeIf(entry -> entry.getValue().email().equalsIgnoreCase(email));
    }

    private boolean expired(Session session, Instant now) {
        return !now.isBefore(session.expiresAt()) || !now.isBefore(session.lastActivity().plus(IDLE_LIMIT));
    }

    private record Session(String email, Instant expiresAt, Instant lastActivity) {}
}
