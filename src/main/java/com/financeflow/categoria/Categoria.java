package com.financeflow.categoria;

import com.financeflow.transacao.TipoTransacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "categorias")
public class Categoria {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(nullable = false, length = 80)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoTransacao tipo;

    @Column(length = 7)
    private String cor;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Categoria() {
    }

    private Categoria(UUID usuarioId, String nome, TipoTransacao tipo, String cor, Instant now) {
        this.id = UUID.randomUUID();
        this.usuarioId = usuarioId;
        this.nome = nome;
        this.tipo = tipo;
        this.cor = cor;
        this.criadoEm = now;
        this.atualizadoEm = now;
    }

    public static Categoria criar(UUID usuarioId, String nome, TipoTransacao tipo, String cor) {
        return new Categoria(usuarioId, nome, tipo, cor, Instant.now());
    }

    public void renomear(String nome, String cor) {
        this.nome = nome;
        this.cor = cor;
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

    public TipoTransacao getTipo() {
        return tipo;
    }

    public String getCor() {
        return cor;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
