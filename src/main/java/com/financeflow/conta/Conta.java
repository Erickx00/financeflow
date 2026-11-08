package com.financeflow.conta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contas")
public class Conta {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(nullable = false, length = 100)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoConta tipo;

    @Column(name = "saldo_inicial", nullable = false, precision = 19, scale = 2)
    private BigDecimal saldoInicial;

    @Column(nullable = false, length = 3)
    private String moeda;

    @Column(nullable = false)
    private boolean ativa;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Conta() {
    }

    private Conta(UUID usuarioId, String nome, TipoConta tipo, BigDecimal saldoInicial, Instant now) {
        this.id = UUID.randomUUID();
        this.usuarioId = usuarioId;
        this.nome = nome;
        this.tipo = tipo;
        this.saldoInicial = saldoInicial;
        this.moeda = "BRL";
        this.ativa = true;
        this.criadoEm = now;
        this.atualizadoEm = now;
    }

    public static Conta abrir(UUID usuarioId, String nome, TipoConta tipo, BigDecimal saldoInicial) {
        return new Conta(usuarioId, nome, tipo, saldoInicial, Instant.now());
    }

    public void renomear(String nome, TipoConta tipo) {
        this.nome = nome;
        this.tipo = tipo;
        this.atualizadoEm = Instant.now();
    }

    public void alterarAtiva(boolean ativa) {
        this.ativa = ativa;
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

    public TipoConta getTipo() {
        return tipo;
    }

    public BigDecimal getSaldoInicial() {
        return saldoInicial;
    }

    public String getMoeda() {
        return moeda;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
