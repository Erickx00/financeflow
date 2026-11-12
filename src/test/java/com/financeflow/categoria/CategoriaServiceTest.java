package com.financeflow.categoria;

import com.financeflow.categoria.dto.RequisicaoCriarCategoria;
import com.financeflow.categoria.dto.RequisicaoAtualizarCategoria;
import com.financeflow.comum.exception.ExcecaoConflito;
import com.financeflow.transacao.TransacaoRepository;
import com.financeflow.transacao.TipoTransacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private CategoriaRepository categorias;
    @Mock
    private TransacaoRepository transacoes;

    private CategoriaService servico;

    @BeforeEach
    void setUp() {
        servico = new CategoriaService(categorias, transacoes);
    }

    @Test
    @DisplayName("nome repetido para o mesmo tipo e rejeitado")
    void rejectsDuplicateNameForSameType() {
        when(categorias.existsByUsuarioIdAndNomeIgnoreCaseAndTipo(USER_ID, "Mercado", TipoTransacao.EXPENSE))
                .thenReturn(true);

        assertThatThrownBy(() -> servico.criar(USER_ID,
                new RequisicaoCriarCategoria("Mercado", TipoTransacao.EXPENSE, null)))
                .isInstanceOf(ExcecaoConflito.class);
        verify(categorias, never()).save(any());
    }

    @Test
    @DisplayName("atualizar nome e cor preserva o tipo da categoria")
    void updateKeepsCategoryType() {
        Categoria categoria = Categoria.criar(USER_ID, "Mercado", TipoTransacao.EXPENSE, "#FFFFFF");
        when(categorias.findByIdAndUsuarioId(categoria.getId(), USER_ID)).thenReturn(Optional.of(categoria));
        when(categorias.existsByUsuarioIdAndNomeIgnoreCaseAndTipo(USER_ID, "Feira", TipoTransacao.EXPENSE))
                .thenReturn(false);

        var response = servico.atualizar(USER_ID, categoria.getId(),
                new RequisicaoAtualizarCategoria("Feira", "#000000"));

        assertThat(response.nome()).isEqualTo("Feira");
        assertThat(response.tipo()).isEqualTo(TipoTransacao.EXPENSE);
        assertThat(response.cor()).isEqualTo("#000000");
    }

    @Test
    @DisplayName("categoria em uso nao pode ser excluida")
    void refusesToDeleteCategoryInUse() {
        Categoria categoria = Categoria.criar(USER_ID, "Mercado", TipoTransacao.EXPENSE, null);
        when(categorias.findByIdAndUsuarioId(categoria.getId(), USER_ID)).thenReturn(Optional.of(categoria));
        when(transacoes.existsByCategoriaIdAndUsuarioId(categoria.getId(), USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> servico.excluir(USER_ID, categoria.getId()))
                .isInstanceOf(ExcecaoConflito.class)
                .hasMessageContaining("em uso");
        verify(categorias, never()).delete(any());
    }

    @Test
    @DisplayName("categoria sem uso e excluida")
    void deletesUnusedCategory() {
        Categoria categoria = Categoria.criar(USER_ID, "Lazer", TipoTransacao.EXPENSE, null);
        when(categorias.findByIdAndUsuarioId(categoria.getId(), USER_ID)).thenReturn(Optional.of(categoria));
        when(transacoes.existsByCategoriaIdAndUsuarioId(categoria.getId(), USER_ID)).thenReturn(false);

        servico.excluir(USER_ID, categoria.getId());

        verify(categorias).delete(categoria);
    }
}
