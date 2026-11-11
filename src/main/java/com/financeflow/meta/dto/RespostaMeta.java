package com.financeflow.goal.dto;

import com.financeflow.goal.StatusMeta;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RespostaMeta(
        UUID id,
        String nome,
        BigDecimal valorAlvo,
        BigDecimal valorAtual,
        LocalDate prazo,
        StatusMeta status,
        Instant criadoEm,
        Instant atualizadoEm) {
}
