-- Dados FICTÍCIOS para demonstração. Execute depois de schema.sql:  mysql -u root -p < banco/dados_demo.sql
-- As datas são relativas a hoje (CURDATE()), então sempre haverá pedidos atrasados para mostrar nos alertas.
USE pedidos_agreste;

INSERT INTO clientes (nome, documento, telefone, cidade) VALUES
    ('Confecções Boa Moda', NULL, '(81) 99999-0001', 'Toritama'),
    ('Atacado Central do Agreste', NULL, '(81) 99999-0002', 'Caruaru'),
    ('Moda Fácil Revenda', NULL, '(81) 99999-0003', 'Santa Cruz do Capibaribe');

SET @c1 = (SELECT id FROM clientes WHERE nome = 'Confecções Boa Moda' LIMIT 1);
SET @c2 = (SELECT id FROM clientes WHERE nome = 'Atacado Central do Agreste' LIMIT 1);
SET @c3 = (SELECT id FROM clientes WHERE nome = 'Moda Fácil Revenda' LIMIT 1);

INSERT INTO pedidos (codigo_pedido, cliente_id, responsavel, origem, destino, regiao_origem, regiao_destino,
                     data_pedido, previsao_entrega, status, observacoes, ultima_localizacao, atualizado_em) VALUES
 ('DEMO-1001', @c1, 'Maria', 'Toritama', 'Recife',   'Agreste', 'Recife',   CURDATE() - INTERVAL 20 DAY, CURDATE() - INTERVAL 6 DAY, 'EM_TRANSPORTE',     'Entrega urgente', 'Caruaru',   NOW() - INTERVAL 3 DAY),
 ('DEMO-1002', @c1, 'Maria', 'Toritama', 'Recife',   'Agreste', 'Recife',   CURDATE() - INTERVAL 15 DAY, CURDATE() - INTERVAL 3 DAY, 'EM_PREPARACAO',     NULL,              NULL,        NOW() - INTERVAL 2 DAY),
 ('DEMO-1003', @c2, 'João',  'Caruaru',  'Recife',   'Agreste', 'Recife',   CURDATE() - INTERVAL 12 DAY, CURDATE() - INTERVAL 1 DAY, 'PENDENTE',          NULL,              NULL,        NOW() - INTERVAL 12 DAY),
 ('DEMO-1004', @c2, 'João',  'Caruaru',  'Garanhuns','Agreste', 'Garanhuns',CURDATE() - INTERVAL 5 DAY,  CURDATE() + INTERVAL 4 DAY, 'EM_PRODUCAO',       NULL,              NULL,        NOW() - INTERVAL 1 DAY),
 ('DEMO-1005', @c3, 'Ana',   'Santa Cruz do Capibaribe', 'Campina Grande', 'Agreste', 'Paraíba', CURDATE() - INTERVAL 9 DAY, CURDATE(), 'AGUARDANDO_COLETA', NULL, NULL, NOW() - INTERVAL 1 DAY),
 ('DEMO-1006', @c3, 'Ana',   'Santa Cruz do Capibaribe', 'Fortaleza', 'Agreste', 'Ceará',    CURDATE() - INTERVAL 18 DAY, CURDATE() - INTERVAL 4 DAY, 'ENTREGUE',      NULL, 'Fortaleza', NOW() - INTERVAL 5 DAY),
 ('DEMO-1007', @c1, 'Maria', 'Toritama', 'Salvador', 'Agreste', 'Bahia',    CURDATE() - INTERVAL 8 DAY,  CURDATE() + INTERVAL 2 DAY, 'CANCELADO',         'Cancelado a pedido do cliente', NULL, NOW() - INTERVAL 6 DAY);

INSERT INTO itens_pedido (pedido_id, descricao, quantidade, unidade)
SELECT id, 'Camisas polo', 120, 'un' FROM pedidos WHERE codigo_pedido LIKE 'DEMO-%';
INSERT INTO itens_pedido (pedido_id, descricao, quantidade, unidade)
SELECT id, 'Bermudas jeans', 60, 'un' FROM pedidos WHERE codigo_pedido IN ('DEMO-1001','DEMO-1004');

-- Histórico mínimo (um registro de cadastro por pedido)
INSERT INTO movimentacoes (pedido_id, status_anterior, status_novo, tipo, data_movimentacao, responsavel, descricao)
SELECT id, NULL, 'PENDENTE', 'CADASTRO', data_pedido, responsavel, 'Pedido cadastrado.'
FROM pedidos WHERE codigo_pedido LIKE 'DEMO-%';
INSERT INTO movimentacoes (pedido_id, status_anterior, status_novo, tipo, data_movimentacao, responsavel, descricao)
SELECT id, 'PENDENTE', status, IF(status = 'CANCELADO', 'CANCELAMENTO', 'ALTERACAO_STATUS'), atualizado_em, responsavel,
       IF(status = 'CANCELADO', 'Cancelado a pedido do cliente', 'Status atualizado (dados de demonstração).')
FROM pedidos WHERE codigo_pedido LIKE 'DEMO-%' AND status <> 'PENDENTE';
