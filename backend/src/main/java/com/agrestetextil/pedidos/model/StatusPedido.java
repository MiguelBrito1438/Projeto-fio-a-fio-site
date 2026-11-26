package com.agrestetextil.pedidos.model;

/**
 * Status operacionais do pedido (Documento de Requisitos, secao 4).
 * "Atrasado" NAO e um status: e uma condicao calculada (RN11).
 *
 * Fluxo normal: PENDENTE -> EM_PRODUCAO -> EM_PREPARACAO -> AGUARDANDO_COLETA -> EM_TRANSPORTE -> ENTREGUE.
 * A ordem de declaracao dos valores define o fluxo; nao reordene.
 */
public enum StatusPedido {
    PENDENTE("Pendente"),
    EM_PRODUCAO("Em produção"),
    EM_PREPARACAO("Em preparação"),
    AGUARDANDO_COLETA("Aguardando coleta"),
    EM_TRANSPORTE("Em transporte"),
    ENTREGUE("Entregue"),
    CANCELADO("Cancelado");

    private final String rotulo;

    StatusPedido(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** Status final: nao participa de atrasos nem de alertas (RN12). */
    public boolean isFinal() {
        return this == ENTREGUE || this == CANCELADO;
    }

    /** Pedido em etapa aberta (qualquer status nao final). */
    public boolean isAberto() {
        return !isFinal();
    }

    /** "Em andamento" no painel: producao, preparacao, coleta e transporte (exclui Pendente). */
    public boolean isEmAndamento() {
        return this == EM_PRODUCAO || this == EM_PREPARACAO
                || this == AGUARDANDO_COLETA || this == EM_TRANSPORTE;
    }

    /** Proxima etapa do fluxo normal, ou null se nao houver (ENTREGUE e CANCELADO). */
    public StatusPedido proximo() {
        return switch (this) {
            case PENDENTE -> EM_PRODUCAO;
            case EM_PRODUCAO -> EM_PREPARACAO;
            case EM_PREPARACAO -> AGUARDANDO_COLETA;
            case AGUARDANDO_COLETA -> EM_TRANSPORTE;
            case EM_TRANSPORTE -> ENTREGUE;
            case ENTREGUE, CANCELADO -> null;
        };
    }
}
