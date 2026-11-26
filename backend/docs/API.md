# Documentação da API REST

Base URL local: `http://localhost:8080`. Formato: JSON (UTF-8). Datas: `AAAA-MM-DD`. Data/hora: `AAAA-MM-DDTHH:mm:ss`.

## Identificação do usuário (versão simplificada, sem login)

Nas operações que **alteram** um pedido (status, localização, edição) e nos parâmetros, o front envia:

| Cabeçalho | Obrigatório | Significado |
|---|---|---|
| `X-Usuario` | sim, nessas operações | Nome de quem executa a ação. Vai para o histórico como "responsável". |
| `X-Perfil` | não | `ADMIN` = usuário autorizado (ações excepcionais). Qualquer outro valor/ausente = operador. |

> Sugestão para o front: um seletor simples de usuário/perfil no topo das telas, enviado em todos os `fetch`.

## Status

`PENDENTE` · `EM_PRODUCAO` · `EM_PREPARACAO` · `AGUARDANDO_COLETA` · `EM_TRANSPORTE` · `ENTREGUE` · `CANCELADO`
(toda resposta traz também `statusRotulo`, ex.: "Em produção", para exibir na tela.)
**Atrasado não é status**: vem em `atrasado` (boolean) e `diasAtraso`.

## Erros (formato único)

```json
{ "timestamp": "2026-11-17T12:00:00", "status": 400, "erro": "Bad Request",
  "mensagem": "Dados inválidos. Verifique os campos informados.",
  "campos": { "itens[0].quantidade": "A quantidade deve ser maior que zero." } }
```

| HTTP | Quando |
|---|---|
| 400 | Campo obrigatório ausente/inválido, JSON malformado, data fora do formato, status desconhecido |
| 403 | Ação exige `X-Perfil: ADMIN` |
| 404 | Pedido/cliente inexistente |
| 409 | Código de pedido duplicado |
| 422 | Regra de negócio violada (transição proibida, justificativa ausente, previsão anterior ao pedido, `X-Usuario` ausente…) |

## Clientes

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/clientes` | Cadastra cliente |
| GET | `/api/clientes?nome=` | Lista (filtro opcional por nome) |
| GET | `/api/clientes/{id}` | Detalhe |

```json
// POST /api/clientes
{ "nome": "Confecções Boa Moda", "documento": "", "telefone": "(81) 99999-0001", "cidade": "Toritama" }
// 201
{ "id": 1, "nome": "Confecções Boa Moda", "documento": null, "telefone": "(81) 99999-0001", "cidade": "Toritama" }
```

## Pedidos

| Método | Rota | RF | Descrição |
|---|---|---|---|
| POST | `/api/pedidos` | RF01 | Cadastra pedido (status inicial sempre `PENDENTE`) |
| GET | `/api/pedidos` | RF02 | Lista paginada com filtros |
| GET | `/api/pedidos/{id}` | RF02 | Detalhe |
| GET | `/api/pedidos/codigo/{codigo}` | RF02 | Detalhe pelo código (sem diferenciar maiúsculas) |
| PUT | `/api/pedidos/{id}` | — | Edita dados de pedido **aberto** (não altera status/código/data do pedido) |
| PATCH | `/api/pedidos/{id}/status` | RF03/RF04 | Altera status e grava histórico automaticamente |
| PATCH | `/api/pedidos/{id}/localizacao` | RF08 | Atualiza última localização (manual) e grava histórico |
| GET | `/api/pedidos/{id}/historico` | RF09 | Linha do tempo em ordem cronológica |

### Cadastrar — `POST /api/pedidos`

```json
{
  "codigo": "PED-2026-001",
  "clienteId": 1,
  "responsavel": "Maria",
  "origem": "Toritama",
  "destino": "Recife",
  "regiaoOrigem": "Agreste",
  "regiaoDestino": "Recife",
  "itens": [
    { "descricao": "Camisas polo", "quantidade": 120, "unidade": "un" },
    { "descricao": "Bermudas jeans", "quantidade": 60, "unidade": "un" }
  ],
  "dataPedido": "2026-11-10",
  "previsaoEntrega": "2026-11-20",
  "observacoes": "Entregar pela manhã",
  "ultimaLocalizacao": null
}
```

Obrigatórios: `codigo`, `clienteId`, `responsavel`, `origem`, `destino`, `regiaoDestino`, `itens` (≥1, cada um com `descricao` e `quantidade` > 0), `dataPedido`, `previsaoEntrega` (≥ `dataPedido`).
Resposta `201`:

```json
{
  "id": 1, "codigo": "PED-2026-001",
  "cliente": { "id": 1, "nome": "Confecções Boa Moda", "documento": null, "telefone": "(81) 99999-0001", "cidade": "Toritama" },
  "responsavel": "Maria", "origem": "Toritama", "destino": "Recife",
  "regiaoOrigem": "Agreste", "regiaoDestino": "Recife",
  "itens": [ { "id": 1, "descricao": "Camisas polo", "quantidade": 120.000, "unidade": "un" } ],
  "dataPedido": "2026-11-10", "previsaoEntrega": "2026-11-20",
  "status": "PENDENTE", "statusRotulo": "Pendente",
  "observacoes": "Entregar pela manhã", "ultimaLocalizacao": null,
  "atualizadoEm": "2026-11-17T12:00:00",
  "atrasado": false, "diasAtraso": 0
}
```

### Consultar — `GET /api/pedidos`

Todos os filtros são opcionais e combináveis:
`codigo` (contém), `cliente` (nome contém), `status`, `de` / `ate` (período pela **data do pedido**), `regiao` (origem ou destino, igual), `atrasado` (`true`/`false`), `page` (0…), `size` (padrão 20), `sort` (ex.: `previsaoEntrega,asc`).

Exemplo: `/api/pedidos?status=EM_TRANSPORTE&atrasado=true&regiao=Recife&page=0&size=10`

```json
{ "content": [ { /* PedidoResponse */ } ],
  "page": { "size": 10, "number": 0, "totalElements": 1, "totalPages": 1 } }
```

### Alterar status — `PATCH /api/pedidos/{id}/status`

Cabeçalho `X-Usuario` obrigatório.

```json
{ "novoStatus": "EM_PRODUCAO", "descricao": "Início da produção", "localizacao": "Toritama" }
```
`descricao` e `localizacao` são opcionais, **exceto** onde a regra exige justificativa (abaixo).

| De → Para | Regra |
|---|---|
| Pendente → Em produção → Em preparação → Aguardando coleta → Em transporte → Entregue | Fluxo normal, uma etapa por vez |
| Qualquer etapa aberta → Cancelado | Exige `descricao` (justificativa) |
| Pendente → etapa posterior (pulando) | Exige `X-Perfil: ADMIN` **e** `descricao` |
| Entregue → etapa aberta (correção excepcional) | Exige `X-Perfil: ADMIN` **e** `descricao` |
| Cancelado → qualquer | Proibido (cadastre novo pedido) |
| Entregue → Cancelado, retroceder, pular fora de Pendente, mesmo status | Proibido (422) |

A mudança para `ENTREGUE` é a confirmação explícita de entrega (tipo `CONFIRMACAO_ENTREGA`). O sistema **nunca** marca entregue sozinho.

### Localização — `PATCH /api/pedidos/{id}/localizacao`

```json
{ "localizacao": "Santa Cruz do Capibaribe", "descricao": "Carga saiu do galpão" }
```
Somente pedidos abertos. Sem GPS: é a última informação conhecida, digitada.

### Histórico — `GET /api/pedidos/{id}/historico`

```json
[
  { "id": 1, "statusAnterior": null, "statusAnteriorRotulo": "—", "statusNovo": "PENDENTE", "statusNovoRotulo": "Pendente",
    "tipo": "CADASTRO", "dataHora": "2026-11-10T09:00:00", "responsavel": "Maria", "localizacao": null, "descricao": "Pedido cadastrado." },
  { "id": 2, "statusAnterior": "PENDENTE", "statusAnteriorRotulo": "Pendente", "statusNovo": "EM_PRODUCAO", "statusNovoRotulo": "Em produção",
    "tipo": "ALTERACAO_STATUS", "dataHora": "2026-11-11T14:30:00", "responsavel": "João", "localizacao": "Toritama", "descricao": "Início da produção" }
]
```
`tipo`: `CADASTRO`, `ALTERACAO_STATUS`, `ATUALIZACAO_LOCALIZACAO`, `CONFIRMACAO_ENTREGA`, `CANCELAMENTO`, `CORRECAO_EXCEPCIONAL`, `EDICAO_DADOS`.
O histórico é **somente leitura**: não existe rota para editar ou apagar movimentações.

## Painel — `GET /api/dashboard?de=&ate=&regiao=`

Filtros opcionais (período pela data do pedido; região de origem ou destino).

```json
{ "totalPedidos": 7, "pendentes": 3, "emAndamento": 2, "entregues": 1, "cancelados": 1, "atrasados": 3,
  "porRegiao": [ { "regiao": "Recife", "total": 7, "atrasados": 3 } ] }
```
Cada pedido conta em **um** status. `emAndamento` = produção + preparação + aguardando coleta + transporte. `atrasados` é calculado em separado (um pedido atrasado também conta em seu status). `porRegiao` agrupa pela região de destino.

## Alertas — `GET /api/alertas`

```json
{
  "atrasosIndividuais": [ { "pedidoId": 1, "codigo": "PED-1", "regiaoDestino": "Recife", "previsaoEntrega": "2026-11-10",
                            "status": "EM_TRANSPORTE", "statusRotulo": "Em transporte", "diasAtraso": 7 } ],
  "concentracoesRegionais": [ { "regiaoDestino": "Recife", "quantidadeAtrasados": 3, "codigos": ["PED-1","PED-2","PED-3"] } ],
  "previsaoHoje": [ { "pedidoId": 5, "codigo": "PED-5", "regiaoDestino": "Recife", "status": "AGUARDANDO_COLETA", "statusRotulo": "Aguardando coleta" } ],
  "semAtualizacao": [ { "pedidoId": 1, "codigo": "PED-1", "regiaoDestino": "Recife", "ultimaAtualizacao": "2026-11-14T10:00:00", "horasSemAtualizacao": 74 } ],
  "parametros": { "limiteAtrasosPorRegiao": 3, "horasSemAtualizacao": 48 }
}
```

## Parâmetros — `GET|PUT /api/parametros`

`PUT` exige `X-Perfil: ADMIN`. Corpo: `{ "limiteAtrasosPorRegiao": 3, "horasSemAtualizacao": 48 }`.

## Exemplo rápido com curl

```bash
curl -X POST localhost:8080/api/clientes -H "Content-Type: application/json" -d '{"nome":"Cliente Teste","cidade":"Caruaru"}'
curl -X POST localhost:8080/api/pedidos -H "Content-Type: application/json" -d @pedido.json
curl -X PATCH localhost:8080/api/pedidos/1/status -H "Content-Type: application/json" -H "X-Usuario: Joao" -d '{"novoStatus":"EM_PRODUCAO"}'
curl localhost:8080/api/pedidos/1/historico
curl localhost:8080/api/dashboard
```
