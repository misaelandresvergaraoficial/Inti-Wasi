package com.intiwasi.backend.repository;

import com.intiwasi.backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByCorreoAndEstado(String correo, Integer estado);

    boolean existsByCorreo(String correo);

    boolean existsByNomUsuario(String nomUsuario);

    boolean existsByTelefono(String telefono);

    List<Usuario> findByEstado(Integer estado);
}