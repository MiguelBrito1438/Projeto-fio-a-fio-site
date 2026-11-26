package com.agrestetextil.pedidos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ItemPedidoRequest(
        @NotBlank(message = "A descrição do produto é obrigatória.")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String descricao,
        @NotNull(message = "A quantidade é obrigatória.")
        @DecimalMin(value = "0", inclusive = false, message = "A quantidade deve ser maior que zero.")
        BigDecimal quantidade,
        @Size(max = 20, message = "A unidade deve ter no máximo 20 caracteres.")
        String unidade) {
}
