package com.financeflow.conta.dto;

import jakarta.validation.constraints.NotNull;

public record RequisicaoStatusConta(@NotNull Boolean ativa) {
}
