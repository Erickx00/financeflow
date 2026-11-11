package com.financeflow.goal;

import com.financeflow.goal.dto.RequisicaoCriarMeta;
import com.financeflow.goal.dto.RequisicaoAporteMeta;
import com.financeflow.goal.dto.RespostaMeta;
import com.financeflow.goal.dto.RequisicaoAtualizarMeta;
import com.financeflow.goal.dto.RequisicaoStatusMeta;
import com.financeflow.seguranca.UsuarioAtual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/metas")
@Tag(name = "Metas")
public class MetaController {

    private final MetaService metaServico;

    public MetaController(MetaService metaServico) {
        this.metaServico = metaServico;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma meta de economia")
    public RespostaMeta criar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RequisicaoCriarMeta request) {
        return metaServico.criar(UsuarioAtual.id(jwt), request);
    }

    @GetMapping
    @Operation(summary = "Lista metas, opcionalmente filtrando por status")
    public List<RespostaMeta> listar(@AuthenticationPrincipal Jwt jwt,
                                   @RequestParam(required = false) StatusMeta status) {
        return metaServico.listar(UsuarioAtual.id(jwt), status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta uma meta")
    public RespostaMeta get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return metaServico.get(UsuarioAtual.id(jwt), id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza nome, alvo e prazo da meta")
    public RespostaMeta atualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                               @Valid @RequestBody RequisicaoAtualizarMeta request) {
        return metaServico.atualizar(UsuarioAtual.id(jwt), id, request);
    }

    @PatchMapping("/{id}/contribuicoes")
    @Operation(summary = "Registra um aporte e conclui a meta ao atingir o alvo")
    public RespostaMeta contribuir(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                   @Valid @RequestBody RequisicaoAporteMeta request) {
        return metaServico.contribuir(UsuarioAtual.id(jwt), id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Cancela uma meta ativa")
    public RespostaMeta alterarStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                     @Valid @RequestBody RequisicaoStatusMeta request) {
        if (request.status() != StatusMeta.CANCELLED) {
            throw new com.financeflow.comum.exception.ExcecaoRegraNegocio(
                    "Apenas o cancelamento e permitido por este endpoint");
        }
        return metaServico.cancelar(UsuarioAtual.id(jwt), id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui uma meta sem aportes")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        metaServico.excluir(UsuarioAtual.id(jwt), id);
        return ResponseEntity.noContent().build();
    }
}
