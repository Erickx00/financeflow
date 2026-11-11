package com.financeflow.goal;

import com.financeflow.goal.dto.RespostaMeta;

public final class MetaMapper {

    private MetaMapper() {
    }

    public static RespostaMeta toResponse(Meta meta) {
        return new RespostaMeta(
                meta.getId(),
                meta.getNome(),
                meta.getValorAlvo(),
                meta.getValorAtual(),
                meta.getPrazo(),
                meta.getStatus(),
                meta.getCriadoEm(),
                meta.getAtualizadoEm());
    }
}
