package com.financeflow.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequisicaoAtualizarUsuario(@NotBlank @Size(max = 120) String nome) {
}
