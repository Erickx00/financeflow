package com.financeflow.seguranca;

import com.financeflow.comum.exception.ExcecaoNaoAutorizado;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/** Extrai a identidade do token no controller, sem confiar em ids do cliente. */
public final class UsuarioAtual {

    private UsuarioAtual() {
    }

    public static UUID id(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (RuntimeException ex) {
            throw new ExcecaoNaoAutorizado("Token invalido");
        }
    }
}
