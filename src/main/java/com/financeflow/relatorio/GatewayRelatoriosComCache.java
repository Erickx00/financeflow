package com.financeflow.relatorio;

import com.financeflow.infraestrutura.cache.NomesCache;
import com.financeflow.transacao.TipoTransacao;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Fronteira de cache. Retorna JSON (String) para que a leitura do Redis seja
 * sempre tipada pelo chamador, sem metadados de tipo no valor armazenado.
 */
@Service
public class GatewayRelatoriosComCache {

    private final CalculadoraRelatorio calculator;
    private final CodificadorJson codec;

    public GatewayRelatoriosComCache(CalculadoraRelatorio calculator, CodificadorJson codec) {
        this.calculator = calculator;
        this.codec = codec;
    }

    @Cacheable(cacheNames = NomesCache.REPORT_SUMMARY, keyGenerator = "reportKeyGenerator")
    public String summaryJson(UUID usuarioId, YearMonth month) {
        return codec.write(calculator.summary(usuarioId, month));
    }

    @Cacheable(cacheNames = NomesCache.REPORT_CATEGORIES, keyGenerator = "reportKeyGenerator")
    public String categoriasJson(UUID usuarioId, YearMonth month, TipoTransacao tipo) {
        return codec.write(calculator.categorias(usuarioId, month, tipo));
    }

    @Cacheable(cacheNames = NomesCache.REPORT_CASH_FLOW, keyGenerator = "reportKeyGenerator")
    public String cashFlowJson(UUID usuarioId, LocalDate from, LocalDate to) {
        return codec.write(calculator.cashFlow(usuarioId, from, to));
    }
}
