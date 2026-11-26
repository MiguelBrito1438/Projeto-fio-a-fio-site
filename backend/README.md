# API de Gestão de Pedidos e Rastreio — Atacadistas do Agreste

Back-end do Projeto Integrador: Java 21 + Spring Boot 3.5 + Spring Data JPA + MySQL. Implementa os requisitos RF01–RF09 e as regras RN01–RN14 do *Documento de Requisitos e Regras de Negócio*.

## Requisitos

- JDK 21 · Maven 3.9+ · MySQL 8.0.16+ (ou 8.4)

## Como executar

1. **Banco** (escolha uma opção):
   - *Automática*: só suba o MySQL; a API cria o banco/tabelas (`ddl-auto=update`).
   - *Por script* (reproduzível): `mysql -u root -p < banco/schema.sql` e rode a API com `DDL_AUTO=none`.
   - Opcional, dados fictícios de demonstração: `mysql -u root -p < banco/dados_demo.sql`.
2. **Credenciais** (nunca no GitHub). Defina variáveis de ambiente (os padrões são `root` e senha vazia):
   ```bash
   export DB_USER=root
   export DB_PASSWORD=sua_senha
   # opcional: export DB_URL="jdbc:mysql://localhost:3306/pedidos_agreste?..."
   ```
   No Windows (PowerShell): `$env:DB_PASSWORD="sua_senha"`. Em IDEs, configure as variáveis na *Run Configuration*.
3. **Subir a API**: `mvn spring-boot:run` → http://localhost:8080
4. **Testes** (usam H2 em memória, não precisam do MySQL): `mvn test`
5. **Gerar o JAR**: `mvn clean package` e `java -jar target/pedidos-agreste-api.jar`

CORS: por padrão libera qualquer origem para `/api/**` (desenvolvimento). Para restringir: `CORS_ORIGINS=http://localhost:5500`.

## Publicação de demonstração no Fly.io

O projeto inclui `Dockerfile` e `fly.toml` para publicar a API usando o perfil H2 e um volume persistente. Isso serve como demonstração da integração; não substitui o MySQL previsto para uma implantação de produção.

Pré-requisitos: instalar Docker e o `flyctl` oficial, criar/entrar em uma conta Fly.io e provisionar o app e o volume uma vez. Na pasta deste projeto:

```bash
fly auth login
fly apps create fio-a-fio-api-miguel
fly volumes create pedidos_data --region gru --size 1 --app fio-a-fio-api-miguel
fly deploy --app fio-a-fio-api-miguel
```

O domínio fica em `https://fio-a-fio-api-miguel.fly.dev`. A URL real só é liberada após concluir a criação da conta e dos recursos no Fly.io. Configure essa origem como `window.FIO_A_FIO_API_BASE = "https://<dominio-da-api>/api"` no `index.html` do front. Para produção, use MySQL gerenciado, backup, autenticação real e restrinja `CORS_ORIGINS` ao domínio do frontend.

## Documentação dos endpoints

Veja [`docs/API.md`](docs/API.md) (rotas, JSONs de requisição/resposta, regras de status e códigos de erro).

## Estrutura

```
src/main/java/com/agrestetextil/pedidos
├── model/        entidades JPA + regras puras (StatusPedido, PoliticaTransicao, Atraso)
├── repository/   Spring Data JPA + filtros dinâmicos (PedidoSpecs)
├── service/      regras de negócio (PedidoService, DashboardService, AlertaService…)
├── controller/   endpoints REST
├── dto/          objetos de entrada/saída (com Bean Validation)
└── exception/    erros padronizados (400/403/404/409/422)
banco/            schema.sql e dados_demo.sql
docs/             API.md
```

## Decisões de projeto

- **Atraso é calculado**, não é status: `hoje > previsaoEntrega` e status ≠ Entregue/Cancelado. Previsão = hoje ainda não é atraso.
- **Histórico transacional e imutável**: o status e o registro do histórico são gravados na mesma transação; não há rota para editar/apagar movimentações.
- **Sem login nesta versão**: o responsável vem do cabeçalho `X-Usuario`; `X-Perfil: ADMIN` representa o "usuário autorizado". Para um produto real, substituir por autenticação (ex.: Spring Security + JWT).
- **Parâmetros configuráveis** (limite de 3 atrasos por região, horas sem atualização) via `/api/parametros`.
- **Fuso horário**: `America/Fortaleza`.

## Fora do escopo (conforme os documentos)

GPS/rastreamento em tempo real, IoT, autenticação completa, importação de pedidos.

## Segurança mínima

Credenciais fora do código; validação de todos os dados de entrada (Bean Validation); acesso ao banco só via JPA (consultas parametrizadas); use apenas dados fictícios em testes e demonstrações.
