package com.agrestetextil.pedidos.exception;

/** Conflito, ex.: codigo de pedido duplicado (HTTP 409). */
public class ConflitoException extends RuntimeException {

    private final String campo;

    public ConflitoException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
