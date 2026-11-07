---
name: tdd-cycle
description: Ciclo vermelho-verde-refatorar para cada regra de negócio do FinanceFlow. Use ao implementar serviço, regra financeira ou endpoint novo.
---

# Ciclo TDD

1. **Vermelho:** escreva o teste da regra antes do código, com nome que descreva o comportamento (`naoPermiteCategoriaDeDespesaEmReceita`). Rode e confirme que falha pelo motivo certo.
2. **Verde:** implemente o mínimo que passa. Sem generalização antecipada.
3. **Refatorar:** nomes, duplicação, fronteiras de camada — com o teste verde a cada passo.

## Regras do projeto
- Regra de negócio mora no `service`, nunca no controller ou no repository.
- Teste de serviço usa Mockito para repositórios; teste de integração usa banco real.
- Cenários financeiros obrigatórios (spec 15.5) têm teste com nome explícito.
- Um teste que não pode falhar não é teste: verifique a asserção por mutação mental do código.
- Não use `Thread.sleep`; use `Clock` injetado e `getExpire` para TTL.

## Critério de pronto
Teste escrito antes, falha comprovada, passa depois, sem `@Disabled` e sem asserção vazia.
