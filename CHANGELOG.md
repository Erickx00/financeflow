# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/); versionamento semântico.

## [0.1.0] — 2026-11-13

### Adicionado
- Autenticação JWT HS256 com Spring Security (Resource Server/Nimbus), registro, login e perfil.
- CRUD de contas com saldo derivado por agregação e recorte por data (`asOf`).
- CRUD de categorias com proteção contra exclusão em uso e tipo imutável.
- CRUD de transações com filtros, paginação com allowlist de ordenação e transições de status.
- Metas com aportes, conclusão automática e cancelamento.
- Relatórios: resumo mensal, mensal por ano, gastos por categoria e fluxo de caixa.
- Cache de relatórios em Redis com TTL de 5 minutos, invalidação por versão após commit e fallback para PostgreSQL.
- Schema Flyway V1–V5 com FK composta por proprietário, coerência de tipo categoria↔transação, `TIMESTAMPTZ` e locking otimista.
- 56 testes (unitários + integração com H2 em modo PostgreSQL e teste opcional com Redis real).
- Docker Compose (PostgreSQL 17 + Redis 7.4), Dockerfile multi-stage e pipeline GitHub Actions com serviços reais.
- Documentação: README, 6 ADRs, revisão de segurança, exemplos de API e plano de execução.

### Segurança
- Isolamento por proprietário em toda consulta; recurso alheio responde 404.
- Papel definido no servidor (sem mass assignment), BCrypt custo 12, senha atual obrigatória para troca.
- Mensagem genérica e custo de BCrypt constante no login (anti-enumeração por tempo).
- Swagger e Actuator desabilitados/reduzidos no perfil de produção.

### Conhecido
- Sem rate limiting, sem revogação de token, sem auditoria de exclusão (ver `README` e `docs/security/SECURITY-REVIEW.md`).
