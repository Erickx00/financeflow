package com.financeflow.relatorio;

import com.financeflow.comum.exception.ExcecaoRegraNegocio;
import com.financeflow.relatorio.dto.RespostaResumoFinanceiro;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String SUMMARY_JSON = """
            {"period":"2026-11","totalIncome":0.00,"totalExpenses":250.90,"balance":-250.90,"savingsRate":0.00}
            """;

    @Mock
    private GatewayRelatoriosComCache gateway;
    @Mock
    private CalculadoraRelatorio calculadora;
    @Mock
    private CodificadorJson codec;

    private RelatorioService servico;

    @BeforeEach
    void setUp() {
        servico = new RelatorioService(gateway, calculadora, codec);
    }

    @Test
    @DisplayName("mes invalido retorna erro de negocio, nunca 500")
    void rejectsInvalidMonth() {
        assertThatThrownBy(() -> servico.summary(USER_ID, "2026-13"))
                .isInstanceOf(ExcecaoRegraNegocio.class)
                .hasMessageContaining("YYYY-MM");
    }

    @Test
    @DisplayName("mes valido delega ao gateway e desserializa o JSON")
    void summaryDelegatesToCachedGateway() {
        when(gateway.summaryJson(USER_ID, YearMonth.of(2026, 11))).thenReturn(SUMMARY_JSON);
        when(codec.read(SUMMARY_JSON, RespostaResumoFinanceiro.class)).thenReturn(
                new RespostaResumoFinanceiro("2026-11", BigDecimal.ZERO, new BigDecimal("250.90"),
                        new BigDecimal("-250.90"), BigDecimal.ZERO));

        RespostaResumoFinanceiro response = servico.summary(USER_ID, "2026-11");

        assertThat(response.period()).isEqualTo("2026-11");
        assertThat(response.totalExpenses()).isEqualByComparingTo("250.90");
        verify(gateway).summaryJson(USER_ID, YearMonth.of(2026, 11));
    }

    @Test
    @DisplayName("intervalo invertido no fluxo de caixa e rejeitado")
    void rejectsInvertedCashFlowRange() {
        assertThatThrownBy(() -> servico.cashFlow(USER_ID,
                LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 1)))
                .isInstanceOf(ExcecaoRegraNegocio.class);
    }

    @Test
    @DisplayName("intervalo maior que 366 dias e rejeitado")
    void rejectsTooLongCashFlowRange() {
        assertThatThrownBy(() -> servico.cashFlow(USER_ID,
                LocalDate.of(2025, 1, 1), LocalDate.of(2026, 11, 1)))
                .isInstanceOf(ExcecaoRegraNegocio.class)
                .hasMessageContaining("366");
    }

    @Test
    @DisplayName("ano fora da faixa e rejeitado")
    void rejectsInvalidYear() {
        assertThatThrownBy(() -> servico.monthly(USER_ID, 1800))
                .isInstanceOf(ExcecaoRegraNegocio.class);
    }

    @Test
    @DisplayName("relatorio de categorias usa o tipo padrao EXPENSE")
    void categoriasUsesRequestedType() {
        when(gateway.categoriasJson(USER_ID, YearMonth.of(2026, 11), TipoTransacao.EXPENSE))
                .thenReturn("{}");
        when(codec.read("{}", com.financeflow.relatorio.dto.RespostaGastosCategoria.class))
                .thenReturn(new com.financeflow.relatorio.dto.RespostaGastosCategoria("2026-11",
                        TipoTransacao.EXPENSE, BigDecimal.ZERO, java.util.List.of()));

        var response = servico.categorias(USER_ID, "2026-11", TipoTransacao.EXPENSE);

        assertThat(response.tipo()).isEqualTo(TipoTransacao.EXPENSE);
        assertThat(response.categorias()).isEmpty();
    }
}
