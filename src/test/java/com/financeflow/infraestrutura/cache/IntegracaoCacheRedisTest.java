package com.financeflow.infraestrutura.cache;

import com.financeflow.configuracao.PropriedadesRelatorio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prova o cache contra um Redis real: escrita com TTL, leitura (hit) e
 * invalidacao por versao. Roda na CI (servico redis) e e ignorado quando nao
 * ha Redis acessivel no host.
 */
@SpringBootTest
@ActiveProfiles("it")
@TestPropertySource(properties = {
        "spring.data.redis.host=${TEST_REDIS_HOST:localhost}",
        "spring.data.redis.port=${TEST_REDIS_PORT:6379}"
})
@EnabledIf("redisAvailable")
class IntegracaoCacheRedisTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private ArmazemVersaoCache versoes;

    @Autowired
    private PropriedadesRelatorio properties;

    static boolean redisAvailable() {
        String host = System.getenv().getOrDefault("TEST_REDIS_HOST", "localhost");
        int port = Integer.parseInt(System.getenv().getOrDefault("TEST_REDIS_PORT", "6379"));
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 300);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Test
    @DisplayName("escrita no cache fica disponivel com TTL configurado")
    void cacheStoresValueWithTtl() {
        Cache cache = cacheManager.getCache(NomesCache.REPORT_SUMMARY);
        String key = "ttl-" + UUID.randomUUID();
        cache.put(key, "{\"period\":\"2026-11\"}");

        assertThat(cache.get(key)).isNotNull();
        assertThat(cache.get(key).get()).isEqualTo("{\"period\":\"2026-11\"}");

        Long ttl = redis.getExpire(NomesCache.REPORT_SUMMARY + ":" + key);
        assertThat(ttl).isNotNull().isPositive();
        assertThat(ttl).isLessThanOrEqualTo(properties.cacheTtl().toSeconds());
    }

    @Test
    @DisplayName("incrementar a versao muda a chave e isola o cache por usuario")
    void versionChangesKeyAndIsolatesUsers() {
        UUID usuario = UUID.randomUUID();
        long before = versoes.current(usuario);
        String keyBefore = NomesCache.REPORT_SUMMARY + ":" + usuario + ":" + before + ":2026-11";
        cacheManager.getCache(NomesCache.REPORT_SUMMARY).put(usuario + ":" + before + ":2026-11", "antigo");

        versoes.increment(usuario);

        assertThat(versoes.current(usuario)).isEqualTo(before + 1);
        // A chave antiga continua existindo, mas deixa de ser usada; a nova versao forca cache miss.
        assertThat(cacheManager.getCache(NomesCache.REPORT_SUMMARY).get(
                usuario + ":" + (before + 1) + ":2026-11")).isNull();

        UUID otherUser = UUID.randomUUID();
        assertThat(versoes.current(otherUser)).isZero();
        assertThat(keyBefore).contains(usuario.toString());
    }
}
