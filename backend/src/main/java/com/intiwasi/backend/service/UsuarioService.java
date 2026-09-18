package com.intiwasi.backend.service;

import com.intiwasi.backend.dto.UsuarioRequest;
import com.intiwasi.backend.dto.UsuarioResponse;
import com.intiwasi.backend.entity.Usuario;
import com.intiwasi.backend.exception.ConflictoException;
import com.intiwasi.backend.exception.RecursoNoEncontradoException;
import com.intiwasi.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UsuarioResponse> listarActivos() {
        return usuarioRepository.findByEstado(1).stream()
                .map(this::mapearAResponse)
                .toList();
    }

    public UsuarioResponse obtenerPorId(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado con ID: " + id
                ));
        return mapearAResponse(usuario);
    }

    public UsuarioResponse registrar(UsuarioRequest request) {
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new ConflictoException("El correo ya se encuentra registrado");
        }

        if (usuarioRepository.existsByNomUsuario(request.getNomUsuario())) {
            throw new ConflictoException("El nombre de usuario ya se encuentra registrado");
        }

        if (request.getTelefono() != null && !request.getTelefono().isBlank()
                && usuarioRepository.existsByTelefono(request.getTelefono())) {
            throw new ConflictoException("El teléfono ya se encuentra registrado");
        }

        if (request.getContrasena() == null || request.getContrasena().isBlank()) {
            throw new IllegalArgumentException("La contraseña es requerida para el registro");
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

    public UsuarioResponse actualizar(Integer id, UsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado con ID: " + id
                ));

        if (!usuario.getCorreo().equalsIgnoreCase(request.getCorreo())
                && usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new ConflictoException("El correo ya se encuentra registrado por otro usuario");
        }

        if (!usuario.getNomUsuario().equalsIgnoreCase(request.getNomUsuario())
                && usuarioRepository.existsByNomUsuario(request.getNomUsuario())) {
            throw new ConflictoException(
                    "El nombre de usuario ya se encuentra registrado por otro usuario"
            );
        }

        if (request.getTelefono() != null && !request.getTelefono().isBlank()
                && !request.getTelefono().equals(usuario.getTelefono())
                && usuarioRepository.existsByTelefono(request.getTelefono())) {
            throw new ConflictoException("El teléfono ya se encuentra registrado por otro usuario");
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

    public void desactivar(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado con ID: " + id
                ));
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
