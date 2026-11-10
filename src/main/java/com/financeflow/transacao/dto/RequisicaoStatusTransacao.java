package com.financeflow.transacao.dto;

import com.financeflow.transacao.StatusTransacao;
import jakarta.validation.constraints.NotNull;

public record RequisicaoStatusTransacao(@NotNull StatusTransacao status) {
}
