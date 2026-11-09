package com.financeflow.autenticacao;

import com.financeflow.autenticacao.dto.RespostaAutenticacao;
import com.financeflow.autenticacao.dto.RequisicaoLogin;
import com.financeflow.autenticacao.dto.RequisicaoCadastro;
import com.financeflow.usuario.dto.RespostaUsuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/autenticacao")
@Tag(name = "Autenticacao")
public class AutenticacaoController {

    private final AutenticacaoService autenticacaoService;

    public AutenticacaoController(AutenticacaoService autenticacaoService) {
        this.autenticacaoService = autenticacaoService;
    }

    @PostMapping("/cadastrar")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um usuario")
    public ResponseEntity<RespostaUsuario> cadastrar(@Valid @RequestBody RequisicaoCadastro request) {
        RespostaUsuario usuario = autenticacaoService.cadastrar(request);
        return ResponseEntity.created(URI.create("/api/v1/usuarios/" + usuario.id())).body(usuario);
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica e devolve o token JWT")
    public RespostaAutenticacao login(@Valid @RequestBody RequisicaoLogin request) {
        return autenticacaoService.login(request);
    }
}
