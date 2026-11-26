package com.agrestetextil.pedidos.dto;

import com.agrestetextil.pedidos.model.Cliente;

public record ClienteResponse(Long id, String nome, String documento, String telefone, String cidade) {

    public static ClienteResponse de(Cliente c) {
        return new ClienteResponse(c.getId(), c.getNome(), c.getDocumento(), c.getTelefone(), c.getCidade());
    }
}
