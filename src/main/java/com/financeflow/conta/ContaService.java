package com.financeflow.conta;

import com.financeflow.conta.dto.RespostaSaldoConta;
import com.financeflow.conta.dto.RespostaConta;
import com.financeflow.conta.dto.RequisicaoCriarConta;
import com.financeflow.conta.dto.RequisicaoAtualizarConta;
import com.financeflow.comum.exception.ExcecaoRegraNegocio;
import com.financeflow.comum.exception.ExcecaoRecursoNaoEncontrado;
import com.financeflow.transacao.TransacaoRepository;
import com.financeflow.transacao.StatusTransacao;
import com.financeflow.transacao.TipoTransacao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ContaService {

    private final ContaRepository accounts;
    private final TransacaoRepository transactions;
    private final Clock clock;

    public ContaService(ContaRepository accounts, TransacaoRepository transactions, Clock clock) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Transactional
    public RespostaConta criar(UUID usuarioId, RequisicaoCriarConta request) {
        String moeda = request.moeda() == null ? "BRL" : request.moeda();
        if (!"BRL".equals(moeda)) {
            throw new ExcecaoRegraNegocio("Apenas a moeda BRL e suportada no MVP");
        }
        Conta conta = Conta.abrir(usuarioId, request.nome().trim(), request.tipo(),
                request.saldoInicial().setScale(2, RoundingMode.HALF_UP));
        return ContaMapper.toResponse(accounts.save(conta));
    }

    @Transactional(readOnly = true)
    public List<RespostaConta> listar(UUID usuarioId) {
        return accounts.findByUsuarioIdOrderByNomeAsc(usuarioId).stream()
                .map(ContaMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RespostaConta get(UUID usuarioId, UUID contaId) {
        return ContaMapper.toResponse(carregar(usuarioId, contaId));
    }

    @Transactional
    public RespostaConta atualizar(UUID usuarioId, UUID contaId, RequisicaoAtualizarConta request) {
        Conta conta = carregar(usuarioId, contaId);
        conta.renomear(request.nome().trim(), request.tipo());
        return ContaMapper.toResponse(conta);
    }

    @Transactional
    public RespostaConta alterarStatus(UUID usuarioId, UUID contaId, boolean ativa) {
        Conta conta = carregar(usuarioId, contaId);
        conta.alterarAtiva(ativa);
        return ContaMapper.toResponse(conta);
    }

    /**
     * Saldo derivado: saldo inicial + receitas concluidas - despesas concluidas ate `asOf`.
     * Nao existe saldo persistido, entao nao existe divergencia possivel (ADR 0006).
     */
    @Transactional(readOnly = true)
    public RespostaSaldoConta saldo(UUID usuarioId, UUID contaId, LocalDate asOf) {
        Conta conta = carregar(usuarioId, contaId);
        LocalDate reference = asOf == null ? LocalDate.now(clock) : asOf;
        BigDecimal signed = transactions.sumSignedAmount(
                usuarioId, contaId, StatusTransacao.COMPLETED, TipoTransacao.INCOME, reference);
        BigDecimal balance = conta.getSaldoInicial()
                .add(signed == null ? BigDecimal.ZERO : signed)
                .setScale(2, RoundingMode.HALF_UP);
        return new RespostaSaldoConta(conta.getId(), balance, reference, conta.getMoeda());
    }

    private Conta carregar(UUID usuarioId, UUID contaId) {
        return accounts.findByIdAndUsuarioId(contaId, usuarioId)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Conta"));
    }
}
