package com.agrestetextil.pedidos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LocalizacaoRequest(
        @NotBlank(message = "A localização é obrigatória.")
        @Size(max = 150, message = "A localização deve ter no máximo 150 caracteres.")
        String localizacao,
        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres.")
        String descricao) {
}
