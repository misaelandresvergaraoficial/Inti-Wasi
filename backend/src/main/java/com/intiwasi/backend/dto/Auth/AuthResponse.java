package com.intiwasi.backend.dto.Auth;

import com.intiwasi.backend.dto.Usuario.UsuarioResponse;

public record AuthResponse(UsuarioResponse usuario, long expiresAt) {}
