package com.financeflow.infraestrutura.cache;

import com.financeflow.configuracao.PropriedadesRelatorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Versao do cache por usuario. Invalidar todos os relatorios passa a ser um
 * INCR (O(1)) em vez de varrer chaves com SCAN/KEYS (ADR 0003).
 */
@Component
public class ArmazemVersaoCache {

    private static final Logger log = LoggerFactory.getLogger(ArmazemVersaoCache.class);
    private static final String KEY_PREFIX = "ff:cachever:";

    private final StringRedisTemplate redis;
    private final Duration retention;

    public ArmazemVersaoCache(StringRedisTemplate redis, PropriedadesRelatorio properties) {
        this.redis = redis;
        // A versao vive mais que as entradas que ela valida, evitando "voltar" para uma versao antiga.
        this.retention = properties.cacheTtl().multipliedBy(4);
    }

    public long current(UUID usuarioId) {
        try {
            String value = redis.opsForValue().get(KEY_PREFIX + usuarioId);
            return value == null ? 0L : Long.parseLong(value);
        } catch (RuntimeException ex) {
            log.warn("Redis indisponivel ao ler a versao do cache; seguindo sem cache");
            return 0L;
        }
    }

    public void increment(UUID usuarioId) {
        try {
            String key = KEY_PREFIX + usuarioId;
            Long value = redis.opsForValue().increment(key);
            if (value != null && value == 1L) {
                redis.expire(key, retention);
            }
        } catch (RuntimeException ex) {
            log.warn("Redis indisponivel ao invalidar o cache; entradas expiram pelo TTL");
        }
    }
}
