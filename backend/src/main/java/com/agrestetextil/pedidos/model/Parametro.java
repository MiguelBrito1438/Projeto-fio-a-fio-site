package com.agrestetextil.pedidos.model;

import jakarta.persistence.*;

/** Parametro configuravel pela administracao (ex.: limite de atrasos para alerta regional). */
@Entity
@Table(name = "parametros")
public class Parametro {

    @Id
    @Column(length = 60)
    private String chave;

    @Column(nullable = false, length = 100)
    private String valor;

    protected Parametro() {
    }

    public Parametro(String chave, String valor) {
        this.chave = chave;
        this.valor = valor;
    }

    public String getChave() { return chave; }
    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }
}
