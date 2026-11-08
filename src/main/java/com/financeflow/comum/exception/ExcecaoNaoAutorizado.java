package com.financeflow.comum.exception;

import org.springframework.http.HttpStatus;

/** Credenciais invalidas ou token nao aceito. */
public class ExcecaoNaoAutorizado extends ExcecaoApi {

    public ExcecaoNaoAutorizado(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }

    public static ExcecaoNaoAutorizado invalidCredentials() {
        return new ExcecaoNaoAutorizado("Credenciais invalidas");
    }
}
