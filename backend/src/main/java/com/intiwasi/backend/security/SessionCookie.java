package com.intiwasi.backend.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class SessionCookie {
    public static final String NAME = "intiwasi.session";
    private final boolean secure;

    public SessionCookie(@Value("${app.auth.cookie-secure:true}") boolean secure) {
        this.secure = secure;
    }

    public String create(String token) {
        return cookie(token).build().toString();
    }

    public String clear() {
        return cookie("").maxAge(0).build().toString();
    }

    private ResponseCookie.ResponseCookieBuilder cookie(String value) {
        return ResponseCookie.from(NAME, value).httpOnly(true).secure(secure).sameSite("Strict").path("/");
    }

    public static String from(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (NAME.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }
}
