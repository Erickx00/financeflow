package com.financeflow.infraestrutura.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * Cache tolerante a falhas: qualquer erro de Redis vira cache miss (get) ou
 * escrita ignorada (put), mantendo a API funcional apenas com PostgreSQL.
 * Os valores sao String (JSON ja serializado pelo chamador) - nunca ha
 * desserializacao polimorfica de dado vindo do Redis.
 */
public class CacheRedisResiliente implements Cache {

    private static final Logger log = LoggerFactory.getLogger(CacheRedisResiliente.class);

    private final String nome;
    private final StringRedisTemplate redis;
    private final Duration ttl;

    public CacheRedisResiliente(String nome, StringRedisTemplate redis, Duration ttl) {
        this.nome = nome;
        this.redis = redis;
        this.ttl = ttl;
    }

    @Override
    public String getName() {
        return nome;
    }

    @Override
    public Object getNativeCache() {
        return redis;
    }

    @Override
    public ValueWrapper get(Object key) {
        String fullKey = fullKey(key);
        try {
            String value = redis.opsForValue().get(fullKey);
            if (value == null) {
                return null;
            }
            return (ValueWrapper) () -> value;
        } catch (RuntimeException ex) {
            log.warn("Cache {} indisponivel na leitura; consultando o PostgreSQL", nome);
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Class<T> tipo) {
        ValueWrapper wrapper = get(key);
        if (wrapper == null) {
            return null;
        }
        Object value = wrapper.get();
        if (value == null) {
            return null;
        }
        if (tipo != null && !tipo.isInstance(value)) {
            throw new IllegalStateException(
                    "Valor em cache nao e do tipo esperado [" + tipo.getName() + "]: " + value);
        }
        return (T) value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, java.util.concurrent.Callable<T> valueLoader) {
        ValueWrapper wrapper = get(key);
        if (wrapper != null) {
            return (T) wrapper.get();
        }
        try {
            T value = valueLoader.call();
            put(key, value);
            return value;
        } catch (Exception ex) {
            throw new ValueRetrievalException(key, valueLoader, ex);
        }
    }

    @Override
    public void put(Object key, Object value) {
        if (value == null) {
            return;
        }
        try {
            redis.opsForValue().set(fullKey(key), value.toString(), ttl);
        } catch (RuntimeException ex) {
            log.warn("Cache {} indisponivel na escrita; resposta segue sem cache", nome);
        }
    }

    @Override
    public void evict(Object key) {
        try {
            redis.delete(fullKey(key));
        } catch (RuntimeException ex) {
            log.warn("Cache {} indisponivel na remocao", nome);
        }
    }

    @Override
    public void clear() {
        // Invalidação e feita por versao (ArmazemVersaoCache), nunca por varredura de chaves.
    }

    private String fullKey(Object key) {
        return nome + ":" + key;
    }
}
