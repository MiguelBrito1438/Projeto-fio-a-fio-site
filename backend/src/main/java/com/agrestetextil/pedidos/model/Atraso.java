package com.agrestetextil.pedidos.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Regra de atraso (Documento de Requisitos, secao 5):
 * atrasado = hoje > previsaoEntrega E status diferente de Entregue E diferente de Cancelado.
 * Se a previsao e hoje, ainda NAO esta atrasado.
 */
public final class Atraso {

    private Atraso() {
    }

    public static boolean estaAtrasado(StatusPedido status, LocalDate previsaoEntrega, LocalDate hoje) {
        return diasDeAtraso(status, previsaoEntrega, hoje) > 0;
    }

    public static long diasDeAtraso(StatusPedido status, LocalDate previsaoEntrega, LocalDate hoje) {
        if (status == null || previsaoEntrega == null || hoje == null || status.isFinal()) {
            return 0;
        }
        if (!hoje.isAfter(previsaoEntrega)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(previsaoEntrega, hoje);
    }
}
