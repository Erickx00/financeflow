---
name: code-review
description: Revisão de diff antes de cada commit de funcionalidade no FinanceFlow. Use ao encerrar uma feature ou antes de commitar mudanças de arquitetura.
---

# Revisão de código

Revise o diff real (`git diff --staged`), não a intenção.

## Checklist
- [ ] A mudança tem um único propósito e mensagem de commit coerente?
- [ ] Controller só orquestra? Regra de negócio está no service?
- [ ] Toda consulta filtra por `user_id` do token?
- [ ] Entidade JPA vazou para resposta ou para o controller?
- [ ] Dinheiro é `BigDecimal` (nunca `double`), com escala definida?
- [ ] Erro previsto tem exceção de domínio e status correto (400/404/409)?
- [ ] Nenhum `catch (Exception)` silencioso; logs sem PII, token ou senha?
- [ ] Migração Flyway é aditiva e idempotente para bancos existentes?
- [ ] Teste cobre o caminho feliz **e** a recusa?
- [ ] Duplicação nova? Pode virar mapper/utilitário?
- [ ] Nomes revelam intenção; nada de `data`, `aux`, `temp`?

## Saída da revisão
Lista curta de achados com severidade (`bloqueia`, `deveria`, `nit`) e o commit que resolve cada um. Sem comentário genérico.
