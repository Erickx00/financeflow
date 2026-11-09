package com.financeflow.autenticacao;

import com.financeflow.autenticacao.dto.RespostaAutenticacao;
import com.financeflow.autenticacao.dto.RequisicaoLogin;
import com.financeflow.autenticacao.dto.RequisicaoCadastro;
import com.financeflow.comum.exception.ExcecaoConflito;
import com.financeflow.comum.exception.ExcecaoNaoAutorizado;
import com.financeflow.seguranca.JwtService;
import com.financeflow.usuario.Usuario;
import com.financeflow.usuario.UsuarioMapper;
import com.financeflow.usuario.UsuarioRepository;
import com.financeflow.usuario.dto.RespostaUsuario;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AutenticacaoService {

    private final UsuarioRepository users;
    private final PasswordEncoder codificadorSenha;
    private final JwtService jwtService;
    /**
     * Hash descartavel usado quando o e-mail nao existe: manter o custo do BCrypt
     * evita descobrir quais e-mails estao cadastrados medindo o tempo da resposta.
     */
    private final String decoyHash;

    public AutenticacaoService(UsuarioRepository users, PasswordEncoder codificadorSenha, JwtService jwtService) {
        this.users = users;
        this.codificadorSenha = codificadorSenha;
        this.jwtService = jwtService;
        this.decoyHash = codificadorSenha.encode("decoy-senha-for-timing");
    }

    @Transactional
    public RespostaUsuario cadastrar(RequisicaoCadastro request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new ExcecaoConflito("E-mail ja cadastrado");
        }
        Usuario usuario = Usuario.cadastrar(request.nome().trim(), email, codificadorSenha.encode(request.senha()));
        try {
            return UsuarioMapper.toResponse(users.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException ex) {
            // Corrida entre duas requisicoes: o indice unico decide e a resposta e a mesma.
            throw new ExcecaoConflito("E-mail ja cadastrado");
        }
    }

    @Transactional(readOnly = true)
    public RespostaAutenticacao login(RequisicaoLogin request) {
        Usuario usuario = users.findByEmail(normalizeEmail(request.email())).orElse(null);
        if (usuario == null) {
            codificadorSenha.matches(request.senha(), decoyHash);
            throw ExcecaoNaoAutorizado.invalidCredentials();
        }
        if (!codificadorSenha.matches(request.senha(), usuario.getSenhaHash())) {
            throw ExcecaoNaoAutorizado.invalidCredentials();
        }
        String token = jwtService.issueToken(usuario.getId(), usuario.getEmail(), usuario.getPapel());
        return RespostaAutenticacao.bearer(token, jwtService.expiresInSeconds());
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
