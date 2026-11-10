package com.financeflow.conta;

import com.financeflow.conta.dto.RespostaConta;

public final class ContaMapper {

    private ContaMapper() {
    }

    public static RespostaConta toResponse(Conta conta) {
        return new RespostaConta(
                conta.getId(),
                conta.getNome(),
                conta.getTipo(),
                conta.getSaldoInicial(),
                conta.getMoeda(),
                conta.isAtiva(),
                conta.getCriadoEm());
    }
}
