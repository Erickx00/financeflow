# Revisão de segurança

Data: Dia 6 · Escopo: API completa (auth, contas, categorias, transações, metas, relatórios, cache).

Checklist aplicado: `.agents/skills/security-review/SKILL.md`.

## 1. Achados e tratamento

| # | Categoria | Achado | Tratamento | Evidência |
|---|---|---|---|---|
| 1 | BOLA/IDOR | Nenhum `findById` sem `user_id` | Toda consulta usa `findByIdAndUserId`; recurso alheio ⇒ 404 | `AuthorizationMatrixTest` |
| 2 | Mass assignment | Registro aceitava `role`? | DTO não tem `role`; servidor força `USER` | `AuthServiceTest.registerNormalizesEmailAndForcesUserRole` |
| 3 | Senha | Troca de senha só com token | Exige `currentPassword`; recusa repetir a senha atual | `UsuarioServico.changePassword`, `RequisicaoAlterarSenha` |
| 4 | Enumeração de e-mail | Login respondia diferente para e-mail inexistente | Mensagem única + BCrypt "isca" para igualar o tempo | `AuthServiceTest` (2 casos) |
| 5 | Força do JWT | Segredo curto passaria despercebido | `PropriedadesJwt` falha no startup com < 32 bytes; expiração máx. 24h | validação no construtor |
| 6 | Claims | Dados sensíveis no token | Apenas `sub`, `email`, `role`, `iat`, `exp` | `ServicoJwt` |
| 7 | Vazamento de erro | Stack trace/SQL para o cliente | `TratadorGlobalExcecoes` mapeia tudo; 500 genérico e logado | `ApiIntegrationTest` (400/401/404/409) |
| 8 | Injeção | Ordenação arbitrária em `sort` | Allowlist `transactionDate, amount, createdAt, description`; direção validada | `ExcecaoRegraNegocio` + teste de 400 |
| 9 | Injeção SQL | Consultas dinâmicas | Apenas JPQL parametrizado / Criteria; nenhum SQL concatenado | revisão de código |
| 10 | Isolamento de cache | Chave sem usuário vazaria relatório | `userId` na chave + versão por usuário | `CacheInfrastructureTest`, `RedisCacheIntegrationTest` |
| 11 | Desserialização | `@class` no Redis permitiria gadget | Cache guarda JSON de DTO sem tipo polimórfico | `GatewayRelatoriosComCache` + `CodificadorJson` |
| 12 | DoS por paginação | `size` ilimitado | Clamp em 100, página ≥ 0 | `TransacaoServico.list` |
| 13 | DoS por intervalo | Fluxo de caixa com anos | Máximo de 366 dias | `RelatorioServico.cashFlow` |
| 14 | Indisponibilidade do Redis | `@Cacheable` propagaria exceção | Cache tolerante: miss/ignora escrita | `reportsWorkWithoutRedis` |
| 15 | Health enganoso | Redis fora derrubava `/health` | Indicador do Redis desabilitado (cache é opcional) | `application.yml` |
| 16 | Superfície exposta | Swagger em produção | `application-prod.yml` desliga springdoc; Actuator só `health` | `application-prod.yml` |
| 17 | CSRF | API stateless | CSRF desabilitado **porque** não há cookie de sessão | `ConfiguracaoSeguranca` |
| 18 | CORS | Origem curinga com credenciais | Nenhum CORS configurado (bloqueado por padrão) | `ConfiguracaoSeguranca` |

## 2. Riscos aceitos (documentados no README)

| Risco | Motivo | Mitigação futura |
|---|---|---|
| Sem rate limiting no login | fora do MVP | Bucket4j/Redis, ou API Gateway |
| Sem revogação de token | JWT stateless, TTL 1h | refresh token + denylist `jti` |
| Exclusão física de transação | simplicidade; saldos derivados | soft delete + auditoria |
| Sem HSTS/TLS na aplicação | responsabilidade do proxy | terminação TLS no ingress |
| Sem lockout de conta | idem rate limiting | contador por e-mail/IP |

## 3. Verificação contínua

- `mvn verify` roda a matriz de autorização e o fluxo de erros a cada commit.
- A CI executa a mesma suíte contra PostgreSQL 17 real, garantindo que as constraints de propriedade (FK composta) também estão ativas no banco.
