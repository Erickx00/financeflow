# ADR 0007 — Nomenclatura: domínio em português, técnica em inglês

**Status:** aceito

## Contexto
O projeto é de domínio financeiro brasileiro (BRL, fuso `America/Sao_Paulo`, mensagens em português), mas usa bibliotecas cujos contratos são em inglês (Spring, JPA, Jackson). Misturar tudo exige uma regra explícita para não virar inconsistência.

## Decisão
- **Domínio em português:** entidades, enums, DTOs e campos — `Usuario`, `Conta`, `Categoria`, `Transacao`, `Meta`, `Papel`, `TipoConta`, `TipoTransacao`, `StatusTransacao`, `StatusMeta`, `saldoInicial`, `dataTransacao`, `valorAlvo`.
- **Sufixos técnicos em inglês:** `Controller`, `Service`, `Repository`, `Mapper`, `Config` — `ContaController`, `ContaService`, `ContaRepository`, `ContaMapper`, `CacheConfig`.
- **Pacotes em português:** `usuario`, `conta`, `categoria`, `transacao`, `meta`, `relatorio`, `autenticacao`, `seguranca`, `configuracao`, `comum`, `infraestrutura.cache`.
- **Banco em português:** `usuarios`, `contas`, `categorias`, `transacoes`, `metas`; colunas `usuario_id`, `senha_hash`, `saldo_inicial`, `data_transacao`, `valor_alvo`, `criado_em`.
- **Rotas em português:** `/api/v1/contas`, `/api/v1/transacoes`, `/api/v1/relatorios/resumo`.
- **Protocolo permanece em inglês:** chaves de cache (`ff:report:*`), tipos e status (`INCOME`, `EXPENSE`, `PENDING`, `COMPLETED`) e propriedades do Spring — são valores técnicos, não vocabulário do domínio.

## Consequências
- Domínio legível sem tradução mental; papel técnico de cada classe reconhecível pelo sufixo.
- Mensagens de erro e contrato da API em português.
- Status e tipos em inglês exigem glossário no README (feito).
