package com.financeflow.usuario;

import com.financeflow.comum.exception.ExcecaoRegraNegocio;
import com.financeflow.comum.exception.ExcecaoRecursoNaoEncontrado;
import com.financeflow.usuario.dto.RequisicaoAlterarSenha;
import com.financeflow.usuario.dto.RequisicaoAtualizarUsuario;
import com.financeflow.usuario.dto.RespostaUsuario;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UsuarioService {

    private final UsuarioRepository users;
    private final PasswordEncoder codificadorSenha;

    public UsuarioService(UsuarioRepository users, PasswordEncoder codificadorSenha) {
        this.users = users;
        this.codificadorSenha = codificadorSenha;
    }

    @Transactional(readOnly = true)
    public RespostaUsuario profile(UUID usuarioId) {
        return UsuarioMapper.toResponse(carregar(usuarioId));
    }

    @Transactional
    public RespostaUsuario updateName(UUID usuarioId, RequisicaoAtualizarUsuario request) {
        Usuario usuario = carregar(usuarioId);
        usuario.renomear(request.nome().trim());
        return UsuarioMapper.toResponse(usuario);
    }

    /**
     * Troca de senha exige a senha atual: um token roubado, sozinho, nao troca a senha.
     */
    @Transactional
    public void alterarSenha(UUID usuarioId, RequisicaoAlterarSenha request) {
        Usuario usuario = carregar(usuarioId);
        if (!codificadorSenha.matches(request.senhaAtual(), usuario.getSenhaHash())) {
            throw new ExcecaoRegraNegocio("Senha atual invalida");
        }
        if (codificadorSenha.matches(request.senhaNova(), usuario.getSenhaHash())) {
            throw new ExcecaoRegraNegocio("A nova senha deve ser diferente da atual");
        }
        usuario.alterarSenha(codificadorSenha.encode(request.senhaNova()));
    }

    private Usuario carregar(UUID usuarioId) {
        return users.findById(usuarioId).orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Usuario"));
    }
}
