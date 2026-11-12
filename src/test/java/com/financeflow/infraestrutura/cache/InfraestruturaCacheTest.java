package com.financeflow.infraestrutura.cache;

import com.financeflow.configuracao.PropriedadesRelatorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InfraestruturaCacheTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private ArmazemVersaoCache versionStore;

    private PropriedadesRelatorio properties;

    @BeforeEach
    void setUp() {
        properties = new PropriedadesRelatorio(Duration.ofMinutes(5), false);
    }

    @Test
    @DisplayName("cache devolve miss quando o Redis esta fora do ar")
    void cacheReturnsMissWhenRedisIsDown() {
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RedisConnectionFailureException("down"));

        CacheRedisResiliente cache = new CacheRedisResiliente(NomesCache.REPORT_SUMMARY, redis,
                properties.cacheTtl());

        assertThat(cache.get("chave")).isNull();
    }

    @Test
    @DisplayName("escrita no cache nao propaga falha do Redis")
    void cacheWriteSwallowsRedisFailure() {
        when(redis.opsForValue()).thenReturn(valueOperations);
        org.mockito.Mockito.doThrow(new RedisConnectionFailureException("down"))
                .when(valueOperations).set(anyString(), anyString(), eq(properties.cacheTtl()));

        CacheRedisResiliente cache = new CacheRedisResiliente(NomesCache.REPORT_SUMMARY, redis,
                properties.cacheTtl());

        assertThatCode(() -> cache.put("chave", "valor")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("escrita bem-sucedida aplica o TTL configurado")
    void cacheWriteAppliesTtl() {
        when(redis.opsForValue()).thenReturn(valueOperations);

        new CacheRedisResiliente(NomesCache.REPORT_SUMMARY, redis, properties.cacheTtl())
                .put("chave", "valor");

        verify(valueOperations).set(eq(NomesCache.REPORT_SUMMARY + ":chave"), eq("valor"),
                eq(Duration.ofMinutes(5)));
    }

    @Test
    @DisplayName("versao do cache volta a zero quando o Redis falha")
    void versionFallsBackToZero() {
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RedisConnectionFailureException("down"));

        assertThat(new ArmazemVersaoCache(redis, properties).current(USER_ID)).isZero();
    }

    @Test
    @DisplayName("incremento de versao nao propaga falha do Redis")
    void versionIncrementSwallowsRedisFailure() {
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenThrow(new RedisConnectionFailureException("down"));

        assertThatCode(() -> new ArmazemVersaoCache(redis, properties).increment(USER_ID))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("chave do cache inclui usuario, versao e parametros")
    void keyIncludesUserVersionAndParameters() {
        when(versionStore.current(USER_ID)).thenReturn(7L);
        GeradorChaveRelatorio generator = new GeradorChaveRelatorio(versionStore);

        Object key = generator.generate(this, null, USER_ID, YearMonth.of(2026, 11));

        assertThat(key).isEqualTo(USER_ID + ":7:2026-11");
    }

    @Test
    @DisplayName("meses diferentes geram chaves diferentes")
    void differentMonthsProduceDifferentKeys() {
        when(versionStore.current(USER_ID)).thenReturn(0L);
        GeradorChaveRelatorio generator = new GeradorChaveRelatorio(versionStore);

        Object november = generator.generate(this, null, USER_ID, YearMonth.of(2026, 11));
        Object december = generator.generate(this, null, USER_ID, YearMonth.of(2026, 12));

        assertThat(november).isNotEqualTo(december);
    }
}
