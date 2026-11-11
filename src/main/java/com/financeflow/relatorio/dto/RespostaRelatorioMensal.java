package com.financeflow.relatorio.dto;

import java.math.BigDecimal;
import java.util.List;

public record RespostaRelatorioMensal(String year, List<MonthSummary> months) {

    public record MonthSummary(String month, BigDecimal totalIncome, BigDecimal totalExpenses, BigDecimal saldo) {
    }
}
