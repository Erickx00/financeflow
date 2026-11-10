package com.financeflow.transacao;

import com.financeflow.conta.Conta;
import com.financeflow.conta.ContaRepository;
import com.financeflow.categoria.Categoria;
import com.financeflow.categoria.CategoriaRepository;
import com.financeflow.comum.exception.ExcecaoRegraNegocio;
import com.financeflow.comum.exception.ExcecaoConflito;
import com.financeflow.comum.exception.ExcecaoRecursoNaoEncontrado;
import com.financeflow.comum.response.RespostaPaginada;
import com.financeflow.transacao.dto.RequisicaoCriarTransacao;
import com.financeflow.transacao.dto.FiltroTransacao;
import com.financeflow.transacao.dto.RespostaTransacao;
import com.financeflow.transacao.dto.RequisicaoAtualizarTransacao;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.UUID;

@Service
public class TransacaoService {

    private static final Set<String> SORTABLE = Set.of("dataTransacao", "valor", "criadoEm", "descricao");
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final TransacaoRepository transactions;
    private final ContaRepository accounts;
    private final CategoriaRepository categories;
    private final ApplicationEventPublisher events;

    public TransacaoService(TransacaoRepository transactions, ContaRepository accounts,
                              CategoriaRepository categories, ApplicationEventPublisher events) {
        this.transactions = transactions;
        this.accounts = accounts;
        this.categories = categories;
        this.events = events;
    }

    @Transactional
    public RespostaTransacao criar(UUID usuarioId, RequisicaoCriarTransacao request) {
        Conta conta = requireActiveAccount(usuarioId, request.contaId());
        requireMatchingCategory(usuarioId, request.categoriaId(), request.tipo());

        Transacao transacao = Transacao.registrar(
                usuarioId,
                conta.getId(),
                request.categoriaId(),
                request.descricao().trim(),
                money(request.valor()),
                request.tipo(),
                request.dataTransacao(),
                request.status());
        Transacao saved = transactions.save(transacao);
        if (saved.getStatus() == StatusTransacao.COMPLETED) {
            publishChange(usuarioId);
        }
        return TransacaoMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public RespostaPaginada<RespostaTransacao> listar(UUID usuarioId, FiltroTransacao filtro, int pagina, int tamanho,
                                                  String ordenarPor, String direcao) {
        if (filtro.startDate() != null && filtro.endDate() != null
                && filtro.startDate().isAfter(filtro.endDate())) {
            throw new ExcecaoRegraNegocio("A data inicial nao pode ser posterior a data final");
        }
        if (pagina < 0) {
            throw new ExcecaoRegraNegocio("A pagina deve ser maior ou igual a zero");
        }
        int tamanhoSeguro = tamanho <= 0 ? DEFAULT_PAGE_SIZE : Math.min(tamanho, MAX_PAGE_SIZE);
        String propriedade = ordenarPor == null || ordenarPor.isBlank() ? "dataTransacao" : ordenarPor;
        if (!SORTABLE.contains(propriedade)) {
            throw new ExcecaoRegraNegocio("Ordenacao nao permitida: " + propriedade);
        }

        Specification<Transacao> especificacao = EspecificacoesTransacao.ownedBy(usuarioId)
                .and(EspecificacoesTransacao.matching(filtro));
        Page<Transacao> resultado = transactions.findAll(especificacao,
                PageRequest.of(pagina, tamanhoSeguro, Sort.by(parseDirection(direcao), propriedade)));
        return RespostaPaginada.from(resultado, TransacaoMapper::toResponse);
    }

    private Sort.Direction parseDirection(String direcao) {
        try {
            return direcao == null || direcao.isBlank()
                    ? Sort.Direction.DESC
                    : Sort.Direction.fromString(direcao);
        } catch (IllegalArgumentException ex) {
            throw new ExcecaoRegraNegocio("Direcao de ordenacao invalida; use asc ou desc");
        }
    }

    @Transactional(readOnly = true)
    public RespostaTransacao get(UUID usuarioId, UUID transacaoId) {
        return TransacaoMapper.toResponse(carregar(usuarioId, transacaoId));
    }

    @Transactional
    public RespostaTransacao atualizar(UUID usuarioId, UUID transacaoId, RequisicaoAtualizarTransacao request) {
        Transacao transacao = carregar(usuarioId, transacaoId);
        requireActiveAccount(usuarioId, request.contaId());
        requireMatchingCategory(usuarioId, request.categoriaId(), request.tipo());

        boolean wasCompleted = transacao.getStatus() == StatusTransacao.COMPLETED;
        transacao.atualizar(
                request.contaId(),
                request.categoriaId(),
                request.descricao().trim(),
                money(request.valor()),
                request.tipo(),
                request.dataTransacao());
        if (wasCompleted) {
            publishChange(usuarioId);
        }
        return TransacaoMapper.toResponse(transacao);
    }

    @Transactional
    public RespostaTransacao alterarStatus(UUID usuarioId, UUID transacaoId, StatusTransacao target) {
        Transacao transacao = carregar(usuarioId, transacaoId);
        StatusTransacao current = transacao.getStatus();
        if (current == target) {
            return TransacaoMapper.toResponse(transacao);
        }
        ensureAllowedTransition(current, target);
        transacao.alterarStatus(target);
        publishChange(usuarioId);
        return TransacaoMapper.toResponse(transacao);
    }

    @Transactional
    public void excluir(UUID usuarioId, UUID transacaoId) {
        Transacao transacao = carregar(usuarioId, transacaoId);
        boolean wasCompleted = transacao.getStatus() == StatusTransacao.COMPLETED;
        transactions.delete(transacao);
        if (wasCompleted) {
            publishChange(usuarioId);
        }
    }

    private void ensureAllowedTransition(StatusTransacao current, StatusTransacao target) {
        boolean allowed = (current == StatusTransacao.PENDING && target == StatusTransacao.COMPLETED)
                || (current == StatusTransacao.COMPLETED && target == StatusTransacao.PENDING);
        if (!allowed) {
            throw new ExcecaoConflito("Transicao de status nao permitida: " + current + " -> " + target);
        }
    }

    private Conta requireActiveAccount(UUID usuarioId, UUID contaId) {
        Conta conta = accounts.findByIdAndUsuarioId(contaId, usuarioId)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Conta"));
        if (!conta.isAtiva()) {
            throw new ExcecaoRegraNegocio("Conta inativa nao aceita movimentacoes");
        }
        return conta;
    }

    private Categoria requireMatchingCategory(UUID usuarioId, UUID categoriaId, TipoTransacao tipo) {
        Categoria categoria = categories.findByIdAndUsuarioId(categoriaId, usuarioId)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Categoria"));
        if (categoria.getTipo() != tipo) {
            throw new ExcecaoRegraNegocio("Categoria " + categoria.getTipo()
                    + " nao pode ser usada em transacao " + tipo);
        }
        return categoria;
    }

    private BigDecimal money(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private Transacao carregar(UUID usuarioId, UUID transacaoId) {
        return transactions.findByIdAndUsuarioId(transacaoId, usuarioId)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Transacao"));
    }

    private void publishChange(UUID usuarioId) {
        events.publishEvent(new EventoTransacaoAlterada(usuarioId));
    }
}
