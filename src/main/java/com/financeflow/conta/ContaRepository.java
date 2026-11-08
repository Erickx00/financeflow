package com.financeflow.conta;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContaRepository extends JpaRepository<Conta, UUID> {

    List<Conta> findByUsuarioIdOrderByNomeAsc(UUID usuarioId);

    Optional<Conta> findByIdAndUsuarioId(UUID id, UUID usuarioId);
}
