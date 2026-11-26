package com.agrestetextil.pedidos.exception;

/** Usuario sem permissao para a operacao (HTTP 403). */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
