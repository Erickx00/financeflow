package com.financeflow.transacao.dto;

import com.financeflow.transacao.StatusTransacao;
import com.financeflow.transacao.TipoTransacao;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RequisicaoCriarTransacao(
        @NotNull UUID contaId,
        @NotNull UUID categoriaId,
        @NotBlank @Size(max = 200) String descricao,
        @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal valor,
        @NotNull TipoTransacao tipo,
        @NotNull LocalDate dataTransacao,
        @NotNull StatusTransacao status) {
}
