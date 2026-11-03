package com.intiwasi.backend.controller;

import com.intiwasi.backend.dto.Auth.AuthRequest;
import com.intiwasi.backend.dto.Auth.AuthResponse;
import com.intiwasi.backend.dto.Comun.ErrorResponse;
import com.intiwasi.backend.dto.Usuario.UsuarioResponse;
import com.intiwasi.backend.repository.UsuarioRepository;
import com.intiwasi.backend.security.JwtUtil;
import com.intiwasi.backend.security.LoginAttemptLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import com.intiwasi.backend.security.SessionCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.csrf.CsrfToken;
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
    private final LoginAttemptLimiter loginAttempts;
    private final SessionCookie sessionCookie;
    private final CsrfTokenRepository csrfTokens;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
                          UsuarioService usuarioService, UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder, LoginAttemptLimiter loginAttempts, SessionCookie sessionCookie,
                          CsrfTokenRepository csrfTokens) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttempts = loginAttempts;
        this.sessionCookie = sessionCookie;
        this.csrfTokens = csrfTokens;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request, HttpServletRequest servletRequest,
                                   HttpServletResponse servletResponse) {
        loginAttempts.check(request.getCorreo(), servletRequest.getRemoteAddr());
        long version = jwtUtil.versionSesion(request.getCorreo());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getCorreo(), request.getContrasena())
            );
            String correo = authentication.getName();
            String token = jwtUtil.generarToken(correo, version);
            loginAttempts.success(request.getCorreo());
            csrfTokens.saveToken(null, servletRequest, servletResponse);
            return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, sessionCookie.create(token))
                    .body(new AuthResponse(usuarioService.obtenerActual(correo), jwtUtil.extraerExpiracion(token)));
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

    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken token) {
        token.getToken();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> obtenerActual(Authentication authentication, HttpServletRequest request) {
        return ResponseEntity.ok(new AuthResponse(usuarioService.obtenerActual(authentication.getName()),
                jwtUtil.extraerExpiracion(SessionCookie.from(request))));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> cerrarSesion(HttpServletRequest request) {
        jwtUtil.cerrarSesion(SessionCookie.from(request));
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, sessionCookie.clear()).build();
    }

    @PostMapping("/actividad")
    public ResponseEntity<Void> registrarActividad(HttpServletRequest request) {
        if (!jwtUtil.registrarActividad(SessionCookie.from(request))) {
            throw new BadCredentialsException("La sesión ya no está activa");
        }
        return ResponseEntity.noContent().build();
    }
}
