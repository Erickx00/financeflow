package com.financeflow.transacao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransacaoRepository
        extends JpaRepository<Transacao, UUID>, JpaSpecificationExecutor<Transacao> {

    Optional<Transacao> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    boolean existsByCategoriaIdAndUsuarioId(UUID categoriaId, UUID usuarioId);

    boolean existsByContaIdAndUsuarioIdAndStatus(UUID contaId, UUID usuarioId, StatusTransacao status);

    /**
     * Saldo assinado (receitas - despesas) das transacoes concluidas ate uma data.
     * Somente transacoes COMPLETED entram no calculo (ADR 0006).
     */
    @Query("""
            select sum(case when t.tipo = :income then t.valor else -t.valor end)
            from Transacao t
            where t.usuarioId = :usuarioId
              and t.contaId = :contaId
              and t.status = :completed
              and t.dataTransacao <= :asOf
            """)
    BigDecimal sumSignedAmount(
            @Param("usuarioId") UUID usuarioId,
            @Param("contaId") UUID contaId,
            @Param("completed") StatusTransacao completed,
            @Param("income") TipoTransacao income,
            @Param("asOf") LocalDate asOf);

    @Query("""
            select t.tipo, sum(t.valor)
            from Transacao t
            where t.usuarioId = :usuarioId
              and t.status = :status
              and t.dataTransacao between :from and :to
            group by t.tipo
            """)
    List<Object[]> sumByTipo(
            @Param("usuarioId") UUID usuarioId,
            @Param("status") StatusTransacao status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            select t.dataTransacao, t.tipo, sum(t.valor)
            from Transacao t
            where t.usuarioId = :usuarioId
              and t.status = :status
              and t.dataTransacao between :from and :to
            group by t.dataTransacao, t.tipo
            order by t.dataTransacao
            """)
    List<Object[]> sumByDay(
            @Param("usuarioId") UUID usuarioId,
            @Param("status") StatusTransacao status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            select c.id, c.nome, c.cor, sum(t.valor)
            from Transacao t, Categoria c
            where c.id = t.categoriaId
              and t.usuarioId = :usuarioId
              and t.status = :status
              and t.tipo = :tipo
              and t.dataTransacao between :from and :to
            group by c.id, c.nome, c.cor
            order by sum(t.valor) desc
            """)
    List<Object[]> sumByCategory(
            @Param("usuarioId") UUID usuarioId,
            @Param("status") StatusTransacao status,
            @Param("tipo") TipoTransacao tipo,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            select month(t.dataTransacao), t.tipo, sum(t.valor)
            from Transacao t
            where t.usuarioId = :usuarioId
              and t.status = :status
              and year(t.dataTransacao) = :year
            group by month(t.dataTransacao), t.tipo
            """)
    List<Object[]> sumByMonth(
            @Param("usuarioId") UUID usuarioId,
            @Param("status") StatusTransacao status,
            @Param("year") int year);
}
