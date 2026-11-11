package com.financeflow.goal.dto;

import com.financeflow.goal.StatusMeta;
import jakarta.validation.constraints.NotNull;

/** Apenas ACTIVE -> CANCELLED e permitido; concluir e automatico pela contribuicao. */
public record RequisicaoStatusMeta(@NotNull StatusMeta status) {
}
