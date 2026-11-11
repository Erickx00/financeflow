package com.financeflow.relatorio;

import com.financeflow.comum.exception.ExcecaoRegraNegocio;
import com.financeflow.relatorio.dto.RespostaFluxoCaixa;
import com.financeflow.relatorio.dto.RespostaGastosCategoria;
import com.financeflow.relatorio.dto.RespostaResumoFinanceiro;
import com.financeflow.relatorio.dto.RespostaRelatorioMensal;
import com.financeflow.transacao.TipoTransacao;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class RelatorioService {

    private static final int MAX_CASH_FLOW_DAYS = 366;

    private final GatewayRelatoriosComCache gateway;
    private final CalculadoraRelatorio calculator;
    private final CodificadorJson codec;

    public RelatorioService(GatewayRelatoriosComCache gateway, CalculadoraRelatorio calculator, CodificadorJson codec) {
        this.gateway = gateway;
        this.calculator = calculator;
        this.codec = codec;
    }

    public RespostaResumoFinanceiro summary(UUID usuarioId, String month) {
        YearMonth period = parseMonth(month);
        return codec.read(gateway.summaryJson(usuarioId, period), RespostaResumoFinanceiro.class);
    }

    public RespostaRelatorioMensal monthly(UUID usuarioId, int year) {
        if (year < 1900 || year > 2100) {
            throw new ExcecaoRegraNegocio("Ano deve estar entre 1900 e 2100");
        }
        return calculator.monthly(usuarioId, year);
    }

    public RespostaGastosCategoria categorias(UUID usuarioId, String month, TipoTransacao tipo) {
        YearMonth period = parseMonth(month);
        return codec.read(gateway.categoriasJson(usuarioId, period, tipo), RespostaGastosCategoria.class);
    }

    public RespostaFluxoCaixa cashFlow(UUID usuarioId, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new ExcecaoRegraNegocio("A data inicial nao pode ser posterior a data final");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_CASH_FLOW_DAYS) {
            throw new ExcecaoRegraNegocio("O intervalo maximo do fluxo de caixa e de " + MAX_CASH_FLOW_DAYS + " dias");
        }
        return codec.read(gateway.cashFlowJson(usuarioId, from, to), RespostaFluxoCaixa.class);
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException | NullPointerException ex) {
            throw new ExcecaoRegraNegocio("Parametro month deve estar no formato YYYY-MM");
        }
    }
}
