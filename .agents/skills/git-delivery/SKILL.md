---
name: git-delivery
description: Organização de commits, histórico e entrega diária do FinanceFlow. Use ao commitar, ao fechar o dia ou antes de revisar o histórico.
---

# Entrega e histórico Git

## Commits
- Conventional Commits: `feat|fix|refactor|test|docs|chore|build|ci(escopo): descrição`.
- Um commit = uma alteração coerente e funcional. Nunca fracionar para bater meta.
- 6 a 15 commits por dia; se o dia comporta 3, o problema é o escopo, não o commit.
- Descrição no imperativo, minúscula, sem ponto final: `feat(transaction): calcula saldo por agregação`.

## Antes de commitar
- [ ] Build e testes verdes (ou ausência de build **explicada** no relatório do dia).
- [ ] `git diff` revisado — nenhum arquivo temporário, `.env`, log ou `target/`.
- [ ] A mudança corresponde exatamente à mensagem.

## Fechamento do dia
- [ ] Atualizar `PROJECT_EXECUTION_PLAN.md` (status, commits, testes, problemas).
- [ ] Relatório do dia: realizado, arquivos principais, commits, testes, pendências.
- [ ] `git log --oneline` legível por um terceiro sem contexto.

## Proibido
`git add .` sem revisão, `--amend` em commit já publicado, `push --force`, commit de segredo.
