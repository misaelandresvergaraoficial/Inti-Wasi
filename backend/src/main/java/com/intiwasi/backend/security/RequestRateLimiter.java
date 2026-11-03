package com.intiwasi.backend.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

final class RequestRateLimiter {
    private static final int MAX_KEYS = 10_000;
    private final Map<String, Counter> counters = new HashMap<>();
    private final Clock clock;
    private final int limit;
    private final long windowSeconds;
    private Instant nextCleanup = Instant.MIN;

    RequestRateLimiter(Clock clock, int limit, long windowSeconds) {
        this.clock = clock;
        this.limit = limit;
        this.windowSeconds = windowSeconds;
    }

    synchronized long check(String key) {
        Instant now = clock.instant();
        if (!now.isBefore(nextCleanup)) {
            counters.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt()));
            nextCleanup = now.plusSeconds(windowSeconds);
        }
        Counter current = counters.get(key);
        if (current == null || !now.isBefore(current.expiresAt())) {
            if (current == null && counters.size() >= MAX_KEYS) return windowSeconds;
            counters.put(key, new Counter(1, now.plusSeconds(windowSeconds)));
            return 0;
        }
        if (current.count() >= limit) {
            return Math.max(1, (Duration.between(now, current.expiresAt()).toMillis() + 999) / 1000);
        }
        counters.put(key, new Counter(current.count() + 1, current.expiresAt()));
        return 0;
    }

    private record Counter(int count, Instant expiresAt) {}
}
