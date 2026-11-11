package com.financeflow.infraestrutura.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class GerenciadorCacheRedisResiliente implements CacheManager {

    private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<>();
    private final StringRedisTemplate redis;
    private final Duration ttl;

    public GerenciadorCacheRedisResiliente(StringRedisTemplate redis, Duration ttl, Collection<String> names) {
        this.redis = redis;
        this.ttl = ttl;
        names.forEach(nome -> caches.put(nome, new CacheRedisResiliente(nome, redis, ttl)));
    }

    @Override
    public Cache getCache(String nome) {
        return caches.computeIfAbsent(nome, key -> new CacheRedisResiliente(key, redis, ttl));
    }

    @Override
    public Collection<String> getCacheNames() {
        return List.copyOf(caches.keySet());
    }
}
