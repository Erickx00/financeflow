package com.financeflow.conta.dto;

import com.financeflow.conta.TipoConta;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RequisicaoCriarConta(
        @NotBlank @Size(max = 100) String nome,
        @NotNull TipoConta tipo,
        @NotNull @Digits(integer = 17, fraction = 2) BigDecimal saldoInicial,
        @Pattern(regexp = "^[A-Z]{3}$", message = "moeda deve ter 3 letras maiusculas") String moeda) {
}
