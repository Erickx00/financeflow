package com.financeflow.relatorio.dto;

import java.math.BigDecimal;
import java.util.List;

public record RespostaFluxoCaixa(
        String from,
        String to,
        List<CashFlowPoint> points,
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal net) {

    public record CashFlowPoint(String date, BigDecimal income, BigDecimal expenses, BigDecimal net) {
    }
}
