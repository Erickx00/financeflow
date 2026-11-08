package com.financeflow.comum.exception;

import org.springframework.http.HttpStatus;

/** Conflito de estado: e-mail duplicado, categoria em uso, transicao invalida. */
public class ExcecaoConflito extends ExcecaoApi {

    public ExcecaoConflito(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
