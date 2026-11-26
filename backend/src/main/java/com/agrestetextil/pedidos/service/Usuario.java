package com.agrestetextil.pedidos.service;

/**
 * Usuario que executa a acao. Versao simplificada (sem login): vem dos cabecalhos X-Usuario e X-Perfil.
 * admin = "usuario autorizado" para acoes excepcionais (RN06).
 */
public record Usuario(String nome, boolean admin) {
}
