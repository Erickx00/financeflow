package com.financeflow.conta.dto;

import com.financeflow.conta.TipoConta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RequisicaoAtualizarConta(
        @NotBlank @Size(max = 100) String nome,
        @NotNull TipoConta tipo) {
}
