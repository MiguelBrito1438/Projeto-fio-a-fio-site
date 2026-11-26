package com.agrestetextil.pedidos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
        @NotBlank(message = "O nome do cliente é obrigatório.")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
        String nome,
        @Size(max = 20, message = "O documento deve ter no máximo 20 caracteres.")
        String documento,
        @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres.")
        String telefone,
        @Size(max = 100, message = "A cidade deve ter no máximo 100 caracteres.")
        String cidade) {
}
