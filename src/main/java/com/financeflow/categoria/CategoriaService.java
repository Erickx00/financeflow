package com.financeflow.categoria;

import com.financeflow.categoria.dto.RespostaCategoria;
import com.financeflow.categoria.dto.RequisicaoCriarCategoria;
import com.financeflow.categoria.dto.RequisicaoAtualizarCategoria;
import com.financeflow.comum.exception.ExcecaoConflito;
import com.financeflow.comum.exception.ExcecaoRecursoNaoEncontrado;
import com.financeflow.transacao.TransacaoRepository;
import com.financeflow.transacao.TipoTransacao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoriaService {

    private final CategoriaRepository categories;
    private final TransacaoRepository transactions;

    public CategoriaService(CategoriaRepository categories, TransacaoRepository transactions) {
        this.categories = categories;
        this.transactions = transactions;
    }

    @Transactional
    public RespostaCategoria criar(UUID usuarioId, RequisicaoCriarCategoria request) {
        String nome = request.nome().trim();
        if (categories.existsByUsuarioIdAndNomeIgnoreCaseAndTipo(usuarioId, nome, request.tipo())) {
            throw new ExcecaoConflito("Ja existe uma categoria com esse nome e tipo");
        }
        Categoria categoria = Categoria.criar(usuarioId, nome, request.tipo(), request.cor());
        return CategoriaMapper.toResponse(categories.save(categoria));
    }

    @Transactional(readOnly = true)
    public List<RespostaCategoria> listar(UUID usuarioId, TipoTransacao tipo) {
        List<Categoria> found = tipo == null
                ? categories.findByUsuarioIdOrderByNomeAsc(usuarioId)
                : categories.findByUsuarioIdAndTipoOrderByNomeAsc(usuarioId, tipo);
        return found.stream().map(CategoriaMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RespostaCategoria get(UUID usuarioId, UUID categoriaId) {
        return CategoriaMapper.toResponse(carregar(usuarioId, categoriaId));
    }

    /**
     * O tipo e imutavel: mudar INCOME para EXPENSE invalidaria transacoes ja existentes.
     */
    @Transactional
    public RespostaCategoria atualizar(UUID usuarioId, UUID categoriaId, RequisicaoAtualizarCategoria request) {
        Categoria categoria = carregar(usuarioId, categoriaId);
        String nome = request.nome().trim();
        if (!nome.equalsIgnoreCase(categoria.getNome())
                && categories.existsByUsuarioIdAndNomeIgnoreCaseAndTipo(usuarioId, nome, categoria.getTipo())) {
            throw new ExcecaoConflito("Ja existe uma categoria com esse nome e tipo");
        }
        categoria.renomear(nome, request.cor());
        return CategoriaMapper.toResponse(categoria);
    }

    /**
     * Categoria em uso nunca e apagada: 409 para o cliente decidir (ADR 0004).
     */
    @Transactional
    public void excluir(UUID usuarioId, UUID categoriaId) {
        Categoria categoria = carregar(usuarioId, categoriaId);
        if (transactions.existsByCategoriaIdAndUsuarioId(categoriaId, usuarioId)) {
            throw new ExcecaoConflito("Categoria em uso por transacoes e nao pode ser excluida");
        }
        categories.delete(categoria);
    }

    private Categoria carregar(UUID usuarioId, UUID categoriaId) {
        return categories.findByIdAndUsuarioId(categoriaId, usuarioId)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Categoria"));
    }
}
