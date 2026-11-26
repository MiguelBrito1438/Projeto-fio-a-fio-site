package com.agrestetextil.pedidos.dto;

import java.util.List;

/** Indicadores do painel geral (RF06). Cada pedido e contado em um unico status. */
public record DashboardResponse(
        long totalPedidos,
        long pendentes,
        long emAndamento,
        long entregues,
        long cancelados,
        long atrasados,
        List<RegiaoResumo> porRegiao) {

    /** Distribuicao por regiao de destino; "atrasados" ajuda a identificar regioes criticas. */
    public record RegiaoResumo(String regiao, long total, long atrasados) {
    }
}
