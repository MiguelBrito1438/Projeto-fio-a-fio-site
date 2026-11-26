package com.agrestetextil.pedidos.controller;

import com.agrestetextil.pedidos.exception.RegraNegocioException;
import com.agrestetextil.pedidos.service.Usuario;

/**
 * Identificacao simplificada do usuario (sem login nesta versao):
 *  - X-Usuario: nome de quem executa a acao (obrigatorio nas alteracoes);
 *  - X-Perfil: ADMIN = usuario autorizado para acoes excepcionais; qualquer outro valor = OPERADOR.
 */
final class UsuarioHeaders {

    private UsuarioHeaders() {
    }

    static Usuario de(String nome, String perfil) {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("Informe o usuário responsável no cabeçalho X-Usuario.");
        }
        return new Usuario(nome.trim(), perfil != null && "ADMIN".equalsIgnoreCase(perfil.trim()));
    }
}
