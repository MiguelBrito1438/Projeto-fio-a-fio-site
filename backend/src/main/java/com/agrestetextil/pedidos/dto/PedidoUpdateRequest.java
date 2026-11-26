package com.agrestetextil.pedidos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** Edicao dos dados de um pedido ainda aberto. Codigo, data do pedido e status nao mudam por aqui. */
public record PedidoUpdateRequest(
        @NotNull(message = "O cliente é obrigatório.")
        Long clienteId,
        @NotBlank(message = "O responsável pelo pedido é obrigatório.")
        @Size(max = 100, message = "O responsável deve ter no máximo 100 caracteres.")
        String responsavel,
        @NotBlank(message = "A origem é obrigatória.")
        @Size(max = 150, message = "A origem deve ter no máximo 150 caracteres.")
        String origem,
        @NotBlank(message = "O destino é obrigatório.")
        @Size(max = 150, message = "O destino deve ter no máximo 150 caracteres.")
        String destino,
        @Size(max = 100, message = "A região de origem deve ter no máximo 100 caracteres.")
        String regiaoOrigem,
        @NotBlank(message = "A região/localidade de destino é obrigatória.")
        @Size(max = 100, message = "A região de destino deve ter no máximo 100 caracteres.")
        String regiaoDestino,
        @NotEmpty(message = "Informe ao menos um produto.")
        @Valid
        List<ItemPedidoRequest> itens,
        @NotNull(message = "A previsão de entrega é obrigatória.")
        LocalDate previsaoEntrega,
        @Size(max = 1000, message = "As observações devem ter no máximo 1000 caracteres.")
        String observacoes) {
}
