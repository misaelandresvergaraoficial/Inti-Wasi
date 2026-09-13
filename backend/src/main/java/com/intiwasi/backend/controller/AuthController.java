package com.intiwasi.backend.controller;

import com.intiwasi.backend.dto.AuthRequest;
import com.intiwasi.backend.dto.AuthResponse;
import com.intiwasi.backend.dto.ErrorResponse;
import com.intiwasi.backend.security.JwtUtil;
import jakarta.validation.Valid;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {

        try {
           
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getCorreo(), request.getContrasena())
            );

            
            String token = jwtUtil.generarToken(request.getCorreo());

          
            String rol = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse("");

            
            return ResponseEntity.ok(new AuthResponse(token, request.getCorreo(), rol));

        } catch (BadCredentialsException ex) {
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(LocalDateTime.now(), HttpStatus.UNAUTHORIZED.value(),
                            "Credenciales incorrectas", "El correo o la contraseña son inválidos"));
        }
    }
}