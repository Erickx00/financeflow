# ADR 0003 — Cache de relatórios com Redis

**Status:** aceito (Dia 1)

## Contexto
A especificação exige cache de relatórios com TTL, invalidação após commit e funcionamento da API quando o Redis cai. A configuração sugerida (`@Cacheable` + `RedisCacheManager`) **não** atende ao requisito de indisponibilidade: uma falha de conexão propaga exceção e derruba a requisição. Além disso, `allEntries=true` no Redis executa `SCAN` e a serialização padrão muda entre Jackson 2 e 3.

## Decisão
Porta explícita `ReportCache` (`get`/`put`/`invalidateUser`), implementada por `RedisReportCache` sobre `StringRedisTemplate`:

- **Chave:** `ff:report:v1:{userId}:v{version}:{tipo}:{params}` — inclui sempre `userId` e a versão do usuário.
- **Versão por usuário:** chave `ff:cachever:{userId}`, incrementada com `INCR` **após o commit**. Invalidar "todos os relatórios do usuário" passa a ser O(1), sem `SCAN`/`KEYS`.
- **TTL:** 300 s, aplicado na escrita; é rede de segurança, não o mecanismo principal.
- **Serialização:** JSON do DTO de resposta (`String`), sem `@class`/default typing polimórfico e sem serialização Java nativa. Os DTOs de relatório usam apenas `String`, `BigDecimal` e inteiros — nada de `Object` no cache.
- **Invalidação:** evento de domínio `EventoTransacaoAlterada` publicado no serviço e tratado com `@TransactionalEventListener(phase = AFTER_COMMIT)`. Exceção na invalidação é capturada e logada (não derruba a resposta já commitada); o TTL cobre a perda.
- **Fallback:** toda operação Redis é protegida individualmente. Em falha: log `WARN` com chave, `get` retorna vazio, `put`/`increment` são ignorados e a resposta vem do PostgreSQL. Timeouts de conexão curtos (`connect-timeout` 500 ms, `timeout` 1 s) evitam somar latência.
- **Headers de evidência:** `X-Cache: HIT|MISS|BYPASS` apenas em perfil `dev` (facilita a demonstração no README e os testes).

## Consequências
- Falha de Redis degrada desempenho, nunca disponibilidade.
- Um cache hit nunca é servido para outro usuário, porque `userId` está na chave e a versão é por usuário.
- Mudança no contrato de um relatório exige incremento do prefixo (`v1` → `v2`); entradas antigas expiram sozinhas.
- Não há single-flight (anti-stampede); aceitável nesta escala e documentado.

## Testes exigidos
hit, miss, TTL (validado por `getExpire`, sem `sleep`), isolamento entre usuários, invalidação após commit, rollback não invalida, Redis fora do ar ⇒ resposta 200 vinda do banco.
