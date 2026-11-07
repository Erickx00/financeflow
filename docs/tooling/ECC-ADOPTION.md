# Adoção seletiva do ECC (affaan-m/ECC)

**Data:** Dia 1 · **Licença do ECC:** MIT · **Fonte:** https://github.com/affaan-m/ECC

## 1. O que o ECC é

Um "sistema operacional de engenharia para agentes": catálogo de 71 agentes, 314 skills, 98 comandos legados, hooks, memória, regras por stack, varredura de segurança (AgentShield) e um instalador universal (`npx ecc-universal`). O alvo principal é o **Claude Code** (plugin `ecc@ecc`), com adaptadores para Codex, Cursor, OpenCode, Gemini, Qwen, Copilot e outros.

Estrutura relevante do repositório: `agents/`, `skills/`, `commands/`, `hooks/`, `rules/` (common + por linguagem), `manifests/`, `scaffolds/`, `install.ps1|sh`, `.claude-plugin/`, `.codex-plugin/`, `.agents/skills/` (32 skills prontas para Codex/Copilot).

## 2. Avaliação de compatibilidade com este ambiente

| Requisito do ECC | Situação aqui | Conclusão |
|---|---|---|
| Claude Code ≥ 2.1 no `PATH` | Harness é o DeepSeek Harness (DSH), não Claude Code | Plugin oficial não se aplica |
| `npx ecc-universal` (npm) | Sem acesso à rede pelo shell | Instalação impossível agora |
| Escrita em `~/.claude` (escopo global) | Fora do workspace, bloqueada pelo sandbox | Instalação global impossível |
| Hooks dependem do modelo de hooks do Claude | DSH tem modelo próprio | Não portável |
| Memória/instintos em `~/.claude` | Fora do workspace | Não utilizável |

Instalar à força produziria configuração duplicada e inerte — exatamente o que o próprio README do ECC desaconselha ("do not stack install methods").

## 3. O que foi adotado

Quatro práticas do catálogo foram **reescritas como skills locais** em `.agents/skills/`, com atribuição MIT, adaptadas ao fluxo deste projeto:

| Skill local | Inspiração no ECC | Uso no projeto |
|---|---|---|
| `tdd-cycle` | skills de TDD/planejamento | Ciclo vermelho→verde→refatorar por funcionalidade |
| `code-review` | agentes de revisão (`reviewer`) | Revisão de diff antes de cada commit de feature |
| `security-review` | AgentShield / `the-security-guide.md` | Revisão OWASP/BOLA no Dia 6 e em toda rota nova |
| `git-delivery` | convenções de commit e entrega | Conventional Commits, 6–15/dia, histórico limpo |

## 4. O que foi rejeitado e por quê

- **Instalação global do plugin/skills (314):** incompatível com o harness e com o sandbox; geraria ruído.
- **Hooks e memória contínua:** dependem do runtime do Claude Code.
- **MCP `chrome-devtools` e afins:** sem navegador/network neste ambiente e sem relação com uma API backend.
- **71 agentes:** a especialização útil aqui (revisão, testes, segurança) já está coberta pelas 4 skills e pelos subagentes nativos do DSH.
- **Regras por stack do ECC:** não há pacote Java/Spring no catálogo de regras; as convenções foram escritas no `PROJECT_EXECUTION_PLAN.md` e nos ADRs.

## 5. Como validar

1. `ls .agents/skills/*/SKILL.md` — as 4 skills existem e têm frontmatter (`name`, `description`).
2. **Validação executada:** após a criação dos arquivos, o catálogo de skills desta sessão passou a listar `code-review`, `tdd-cycle`, `security-review` e `git-delivery` — ou seja, o harness descobre `.agents/skills/` automaticamente e elas são invocáveis.
3. Cada relatório diário referencia a skill usada (evidência de uso real).
4. As checklists de `code-review` e `security-review` são aplicadas antes de encerrar o dia; os achados viram commits `fix:`/`refactor:`.
5. Nenhuma configuração global foi alterada: `git status` do repositório é a única fonte de mudança.

> Nota: os plugins oficiais do ECC (hooks, memória, 314 skills, MCP) não foram instalados por incompatibilidade de harness e de sandbox; a adoção é deliberadamente seletiva e vive dentro do repositório, versionada junto com o código.
