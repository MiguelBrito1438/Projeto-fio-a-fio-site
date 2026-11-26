package com.agrestetextil.pedidos.dto;

import com.agrestetextil.pedidos.model.Pedido;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Long id,
        String codigo,
        ClienteResponse cliente,
        String responsavel,
        String origem,
        String destino,
        String regiaoOrigem,
        String regiaoDestino,
        List<ItemPedidoResponse> itens,
        LocalDate dataPedido,
        LocalDate previsaoEntrega,
        String status,
        String statusRotulo,
        String observacoes,
        String ultimaLocalizacao,
        LocalDateTime atualizadoEm,
        boolean atrasado,
        long diasAtraso) {

    public static PedidoResponse de(Pedido p, LocalDate hoje) {
        return new PedidoResponse(
                p.getId(),
                p.getCodigo(),
                ClienteResponse.de(p.getCliente()),
                p.getResponsavel(),
                p.getOrigem(),
                p.getDestino(),
                p.getRegiaoOrigem(),
                p.getRegiaoDestino(),
                p.getItens().stream().map(ItemPedidoResponse::de).toList(),
                p.getDataPedido(),
                p.getPrevisaoEntrega(),
                p.getStatus().name(),
                p.getStatus().getRotulo(),
                p.getObservacoes(),
                p.getUltimaLocalizacao(),
                p.getAtualizadoEm(),
                p.estaAtrasado(hoje),
                p.diasDeAtraso(hoje));
    }
}
