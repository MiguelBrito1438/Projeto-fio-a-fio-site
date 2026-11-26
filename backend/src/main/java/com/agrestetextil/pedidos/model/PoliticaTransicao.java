package com.agrestetextil.pedidos.model;

/**
 * Regras de transicao de status (Documento de Requisitos, secao 4).
 * Classe pura (sem Spring/JPA) para facilitar testes.
 */
public final class PoliticaTransicao {

    /** Resultado de uma tentativa de transicao. */
    public record Resultado(boolean permitida, TipoMovimentacao tipo, boolean exigeAdmin,
                            boolean exigeJustificativa, String motivoRecusa) {

        static Resultado ok(TipoMovimentacao tipo, boolean exigeAdmin, boolean exigeJustificativa) {
            return new Resultado(true, tipo, exigeAdmin, exigeJustificativa, null);
        }

        static Resultado recusada(String motivo) {
            return new Resultado(false, null, false, false, motivo);
        }
    }

    private PoliticaTransicao() {
    }

    public static Resultado avaliar(StatusPedido atual, StatusPedido novo) {
        if (atual == novo) {
            return Resultado.recusada("O pedido já está com o status " + atual.getRotulo() + ".");
        }

        // Cancelado nunca volta ao fluxo; retomada = novo pedido vinculado.
        if (atual == StatusPedido.CANCELADO) {
            return Resultado.recusada("Pedido cancelado não pode ser alterado. "
                    + "Para retomar, cadastre um novo pedido vinculado ao anterior.");
        }

        // Cancelamento: somente antes da entrega, com justificativa.
        if (novo == StatusPedido.CANCELADO) {
            if (atual == StatusPedido.ENTREGUE) {
                return Resultado.recusada("Pedido entregue não pode ser cancelado.");
            }
            return Resultado.ok(TipoMovimentacao.CANCELAMENTO, false, true);
        }

        // Entregue e final; correcao excepcional so por usuario autorizado, com justificativa.
        if (atual == StatusPedido.ENTREGUE) {
            return Resultado.ok(TipoMovimentacao.CORRECAO_EXCEPCIONAL, true, true);
        }

        // Fluxo normal: avanca exatamente uma etapa.
        if (novo == atual.proximo()) {
            TipoMovimentacao tipo = novo == StatusPedido.ENTREGUE
                    ? TipoMovimentacao.CONFIRMACAO_ENTREGA
                    : TipoMovimentacao.ALTERACAO_STATUS;
            return Resultado.ok(tipo, false, false);
        }

        // De Pendente direto para etapa posterior: usuario autorizado + justificativa.
        if (atual == StatusPedido.PENDENTE && novo.ordinal() > atual.ordinal()) {
            TipoMovimentacao tipo = novo == StatusPedido.ENTREGUE
                    ? TipoMovimentacao.CONFIRMACAO_ENTREGA
                    : TipoMovimentacao.ALTERACAO_STATUS;
            return Resultado.ok(tipo, true, true);
        }

        return Resultado.recusada("Transição não permitida: de " + atual.getRotulo()
                + " para " + novo.getRotulo() + ".");
    }
}
