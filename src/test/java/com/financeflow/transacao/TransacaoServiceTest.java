package com.financeflow.transacao;

import com.financeflow.conta.Conta;
import com.financeflow.conta.ContaRepository;
import com.financeflow.conta.TipoConta;
import com.financeflow.categoria.Categoria;
import com.financeflow.categoria.CategoriaRepository;
import com.financeflow.comum.exception.ExcecaoRegraNegocio;
import com.financeflow.comum.exception.ExcecaoConflito;
import com.financeflow.comum.exception.ExcecaoRecursoNaoEncontrado;
import com.financeflow.transacao.dto.RequisicaoCriarTransacao;
import com.financeflow.transacao.dto.RespostaTransacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
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
class TransacaoServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();

    @Mock
    private TransacaoRepository transacoes;
    @Mock
    private ContaRepository contas;
    @Mock
    private CategoriaRepository categorias;
    @Mock
    private ApplicationEventPublisher eventos;

    private TransacaoService servico;

    @BeforeEach
    void setUp() {
        servico = new TransacaoService(transacoes, contas, categorias, eventos);
    }

    @Test
    @DisplayName("rejeita transacao em conta inativa")
    void rejectsInactiveAccount() {
        Conta conta = Conta.abrir(USER_ID, "Conta", TipoConta.CHECKING, BigDecimal.TEN);
        conta.alterarAtiva(false);
        when(contas.findByIdAndUsuarioId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(conta));

        assertThatThrownBy(() -> servico.criar(USER_ID, request(TipoTransacao.EXPENSE,
                StatusTransacao.COMPLETED)))
                .isInstanceOf(ExcecaoRegraNegocio.class)
                .hasMessageContaining("inativa");
        verify(transacoes, never()).save(any());
    }

    @Test
    @DisplayName("rejeita categoria de tipo incompativel com a transacao")
    void rejectsCategoryTypeMismatch() {
        when(contas.findByIdAndUsuarioId(ACCOUNT_ID, USER_ID))
                .thenReturn(Optional.of(Conta.abrir(USER_ID, "Conta", TipoConta.CASH, BigDecimal.ZERO)));
        when(categorias.findByIdAndUsuarioId(CATEGORY_ID, USER_ID))
                .thenReturn(Optional.of(Categoria.criar(USER_ID, "Salario", TipoTransacao.INCOME, null)));

        assertThatThrownBy(() -> servico.criar(USER_ID, request(TipoTransacao.EXPENSE,
                StatusTransacao.COMPLETED)))
                .isInstanceOf(ExcecaoRegraNegocio.class)
                .hasMessageContaining("nao pode ser usada");
    }

    @Test
    @DisplayName("conta de outro usuario responde como inexistente")
    void rejectsAccountFromAnotherUser() {
        when(contas.findByIdAndUsuarioId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.criar(USER_ID, request(TipoTransacao.EXPENSE,
                StatusTransacao.PENDING)))
                .isInstanceOf(ExcecaoRecursoNaoEncontrado.class);
    }

    @Test
    @DisplayName("transacao concluida publica evento de invalidacao de cache")
    void completedTransactionPublishesEvent() {
        stubOwnedAccountAndCategory();
        when(transacoes.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RespostaTransacao response = servico.criar(USER_ID,
                request(TipoTransacao.EXPENSE, StatusTransacao.COMPLETED));

        assertThat(response.status()).isEqualTo(StatusTransacao.COMPLETED);
        verify(eventos).publishEvent(new EventoTransacaoAlterada(USER_ID));
    }

    @Test
    @DisplayName("transacao pendente nao invalida relatorios")
    void pendingTransactionDoesNotPublishEvent() {
        stubOwnedAccountAndCategory();
        when(transacoes.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        servico.criar(USER_ID, request(TipoTransacao.EXPENSE, StatusTransacao.PENDING));

        verify(eventos, never()).publishEvent(any());
    }

    @Test
    @DisplayName("mudanca de status invalida o cache uma unica vez")
    void statusChangeInvalidatesCache() {
        Transacao transacao = Transacao.registrar(USER_ID, ACCOUNT_ID, CATEGORY_ID, "Compra",
                new BigDecimal("10.00"), TipoTransacao.EXPENSE, LocalDate.now(), StatusTransacao.PENDING);
        when(transacoes.findByIdAndUsuarioId(any(), eq(USER_ID))).thenReturn(Optional.of(transacao));

        servico.alterarStatus(USER_ID, transacao.getId(), StatusTransacao.COMPLETED);

        assertThat(transacao.getStatus()).isEqualTo(StatusTransacao.COMPLETED);
        verify(eventos).publishEvent(new EventoTransacaoAlterada(USER_ID));
    }

    @Test
    @DisplayName("PATCH com o mesmo status e idempotente e nao invalida o cache")
    void sameStatusIsIdempotent() {
        Transacao transacao = Transacao.registrar(USER_ID, ACCOUNT_ID, CATEGORY_ID, "Compra",
                new BigDecimal("10.00"), TipoTransacao.EXPENSE, LocalDate.now(), StatusTransacao.COMPLETED);
        when(transacoes.findByIdAndUsuarioId(any(), eq(USER_ID))).thenReturn(Optional.of(transacao));

        servico.alterarStatus(USER_ID, transacao.getId(), StatusTransacao.COMPLETED);

        verify(eventos, never()).publishEvent(any());
    }

    private void stubOwnedAccountAndCategory() {
        when(contas.findByIdAndUsuarioId(ACCOUNT_ID, USER_ID))
                .thenReturn(Optional.of(Conta.abrir(USER_ID, "Conta", TipoConta.CHECKING, BigDecimal.ZERO)));
        when(categorias.findByIdAndUsuarioId(CATEGORY_ID, USER_ID))
                .thenReturn(Optional.of(Categoria.criar(USER_ID, "Mercado", TipoTransacao.EXPENSE, null)));
    }

    private RequisicaoCriarTransacao request(TipoTransacao tipo, StatusTransacao status) {
        return new RequisicaoCriarTransacao(ACCOUNT_ID, CATEGORY_ID, "Compra", new BigDecimal("25.90"),
                tipo, LocalDate.of(2026, 11, 13), status);
    }
}
