package com.financeflow.comum.exception;

import org.springframework.http.HttpStatus;

/** Entrada sintaticamente valida, mas que viola uma regra de negocio. */
public class ExcecaoRegraNegocio extends ExcecaoApi {

    public ExcecaoRegraNegocio(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
