package com.financeflow.relatorio.dto;

import java.math.BigDecimal;

public record RespostaResumoFinanceiro(
        String period,
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal saldo,
        BigDecimal savingsRate) {
}
