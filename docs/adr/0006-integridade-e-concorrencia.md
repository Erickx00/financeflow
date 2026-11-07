# ADR 0006 — Integridade, dinheiro e concorrência

**Status:** aceito (Dia 1)

## Contexto
A spec sugere "avaliar" integridade referencial por proprietário e não trata concorrência, timezone nem restrição de moeda.

## Decisão

### Banco de dados
- Auditoria em `TIMESTAMPTZ` (não `TIMESTAMP`), mapeada para `Instant` — UTC explícito.
- Dinheiro em `NUMERIC(19,2)`; `amount > 0` via `CHECK`. `initial_balance` aceita valor negativo (conta pode nascer no vermelho); não há `CHECK` para ele.
- `currency VARCHAR(3) NOT NULL DEFAULT 'BRL' CHECK (currency = 'BRL')` — sem conversão no MVP.
- **Integridade por proprietário no banco:** `UNIQUE (id, user_id)` em `accounts` e `categories`, com FK composta `(account_id, user_id)` e `(category_id, user_id)` em `transactions`. Assim, o banco impede transação apontando para conta/categoria de outro usuário, mesmo com bug no serviço.
- `ON DELETE RESTRICT` nas FKs financeiras; e-mail único por `lower(email)` (índice funcional).
- Índices: `transactions (user_id, transaction_date) WHERE status = 'COMPLETED'` (relatórios), `(user_id, category_id)`, `(account_id, transaction_date)`, `categories (user_id, type)`, `goals (user_id, status)`.

### Aplicação
- `@Version` (optimistic locking) em `transactions` e `goals`; conflito mapeado para **409**.
- Toda consulta filtra por `user_id` do token; nunca por id isolado.
- Paginação: página 0, tamanho 20 (máx. 100), ordenação por allowlist (`transactionDate`, `amount`, `createdAt`).
- Saldo: `GET /accounts/{id}/balance?asOf=YYYY-MM-DD` (default hoje), `saldo = inicial + receitas concluídas − despesas concluídas` até `asOf`.
- Relógio injetado (`Clock`) com fuso de negócio configurável (`financeflow.time.zone`, default `America/Sao_Paulo`): "hoje" é o dia local do usuário, não o dia UTC. Instantes de auditoria continuam em UTC.

## Consequências
- O schema passa a garantir o invariante de propriedade, não só o serviço.
- `@Version` adiciona uma coluna por tabela e exige tratamento de 409 no cliente.
- O `Clock` com fuso evita que uma consulta feita às 22h em São Paulo use o dia seguinte (UTC) como "hoje".
