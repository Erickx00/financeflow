package com.financeflow.infraestrutura.cache;

import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Chave = usuario + versao + parametros do relatorio. A versao entra na chave
 * para que invalidar seja um incremento, sem remover chaves antigas.
 */
public class GeradorChaveRelatorio implements KeyGenerator {

    private final ArmazemVersaoCache versions;

    public GeradorChaveRelatorio(ArmazemVersaoCache versions) {
        this.versions = versions;
    }

    @Override
    public Object generate(Object target, Method method, Object... params) {
        UUID usuarioId = (UUID) params[0];
        StringBuilder key = new StringBuilder(64)
                .append(usuarioId)
                .append(':')
                .append(versions.current(usuarioId));
        for (int index = 1; index < params.length; index++) {
            key.append(':').append(render(params[index]));
        }
        return key.toString();
    }

    private String render(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof YearMonth yearMonth) {
            return yearMonth.toString();
        }
        if (value instanceof LocalDate date) {
            return date.toString();
        }
        return value.toString();
    }
}
