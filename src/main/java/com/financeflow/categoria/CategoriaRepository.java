package com.financeflow.categoria;

import com.financeflow.transacao.TipoTransacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {

    List<Categoria> findByUsuarioIdOrderByNomeAsc(UUID usuarioId);

    List<Categoria> findByUsuarioIdAndTipoOrderByNomeAsc(UUID usuarioId, TipoTransacao tipo);

    Optional<Categoria> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    boolean existsByUsuarioIdAndNomeIgnoreCaseAndTipo(UUID usuarioId, String nome, TipoTransacao tipo);
}
