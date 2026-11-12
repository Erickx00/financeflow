package com.financeflow.goal;

import com.financeflow.comum.exception.ExcecaoConflito;
import com.financeflow.goal.dto.RequisicaoAporteMeta;
import com.financeflow.goal.dto.RespostaMeta;
import com.financeflow.goal.dto.RequisicaoAtualizarMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetaServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private MetaRepository metas;

    private MetaService servico;

    @BeforeEach
    void setUp() {
        servico = new MetaService(metas);
    }

    @Test
    @DisplayName("contribuicao que atinge o alvo conclui a meta")
    void contributionReachingTargetCompletesGoal() {
        Meta meta = Meta.criar(USER_ID, "Notebook", new BigDecimal("5000.00"), null);
        when(metas.findByIdAndUsuarioId(meta.getId(), USER_ID)).thenReturn(Optional.of(meta));

        RespostaMeta response = servico.contribuir(USER_ID, meta.getId(),
                new RequisicaoAporteMeta(new BigDecimal("5000.00")));

        assertThat(response.status()).isEqualTo(StatusMeta.COMPLETED);
        assertThat(response.valorAtual()).isEqualByComparingTo("5000.00");
    }

    @Test
    @DisplayName("contribuicao parcial mantem a meta ativa")
    void partialContributionKeepsGoalActive() {
        Meta meta = Meta.criar(USER_ID, "Notebook", new BigDecimal("5000.00"), null);
        when(metas.findByIdAndUsuarioId(meta.getId(), USER_ID)).thenReturn(Optional.of(meta));

        RespostaMeta response = servico.contribuir(USER_ID, meta.getId(),
                new RequisicaoAporteMeta(new BigDecimal("300.00")));

        assertThat(response.status()).isEqualTo(StatusMeta.ACTIVE);
        assertThat(response.valorAtual()).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("meta cancelada nao aceita contribuicao")
    void cancelledGoalRejectsContribution() {
        Meta meta = Meta.criar(USER_ID, "Viagem", new BigDecimal("1000.00"), null);
        meta.cancelar();
        when(metas.findByIdAndUsuarioId(meta.getId(), USER_ID)).thenReturn(Optional.of(meta));

        assertThatThrownBy(() -> servico.contribuir(USER_ID, meta.getId(),
                new RequisicaoAporteMeta(BigDecimal.TEN)))
                .isInstanceOf(ExcecaoConflito.class);
    }

    @Test
    @DisplayName("meta com aportes nao pode ser excluida")
    void goalWithContributionsCannotBeDeleted() {
        Meta meta = Meta.criar(USER_ID, "Viagem", new BigDecimal("1000.00"), null);
        meta.contribuir(new BigDecimal("50.00"));
        when(metas.findByIdAndUsuarioId(meta.getId(), USER_ID)).thenReturn(Optional.of(meta));

        assertThatThrownBy(() -> servico.excluir(USER_ID, meta.getId()))
                .isInstanceOf(ExcecaoConflito.class);
        verify(metas, never()).delete(any());
    }

    @Test
    @DisplayName("reduzir o alvo abaixo do acumulado conclui a meta")
    void loweringTargetBelowCurrentCompletesGoal() {
        Meta meta = Meta.criar(USER_ID, "Curso", new BigDecimal("1000.00"), null);
        meta.contribuir(new BigDecimal("800.00"));
        when(metas.findByIdAndUsuarioId(meta.getId(), USER_ID)).thenReturn(Optional.of(meta));

        RespostaMeta response = servico.atualizar(USER_ID, meta.getId(),
                new RequisicaoAtualizarMeta("Curso", new BigDecimal("600.00"), LocalDate.now().plusMonths(2)));

        assertThat(response.status()).isEqualTo(StatusMeta.COMPLETED);
    }
}
