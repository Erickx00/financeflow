package com.financeflow.relatorio;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Contrato unico de serializacao dos relatorios em cache. Sem tipos
 * polimorficos embutidos: quem le sabe exatamente qual DTO esperar.
 */
@Component
public class CodificadorJson {

    private final ObjectMapper objectMapper;

    public CodificadorJson(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao serializar resposta de relatorio", ex);
        }
    }

    public <T> T read(String json, Class<T> tipo) {
        try {
            return objectMapper.readValue(json, tipo);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao desserializar resposta de relatorio", ex);
        }
    }
}
