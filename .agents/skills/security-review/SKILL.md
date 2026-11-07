---
name: security-review
description: Revisão de segurança OWASP/BOLA para endpoints e fluxos do FinanceFlow. Use em toda rota nova, mudança de autenticação ou antes de encerrar o dia 6.
---

# Revisão de segurança

## Identidade e autorização (BOLA/IDOR)
- [ ] Toda consulta/escrita parte do `userId` do token — nunca de id do corpo/query?
- [ ] Recurso de terceiro responde 404 (não revela existência)?
- [ ] `role` do DTO é ignorado; `ADMIN` não acessa dados financeiros de terceiro?
- [ ] Endpoint administrativo exige `hasRole('ADMIN')` e nenhum dado financeiro?

## Entradas
- [ ] Bean Validation na borda **e** validação de negócio no service?
- [ ] Enum/`UUID`/data inválidos retornam 400 (nunca 500)?
- [ ] Ordenação e paginação têm allowlist e limite máximo?
- [ ] Datas de relatório têm intervalo limitado (evita varredura ilimitada)?

## Segredos e logs
- [ ] Nenhum segredo, senha, hash ou token em log, resposta ou repositório?
- [ ] `JWT_SECRET` vem de ambiente e tem ≥ 32 bytes?
- [ ] Stack trace não chega ao cliente?
- [ ] Login responde genérico (não revela e-mail cadastrado)?

## Sessão e transporte
- [ ] API stateless: CSRF desabilitado **porque** não há cookie de sessão?
- [ ] Swagger desabilitado em produção?
- [ ] CORS restrito ou ausente (sem `*` com credenciais)?

## Saída
Achados com severidade e teste que comprova a correção. Vulnerabilidade sem teste de regressão não está corrigida.
