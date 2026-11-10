package com.financeflow.transacao;

import com.financeflow.comum.response.RespostaPaginada;
import com.financeflow.seguranca.UsuarioAtual;
import com.financeflow.transacao.dto.RequisicaoCriarTransacao;
import com.financeflow.transacao.dto.FiltroTransacao;
import com.financeflow.transacao.dto.RespostaTransacao;
import com.financeflow.transacao.dto.RequisicaoAtualizarTransacao;
import com.financeflow.transacao.dto.RequisicaoStatusTransacao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transacoes")
@Tag(name = "Transacoes")
public class TransacaoController {

    private final TransacaoService transacaoService;

    public TransacaoController(TransacaoService transacaoService) {
        this.transacaoService = transacaoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra uma receita ou despesa")
    public RespostaTransacao criar(@AuthenticationPrincipal Jwt jwt,
                                      @Valid @RequestBody RequisicaoCriarTransacao request) {
        return transacaoService.criar(UsuarioAtual.id(jwt), request);
    }

    @GetMapping
    @Operation(summary = "Lista transacoes com filtros e paginacao")
    public RespostaPaginada<RespostaTransacao> listar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) TipoTransacao tipo,
            @RequestParam(required = false) StatusTransacao status,
            @RequestParam(required = false) UUID contaId,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho,
            @RequestParam(defaultValue = "dataTransacao") String ordenarPor,
            @RequestParam(defaultValue = "desc") String direcao) {
        FiltroTransacao filtro = new FiltroTransacao(tipo, status, contaId, categoriaId, dataInicio, dataFim);
        return transacaoService.listar(UsuarioAtual.id(jwt), filtro, pagina, tamanho, ordenarPor, direcao);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta uma transacao")
    public RespostaTransacao get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return transacaoService.get(UsuarioAtual.id(jwt), id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza os dados da transacao (o status nao muda por aqui)")
    public RespostaTransacao atualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                      @Valid @RequestBody RequisicaoAtualizarTransacao request) {
        return transacaoService.atualizar(UsuarioAtual.id(jwt), id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Altera o status entre PENDING e COMPLETED")
    public RespostaTransacao alterarStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                            @Valid @RequestBody RequisicaoStatusTransacao request) {
        return transacaoService.alterarStatus(UsuarioAtual.id(jwt), id, request.status());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui a transacao; saldo e relatorios sao derivados")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        transacaoService.excluir(UsuarioAtual.id(jwt), id);
        return ResponseEntity.noContent().build();
    }
}
