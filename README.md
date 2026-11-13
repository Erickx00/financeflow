# FinanceFlow

API REST para gerenciamento financeiro pessoal — receitas, despesas, contas, categorias, metas e relatórios, com autenticação JWT, isolamento total por usuário e cache de relatórios em Redis.

| | |
|---|---|
| **Linguagem** | Java 25 (LTS) |
| **Framework** | Spring Boot 4.1.1 / Spring Security 7 |
| **Banco** | PostgreSQL 17 (fonte oficial dos dados) |
| **Cache** | Redis 7.4 (somente leitura de relatórios) |
| **Build** | Maven |
| **Testes** | 56 testes (unitários + integração), JUnit 6 + Mockito + AssertJ + MockMvc |

## Como executar

```bash
# 1. PostgreSQL 17 e Redis 7.4
docker compose up -d

# 2. Variáveis de ambiente
cp .env.example .env
# JWT_SECRET precisa ter no mínimo 32 bytes: openssl rand -base64 48

# 3. Aplicação (Flyway aplica as migrações no startup)
mvn spring-boot:run

curl http://localhost:8080/actuator/health
open http://localhost:8080/swagger-ui.html
```

A aplicação **falha no startup** se `JWT_SECRET` estiver ausente ou tiver menos de 32 bytes — validação intencional em `PropriedadesJwt`.

## Endpoints

Prefixo `/api/v1`. Rotas financeiras exigem `Authorization: Bearer <jwt>`.

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/autenticacao/cadastrar` | Cadastro (201) |
| `POST` | `/autenticacao/login` | Login (200, devolve `accessToken`) |
| `GET` | `/usuarios/perfil` | Perfil do usuário |
| `PUT` | `/usuarios/perfil` | Atualiza nome |
| `PATCH` | `/usuarios/perfil/senha` | Troca de senha (exige a atual, 204) |
| `POST` | `/contas` | Cria conta |
| `GET` | `/contas` | Lista contas |
| `GET` | `/contas/{id}` | Consulta conta |
| `PUT` | `/contas/{id}` | Atualiza nome e tipo |
| `PATCH` | `/contas/{id}/status` | Ativa/desativa |
| `GET` | `/contas/{id}/saldo?dataReferencia=YYYY-MM-DD` | Saldo derivado até a data |
| `POST` | `/categorias` | Cria categoria (`INCOME`/`EXPENSE`) |
| `GET` | `/categorias?type=` | Lista com filtro |
| `PUT` | `/categorias/{id}` | Atualiza nome/cor (tipo imutável) |
| `DELETE` | `/categorias/{id}` | Exclui (409 se houver transações) |
| `POST` | `/transacoes` | Registra transação |
| `GET` | `/transacoes?tipo=&status=&contaId=&categoriaId=&dataInicio=&dataFim=&pagina=&tamanho=&ordenarPor=&direcao=` | Filtros + paginação |
| `PUT` | `/transacoes/{id}` | Atualiza (status não muda aqui) |
| `PATCH` | `/transacoes/{id}/status` | `PENDING` ↔ `COMPLETED` |
| `DELETE` | `/transacoes/{id}` | Exclui |
| `POST` | `/metas` | Cria meta |
| `PATCH` | `/metas/{id}/contribuicoes` | Aporte (conclui ao atingir o alvo) |
| `PATCH` | `/metas/{id}/status` | Cancela |
| `GET` | `/relatorios/resumo?mes=YYYY-MM` | Resumo do mês (cacheado) |
| `GET` | `/relatorios/mensal?ano=YYYY` | 12 meses (sem cache) |
| `GET` | `/relatorios/categorias?mes=YYYY-MM&tipo=EXPENSE` | Gastos por categoria (cacheado) |
| `GET` | `/relatorios/fluxo-caixa?de=&ate=` | Fluxo diário (cacheado) |

Exemplos de corpo:

```json
{"nome":"Erick","email":"erick@example.com","senha":"SenhaMuitoForte123"}
{"nome":"Conta principal","tipo":"CHECKING","saldoInicial":1000.00,"moeda":"BRL"}
{"nome":"Mercado","tipo":"EXPENSE","cor":"#FF8800"}
{"contaId":"...","categoriaId":"...","descricao":"Compra","valor":250.90,
 "tipo":"EXPENSE","dataTransacao":"2026-11-13","status":"COMPLETED"}
{"nome":"Notebook","valorAlvo":5000.00,"prazo":"2027-06-30"}
```

## Arquitetura

Monólito modular por domínio. Classes de domínio em português; sufixos técnicos (`Controller`, `Service`, `Repository`, `Mapper`, `Config`) permanecem em inglês.

```text
com.financeflow
├── autenticacao/    AutenticacaoController, AutenticacaoService, JwtService
├── usuario/         Usuario, UsuarioService, UsuarioRepository
├── conta/           Conta, ContaService, ContaRepository, ContaController
├── categoria/       Categoria, CategoriaService, ...
├── transacao/       Transacao, TransacaoService, TransacaoRepository, ...
├── meta/            Meta, MetaService, ...
├── relatorio/       CalculadoraRelatorio, RelatorioService, gateway de cache
├── seguranca/       SegurancaConfig, ServicoJwt, UsuarioAtual
├── infraestrutura/  cache resiliente, versão por usuário, listener AFTER_COMMIT
├── configuracao/    CacheConfig, OpenApiConfig, RelogioConfig
└── comum/           exceções, RespostaErro, RespostaPaginada
```

**Regras:** controller não acessa repository; entidade não sai do service; DTO nunca é entidade; o `usuarioId` vem sempre do token.

### Banco (português)

`usuarios`, `contas`, `categorias`, `transacoes`, `metas` — com colunas em português (`usuario_id`, `senha_hash`, `saldo_inicial`, `data_transacao`, `valor_alvo`, `criado_em`…).

O schema garante no banco o que normalmente fica só no serviço:

- FK composta `(conta_id, usuario_id)` e `(categoria_id, usuario_id)` → impossível registrar transação em conta/categoria de outro usuário;
- FK composta `(categoria_id, tipo)` → impossível usar categoria `EXPENSE` em transação `INCOME`;
- `CHECK` de valor positivo, status e tipo; `TIMESTAMPTZ` para auditoria; `versao` para locking otimista.

## Cache de relatórios (Redis)

| Item | Decisão |
|---|---|
| Chave | `ff:report:{tipo}:{usuarioId}:v{versao}:{parametros}` |
| TTL | 300 s |
| Invalidação | `INCR ff:cachever:{usuarioId}` **após o commit** (`@TransactionalEventListener(AFTER_COMMIT)`) |
| Isolamento | `usuarioId` sempre na chave e versão por usuário |
| Serialização | JSON do DTO, sem `@class`/polimorfismo |
| Falha do Redis | `get` vira cache miss e `put` é ignorado — API responde pelo PostgreSQL |

Invalidar todos os relatórios do usuário é um `INCR` O(1), sem `SCAN`/`KEYS`. Alteração que sofra rollback não invalida nada.

## Tratamento de erros

`TratadorGlobalExcecoes` (`@RestControllerAdvice`) devolve sempre o mesmo contrato:

```json
{
  "timestamp": "2026-11-13T22:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Categoria em uso por transacoes e nao pode ser excluida",
  "path": "/api/v1/categorias/<id>",
  "errors": []
}
```

Exceções de domínio: `ExcecaoApi` (base com status), `ExcecaoRecursoNaoEncontrado` (404), `ExcecaoConflito` (409), `ExcecaoRegraNegocio` (400), `ExcecaoNaoAutorizado` (401). Erros de validação trazem `errors[{field,message}]`; stack trace nunca chega ao cliente.

## Segurança

- JWT HS256 (Nimbus), `sub` = UUID do usuário, claim `papel`.
- Toda consulta filtra por `usuario_id`; recurso alheio responde **404**.
- Registro força `papel = USER`; BCrypt custo 12; troca de senha exige a atual.
- Login responde genérico e mantém o custo do BCrypt para e-mail inexistente (anti-enumeração por tempo).
- Swagger desligado em produção; Actuator expõe apenas `health`.

## Testes

```bash
mvn test      # unitários + integração (H2 em modo PostgreSQL, Redis opcional)
mvn verify    # build + testes + empacotamento
```

| Classe | Cobre |
|---|---|
| `CalculadoraRelatorioTest` | saldo, taxa de economia (sem divisão por zero), percentuais, fluxo diário, 12 meses |
| `RelatorioServiceTest` | validação de `month`, intervalo do fluxo, delegação ao cache |
| `TransacaoServiceTest` | conta inativa, categoria incompatível, conta de terceiro, evento de invalidação, idempotência |
| `ContaServiceTest` | moeda não suportada, saldo derivado, conta de terceiro |
| `CategoriaServiceTest` | nome duplicado, tipo imutável, exclusão bloqueada em uso |
| `MetaServiceTest` | aporte que conclui, aporte parcial, meta cancelada, exclusão com aportes |
| `AutenticacaoServiceTest` | e-mail normalizado, duplicidade, credenciais inválidas, emissão de token |
| `UsuarioServiceTest` | troca de senha exigindo a senha atual |
| `InfraestruturaCacheTest` | fallback em falha de Redis, TTL, versão, composição de chave |
| `IntegracaoCacheRedisTest` | hit e TTL contra Redis real (skip sem Redis) |
| `IntegracaoApiTest` | fluxo HTTP completo, 401, 400, 409, Redis indisponível |
| `MatrizAutorizacaoTest` | BOLA/IDOR: leitura e escrita de recurso alheio ⇒ 404 |

CI (`.github/workflows/ci.yml`): H2 + Redis real, depois PostgreSQL 17 real com Flyway e `ddl-auto: validate`.

## Decisões

Registradas em [`docs/adr/`](docs/adr): stack, autenticação, cache, status/exclusão, contrato de relatórios e integridade/concorrência. Processo em [`PROJECT_EXECUTION_PLAN.md`](PROJECT_EXECUTION_PLAN.md).

## Limitações conhecidas

- Sem refresh token/revogação: token vale até expirar (1 h).
- Sem rate limiting no login.
- Exclusão de transação é física (saldos e relatórios são derivados).
- Apenas `BRL`, moeda imutável após a criação.
- Sem single-flight no cache.
- `/relatorios/mensal` não é cacheado.
