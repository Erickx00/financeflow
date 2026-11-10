package com.financeflow.categoria.dto;

import com.financeflow.transacao.TipoTransacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RequisicaoCriarCategoria(
        @NotBlank @Size(max = 80) String nome,
        @NotNull TipoTransacao tipo,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "cor deve estar no formato #RRGGBB") String cor) {
}
