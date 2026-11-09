package com.financeflow.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequisicaoAlterarSenha(
        @NotBlank String senhaAtual,
        @NotBlank @Size(min = 12, max = 100) String senhaNova) {
}
