package com.intiwasi.backend.security;

import com.intiwasi.backend.dto.Comun.ErrorResponse;
import com.intiwasi.backend.config.ApiLimitProperties;
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
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.Semaphore;

public class ApiRateLimitFilter extends OncePerRequestFilter {
    private final Clock clock;
    private final ObjectMapper mapper;
    private final RequestRateLimiter users;
    private final RequestRateLimiter anonymousIps;
    private final RequestRateLimiter anonymousTotal;
    private final RequestRateLimiter global;
    private final Semaphore anonymousRequests;
    private final Semaphore exports;

    public ApiRateLimitFilter(Clock clock, ObjectMapper mapper, ApiLimitProperties limits) {
        this.clock = clock;
        this.mapper = mapper;
        this.users = new RequestRateLimiter(clock, limits.requests(), limits.windowSeconds());
        this.anonymousIps = new RequestRateLimiter(clock, limits.anonymousRequests(), limits.windowSeconds());
        this.anonymousTotal = new RequestRateLimiter(clock, limits.anonymousGlobalRequests(), limits.windowSeconds());
        this.global = new RequestRateLimiter(clock, limits.globalRequests(), limits.windowSeconds());
        this.anonymousRequests = new Semaphore(limits.concurrentAnonymousRequests());
        this.exports = new Semaphore(limits.concurrentExports());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }
        boolean anonymous = auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken;
        long retry = 0;
        if (anonymous) {
            retry = anonymousIps.check(request.getRemoteAddr());
            if (retry == 0) retry = anonymousTotal.check("anonymous");
        } else if (!isExcluded(path)) {
            retry = users.check(auth.getName().toLowerCase(Locale.ROOT));
        }
        if (retry > 0) {
            reject(response, retry);
            return;
        }
        if (anonymous && !anonymousRequests.tryAcquire()) {
            reject(response, 1);
            return;
        }
        boolean exportAcquired = false;
        try {
            retry = global.check("api");
            if (retry > 0) {
                reject(response, retry);
                return;
            }
            if (!anonymous && path.matches("/api/reportes/[^/]+/exportar")) {
                exportAcquired = exports.tryAcquire();
                if (!exportAcquired) {
                    reject(response, 1);
                    return;
                }
            }
            chain.doFilter(request, response);
        } finally {
            if (exportAcquired) exports.release();
            if (anonymous) anonymousRequests.release();
        }
    }

    private boolean isExcluded(String path) {
        return path.equals("/api/auth/csrf") || path.equals("/api/auth/login")
                || path.equals("/api/auth/actividad") || path.equals("/api/auth/logout");
    }

    private void reject(HttpServletResponse response, long retrySeconds) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(retrySeconds));
        response.setHeader("Cache-Control", "no-store");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), new ErrorResponse(LocalDateTime.now(clock), 429,
                "Too Many Requests", "Límite temporal alcanzado. Intente nuevamente."));
    }

}
