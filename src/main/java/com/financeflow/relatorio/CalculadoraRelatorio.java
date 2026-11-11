package com.financeflow.relatorio;

import com.financeflow.relatorio.dto.RespostaFluxoCaixa;
import com.financeflow.relatorio.dto.RespostaGastosCategoria;
import com.financeflow.relatorio.dto.RespostaResumoFinanceiro;
import com.financeflow.relatorio.dto.RespostaRelatorioMensal;
import com.financeflow.transacao.TransacaoRepository;
import com.financeflow.transacao.StatusTransacao;
import com.financeflow.transacao.TipoTransacao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Calcula os relatorios a partir das transacoes concluidas. Nao conhece cache:
 * essa responsabilidade fica no gateway (ADR 0003).
 */
@Service
public class CalculadoraRelatorio {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final TransacaoRepository transactions;

    public CalculadoraRelatorio(TransacaoRepository transactions) {
        this.transactions = transactions;
    }

    @Transactional(readOnly = true)
    public RespostaResumoFinanceiro summary(UUID usuarioId, YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        Totals totals = totals(usuarioId, from, to);
        BigDecimal balance = totals.income().subtract(totals.expenses()).setScale(2, RoundingMode.HALF_UP);
        return new RespostaResumoFinanceiro(month.toString(), totals.income(), totals.expenses(), balance,
                savingsRate(totals.income(), totals.expenses()));
    }

    @Transactional(readOnly = true)
    public RespostaRelatorioMensal monthly(UUID usuarioId, int year) {
        Map<Integer, BigDecimal[]> byMonth = new HashMap<>();
        for (Object[] row : transactions.sumByMonth(usuarioId, StatusTransacao.COMPLETED, year)) {
            int month = ((Number) row[0]).intValue();
            TipoTransacao tipo = (TipoTransacao) row[1];
            BigDecimal value = money(row[2]);
            BigDecimal[] values = byMonth.computeIfAbsent(month, key -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if (tipo == TipoTransacao.INCOME) {
                values[0] = values[0].add(value);
            } else {
                values[1] = values[1].add(value);
            }
        }

        List<RespostaRelatorioMensal.MonthSummary> months = new ArrayList<>(12);
        for (int month = 1; month <= 12; month++) {
            BigDecimal[] values = byMonth.getOrDefault(month,
                    new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal income = values[0].setScale(2, RoundingMode.HALF_UP);
            BigDecimal expenses = values[1].setScale(2, RoundingMode.HALF_UP);
            months.add(new RespostaRelatorioMensal.MonthSummary(
                    YearMonth.of(year, month).toString(), income, expenses,
                    income.subtract(expenses).setScale(2, RoundingMode.HALF_UP)));
        }
        return new RespostaRelatorioMensal(String.valueOf(year), months);
    }

    @Transactional(readOnly = true)
    public RespostaGastosCategoria categorias(UUID usuarioId, YearMonth month, TipoTransacao tipo) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        List<Object[]> rows = transactions.sumByCategory(usuarioId, StatusTransacao.COMPLETED, tipo, from, to);

        BigDecimal total = rows.stream()
                .map(row -> money(row[3]))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        List<RespostaGastosCategoria.CategoryTotal> categories = rows.stream()
                .map(row -> new RespostaGastosCategoria.CategoryTotal(
                        (UUID) row[0],
                        (String) row[1],
                        (String) row[2],
                        money(row[3]),
                        percentage(money(row[3]), total)))
                .toList();

        return new RespostaGastosCategoria(month.toString(), tipo, total, categories);
    }

    @Transactional(readOnly = true)
    public RespostaFluxoCaixa cashFlow(UUID usuarioId, LocalDate from, LocalDate to) {
        Map<LocalDate, BigDecimal[]> byDay = new LinkedHashMap<>();
        for (Object[] row : transactions.sumByDay(usuarioId, StatusTransacao.COMPLETED, from, to)) {
            LocalDate date = (LocalDate) row[0];
            TipoTransacao tipo = (TipoTransacao) row[1];
            BigDecimal value = money(row[2]);
            BigDecimal[] values = byDay.computeIfAbsent(date, key -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if (tipo == TipoTransacao.INCOME) {
                values[0] = values[0].add(value);
            } else {
                values[1] = values[1].add(value);
            }
        }

        List<RespostaFluxoCaixa.CashFlowPoint> points = byDay.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .map(entry -> {
                    BigDecimal income = entry.getValue()[0].setScale(2, RoundingMode.HALF_UP);
                    BigDecimal expenses = entry.getValue()[1].setScale(2, RoundingMode.HALF_UP);
                    return new RespostaFluxoCaixa.CashFlowPoint(
                            entry.getKey().toString(), income, expenses,
                            income.subtract(expenses).setScale(2, RoundingMode.HALF_UP));
                })
                .toList();

        BigDecimal totalIncome = points.stream()
                .map(RespostaFluxoCaixa.CashFlowPoint::income)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalExpenses = points.stream()
                .map(RespostaFluxoCaixa.CashFlowPoint::expenses)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new RespostaFluxoCaixa(from.toString(), to.toString(), points, totalIncome, totalExpenses,
                totalIncome.subtract(totalExpenses).setScale(2, RoundingMode.HALF_UP));
    }

    private Totals totals(UUID usuarioId, LocalDate from, LocalDate to) {
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;
        for (Object[] row : transactions.sumByTipo(usuarioId, StatusTransacao.COMPLETED, from, to)) {
            TipoTransacao tipo = (TipoTransacao) row[0];
            BigDecimal value = money(row[1]);
            if (tipo == TipoTransacao.INCOME) {
                income = income.add(value);
            } else {
                expenses = expenses.add(value);
            }
        }
        return new Totals(income.setScale(2, RoundingMode.HALF_UP), expenses.setScale(2, RoundingMode.HALF_UP));
    }

    private BigDecimal savingsRate(BigDecimal income, BigDecimal expenses) {
        if (income.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return income.subtract(expenses)
                .multiply(HUNDRED)
                .divide(income, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentage(BigDecimal value, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.multiply(HUNDRED).divide(total, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal money(Object value) {
        return value == null ? BigDecimal.ZERO : ((BigDecimal) value).setScale(2, RoundingMode.HALF_UP);
    }

    private record Totals(BigDecimal income, BigDecimal expenses) {
    }
}
