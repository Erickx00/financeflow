package com.financeflow.autenticacao.dto;

public record RespostaAutenticacao(String accessToken, String tokenType, long expiresIn) {

    public static RespostaAutenticacao bearer(String accessToken, long expiresIn) {
        return new RespostaAutenticacao(accessToken, "Bearer", expiresIn);
    }
}
