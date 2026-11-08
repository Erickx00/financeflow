package com.financeflow.comum.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
public class TratadorGlobalExcecoes {

    private static final Logger log = LoggerFactory.getLogger(TratadorGlobalExcecoes.class);

    @ExceptionHandler(ExcecaoApi.class)
    public ResponseEntity<RespostaErro> handleApi(ExcecaoApi ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatus())
                .body(RespostaErro.of(ex.getStatus(), ex.getMessage(), path(request)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespostaErro> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        List<RespostaErro.FieldIssue> issues = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new RespostaErro.FieldIssue(error.getField(), error.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(RespostaErro.validation(
                HttpStatus.BAD_REQUEST, "Requisicao invalida", path(request), issues));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<RespostaErro> handleConstraint(ConstraintViolationException ex,
                                                          HttpServletRequest request) {
        List<RespostaErro.FieldIssue> issues = ex.getConstraintViolations().stream()
                .map(violation -> new RespostaErro.FieldIssue(
                        violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();
        return ResponseEntity.badRequest().body(RespostaErro.validation(
                HttpStatus.BAD_REQUEST, "Requisicao invalida", path(request), issues));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<RespostaErro> handleBadParameter(Exception ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(RespostaErro.of(
                HttpStatus.BAD_REQUEST, "Parametro invalido ou ausente", path(request)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespostaErro> handleUnreadable(HttpMessageNotReadableException ex,
                                                          HttpServletRequest request) {
        return ResponseEntity.badRequest().body(RespostaErro.of(
                HttpStatus.BAD_REQUEST, "Corpo da requisicao invalido", path(request)));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<RespostaErro> handleOptimisticLock(OptimisticLockingFailureException ex,
                                                              HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(RespostaErro.of(
                HttpStatus.CONFLICT,
                "O recurso foi alterado por outra requisicao; tente novamente",
                path(request)));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RespostaErro> handleIntegrity(DataIntegrityViolationException ex,
                                                         HttpServletRequest request) {
        log.warn("Violacao de integridade em {}", path(request));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(RespostaErro.of(
                HttpStatus.CONFLICT, "Operacao conflita com dados existentes", path(request)));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<RespostaErro> handleNoResource(NoResourceFoundException ex,
                                                          HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(RespostaErro.of(
                HttpStatus.NOT_FOUND, "Recurso nao encontrado", path(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespostaErro> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado em {}", path(request), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(RespostaErro.of(
                HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado", path(request)));
    }

    private String path(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
