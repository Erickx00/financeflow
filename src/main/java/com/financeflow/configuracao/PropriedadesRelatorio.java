package com.financeflow.configuracao;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "financeflow.reports")
public record PropriedadesRelatorio(Duration cacheTtl, boolean exposeCacheHeader) {

    public PropriedadesRelatorio {
        if (cacheTtl == null || cacheTtl.isNegative() || cacheTtl.isZero()) {
            cacheTtl = Duration.ofMinutes(5);
        }
    }
}
