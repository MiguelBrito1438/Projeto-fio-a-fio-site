package com.agrestetextil.pedidos.dto;

import com.agrestetextil.pedidos.model.StatusPedido;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StatusRequest(
        @NotNull(message = "O novo status é obrigatório.")
        StatusPedido novoStatus,
        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres.")
        String descricao,
        @Size(max = 150, message = "A localização deve ter no máximo 150 caracteres.")
        String localizacao) {
}
