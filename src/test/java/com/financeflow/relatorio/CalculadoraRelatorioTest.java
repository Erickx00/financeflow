package com.financeflow.relatorio;

import com.financeflow.relatorio.dto.RespostaFluxoCaixa;
import com.financeflow.relatorio.dto.RespostaGastosCategoria;
import com.financeflow.relatorio.dto.RespostaResumoFinanceiro;
import com.financeflow.relatorio.dto.RespostaRelatorioMensal;
import com.financeflow.transacao.TransacaoRepository;
import com.financeflow.transacao.StatusTransacao;
import com.financeflow.transacao.TipoTransacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculadoraRelatorioTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private TransacaoRepository transacoes;

    private CalculadoraRelatorio calculadora;

    @BeforeEach
    void setUp() {
        calculadora = new CalculadoraRelatorio(transacoes);
    }

    @Test
    @DisplayName("resumo calcula saldo e taxa de economia do mes")
    void summaryCalculatesBalanceAndSavingsRate() {
        when(transacoes.sumByTipo(eq(USER_ID), eq(StatusTransacao.COMPLETED), any(), any()))
                .thenReturn(List.of(
                        new Object[]{TipoTransacao.INCOME, new BigDecimal("3500.00")},
                        new Object[]{TipoTransacao.EXPENSE, new BigDecimal("1250.00")}));

        RespostaResumoFinanceiro summary = calculadora.summary(USER_ID, YearMonth.of(2026, 11));

        assertThat(summary.period()).isEqualTo("2026-11");
        assertThat(summary.totalIncome()).isEqualByComparingTo("3500.00");
        assertThat(summary.totalExpenses()).isEqualByComparingTo("1250.00");
        assertThat(summary.saldo()).isEqualByComparingTo("2250.00");
        assertThat(summary.savingsRate()).isEqualByComparingTo("64.29");
    }

    @Test
    @DisplayName("sem receita a taxa de economia e zero, nunca divisao por zero")
    void summaryWithoutIncomeUsesZeroRate() {
        when(transacoes.sumByTipo(any(), any(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{TipoTransacao.EXPENSE, new BigDecimal("200.00")}));

        RespostaResumoFinanceiro summary = calculadora.summary(USER_ID, YearMonth.of(2026, 11));

        assertThat(summary.savingsRate()).isEqualByComparingTo("0.00");
        assertThat(summary.saldo()).isEqualByComparingTo("-200.00");
    }

    @Test
    @DisplayName("mes sem transacoes retorna zeros")
    void summaryWithoutTransactionsReturnsZeros() {
        when(transacoes.sumByTipo(any(), any(), any(), any())).thenReturn(List.<Object[]>of());

        RespostaResumoFinanceiro summary = calculadora.summary(USER_ID, YearMonth.of(2026, 1));

        assertThat(summary.totalIncome()).isEqualByComparingTo("0.00");
        assertThat(summary.totalExpenses()).isEqualByComparingTo("0.00");
        assertThat(summary.saldo()).isEqualByComparingTo("0.00");
        assertThat(summary.savingsRate()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("gastos por categoria calculam total e percentual")
    void categoriasCalculateTotalsAndPercentages() {
        UUID mercado = UUID.randomUUID();
        UUID transporte = UUID.randomUUID();
        when(transacoes.sumByCategory(eq(USER_ID), eq(StatusTransacao.COMPLETED),
                eq(TipoTransacao.EXPENSE), any(), any()))
                .thenReturn(List.of(
                        new Object[]{mercado, "Mercado", "#FF0000", new BigDecimal("250.00")},
                        new Object[]{transporte, "Transporte", null, new BigDecimal("750.00")}));

        RespostaGastosCategoria response = calculadora.categorias(USER_ID, YearMonth.of(2026, 11),
                TipoTransacao.EXPENSE);

        assertThat(response.total()).isEqualByComparingTo("1000.00");
        assertThat(response.categorias()).hasSize(2);
        assertThat(response.categorias().get(0).percentage()).isEqualByComparingTo("25.00");
        assertThat(response.categorias().get(1).percentage()).isEqualByComparingTo("75.00");
    }

    @Test
    @DisplayName("fluxo de caixa agrega por dia e calcula totais")
    void cashFlowAggregatesByDay() {
        when(transacoes.sumByDay(eq(USER_ID), eq(StatusTransacao.COMPLETED), any(), any()))
                .thenReturn(List.of(
                        new Object[]{LocalDate.of(2026, 11, 1), TipoTransacao.INCOME, new BigDecimal("100.00")},
                        new Object[]{LocalDate.of(2026, 11, 1), TipoTransacao.EXPENSE, new BigDecimal("40.00")},
                        new Object[]{LocalDate.of(2026, 11, 3), TipoTransacao.EXPENSE, new BigDecimal("10.00")}));

        RespostaFluxoCaixa response = calculadora.cashFlow(USER_ID, LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30));

        assertThat(response.points()).hasSize(2);
        assertThat(response.points().get(0).date()).isEqualTo("2026-11-01");
        assertThat(response.points().get(0).net()).isEqualByComparingTo("60.00");
        assertThat(response.points().get(1).net()).isEqualByComparingTo("-10.00");
        assertThat(response.totalIncome()).isEqualByComparingTo("100.00");
        assertThat(response.totalExpenses()).isEqualByComparingTo("50.00");
        assertThat(response.net()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("relatorio mensal preenche os 12 meses, inclusive os vazios")
    void monthlyFillsAllMonths() {
        when(transacoes.sumByMonth(eq(USER_ID), eq(StatusTransacao.COMPLETED), eq(2026)))
                .thenReturn(List.of(
                        new Object[]{11, TipoTransacao.INCOME, new BigDecimal("100.00")},
                        new Object[]{11, TipoTransacao.EXPENSE, new BigDecimal("40.00")}));

        RespostaRelatorioMensal response = calculadora.monthly(USER_ID, 2026);

        assertThat(response.months()).hasSize(12);
        assertThat(response.months().get(0).month()).isEqualTo("2026-01");
        assertThat(response.months().get(0).saldo()).isEqualByComparingTo("0.00");
        assertThat(response.months().get(10).month()).isEqualTo("2026-11");
        assertThat(response.months().get(10).saldo()).isEqualByComparingTo("60.00");
    }
}
