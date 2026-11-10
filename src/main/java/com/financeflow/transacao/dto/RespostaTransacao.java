package com.financeflow.transacao.dto;

import com.financeflow.transacao.StatusTransacao;
import com.financeflow.transacao.TipoTransacao;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RespostaTransacao(
        UUID id,
        UUID contaId,
        UUID categoriaId,
        String descricao,
        BigDecimal valor,
        TipoTransacao tipo,
        LocalDate dataTransacao,
        StatusTransacao status,
        Instant criadoEm,
        Instant atualizadoEm) {
}
