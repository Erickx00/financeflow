package com.financeflow.comum.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

/**
 * Contrato unico de erro da API. Nunca inclui stack trace, SQL ou detalhes internos.
 */
public record RespostaErro(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldIssue> errors) {

    public record FieldIssue(String field, String message) {
    }

    public static RespostaErro of(HttpStatus status, String message, String path) {
        return new RespostaErro(Instant.now(), status.value(), status.getReasonPhrase(), message, path, List.of());
    }

    public static RespostaErro validation(HttpStatus status, String message, String path, List<FieldIssue> issues) {
        return new RespostaErro(Instant.now(), status.value(), status.getReasonPhrase(), message, path, issues);
    }
}
