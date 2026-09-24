package com.intiwasi.backend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secreto}")
    private String secreto;

    @Value("${jwt.expiracion-ms}")
    private long expiracionMs;

    
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }

    
    public String generarToken(String correo) {
        return Jwts.builder()
                .subject(correo)                              
                .issuedAt(new Date())                         
                .expiration(new Date(System.currentTimeMillis() + expiracionMs)) 
                .signWith(getSecretKey())                     
                .compact();
    }

   
    public String extraerCorreo(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    
    public boolean validarToken(String token, String correoUsuario) {
        String correoEnToken = extraerCorreo(token);
        return correoEnToken.equals(correoUsuario) && !isTokenExpired(token);
    }

    
    private boolean isTokenExpired(String token) {
        Date expiracion = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
        return expiracion.before(new Date());
    }
}
