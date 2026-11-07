# ADR 0005 — Contrato dos relatórios

**Status:** aceito (Dia 1)

## Contexto
A spec v1.1 apresenta três contratos incompatíveis para os mesmos relatórios (`expenses-by-category` × `categories`; `month=2026-11` × `month=11&year=2026`). Como o DTO de resposta é o valor gravado no cache, o contrato precisa ser fechado antes da implementação.

## Decisão
Parâmetro único `month=YYYY-MM` (ISO-8601), validado por `@Pattern`, em todas as rotas de mês.

| Rota | Parâmetros | Resposta |
|---|---|---|
| `GET /reports/summary` | `month` | `period`, `totalIncome`, `totalExpenses`, `balance`, `savingsRate` |
| `GET /reports/monthly` | `year` | lista de `{month, totalIncome, totalExpenses, balance}` (12 meses, zero quando vazio) |
| `GET /reports/categories` | `month`, `type` (default `EXPENSE`) | `period`, `total`, lista `{categoryId, name, color, total, percentage}` |
| `GET /reports/cash-flow` | `from`, `to` (máx. 366 dias, `from ≤ to`) | `from`, `to`, `points[{date, income, expenses, net}]`, `totalIncome`, `totalExpenses`, `net` |

Regras:
- Somente transações `COMPLETED` do usuário autenticado, por `transaction_date`.
- `savingsRate = (income - expenses) / income * 100`, escala 2, `HALF_UP`; **`totalIncome = 0` ⇒ `savingsRate = 0`** (nunca divisão por zero). Valores negativos são permitidos e refletem gasto maior que receita.
- Dinheiro em `BigDecimal` com escala 2; percentuais com escala 2.
- `/reports/monthly` não é cacheado no MVP (consulta barata e cobriria 12 chaves); os outros três são.
- Datas de série em ISO-8601 (`String`) para manter o contrato do cache estável e independente de timezone.

## Consequências
- Frontend e testes têm um contrato único; cache e API não divergem.
- Mudança de forma de resposta exige `v2` na chave de cache.
