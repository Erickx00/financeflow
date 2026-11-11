package com.financeflow.relatorio.dto;

import com.financeflow.transacao.TipoTransacao;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RespostaGastosCategoria(
        String period,
        TipoTransacao tipo,
        BigDecimal total,
        List<CategoryTotal> categorias) {

    public record CategoryTotal(
            UUID categoriaId,
            String nome,
            String cor,
            BigDecimal total,
            BigDecimal percentage) {
    }
}
