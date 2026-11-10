package com.financeflow.transacao;

import com.financeflow.transacao.dto.RespostaTransacao;

public final class TransacaoMapper {

    private TransacaoMapper() {
    }

    public static RespostaTransacao toResponse(Transacao transacao) {
        return new RespostaTransacao(
                transacao.getId(),
                transacao.getContaId(),
                transacao.getCategoriaId(),
                transacao.getDescricao(),
                transacao.getValor(),
                transacao.getTipo(),
                transacao.getDataTransacao(),
                transacao.getStatus(),
                transacao.getCriadoEm(),
                transacao.getAtualizadoEm());
    }
}
