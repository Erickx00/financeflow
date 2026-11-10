package com.financeflow.categoria.dto;

import com.financeflow.transacao.TipoTransacao;

import java.time.Instant;
import java.util.UUID;

public record RespostaCategoria(UUID id, String nome, TipoTransacao tipo, String cor, Instant criadoEm) {
}
