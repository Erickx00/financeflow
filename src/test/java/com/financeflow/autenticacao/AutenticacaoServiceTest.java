package com.financeflow.autenticacao;

import com.financeflow.autenticacao.dto.RespostaAutenticacao;
import com.financeflow.autenticacao.dto.RequisicaoLogin;
import com.financeflow.autenticacao.dto.RequisicaoCadastro;
import com.financeflow.comum.exception.ExcecaoConflito;
import com.financeflow.comum.exception.ExcecaoNaoAutorizado;
import com.financeflow.seguranca.JwtService;
import com.financeflow.usuario.Papel;
import com.financeflow.usuario.Usuario;
import com.financeflow.usuario.UsuarioRepository;
import com.financeflow.usuario.dto.RespostaUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutenticacaoServiceTest {

    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private PasswordEncoder codificadorSenha;
    @Mock
    private JwtService jwtService;

    private AutenticacaoService servicoAutenticacao;

    @BeforeEach
    void setUp() {
        servicoAutenticacao = new AutenticacaoService(usuarios, codificadorSenha, jwtService);
    }

    @Test
    @DisplayName("cadastro normaliza o e-mail e forca o papel USER")
    void registerNormalizesEmailAndForcesUserRole() {
        when(usuarios.existsByEmail("erick@example.com")).thenReturn(false);
        when(codificadorSenha.encode("SenhaMuitoForte123")).thenReturn("hash");
        when(usuarios.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RespostaUsuario response = servicoAutenticacao.cadastrar(
                new RequisicaoCadastro("Erick", "  Erick@Example.com  ", "SenhaMuitoForte123"));

        assertThat(response.email()).isEqualTo("erick@example.com");
        assertThat(response.papel()).isEqualTo(Papel.USER);
        verify(usuarios).existsByEmail("erick@example.com");
    }

    @Test
    @DisplayName("e-mail duplicado retorna conflito")
    void registerRejectsDuplicateEmail() {
        when(usuarios.existsByEmail("erick@example.com")).thenReturn(true);

        assertThatThrownBy(() -> servicoAutenticacao.cadastrar(
                new RequisicaoCadastro("Erick", "erick@example.com", "SenhaMuitoForte123")))
                .isInstanceOf(ExcecaoConflito.class)
                .hasMessageContaining("E-mail ja cadastrado");
    }

    @Test
    @DisplayName("login com senha errada nao revela se o e-mail existe")
    void loginRejectsWrongPassword() {
        Usuario usuario = Usuario.cadastrar("Erick", "erick@example.com", "hash");
        when(usuarios.findByEmail("erick@example.com")).thenReturn(Optional.of(usuario));
        when(codificadorSenha.matches("errada", "hash")).thenReturn(false);

        assertThatThrownBy(() -> servicoAutenticacao.login(new RequisicaoLogin("erick@example.com", "errada")))
                .isInstanceOf(ExcecaoNaoAutorizado.class)
                .hasMessage("Credenciais invalidas");
    }

    @Test
    @DisplayName("login de e-mail inexistente usa a mesma mensagem generica")
    void loginRejectsUnknownEmail() {
        when(usuarios.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicoAutenticacao.login(new RequisicaoLogin("ninguem@example.com", "qualquer")))
                .isInstanceOf(ExcecaoNaoAutorizado.class)
                .hasMessage("Credenciais invalidas");
    }

    @Test
    @DisplayName("login valido devolve token e expiracao")
    void loginIssuesToken() {
        Usuario usuario = Usuario.cadastrar("Erick", "erick@example.com", "hash");
        when(usuarios.findByEmail("erick@example.com")).thenReturn(Optional.of(usuario));
        when(codificadorSenha.matches("SenhaMuitoForte123", "hash")).thenReturn(true);
        when(jwtService.issueToken(usuario.getId(), "erick@example.com", Papel.USER)).thenReturn("jwt-token");
        when(jwtService.expiresInSeconds()).thenReturn(3600L);

        RespostaAutenticacao response = servicoAutenticacao.login(
                new RequisicaoLogin("Erick@Example.com", "SenhaMuitoForte123"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
    }
}
