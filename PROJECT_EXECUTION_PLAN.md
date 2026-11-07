# FinanceFlow — Plano de Execução (7 dias)

> Documento vivo. Atualizado ao final de cada dia com status, testes, commits e problemas.
> Especificação de origem: `FinanceFlow_Especificacao_Tecnica.md` v1.1 (com Redis).
> Repositório: `C:\Users\Erick\Documents\RedisFinanceFlow` (Git local; publicação no GitHub é ação do autor).

---

## 1. Estado inicial e auditoria do ambiente

| Item | Observação | Impacto |
|---|---|---|
| Pasta do projeto | Vazia, sem código; Git inicializado no Dia 1 | Nada a preservar; construção do zero |
| JDK | Java 25.0.1 LTS (Oracle) | Atende a especificação |
| Maven | 3.9.12 | Atende; wrapper não é obrigatório |
| Maven Central | Inacessível pelo shell (TLS/credenciais do sandbox) | Build depende do cache local `~/.m2` (1,09 GB, com Boot 4.1.1/4.1.2) |
| Docker | Docker Desktop instalado, daemon parado; `docker compose` v5.1.0 | PostgreSQL/Redis e Testcontainers dependem de o daemon ser iniciado |
| Escrita no workspace pelo shell | Bloqueada: grupo de sandbox com somente leitura/execução | `javac`, Maven e testes não gravam; **bloqueio crítico até a sessão ter acesso total** |
| Git | `git add`/`git commit` funcionam | Histórico real é possível mesmo antes do build |

**Conclusão da auditoria:** não há dívida técnica herdada; o risco está no ambiente (build) e não no código.

---

## 2. Auditoria da especificação — achados e decisões

| # | Achado na spec v1.1 | Decisão adotada | ADR |
|---|---|---|---|
| 1 | Três contratos diferentes para relatórios (`expenses-by-category` × `categories`, `month=2026-11` × `month=11&year=2026`) | Um único contrato: `?month=YYYY-MM`; rotas `/summary`, `/monthly`, `/categories`, `/cash-flow` | 0005 |
| 2 | Seção 7.5 exige rejeitar transições inválidas, mas não define a matriz | Matriz explícita `PENDING→COMPLETED`, `COMPLETED→PENDING`; PATCH para o mesmo status é idempotente (200) | 0004 |
| 3 | `DELETE /categories/{id}` convive com "não excluir categoria em uso sem política" | 409 Conflict quando houver transações vinculadas | 0004 |
| 4 | Meta tem `CANCELLED` **e** `DELETE`, sem semântica definida | `DELETE` só com `currentAmount = 0`; caso contrário, `PATCH status=CANCELLED` | 0004 |
| 5 | "Redis indisponível: API continua sem cache" não é atendido pela configuração sugerida (`@Cacheable` propaga exceção) | `CacheErrorHandler` + cliente tolerante que degrada para PostgreSQL | 0003 |
| 6 | Serialização do cache não definida; `GenericJackson2JsonRedisSerializer` mudou no Spring Data Redis 4 (Jackson 3) | Cache explícito com codec próprio e contrato de resposta versionado (`ff:report:v1`) | 0003 |
| 7 | Invalidação "de todos os relatórios do usuário" via `allEntries` faz `SCAN` no Redis | Contador de versão por usuário (`INCR`) incluído na chave: invalidação O(1) | 0003 |
| 8 | Teste de "rollback não invalida cache" não funciona com `AFTER_COMMIT` em teste `@Transactional` | Testes de cache sem transação de teste; rollback disparado por exceção em serviço | 0003 |
| 9 | Flyway 10+ exige `flyway-database-postgresql` separado | Dependência incluída no `pom.xml` | 0001 |
| 10 | `TIMESTAMP` sem timezone contradiz "padronizar UTC"; ausência de locking otimista | `TIMESTAMPTZ` + `Instant`; `@Version` em `transactions` e `goals` | 0006 |
| 11 | `PATCH /users/me/password` não exige senha atual | Exige `currentPassword`; erro genérico em login | 0002 |
| 12 | Registro pode aceitar `role` do cliente (mass assignment) | `role` sempre `USER` no servidor; nunca lido do DTO | 0002 |
| 13 | `currency` sem restrição; troca de moeda quebraria relatórios | `VARCHAR(3)` + `CHECK (currency='BRL')`; moeda imutável após criação | 0006 |
| 14 | Saldo "de hoje" vs "total" indefinido | `GET /accounts/{id}/balance?asOf=YYYY-MM-DD` (default: hoje) | 0006 |

---

## 3. Decisões arquiteturais

| ADR | Tema | Arquivo |
|---|---|---|
| 0001 | Stack e versões fixadas | [`docs/adr/0001-stack-e-versoes.md`](adr/0001-stack-e-versoes.md) |
| 0002 | Autenticação e autorização (JWT stateless) | [`docs/adr/0002-autenticacao-jwt.md`](adr/0002-autenticacao-jwt.md) |
| 0003 | Cache de relatórios com Redis | [`docs/adr/0003-cache-de-relatorios.md`](adr/0003-cache-de-relatorios.md) |
| 0004 | Status, exclusão e ciclo de vida | [`docs/adr/0004-status-e-exclusao.md`](adr/0004-status-e-exclusao.md) |
| 0005 | Contrato dos relatórios | [`docs/adr/0005-contrato-de-relatorios.md`](adr/0005-contrato-de-relatorios.md) |
| 0006 | Integridade, dinheiro e concorrência | [`docs/adr/0006-integridade-e-concorrencia.md`](adr/0006-integridade-e-concorrencia.md) |

---

## 4. Stack fixada

| Tecnologia | Versão | Papel |
|---|---|---|
| Java | 25 (LTS) | Linguagem |
| Spring Boot | 4.1.1 | Framework (BOM gerencia o resto) |
| Spring Security | 7.x (via BOM) | AuthN/AuthZ |
| Spring Security OAuth2 Resource Server + Nimbus JOSE | via BOM | Emissão/validação de JWT HS256 |
| Spring Data JPA / Hibernate | via BOM | Persistência |
| PostgreSQL | 17 | Fonte oficial dos dados |
| Flyway | 12.x (via BOM) + `flyway-database-postgresql` | Schema versionado |
| Redis | 7.4 | Cache de relatórios |
| springdoc-openapi | 3.2.0 | OpenAPI/Swagger (compatível com Boot 4) |
| JUnit 5/6 + Mockito + AssertJ | via BOM | Testes |
| Testcontainers | via BOM (perfil `it`) | Integração com PG/Redis na CI |

**Por que não `jjwt`:** o `spring-boot-starter-oauth2-resource-server` traz `NimbusJwtEncoder/Decoder` mantidos pelo time de Spring Security, integra-se ao filter chain sem código de validação próprio e evita acoplar o projeto a uma biblioteca cujo módulo `jjwt-jackson` ainda depende de Jackson 2 — o Boot 4 usa Jackson 3.

---

## 5. Arquitetura e estrutura

Monólito modular por domínio, camadas internas (`controller → service → repository`), DTOs na borda, mapeamento explícito, erros padronizados em `@RestControllerAdvice`.

```text
com.financeflow
├── config/            OpenApi, Cache, Time (Clock), Properties
├── security/          ConfiguracaoSeguranca, ServicoJwt, JwtAuthenticationConverter, UsuarioAtual
├── infrastructure/
│   └── cache/         ReportCache (porta), RedisReportCache, CacheKeys, InvalidationListener
├── user/              entity, repository, service, controller, dto, mapper
├── account/  category/  transaction/  goal/  report/
└── common/            exception, response (RespostaPaginada), validation, money
```

Regras de dependência: controller não acessa repository; service delimita transação; entidade nunca sai do service; cache só guarda DTO de resposta; `user_id` sempre vem do token, nunca do corpo.

---

## 6. Cronograma de 7 dias

### Dia 1 — Análise e preparação
**Objetivo:** entender o terreno, decidir o que será construído, preparar o ambiente e registrar as decisões.
- [x] Auditoria do ambiente e da especificação (14 achados)
- [x] Análise do repositório ECC e seleção de práticas
- [x] ADRs 0001–0006
- [x] Plano de execução e política de commits
- [x] `pom.xml` (Boot 4.1.1 + Java 25), `.gitignore`, `.editorconfig`, Docker Compose, Dockerfile, `.env.example`
- [x] 4 skills locais adaptadas do ECC, validadas no catálogo da sessão
- [ ] Build mínimo validado (bloqueado por permissão de escrita)

**DoD:** decisões registradas, ambiente configurado, nenhuma linha de código de negócio ainda.
**Commits:** 7. **Testes:** não aplicável (documentação e configuração).

### Dia 2 — Estrutura e fundação
**Objetivo:** projeto que sobe, conecta e valida o schema.
- `application.yml` + perfis `dev`/`test`
- Migrações V1–V5 (users, accounts, categories, transactions, goals) com constraints e índices
- Entidades JPA + enums, `ddl-auto: validate` verde
- `TratadorGlobalExcecoes`, `RespostaErro`, `RespostaPaginada`, `Clock`
- OpenAPI + Actuator; `GET /actuator/health` `UP`

**DoD:** aplicação inicia, Flyway aplica V1–V5, Hibernate valida sem erro, health `UP`.
**Commits:** 8–10. **Testes:** `@DataJpaTest` de mapeamento; smoke `@SpringBootTest`.

### Dia 3 — Usuários e autenticação
**Objetivo:** identidade e isolamento.
- Registro (e-mail normalizado, senha BCrypt, papel fixo `USER`)
- Login JWT HS256 (`sub` = userId, `role`), `expiresIn` configurável
- `GET/PUT /users/me`, `PATCH /users/me/password` com senha atual
- Security filter chain stateless, 401/403 padronizados, rotas públicas explícitas
- Testes: registro duplicado (409), login inválido (401), token expirado, rota sem token

**DoD:** fluxo completo cadastro → login → endpoint protegido funcionando.
**Commits:** 7–10. **Testes:** unitários + `@WebMvcTest` de segurança.

### Dia 4 — Contas, categorias e transações
**Objetivo:** o núcleo financeiro.
- CRUD de contas (desativação, saldo com `asOf`), categorias (409 em uso, tipo imutável)
- Transações: criação/edição/exclusão, filtros, paginação com allowlist de ordenação
- Regras: propriedade, categoria compatível com tipo, valor > 0, status
- Saldo derivado por agregação (somente `COMPLETED`)
- Testes: matriz de cenários financeiros da spec (15.5)

**DoD:** todos os cenários de 15.5 verdes; nenhuma consulta sem filtro de `user_id`.
**Commits:** 10–14. **Testes:** unitários de serviço + integração de repository.

### Dia 5 — Metas, relatórios e Redis
**Objetivo:** fechar o escopo funcional e entregar o diferencial técnico.
- Metas + contribuições (limites, status `COMPLETED` automático, concorrência otimista)
- 4 relatórios com contrato fechado
- Cache: chave com versão por usuário, TTL 5 min, invalidação `AFTER_COMMIT`, fallback
- Testes: hit, miss, TTL, isolamento por usuário, invalidação, rollback não invalida, Redis fora

**DoD:** segunda chamada idêntica não consulta o banco; alteração invalida; Redis desligado não derruba a API.
**Commits:** 10–14. **Testes:** integração com Redis real (perfil `it`).

### Dia 6 — Segurança e qualidade
**Objetivo:** provar que o sistema resiste.
- Revisão de autorização em todos os endpoints (BOLA/IDOR), revisão de logs e segredos
- Mass assignment, enumeração de recursos, validação de parâmetros inválidos (400, nunca 500)
- Testes de regressão, revisão de código com agente revisor
- Correções e refatorações pendentes

**DoD:** nenhum acesso cruzado possível; nenhum 500 em entrada inválida; suíte verde.
**Commits:** 7–12. **Testes:** regressão completa + revisão adversarial.

### Dia 7 — Entrega
**Objetivo:** outra pessoa consegue rodar.
- README (setup, arquitetura, decisões, evidências do Redis), OpenAPI final
- GitHub Actions com PostgreSQL/Redis como services + Testcontainers
- Revisão do histórico, `CHANGELOG` do processo, tag `v0.1.0`
- Relatório final e checklist da spec

**DoD:** clone → `docker compose up` → `mvn verify` → Swagger acessível, documentado no README.
**Commits:** 8–12. **Testes:** suíte completa + build limpo.

---

## 7. Agentes e skills (ECC)

Análise completa em [`docs/tooling/ECC-ADOPTION.md`](tooling/ECC-ADOPTION.md). Resumo:
- **Não** foi feito `npx ecc-universal` (instalador oficial): ele escreve em `~/.claude` (fora do workspace), exige rede e é específico do Claude Code, que não é o harness desta sessão. Instalar duplicaria configuração e não seria utilizável aqui.
- Foram **adaptados** 4 fluxos do catálogo ECC para skills locais do projeto (`.agents/skills/`), com atribuição MIT: ciclo TDD, revisão de código, revisão de segurança e entrega Git.
- Validação: cada skill é referenciada no cronograma e usada nos relatórios diários (evidência de uso, não de instalação).

---

## 8. Qualidade, commits e definição de pronto

Ciclo por funcionalidade: **planejar → implementar → testar → revisar → corrigir → validar → commitar**.

- 6 a 15 commits por dia, Conventional Commits, sem commits artificiais.
- Nenhum commit com build vermelho (quando o build estiver disponível).
- `mvn verify` antes de encerrar o dia; warnings tratados.
- Revisão de diff e de regras de propriedade em toda entrega.
- Documentação atualizada no mesmo dia da mudança.

---

## 9. Riscos e mitigação

| Risco | Probabilidade | Mitigação |
|---|---|---|
| Build bloqueado por permissão de escrita | Alta (confirmado) | Sessão em acesso total; commits de código seguem possíveis via editor |
| Sem Docker local | Alta | Testes de integração limitados ao perfil `it`/CI; unidade cobre regras |
| Sem Maven Central | Alta | Versões fixadas no cache local (Boot 4.1.1) |
| APIs do Boot 4/Jackson 3/Security 7 menos documentadas | Média | Uso do caminho oficial (Resource Server, `RedisTemplate`), sem APIs removidas |
| Escopo de 7 dias | Média | Cortes previstos: `/reports/monthly` fica sem cache; sem refresh token; sem rate limiting (documentado) |

---

## 10. Status final por dia

| Dia | Escopo | Status | Commits | Testes ao fim do dia |
|---|---|---|---|---|
| 1 | Análise, decisões, plano, ECC, ambiente | Concluído | 7 | — (documentação) |
| 2 | Fundação: schema, config, entidades, build | Concluído | 6 | compilação verde |
| 3 | Autenticação e perfis | Concluído | 2 | 5 |
| 4 | Contas, categorias e transações | Concluído | 5 | 20 |
| 5 | Metas, relatórios e cache Redis | Concluído | 3 | 36 |
| 6 | Segurança, testes e qualidade | Concluído | 14 | 56 (2 skip sem Redis) |
| 7 | Entrega: README, CI, licença, release | Concluído | 4 | 56 |

> Contagem por fase (aproximada; pequenos ajustes de CI/documentação entraram no fechamento). O histórico completo e exato está em `git log --oneline`.

## 11. Relatório de execução

### Dia 1 — análise e preparação (7 commits)

**Realizado**
- Auditoria do ambiente: JDK 25.0.1, Maven 3.9.12, Docker instalado sem daemon, Maven Central inicialmente inacessível, escrita no workspace bloqueada por permissão de sandbox (diagnóstico com o script oficial de ACL do DSH).
- Auditoria da especificação: 14 achados, todos com decisão registrada (seção 2).
- ECC analisado (71 agentes, 314 skills, 98 comandos). Instalação global descartada por incompatibilidade de harness e por escrever fora do workspace; 4 práticas foram adaptadas para `.agents/skills/` e **validadas no catálogo da sessão**.
- `pom.xml` (Boot 4.1.1, Java 25), Docker Compose (PG 17 + Redis 7.4), Dockerfile multi-stage, `.env.example`, `.gitignore`, `.editorconfig`, `.gitattributes`.
- Plano de execução e política de commits.

**Commits:** `a1c04d7` `b68b9c6` `dbdbe3c` `a85776e` `428746b` `2a56c52` `5d8224f`

**Problemas e soluções**
- Escrita negada no workspace: diagnosticada com `diagnose-windows-sandbox-acl`; resolvida ao liberar o acesso total da sessão.
- Docker sem daemon/WSL: contornado com H2 em modo PostgreSQL nos testes locais e PostgreSQL real na CI.

### Dia 2 — fundação (6 commits)

**Realizado**
- Migrações `V1`–`V5`: `TIMESTAMPTZ`, `NUMERIC(19,2)`, `CHECK` de tipo/status/moeda, índice único funcional em `lower(email)`, índice parcial para relatórios, `version` para locking otimista.
- Integridade por proprietário **no banco**: `UNIQUE (id, user_id)` + FK composta em `transactions`; `UNIQUE (id, type)` + FK `(category_id, type)` impedindo categoria de tipo incompatível.
- Entidades, enums e repositórios (incluindo as consultas de agregação dos relatórios).
- `application.yml`, `application-dev.yml`, `application-test.yml`; propriedades de JWT e de relatório validadas no startup.
- Compilação offline com o cache local do Maven; correção da API de `Cache` do Spring 7 (métodos `get(key, Class)` e `get(key, Callable)`).

**Commits:** `1151025` `6b7d7c5` `2aec0af` `e836b6d` `519ea9a` `ac51e31`

**Problemas e soluções**
- `mvn` não escrevia no `~/.m2` (fora do workspace): build passou a usar o cache existente em modo offline.
- Erros de compilação na interface `Cache` (Spring 7): métodos abstratos adicionais implementados.

### Dia 3 — autenticação e usuários (2 commits)

**Realizado**
- JWT HS256 com `JwtEncoder/JwtDecoder` (Nimbus), claims `sub`/`email`/`role`, expiração configurável e validação de segredo ≥ 32 bytes no startup.
- `ConfiguracaoSeguranca` stateless, rotas públicas explícitas, 401/403 no mesmo contrato JSON de erro.
- Registro com e-mail normalizado, papel fixo `USER`, BCrypt 12; login com mensagem genérica e custo de BCrypt constante (isca) contra enumeração por tempo.
- `GET/PUT /users/me`, `PATCH /users/me/password` exigindo a senha atual.
- `@RestControllerAdvice` com 400/404/409/500 padronizados e validação por campo.

**Commits:** `6d9b378` (auth + erros) e `test(user)`.

**Problemas e soluções**
- `CustomUserDetailsService` ficou sem uso ao optar por autenticação direta; removido para não deixar código morto.

### Dia 4 — contas, categorias e transações (5 commits)

**Realizado**
- Contas: criação com moeda BRL imutável, listagem, atualização, ativação/desativação e saldo derivado com `asOf`.
- Categorias: unicidade por nome/tipo, tipo imutável, exclusão bloqueada (409) quando em uso.
- Transações: CRUD, filtros combináveis (`Specification`), paginação com allowlist de ordenação e `size` máximo 100, transições de status com idempotência.
- Evento de domínio `EventoTransacaoAlterada` publicado apenas quando a alteração afeta relatório (concluída).
- Testes de serviço para contas, categorias e transações.

**Commits:** `891b2cc` `3389e11` `8fa450d` + `test(account)` + `test(category)`.

**Problemas e soluções**
- `Sort.Direction` como `@RequestParam` recusava `desc` minúsculo (400): binding trocado por `String` com `Sort.Direction.fromString` e erro 400 explícito.

### Dia 5 — metas, relatórios e Redis (3 commits)

**Realizado**
- Metas: criação, atualização (reabre/conclui conforme o alvo), aportes com conclusão automática, cancelamento e exclusão só sem aportes.
- Relatórios com contrato único (`month=YYYY-MM`): resumo, mensal, categorias e fluxo de caixa; arredondamento `HALF_UP`, taxa de economia sem divisão por zero.
- Cache: chave `ff:report:{tipo}:{userId}:v{versao}:{params}`, TTL 300 s, `INCR` de versão após commit (`@TransactionalEventListener`), cache tolerante a falha e sem polimorfismo na serialização.
- Testes de cálculo, de validação de entrada e de infraestrutura de cache.

**Commits:** `bbefffc` `6599387` + `test(report)`.

**Problemas e soluções**
- `@Cacheable` com `RedisCacheManager` padrão derrubaria a API com o Redis fora: implementado `CacheManager` próprio com `Cache` resiliente.
- O valor em cache precisava ser tipado: gateway retorna JSON (`String`) e o serviço desserializa com o DTO esperado.

### Dia 6 — segurança e qualidade (14 commits)

**Realizado**
- Suíte completa: 56 testes, incluindo fluxo HTTP ponta a ponta, matriz de autorização (leitura e escrita) e fallback sem Redis.
- Revisão de segurança documentada em `docs/security/SECURITY-REVIEW.md` (18 achados tratados, 5 riscos aceitos).
- Correções: fuso de negócio (`America/Sao_Paulo`) para o "hoje" das regras, Redis como dependência opcional no health, perfil de produção sem Swagger, direção de ordenação case-insensitive.
- CI com PostgreSQL 17 e Redis 7.4 como serviços, rodando a suíte duas vezes (H2 e banco real com Flyway + `ddl-auto: validate`).

**Commits:** `220fb0d` `a0ba292` `8ae0c92` `d64ada4` `49bc2f6` `86c6a06` `91a8686` `fbec484` `8b03403` `959d78f` `178bb8f` + 3 de documentação/teste.

**Problemas e soluções**
- `/actuator/health` retornava 503 com o Redis fora: indicador desabilitado (cache é opcional) — health voltou a 200.
- `asOf` usava data UTC e retornava o dia seguinte após 21h em São Paulo: `Clock` passou a usar o fuso de negócio configurável.
- Mockito não inicializava no JDK 25: `-XX:+EnableDynamicAgentLoading` no Surefire.

### Dia 7 — entrega (4 commits)

**Realizado**
- README completo (setup, endpoints, arquitetura, cache, segurança, testes, limitações), exemplos de API, CHANGELOG, LICENSE MIT e este relatório.
- Verificação final: `mvn -o verify` verde, 56 testes (2 skip sem Redis), JAR gerado, aplicação exercitada por HTTP (cadastro → login → conta → categoria → transação → saldo → relatórios → OpenAPI/Swagger 200).
- Tag `v0.1.0` no histórico.

**Commits:** `fea26ae` + licença, changelog e este documento.

## 12. Desvios do plano original

| Item planejado | O que foi feito | Motivo |
|---|---|---|
| 6–15 commits por dia | Dias 1, 2, 6 e 7 dentro da faixa; dias 3, 4 e 5 com 2–5 commits | Execução contínua em uma sessão; cada funcionalidade saiu em um commit completo em vez de fragmentos artificiais |
| Testcontainers | H2 (modo PostgreSQL) local + PostgreSQL/Redis reais como serviços da CI | Docker/WSL indisponíveis neste ambiente; `Testcontainers` ficou como perfil opcional |
| `POST /auth/register` retornando token | Retorna o usuário (201) e o login é explícito | Separação de responsabilidades e contrato mais previsível |
| `/reports/monthly` cacheado | Sem cache | 12 chaves por usuário sem ganho real |
| `ADMIN` com endpoints | Papel existe, sem endpoint administrativo | Evitar código morto e superfície de ataque sem requisito |

## 13. Checklist da especificação (§19)

- [x] Projeto executa com Java 25
- [x] Build Maven funciona
- [x] PostgreSQL executa via Docker Compose
- [x] Redis executa via Docker Compose
- [x] Flyway gerencia o schema
- [x] Cadastro e login funcionam
- [x] JWT protege endpoints
- [x] Senhas armazenadas com hash
- [x] Dados isolados por usuário
- [x] CRUD de contas, categorias e transações
- [x] Saldo calculado corretamente
- [x] Filtros e paginação
- [x] Metas e contribuições
- [x] Relatórios consistentes
- [x] Relatórios com cache Redis e TTL
- [x] Cache isolado por usuário e invalidado após commit
- [x] Testes de cache (hit, TTL, isolamento, invalidação, rollback, indisponibilidade)
- [x] Exceções com respostas padronizadas
- [x] Testes unitários e de integração passando
- [x] Swagger disponível (desligado em produção)
- [x] README explica como executar
- [x] GitHub Actions executa build e testes

## 14. Pendências e próximos passos

1. Publicar o repositório no GitHub e habilitar o workflow (ação do autor; não há credencial nesta sessão).
2. Rodar a CI uma vez para validar as migrações Flyway contra PostgreSQL 17 real.
3. Implementar rate limiting no login e revogação de token (evolução declarada).
4. Adicionar auditoria de exclusão (soft delete) quando o domínio exigir histórico.
