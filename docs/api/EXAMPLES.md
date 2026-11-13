# Exemplos de uso da API

Base: `http://localhost:8080/api/v1`

## 1. Cadastro

```bash
curl -i -X POST http://localhost:8080/api/v1/autenticacao/cadastrar \
  -H "Content-Type: application/json" \
  -d '{"nome":"Erick","email":"erick@example.com","senha":"SenhaMuitoForte123"}'
```

`201 Created`

```json
{"id":"...","nome":"Erick","email":"erick@example.com","papel":"USER","criadoEm":"2026-11-13T22:00:00Z"}
```

## 2. Login

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/autenticacao/login \
  -H "Content-Type: application/json" \
  -d '{"email":"erick@example.com","senha":"SenhaMuitoForte123"}' | jq -r .accessToken)
```

```json
{"accessToken":"eyJ...","tokenType":"Bearer","expiresIn":3600}
```

## 3. Conta

```bash
curl -s -X POST http://localhost:8080/api/v1/contas \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nome":"Conta principal","tipo":"CHECKING","saldoInicial":1000.00,"moeda":"BRL"}'
```

## 4. Categoria

```bash
curl -s -X POST http://localhost:8080/api/v1/categorias \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nome":"Mercado","tipo":"EXPENSE","cor":"#FF8800"}'
```

## 5. Transação

```bash
curl -s -X POST http://localhost:8080/api/v1/transacoes \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"contaId":"<id>","categoriaId":"<id>","descricao":"Compra no supermercado",
       "valor":250.90,"tipo":"EXPENSE","dataTransacao":"2026-11-13","status":"COMPLETED"}'
```

## 6. Saldo e relatórios

```bash
curl -s "http://localhost:8080/api/v1/contas/<id>/saldo?dataReferencia=2026-11-30" -H "Authorization: Bearer $TOKEN"
curl -s "http://localhost:8080/api/v1/relatorios/resumo?mes=2026-11" -H "Authorization: Bearer $TOKEN"
curl -s "http://localhost:8080/api/v1/relatorios/categorias?mes=2026-11&tipo=EXPENSE" -H "Authorization: Bearer $TOKEN"
curl -s "http://localhost:8080/api/v1/relatorios/fluxo-caixa?de=2026-11-01&ate=2026-11-30" -H "Authorization: Bearer $TOKEN"
curl -s "http://localhost:8080/api/v1/relatorios/mensal?ano=2026" -H "Authorization: Bearer $TOKEN"
```

Resposta de `/relatorios/resumo`:

```json
{"period":"2026-11","totalIncome":0.00,"totalExpenses":250.90,"saldo":-250.90,"savingsRate":0.00}
```

## 7. Filtros e paginação

```bash
curl -s "http://localhost:8080/api/v1/transacoes?tipo=EXPENSE&dataInicio=2026-11-01&dataFim=2026-11-30&pagina=0&tamanho=10&ordenarPor=valor&direcao=desc" \
  -H "Authorization: Bearer $TOKEN"
```

```json
{"content":[...],"page":0,"size":10,"totalElements":1,"totalPages":1}
```

## 8. Metas

```bash
curl -s -X POST http://localhost:8080/api/v1/metas -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"nome":"Notebook","valorAlvo":5000.00,"prazo":"2027-06-30"}'

curl -s -X PATCH http://localhost:8080/api/v1/metas/<id>/contribuicoes -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"valor":300.00}'
```

## 9. Erros

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
