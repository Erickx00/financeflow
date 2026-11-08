package com.financeflow.comum.exception;

import org.springframework.http.HttpStatus;

/**
 * Recurso inexistente ou pertencente a outro usuario. A mesma resposta e usada
 * nos dois casos para nao revelar a existencia de dados de terceiros (ADR 0002).
 */
public class ExcecaoRecursoNaoEncontrado extends ExcecaoApi {

    public ExcecaoRecursoNaoEncontrado(String resource) {
        super(HttpStatus.NOT_FOUND, resource + " nao encontrado");
    }
}
