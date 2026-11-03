package com.intiwasi.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.api-limit")
public record ApiLimitProperties(
        @DefaultValue("300") int requests,
        @DefaultValue("60") long windowSeconds,
        @DefaultValue("20") int concurrentRequests,
        @DefaultValue("4") int concurrentAnonymousRequests,
        @DefaultValue("60") int anonymousRequests,
        @DefaultValue("120") int anonymousGlobalRequests,
        @DefaultValue("1200") int globalRequests,
        @DefaultValue("2") int concurrentExports) {

    public ApiLimitProperties {
        if (requests <= 0 || windowSeconds <= 0 || concurrentRequests <= 0
                || concurrentAnonymousRequests <= 0 || anonymousRequests <= 0
                || anonymousGlobalRequests <= 0 || globalRequests <= 0 || concurrentExports <= 0) {
            throw new IllegalArgumentException("Los límites de API deben ser positivos");
        }
        if (concurrentAnonymousRequests >= concurrentRequests) {
            throw new IllegalArgumentException("Las solicitudes anónimas deben dejar capacidad para usuarios autenticados");
        }
    }
}
