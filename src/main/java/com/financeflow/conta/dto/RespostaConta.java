package com.financeflow.conta.dto;

import com.financeflow.conta.TipoConta;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RespostaConta(
        UUID id,
        String nome,
        TipoConta tipo,
        BigDecimal saldoInicial,
        String moeda,
        boolean ativa,
        Instant criadoEm) {
}
