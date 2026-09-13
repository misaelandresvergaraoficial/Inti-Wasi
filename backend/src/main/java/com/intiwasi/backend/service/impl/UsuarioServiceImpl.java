package com.intiwasi.backend.service.impl;

import com.intiwasi.backend.dto.UsuarioRequest;
import com.intiwasi.backend.dto.UsuarioResponse;
import com.intiwasi.backend.entity.Usuario;
import com.intiwasi.backend.repository.UsuarioRepository;
import com.intiwasi.backend.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<UsuarioResponse> listarActivos() {
        return usuarioRepository.findByEstado(1).stream()
                .map(this::mapearAResponse)
                .toList();
    }

    @Override
    public UsuarioResponse obtenerPorId(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
        return mapearAResponse(usuario);
    }

    @Override
    public UsuarioResponse registrar(UsuarioRequest request) {
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new RuntimeException("El correo ya se encuentra registrado");
        }

        if (usuarioRepository.existsByNomUsuario(request.getNomUsuario())) {
            throw new RuntimeException("El nombre de usuario ya se encuentra registrado");
        }

        if (request.getTelefono() != null && !request.getTelefono().isBlank()) {
            if (usuarioRepository.existsByTelefono(request.getTelefono())) {
                throw new RuntimeException("El teléfono ya se encuentra registrado");
            }
        }

        if (request.getContrasena() == null || request.getContrasena().isBlank()) {
            throw new RuntimeException("La contraseña es requerida para el registro");
        }

        Usuario usuario = new Usuario();
        usuario.setNomUsuario(request.getNomUsuario());
        usuario.setCorreo(request.getCorreo());
        usuario.setContrasena(passwordEncoder.encode(request.getContrasena()));
        usuario.setRol(request.getRol());
        usuario.setTelefono(request.getTelefono());
        usuario.setEstado(1);

        return mapearAResponse(usuarioRepository.save(usuario));
    }

    @Override
    public UsuarioResponse actualizar(Integer id, UsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        if (!usuario.getCorreo().equalsIgnoreCase(request.getCorreo()) &&
                usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new RuntimeException("El correo ya se encuentra registrado por otro usuario");
        }

        if (!usuario.getNomUsuario().equalsIgnoreCase(request.getNomUsuario()) &&
                usuarioRepository.existsByNomUsuario(request.getNomUsuario())) {
            throw new RuntimeException("El nombre de usuario ya se encuentra registrado por otro usuario");
        }

        if (request.getTelefono() != null && !request.getTelefono().isBlank()) {
            if (!request.getTelefono().equals(usuario.getTelefono()) &&
                    usuarioRepository.existsByTelefono(request.getTelefono())) {
                throw new RuntimeException("El teléfono ya se encuentra registrado por otro usuario");
            }
        }

        usuario.setNomUsuario(request.getNomUsuario());
        usuario.setCorreo(request.getCorreo());
        usuario.setRol(request.getRol());
        usuario.setTelefono(request.getTelefono());

        if (request.getContrasena() != null && !request.getContrasena().isBlank()) {
            usuario.setContrasena(passwordEncoder.encode(request.getContrasena()));
        }

        return mapearAResponse(usuarioRepository.save(usuario));
    }

    @Override
    public void desactivar(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
        usuario.setEstado(0);
        usuarioRepository.save(usuario);
    }

    private UsuarioResponse mapearAResponse(Usuario usuario) {
        return UsuarioResponse.builder()
                .idUsuario(usuario.getIdUsuario())
                .nomUsuario(usuario.getNomUsuario())
                .correo(usuario.getCorreo())
                .rol(usuario.getRol())
                .telefono(usuario.getTelefono())
                .estado(usuario.getEstado())
                .fechaRegistro(usuario.getFechaRegistro())
                .build();
    }
}