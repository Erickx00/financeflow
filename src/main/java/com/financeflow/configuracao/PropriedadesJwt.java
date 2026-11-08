package com.financeflow.configuracao;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Configuracao do JWT. O segredo exige no minimo 32 bytes (256 bits) para HS256;
 * a aplicacao falha no startup se a configuracao for insegura.
 */
@ConfigurationProperties(prefix = "financeflow.jwt")
public record PropriedadesJwt(String secret, Duration expiration) {

    public PropriedadesJwt {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("financeflow.jwt.secret must be configured");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "financeflow.jwt.secret must have at least 32 bytes for HS256 (received "
                            + secret.getBytes(StandardCharsets.UTF_8).length + ")");
        }
        if (expiration == null || expiration.isZero() || expiration.isNegative()) {
            throw new IllegalStateException("financeflow.jwt.expiration must be a positive duration");
        }
        if (expiration.compareTo(Duration.ofDays(1)) > 0) {
            throw new IllegalStateException("financeflow.jwt.expiration must not exceed 24h");
        }
    }
}
