package com.intiwasi.backend.security;

import com.intiwasi.backend.dto.Comun.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class ApiRateLimitFilter extends OncePerRequestFilter {
    private static final int MAX_KEYS = 10_000;
    private final Map<String, Counter> counters = new HashMap<>();
    private final Clock clock;
    private final ObjectMapper mapper;
    private final int limit;
    private final long windowSeconds;
    private final Semaphore exports;
    private Instant nextCleanup = Instant.MIN;

    public ApiRateLimitFilter(Clock clock, ObjectMapper mapper, int limit, long windowSeconds, int maxExports) {
        if (limit <= 0 || windowSeconds <= 0 || maxExports <= 0) {
            throw new IllegalArgumentException("Los límites de API deben ser positivos");
        }
        this.clock = clock;
        this.mapper = mapper;
        this.limit = limit;
        this.windowSeconds = windowSeconds;
        this.exports = new Semaphore(maxExports);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!path.startsWith("/api/") || isExcluded(path) || auth == null
                || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            chain.doFilter(request, response);
            return;
        }
        long retry = check(auth.getName());
        if (retry > 0) {
            reject(response, retry);
            return;
        }
        boolean export = path.matches("/api/reportes/[^/]+/exportar");
        if (export && !exports.tryAcquire()) {
            reject(response, 1);
            return;
        }
        try {
            chain.doFilter(request, response);
        } finally {
            if (export) exports.release();
        }
    }

    private boolean isExcluded(String path) {
        return path.equals("/api/auth/csrf") || path.equals("/api/auth/login")
                || path.equals("/api/auth/actividad") || path.equals("/api/auth/logout");
    }

    private synchronized long check(String identity) {
        Instant now = clock.instant();
        if (!now.isBefore(nextCleanup)) {
            counters.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt()));
            nextCleanup = now.plusSeconds(windowSeconds);
        }
        String key = identity.toLowerCase(Locale.ROOT);
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

    private void reject(HttpServletResponse response, long retrySeconds) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(retrySeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), new ErrorResponse(LocalDateTime.now(clock), 429,
                "Too Many Requests", "Límite temporal alcanzado. Intente nuevamente."));
    }

    private record Counter(int count, Instant expiresAt) {}
}
