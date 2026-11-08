package com.financeflow.configuracao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Relogio unico da aplicacao. O fuso define qual e o "hoje" das regras de
 * negocio (saldo ate a data, prazo de meta); os instantes de auditoria
 * continuam em UTC, porque Clock.instant() e sempre UTC.
 */
@Configuration
public class RelogioConfig {

    @Bean
    public Clock clock(@Value("${financeflow.time.zone:America/Sao_Paulo}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
