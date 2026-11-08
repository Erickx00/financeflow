package com.financeflow.configuracao;

import com.financeflow.infraestrutura.cache.NomesCache;
import com.financeflow.infraestrutura.cache.ArmazemVersaoCache;
import com.financeflow.infraestrutura.cache.GeradorChaveRelatorio;
import com.financeflow.infraestrutura.cache.GerenciadorCacheRedisResiliente;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(StringRedisTemplate redis, PropriedadesRelatorio properties) {
        return new GerenciadorCacheRedisResiliente(redis, properties.cacheTtl(), NomesCache.ALL);
    }

    @Bean
    public KeyGenerator reportKeyGenerator(ArmazemVersaoCache versions) {
        return new GeradorChaveRelatorio(versions);
    }
}
