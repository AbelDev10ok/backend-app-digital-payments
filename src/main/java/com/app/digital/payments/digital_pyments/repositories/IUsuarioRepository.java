package com.app.digital.payments.digital_pyments.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.digital.payments.digital_pyments.models.Usuario;

@Repository
public interface IUsuarioRepository extends JpaRepository<Usuario,Long> {
    boolean existsByEmail(String email);
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
}