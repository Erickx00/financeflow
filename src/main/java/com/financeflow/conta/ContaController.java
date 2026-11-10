package com.financeflow.conta;

import com.financeflow.conta.dto.RespostaSaldoConta;
import com.financeflow.conta.dto.RespostaConta;
import com.financeflow.conta.dto.RequisicaoCriarConta;
import com.financeflow.conta.dto.RequisicaoAtualizarConta;
import com.financeflow.conta.dto.RequisicaoStatusConta;
import com.financeflow.seguranca.UsuarioAtual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contas")
@Tag(name = "Contas")
public class ContaController {

    private final ContaService contaService;

    public ContaController(ContaService contaService) {
        this.contaService = contaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma conta financeira")
    public RespostaConta criar(@AuthenticationPrincipal Jwt jwt,
                                  @Valid @RequestBody RequisicaoCriarConta request) {
        return contaService.criar(UsuarioAtual.id(jwt), request);
    }

    @GetMapping
    @Operation(summary = "Lista as contas do usuario autenticado")
    public List<RespostaConta> listar(@AuthenticationPrincipal Jwt jwt) {
        return contaService.listar(UsuarioAtual.id(jwt));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta uma conta do usuario autenticado")
    public RespostaConta get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return contaService.get(UsuarioAtual.id(jwt), id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza nome e tipo da conta")
    public RespostaConta atualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                  @Valid @RequestBody RequisicaoAtualizarConta request) {
        return contaService.atualizar(UsuarioAtual.id(jwt), id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Ativa ou desativa a conta")
    public RespostaConta alterarStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                        @Valid @RequestBody RequisicaoStatusConta request) {
        return contaService.alterarStatus(UsuarioAtual.id(jwt), id, Boolean.TRUE.equals(request.ativa()));
    }

    @GetMapping("/{id}/saldo")
    @Operation(summary = "Calcula o saldo derivado das transacoes concluidas ate a data informada")
    public RespostaSaldoConta saldo(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataReferencia) {
        return contaService.saldo(UsuarioAtual.id(jwt), id, dataReferencia);
    }
}
