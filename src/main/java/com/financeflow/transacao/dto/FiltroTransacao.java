package com.financeflow.transacao.dto;

import com.financeflow.transacao.StatusTransacao;
import com.financeflow.transacao.TipoTransacao;

import java.time.LocalDate;
import java.util.UUID;

public record FiltroTransacao(
        TipoTransacao tipo,
        StatusTransacao status,
        UUID contaId,
        UUID categoriaId,
        LocalDate dataInicio,
        LocalDate dataFim) {
}
