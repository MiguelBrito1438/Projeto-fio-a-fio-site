package com.agrestetextil.pedidos.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Registro do historico. Imutavel: nunca e atualizado nem apagado (RN09).
 * Correcoes sao feitas criando uma nova movimentacao.
 */
@Entity
@Immutable
@Table(name = "movimentacoes", indexes = {
        @Index(name = "idx_mov_pedido", columnList = "pedido_id")
})
public class Movimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status_anterior", length = 30)
    private StatusPedido statusAnterior;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status_novo", nullable = false, length = 30)
    private StatusPedido statusNovo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private TipoMovimentacao tipo;

    @Column(name = "data_movimentacao", nullable = false)
    private LocalDateTime dataHora;

    @Column(nullable = false, length = 100)
    private String responsavel;

    @Column(length = 150)
    private String localizacao;

    @Column(length = 500)
    private String descricao;

    public Long getId() { return id; }
    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }
    public StatusPedido getStatusAnterior() { return statusAnterior; }
    public void setStatusAnterior(StatusPedido statusAnterior) { this.statusAnterior = statusAnterior; }
    public StatusPedido getStatusNovo() { return statusNovo; }
    public void setStatusNovo(StatusPedido statusNovo) { this.statusNovo = statusNovo; }
    public TipoMovimentacao getTipo() { return tipo; }
    public void setTipo(TipoMovimentacao tipo) { this.tipo = tipo; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public String getResponsavel() { return responsavel; }
    public void setResponsavel(String responsavel) { this.responsavel = responsavel; }
    public String getLocalizacao() { return localizacao; }
    public void setLocalizacao(String localizacao) { this.localizacao = localizacao; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}
