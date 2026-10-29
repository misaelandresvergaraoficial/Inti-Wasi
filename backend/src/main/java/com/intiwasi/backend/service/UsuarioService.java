package com.intiwasi.backend.service;

import com.intiwasi.backend.dto.Usuario.UsuarioRequest;
import com.intiwasi.backend.dto.Usuario.UsuarioResponse;
import com.intiwasi.backend.entity.Usuario;
import com.intiwasi.backend.exception.ConflictoException;
import com.intiwasi.backend.exception.RecursoNoEncontradoException;
import com.intiwasi.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.intiwasi.backend.exception.ReglaNegocioException;

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

    public List<UsuarioResponse> listarTodos() {
        return usuarioRepository.findAllByOrderByIdUsuarioAsc().stream()
                .map(this::mapearAResponse)
                .toList();
    }

    public UsuarioResponse obtenerActual(String correo) {
        Usuario usuario = usuarioRepository.findByCorreoAndEstado(correo, 1)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario activo no encontrado"));
        return mapearAResponse(usuario);
    }

    public UsuarioResponse obtenerPorId(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado con ID: " + id
                ));
        return mapearAResponse(usuario);
    }

    @Transactional
    public UsuarioResponse registrar(UsuarioRequest request) {
        validarDatos(request);
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new ConflictoException("El correo ya está en uso. Puedes añadir la inicial del apellido al nombre");
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

    @Transactional
    public UsuarioResponse actualizar(Integer id, UsuarioRequest request) {
        validarDatos(request);
        List<Usuario> usuarios = usuarioRepository.bloquearUsuarios();
        Usuario usuario = buscarBloqueado(usuarios, id);
        validarAdministrador(usuarios, usuario, request.getRol(), usuario.getEstado());

        if (!usuario.getCorreo().equalsIgnoreCase(request.getCorreo())
                && usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new ConflictoException("El correo ya está en uso. Puedes añadir la inicial del apellido al nombre");
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

    @Transactional
    public void desactivar(Integer id) {
        cambiarEstado(id, 0);
    }

    @Transactional
    public UsuarioResponse cambiarEstado(Integer id, Integer estado) {
        if (estado == null || (estado != 0 && estado != 1)) {
            throw new IllegalArgumentException("El estado debe ser 0 o 1");
        }
        List<Usuario> usuarios = usuarioRepository.bloquearUsuarios();
        Usuario usuario = buscarBloqueado(usuarios, id);
        validarAdministrador(usuarios, usuario, usuario.getRol(), estado);
        usuario.setEstado(estado);
        return mapearAResponse(usuarioRepository.save(usuario));
    }

    private Usuario buscarBloqueado(List<Usuario> usuarios, Integer id) {
        return usuarios.stream().filter(u -> u.getIdUsuario().equals(id)).findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con ID: " + id));
    }

    private void validarAdministrador(List<Usuario> usuarios, Usuario usuario, String rol, Integer estado) {
        boolean quedaAdministrador = usuarios.stream().anyMatch(u -> {
            boolean editado = u.getIdUsuario().equals(usuario.getIdUsuario());
            return "Administrador".equals(editado ? rol : u.getRol())
                    && Integer.valueOf(1).equals(editado ? estado : u.getEstado());
        });
        if (!quedaAdministrador) {
            throw new ReglaNegocioException("Siempre debe haber al menos un administrador activo. "
                    + "Activa o asigna otro administrador antes de realizar este cambio.");
        }
    }

    private void validarDatos(UsuarioRequest request) {
        String correo = request.getCorreo();
        if (correo == null || !correo.matches("^(admin|operador)\\.[a-z]+@intiwasi\\.pe$")) {
            throw new IllegalArgumentException("correo: Usa rol.nombre@intiwasi.pe, con letras minúsculas y sin números");
        }
        String prefijo = switch (request.getRol() == null ? "" : request.getRol()) {
            case "Administrador" -> "admin.";
            case "Operador de Almacén" -> "operador.";
            default -> throw new IllegalArgumentException("El rol no es válido");
        };
        if (!correo.startsWith(prefijo)) {
            throw new IllegalArgumentException("correo: El correo no coincide con el rol seleccionado. Usa "
                    + prefijo + "nombre@intiwasi.pe");
        }
        String telefono = request.getTelefono();
        telefono = telefono == null || telefono.isBlank() ? null : telefono.trim();
        if (telefono != null && !telefono.matches("9\\d{8}")) {
            throw new IllegalArgumentException(
                    "telefono: Ingresa 9 dígitos que empiecen con 9 (ej. 979348552)"
            );
        }
        request.setTelefono(telefono);
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
