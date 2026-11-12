package com.financeflow.conta;

import com.financeflow.conta.dto.RespostaSaldoConta;
import com.financeflow.conta.dto.RequisicaoCriarConta;
import com.financeflow.comum.exception.ExcecaoRegraNegocio;
import com.financeflow.comum.exception.ExcecaoRecursoNaoEncontrado;
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
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContaServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private ContaRepository contas;
    @Mock
    private TransacaoRepository transacoes;

    private ContaService servico;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-13T10:00:00Z"), ZoneOffset.UTC);
        servico = new ContaService(contas, transacoes, clock);
    }

    @Test
    @DisplayName("moeda diferente de BRL e rejeitada no MVP")
    void rejectsUnsupportedCurrency() {
        RequisicaoCriarConta request = new RequisicaoCriarConta("Conta", TipoConta.CASH,
                BigDecimal.TEN, "USD");

        assertThatThrownBy(() -> servico.criar(USER_ID, request))
                .isInstanceOf(ExcecaoRegraNegocio.class)
                .hasMessageContaining("BRL");
        verify(contas, never()).save(any());
    }

    @Test
    @DisplayName("saldo soma saldo inicial e transacoes concluidas")
    void balanceAddsInitialBalanceAndTransactions() {
        Conta conta = Conta.abrir(USER_ID, "Conta", TipoConta.CHECKING, new BigDecimal("1000.00"));
        when(contas.findByIdAndUsuarioId(conta.getId(), USER_ID)).thenReturn(Optional.of(conta));
        when(transacoes.sumSignedAmount(eq(USER_ID), eq(conta.getId()),
                eq(StatusTransacao.COMPLETED), eq(TipoTransacao.INCOME), any()))
                .thenReturn(new BigDecimal("-250.90"));

        RespostaSaldoConta balance = servico.saldo(USER_ID, conta.getId(), null);

        assertThat(balance.saldo()).isEqualByComparingTo("749.10");
        assertThat(balance.asOf()).isEqualTo(LocalDate.of(2026, 11, 13));
        assertThat(balance.moeda()).isEqualTo("BRL");
    }

    @Test
    @DisplayName("saldo sem transacoes e o saldo inicial")
    void balanceWithoutTransactionsIsInitialBalance() {
        Conta conta = Conta.abrir(USER_ID, "Conta", TipoConta.SAVINGS, new BigDecimal("50.00"));
        when(contas.findByIdAndUsuarioId(conta.getId(), USER_ID)).thenReturn(Optional.of(conta));
        when(transacoes.sumSignedAmount(any(), any(), any(), any(), any())).thenReturn(null);

        RespostaSaldoConta balance = servico.saldo(USER_ID, conta.getId(),
                LocalDate.of(2026, 12, 31));

        assertThat(balance.saldo()).isEqualByComparingTo("50.00");
        assertThat(balance.asOf()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    @DisplayName("conta de outro usuario responde como inexistente")
    void rejectsAccountFromAnotherUser() {
        UUID contaId = UUID.randomUUID();
        when(contas.findByIdAndUsuarioId(contaId, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.get(USER_ID, contaId))
                .isInstanceOf(ExcecaoRecursoNaoEncontrado.class);
    }
}
