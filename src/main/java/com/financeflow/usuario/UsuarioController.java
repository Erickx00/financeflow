package com.financeflow.usuario;

import com.financeflow.seguranca.UsuarioAtual;
import com.financeflow.usuario.dto.RequisicaoAlterarSenha;
import com.financeflow.usuario.dto.RequisicaoAtualizarUsuario;
import com.financeflow.usuario.dto.RespostaUsuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/perfil")
    @Operation(summary = "Consulta o perfil do usuario autenticado")
    public RespostaUsuario me(@AuthenticationPrincipal Jwt jwt) {
        return usuarioService.profile(UsuarioAtual.id(jwt));
    }

    @PutMapping("/perfil")
    @Operation(summary = "Atualiza o perfil do usuario autenticado")
    public RespostaUsuario atualizar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RequisicaoAtualizarUsuario request) {
        return usuarioService.updateName(UsuarioAtual.id(jwt), request);
    }

    @PatchMapping("/perfil/senha")
    @Operation(summary = "Altera a senha, exigindo a senha atual")
    public ResponseEntity<Void> alterarSenha(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody RequisicaoAlterarSenha request) {
        usuarioService.alterarSenha(UsuarioAtual.id(jwt), request);
        return ResponseEntity.noContent().build();
    }
}
