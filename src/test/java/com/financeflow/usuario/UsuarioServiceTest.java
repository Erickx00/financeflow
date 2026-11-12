package com.financeflow.usuario;

import com.financeflow.comum.exception.ExcecaoRegraNegocio;
import com.financeflow.usuario.dto.RequisicaoAlterarSenha;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private PasswordEncoder codificadorSenha;

    private UsuarioService servico;

    @BeforeEach
    void setUp() {
        servico = new UsuarioService(usuarios, codificadorSenha);
    }

    @Test
    @DisplayName("troca de senha exige a senha atual correta")
    void changePasswordRequiresCurrentPassword() {
        Usuario usuario = Usuario.cadastrar("Erick", "erick@example.com", "hash-atual");
        when(usuarios.findById(USER_ID)).thenReturn(Optional.of(usuario));
        when(codificadorSenha.matches("errada", "hash-atual")).thenReturn(false);

        assertThatThrownBy(() -> servico.alterarSenha(USER_ID,
                new RequisicaoAlterarSenha("errada", "SenhaNovaMuitoForte123")))
                .isInstanceOf(ExcecaoRegraNegocio.class)
                .hasMessageContaining("Senha atual invalida");
    }

    @Test
    @DisplayName("nova senha igual a atual e rejeitada")
    void rejectsSamePassword() {
        Usuario usuario = Usuario.cadastrar("Erick", "erick@example.com", "hash-atual");
        when(usuarios.findById(USER_ID)).thenReturn(Optional.of(usuario));
        when(codificadorSenha.matches("SenhaMuitoForte123", "hash-atual")).thenReturn(true);

        assertThatThrownBy(() -> servico.alterarSenha(USER_ID,
                new RequisicaoAlterarSenha("SenhaMuitoForte123", "SenhaMuitoForte123")))
                .isInstanceOf(ExcecaoRegraNegocio.class)
                .hasMessageContaining("diferente");
    }

    @Test
    @DisplayName("troca valida atualiza o hash")
    void changePasswordStoresNewHash() {
        Usuario usuario = Usuario.cadastrar("Erick", "erick@example.com", "hash-atual");
        when(usuarios.findById(USER_ID)).thenReturn(Optional.of(usuario));
        when(codificadorSenha.matches("SenhaAtualForte123", "hash-atual")).thenReturn(true);
        when(codificadorSenha.matches("SenhaNovaForte456", "hash-atual")).thenReturn(false);
        when(codificadorSenha.encode("SenhaNovaForte456")).thenReturn("hash-novo");

        servico.alterarSenha(USER_ID, new RequisicaoAlterarSenha("SenhaAtualForte123", "SenhaNovaForte456"));

        verify(codificadorSenha).encode("SenhaNovaForte456");
    }
}
