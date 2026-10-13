package com.intiwasi.backend.repository;

import com.intiwasi.backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u order by u.idUsuario")
    List<Usuario> bloquearUsuarios();

    Optional<Usuario> findByCorreoAndEstado(String correo, Integer estado);
    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    boolean existsByNomUsuario(String nomUsuario);

    boolean existsByTelefono(String telefono);

    List<Usuario> findByEstado(Integer estado);
    List<Usuario> findAllByOrderByIdUsuarioAsc();
}
