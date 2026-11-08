package com.financeflow.comum.exception;

import org.springframework.http.HttpStatus;

/**
 * Excecao de dominio com status HTTP associado. O handler global traduz para
 * o contrato de erro padronizado, sem expor detalhes internos.
 */
public class ExcecaoApi extends RuntimeException {

    private final HttpStatus status;

    protected ExcecaoApi(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
