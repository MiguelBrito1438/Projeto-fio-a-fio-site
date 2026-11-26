package com.agrestetextil.pedidos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** Cadastro de pedido (RF01). O status inicial e sempre PENDENTE. */
public record PedidoRequest(
        @NotBlank(message = "O código do pedido é obrigatório.")
        @Size(max = 40, message = "O código deve ter no máximo 40 caracteres.")
        String codigo,
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
        @NotNull(message = "A data do pedido é obrigatória.")
        LocalDate dataPedido,
        @NotNull(message = "A previsão de entrega é obrigatória.")
        LocalDate previsaoEntrega,
        @Size(max = 1000, message = "As observações devem ter no máximo 1000 caracteres.")
        String observacoes,
        @Size(max = 150, message = "A localização deve ter no máximo 150 caracteres.")
        String ultimaLocalizacao) {
}
