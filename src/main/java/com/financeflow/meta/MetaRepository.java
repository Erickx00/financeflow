package com.financeflow.goal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MetaRepository extends JpaRepository<Meta, UUID> {

    List<Meta> findByUsuarioIdOrderByCriadoEmDesc(UUID usuarioId);

    Optional<Meta> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    List<Meta> findByUsuarioIdAndStatusOrderByCriadoEmDesc(UUID usuarioId, StatusMeta status);
}
