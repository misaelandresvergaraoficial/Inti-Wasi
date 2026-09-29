package com.intiwasi.backend.controller;

import com.intiwasi.backend.dto.AuthRequest;
import com.intiwasi.backend.dto.AuthResponse;
import com.intiwasi.backend.dto.ErrorResponse;
import com.intiwasi.backend.dto.UsuarioResponse;
import com.intiwasi.backend.repository.UsuarioRepository;
import com.intiwasi.backend.security.JwtUtil;
import com.intiwasi.backend.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
                          UsuarioService usuarioService, UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getCorreo(), request.getContrasena())
            );
            String token = jwtUtil.generarToken(request.getCorreo());
            String rol = authentication.getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .findFirst()
                    .orElse("");
            return ResponseEntity.ok(new AuthResponse(token, request.getCorreo(), rol));
        } catch (BadCredentialsException ex) {
            boolean cuentaInactiva = usuarioRepository.findByCorreo(request.getCorreo())
                    .filter(usuario -> usuario.getEstado() == 0)
                    .filter(usuario -> passwordEncoder.matches(
                            request.getContrasena(), usuario.getContrasena()))
                    .isPresent();
            if (cuentaInactiva) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse(LocalDateTime.now(), HttpStatus.UNAUTHORIZED.value(),
                                "Cuenta inactiva", "Su cuenta no está disponible. Contacte al Administrador."));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(LocalDateTime.now(), HttpStatus.UNAUTHORIZED.value(),
                            "Credenciales incorrectas", "El correo o la contraseña son inválidos"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> obtenerActual(Authentication authentication) {
        return ResponseEntity.ok(usuarioService.obtenerActual(authentication.getName()));
    }
}
