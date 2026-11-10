package com.financeflow.categoria;

import com.financeflow.categoria.dto.RespostaCategoria;
import com.financeflow.categoria.dto.RequisicaoCriarCategoria;
import com.financeflow.categoria.dto.RequisicaoAtualizarCategoria;
import com.financeflow.seguranca.UsuarioAtual;
import com.financeflow.transacao.TipoTransacao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/v1/categorias")
@Tag(name = "Categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma categoria de receita ou despesa")
    public RespostaCategoria criar(@AuthenticationPrincipal Jwt jwt,
                                   @Valid @RequestBody RequisicaoCriarCategoria request) {
        return categoriaService.criar(UsuarioAtual.id(jwt), request);
    }

    @GetMapping
    @Operation(summary = "Lista categorias, opcionalmente filtrando por tipo")
    public List<RespostaCategoria> listar(@AuthenticationPrincipal Jwt jwt,
                                       @RequestParam(required = false) TipoTransacao tipo) {
        return categoriaService.listar(UsuarioAtual.id(jwt), tipo);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta uma categoria")
    public RespostaCategoria get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return categoriaService.get(UsuarioAtual.id(jwt), id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza nome e cor da categoria (tipo e imutavel)")
    public RespostaCategoria atualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                   @Valid @RequestBody RequisicaoAtualizarCategoria request) {
        return categoriaService.atualizar(UsuarioAtual.id(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui uma categoria sem transacoes vinculadas")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        categoriaService.excluir(UsuarioAtual.id(jwt), id);
        return ResponseEntity.noContent().build();
    }
}
