package com.financeflow.goal;

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
@Table(name = "metas")
public class Meta {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "valor_alvo", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorAlvo;

    @Column(name = "valor_atual", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorAtual;

    @Column
    private LocalDate prazo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusMeta status;

    @Version
    @Column(nullable = false)
    private long versao;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Meta() {
    }

    private Meta(UUID usuarioId, String nome, BigDecimal valorAlvo, LocalDate prazo, Instant now) {
        this.id = UUID.randomUUID();
        this.usuarioId = usuarioId;
        this.nome = nome;
        this.valorAlvo = valorAlvo;
        this.valorAtual = BigDecimal.ZERO.setScale(2);
        this.prazo = prazo;
        this.status = StatusMeta.ACTIVE;
        this.criadoEm = now;
        this.atualizadoEm = now;
    }

    public static Meta criar(UUID usuarioId, String nome, BigDecimal valorAlvo, LocalDate prazo) {
        return new Meta(usuarioId, nome, valorAlvo, prazo, Instant.now());
    }

    public void updateDetails(String nome, BigDecimal valorAlvo, LocalDate prazo) {
        this.nome = nome;
        this.valorAlvo = valorAlvo;
        this.prazo = prazo;
        if (status != StatusMeta.CANCELLED) {
            // Elevar o alvo reabre a meta; reduzir abaixo do acumulado conclui.
            this.status = valorAtual.compareTo(valorAlvo) >= 0 ? StatusMeta.COMPLETED : StatusMeta.ACTIVE;
        }
        this.atualizadoEm = Instant.now();
    }

    public void contribuir(BigDecimal valor) {
        this.valorAtual = this.valorAtual.add(valor);
        if (valorAtual.compareTo(valorAlvo) >= 0) {
            this.status = StatusMeta.COMPLETED;
        }
        this.atualizadoEm = Instant.now();
    }

    public void cancelar() {
        this.status = StatusMeta.CANCELLED;
        this.atualizadoEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getValorAlvo() {
        return valorAlvo;
    }

    public BigDecimal getValorAtual() {
        return valorAtual;
    }

    public LocalDate getPrazo() {
        return prazo;
    }

    public StatusMeta getStatus() {
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
