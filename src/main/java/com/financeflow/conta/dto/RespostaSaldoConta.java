package com.financeflow.conta.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RespostaSaldoConta(UUID contaId, BigDecimal saldo, LocalDate asOf, String moeda) {
}
