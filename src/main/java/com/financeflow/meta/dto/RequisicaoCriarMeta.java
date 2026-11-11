package com.financeflow.goal.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RequisicaoCriarMeta(
        @NotBlank @Size(max = 120) String nome,
        @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal valorAlvo,
        @FutureOrPresent LocalDate prazo) {
}
