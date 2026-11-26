package com.agrestetextil.pedidos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ParametrosRequest(
        @NotNull(message = "Informe o limite de atrasos por região.")
        @Min(value = 1, message = "O limite deve ser de pelo menos 1 pedido.")
        Integer limiteAtrasosPorRegiao,
        @NotNull(message = "Informe o período sem atualização, em horas.")
        @Min(value = 1, message = "O período deve ser de pelo menos 1 hora.")
        Integer horasSemAtualizacao) {
}
