package com.financeflow.categoria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RequisicaoAtualizarCategoria(
        @NotBlank @Size(max = 80) String nome,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "cor deve estar no formato #RRGGBB") String cor) {
}
