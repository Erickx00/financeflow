# ADR 0004 — Status, exclusão e ciclo de vida

**Status:** aceito (Dia 1)

## Contexto
A spec define status de transação e de meta, mas não fixa as transições, nem a política de exclusão de transações, categorias e metas.

## Decisão

### Transações
- Matriz permitida: `PENDING → COMPLETED`, `COMPLETED → PENDING`. PATCH com o mesmo status é idempotente (200, sem efeito).
- Exclusão **física** no MVP (documentada), com saldos e relatórios sempre derivados por consulta — não há saldo persistido a corrigir. Auditoria de exclusão fica para a evolução.
- Alterar `status` é a única forma de mudar status (PUT não aceita `status`).

### Contas
- Sem `DELETE`: desativação por `PATCH /accounts/{id}/status`.
- Conta inativa não aceita transações novas (409) e continua no histórico dos relatórios.
- `currency` imutável após a criação.

### Categorias
- `DELETE` retorna **409 Conflict** quando existir transação vinculada; nunca apaga em cascata.
- Alterar `type` (INCOME↔EXPENSE) com transações vinculadas é rejeitado (409).

### Metas
- Transições: `ACTIVE → COMPLETED` (automática ao atingir o alvo), `ACTIVE → CANCELLED`, `COMPLETED → ACTIVE` se o alvo for elevado. `CANCELLED` é terminal.
- `DELETE` permitido apenas com `currentAmount = 0`; caso contrário, 409 e orientação para cancelar.
- Contribuir para meta `COMPLETED`/`CANCELLED` ⇒ 409. Contribuição acima do alvo é aceita e conclui a meta.

## Consequências
- Regras explícitas e testáveis, sem "estados impossíveis".
- Perda de histórico de exclusão de transação é uma limitação consciente do MVP, registrada no README.
