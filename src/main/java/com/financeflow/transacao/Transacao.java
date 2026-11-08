package com.financeflow.transacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "transacoes")
public class Transacao {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "conta_id", nullable = false)
    private UUID contaId;

    @Column(name = "categoria_id", nullable = false)
    private UUID categoriaId;

    @Column(nullable = false, length = 200)
    private String descricao;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoTransacao tipo;

    @Column(name = "data_transacao", nullable = false)
    private LocalDate dataTransacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusTransacao status;

    @Version
    @Column(nullable = false)
    private long versao;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Transacao() {
    }

    private Transacao(UUID usuarioId, UUID contaId, UUID categoriaId, String descricao, BigDecimal valor,
                        TipoTransacao tipo, LocalDate dataTransacao, StatusTransacao status, Instant now) {
        this.id = UUID.randomUUID();
        this.usuarioId = usuarioId;
        this.contaId = contaId;
        this.categoriaId = categoriaId;
        this.descricao = descricao;
        this.valor = valor;
        this.tipo = tipo;
        this.dataTransacao = dataTransacao;
        this.status = status;
        this.criadoEm = now;
        this.atualizadoEm = now;
    }

    public static Transacao registrar(UUID usuarioId, UUID contaId, UUID categoriaId, String descricao,
                                     BigDecimal valor, TipoTransacao tipo, LocalDate dataTransacao,
                                     StatusTransacao status) {
        return new Transacao(usuarioId, contaId, categoriaId, descricao, valor, tipo, dataTransacao, status,
                Instant.now());
    }

    public void atualizar(UUID contaId, UUID categoriaId, String descricao, BigDecimal valor,
                       TipoTransacao tipo, LocalDate dataTransacao) {
        this.contaId = contaId;
        this.categoriaId = categoriaId;
        this.descricao = descricao;
        this.valor = valor;
        this.tipo = tipo;
        this.dataTransacao = dataTransacao;
        this.atualizadoEm = Instant.now();
    }

    public void alterarStatus(StatusTransacao status) {
        this.status = status;
        this.atualizadoEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public UUID getContaId() {
        return contaId;
    }

    public UUID getCategoriaId() {
        return categoriaId;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }

    public LocalDate getDataTransacao() {
        return dataTransacao;
    }

    public StatusTransacao getStatus() {
        return status;
    }

    public long getVersao() {
        return versao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
