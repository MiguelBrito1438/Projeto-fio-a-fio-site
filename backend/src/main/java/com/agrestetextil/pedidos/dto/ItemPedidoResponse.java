package com.agrestetextil.pedidos.dto;

import com.agrestetextil.pedidos.model.ItemPedido;

import java.math.BigDecimal;

public record ItemPedidoResponse(Long id, String descricao, BigDecimal quantidade, String unidade) {

    public static ItemPedidoResponse de(ItemPedido i) {
        return new ItemPedidoResponse(i.getId(), i.getDescricao(), i.getQuantidade(), i.getUnidade());
    }
}
