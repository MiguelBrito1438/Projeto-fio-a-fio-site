# Fio a Fio — gestão têxtil

Sistema de gestão e acompanhamento de pedidos para atacadistas e confecções do Agreste Pernambucano. O projeto integrador reúne o painel web e uma API REST em Java/Spring Boot.

## Estrutura

- `frontend/`: interface estática (HTML, CSS e JavaScript), pronta para publicação no Cloudflare Workers Static Assets.
- `backend/`: API Spring Boot, documentação de rotas, scripts SQL e configuração de container.

## Front-end

Os arquivos publicados ficam em `frontend/public/`. Para visualizar, abra `index.html` com um servidor estático local. Para publicar no Cloudflare, instale Node.js e execute `npx wrangler deploy` dentro de `frontend/` com a conta Cloudflare autenticada.

A API ainda não tem uma URL pública. Enquanto `window.FIO_A_FIO_API_BASE` estiver vazio, o site informa que está usando dados de demonstração; não configure uma URL até a API estar hospedada e com CORS configurado para o domínio do site.

## API

Requisitos: Java 21, Maven 3.9+ e MySQL 8. Consulte [`backend/README.md`](backend/README.md) e [`backend/docs/API.md`](backend/docs/API.md) para executar a API, configurar o banco e consultar os endpoints.

Use somente dados fictícios ao testar. Não envie credenciais, banco local ou arquivos compilados ao GitHub.
