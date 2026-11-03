package com.intiwasi.backend.security;

import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class LoginAttemptLimiter {
    private static final int MAX_KEYS = 10_000;
    private final Map<String, Attempts> accounts = new HashMap<>();
    private final Map<String, Attempts> origins = new HashMap<>();
    private final Clock clock;
    private Instant nextCleanup = Instant.MIN;

    public LoginAttemptLimiter(Clock clock) { this.clock = clock; }

    public synchronized void check(String email, String origin) {
        Instant now = clock.instant();
        if (!now.isBefore(nextCleanup)) {
            accounts.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt()));
            origins.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt()));
            nextCleanup = now.plusSeconds(60);
        }
        String accountKey = email.strip().toLowerCase(Locale.ROOT);
        Attempts account = current(accounts.get(accountKey), now, Duration.ofMinutes(15));
        Attempts source = current(origins.get(origin), now, Duration.ofMinutes(1));
        if ((!accounts.containsKey(accountKey) || !origins.containsKey(origin))
                && accounts.size() + origins.size() >= MAX_KEYS) throw new LoginThrottledException(60);
        if (source.count() >= 60) throw throttled(now, source.expiresAt());
        origins.put(origin, new Attempts(source.count() + 1, source.expiresAt(), Instant.MIN));
        if (now.isBefore(account.nextAllowed())) throw throttled(now, account.nextAllowed());
        int attempts = account.count() + 1;
        long delay = attempts < 5 ? 0 : Math.min(300, 1L << Math.min(9, attempts - 5));
        accounts.put(accountKey, new Attempts(attempts, account.expiresAt(), now.plusSeconds(delay)));
    }

    public synchronized void success(String email) {
        accounts.remove(email.strip().toLowerCase(Locale.ROOT));
    }

    private Attempts current(Attempts attempts, Instant now, Duration window) {
        return attempts == null || !now.isBefore(attempts.expiresAt())
                ? new Attempts(0, now.plus(window), Instant.MIN) : attempts;
    }

    private LoginThrottledException throttled(Instant now, Instant until) {
        return new LoginThrottledException((int) Math.max(1, (Duration.between(now, until).toMillis() + 999) / 1000));
    }

    private record Attempts(int count, Instant expiresAt, Instant nextAllowed) {}
}
