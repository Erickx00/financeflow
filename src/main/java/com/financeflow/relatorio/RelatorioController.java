package com.financeflow.relatorio;

import com.financeflow.relatorio.dto.RespostaFluxoCaixa;
import com.financeflow.relatorio.dto.RespostaGastosCategoria;
import com.financeflow.relatorio.dto.RespostaResumoFinanceiro;
import com.financeflow.relatorio.dto.RespostaRelatorioMensal;
import com.financeflow.seguranca.UsuarioAtual;
import com.financeflow.transacao.TipoTransacao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/relatorios")
@Tag(name = "Relatorios")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/resumo")
    @Operation(summary = "Resumo financeiro do mes (cacheado)")
    public RespostaResumoFinanceiro resumo(@AuthenticationPrincipal Jwt jwt,
                                           @RequestParam String mes) {
        return relatorioService.summary(UsuarioAtual.id(jwt), mes);
    }

    @GetMapping("/mensal")
    @Operation(summary = "Resumo dos 12 meses de um ano (sem cache)")
    public RespostaRelatorioMensal mensal(@AuthenticationPrincipal Jwt jwt,
                                          @RequestParam int ano) {
        return relatorioService.monthly(UsuarioAtual.id(jwt), ano);
    }

    @GetMapping("/categorias")
    @Operation(summary = "Gastos (ou receitas) por categoria no mes (cacheado)")
    public RespostaGastosCategoria gastosPorCategoria(@AuthenticationPrincipal Jwt jwt,
                                                      @RequestParam String mes,
                                                      @RequestParam(defaultValue = "EXPENSE") TipoTransacao tipo) {
        return relatorioService.categorias(UsuarioAtual.id(jwt), mes, tipo);
    }

    @GetMapping("/fluxo-caixa")
    @Operation(summary = "Fluxo de caixa diario no intervalo (cacheado)")
    public RespostaFluxoCaixa fluxoCaixa(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return relatorioService.cashFlow(UsuarioAtual.id(jwt), de, ate);
    }
}
