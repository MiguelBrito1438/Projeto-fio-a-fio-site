-- =====================================================================
-- Sistema de Gestão de Pedidos e Rastreio - Atacadistas do Agreste
-- Script de criação do banco (MySQL 8.0.16+). Reproduzível: pode ser executado várias vezes.
-- Uso:  mysql -u root -p < banco/schema.sql
-- Depois rode a API com a variável DDL_AUTO=none para ela não alterar as tabelas.
-- Mantenha este script alinhado às entidades em src/main/java/.../model.
-- =====================================================================
CREATE DATABASE IF NOT EXISTS pedidos_agreste CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE pedidos_agreste;

CREATE TABLE IF NOT EXISTS clientes (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    nome       VARCHAR(150) NOT NULL,
    documento  VARCHAR(20)  NULL,
    telefone   VARCHAR(20)  NULL,
    cidade     VARCHAR(100) NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS pedidos (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    codigo_pedido       VARCHAR(40)   NOT NULL,
    cliente_id          BIGINT        NOT NULL,
    responsavel         VARCHAR(100)  NOT NULL,
    origem              VARCHAR(150)  NOT NULL,
    destino             VARCHAR(150)  NOT NULL,
    regiao_origem       VARCHAR(100)  NULL,
    regiao_destino      VARCHAR(100)  NOT NULL,
    data_pedido         DATE          NOT NULL,
    previsao_entrega    DATE          NOT NULL,
    status              VARCHAR(30)   NOT NULL DEFAULT 'PENDENTE',
    observacoes         VARCHAR(1000) NULL,
    ultima_localizacao  VARCHAR(150)  NULL,
    atualizado_em       DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_pedidos_codigo UNIQUE (codigo_pedido),
    CONSTRAINT fk_pedidos_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT ck_pedidos_status CHECK (status IN
        ('PENDENTE','EM_PRODUCAO','EM_PREPARACAO','AGUARDANDO_COLETA','EM_TRANSPORTE','ENTREGUE','CANCELADO')),
    CONSTRAINT ck_pedidos_datas CHECK (previsao_entrega >= data_pedido),
    INDEX idx_pedidos_status (status),
    INDEX idx_pedidos_previsao (previsao_entrega),
    INDEX idx_pedidos_regiao_destino (regiao_destino)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS itens_pedido (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    pedido_id   BIGINT        NOT NULL,
    descricao   VARCHAR(255)  NOT NULL,
    quantidade  DECIMAL(12,3) NOT NULL,
    unidade     VARCHAR(20)   NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_itens_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos (id) ON DELETE CASCADE,
    CONSTRAINT ck_itens_quantidade CHECK (quantidade > 0)
) ENGINE=InnoDB;

-- Histórico: somente INSERT. A API nunca atualiza nem apaga registros desta tabela (RN09).
CREATE TABLE IF NOT EXISTS movimentacoes (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    pedido_id          BIGINT       NOT NULL,
    status_anterior    VARCHAR(30)  NULL,
    status_novo        VARCHAR(30)  NOT NULL,
    tipo               VARCHAR(30)  NOT NULL,
    data_movimentacao  DATETIME(6)  NOT NULL,
    responsavel        VARCHAR(100) NOT NULL,
    localizacao        VARCHAR(150) NULL,
    descricao          VARCHAR(500) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_mov_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos (id),
    INDEX idx_mov_pedido (pedido_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS parametros (
    chave  VARCHAR(60)  NOT NULL,
    valor  VARCHAR(100) NOT NULL,
    PRIMARY KEY (chave)
) ENGINE=InnoDB;

INSERT IGNORE INTO parametros (chave, valor) VALUES
    ('ALERTA_LIMITE_ATRASOS_REGIAO', '3'),
    ('ALERTA_HORAS_SEM_ATUALIZACAO', '48');
