package com.agrestetextil.pedidos.model;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos", indexes = {
        @Index(name = "idx_pedidos_status", columnList = "status"),
        @Index(name = "idx_pedidos_previsao", columnList = "previsao_entrega"),
        @Index(name = "idx_pedidos_regiao_destino", columnList = "regiao_destino")
})
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_pedido", nullable = false, unique = true, length = 40)
    private String codigo;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false, length = 100)
    private String responsavel;

    @Column(nullable = false, length = 150)
    private String origem;

    @Column(nullable = false, length = 150)
    private String destino;

    @Column(name = "regiao_origem", length = 100)
    private String regiaoOrigem;

    @Column(name = "regiao_destino", nullable = false, length = 100)
    private String regiaoDestino;

    @Column(name = "data_pedido", nullable = false)
    private LocalDate dataPedido;

    @Column(name = "previsao_entrega", nullable = false)
    private LocalDate previsaoEntrega;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private StatusPedido status = StatusPedido.PENDENTE;

    @Column(length = 1000)
    private String observacoes;

    @Column(name = "ultima_localizacao", length = 150)
    private String ultimaLocalizacao;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @BatchSize(size = 50)
    private List<ItemPedido> itens = new ArrayList<>();

    public void adicionarItem(ItemPedido item) {
        item.setPedido(this);
        this.itens.add(item);
    }

    public void limparItens() {
        this.itens.clear();
    }

    public boolean estaAtrasado(LocalDate hoje) {
        return Atraso.estaAtrasado(status, previsaoEntrega, hoje);
    }

    public long diasDeAtraso(LocalDate hoje) {
        return Atraso.diasDeAtraso(status, previsaoEntrega, hoje);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public String getResponsavel() { return responsavel; }
    public void setResponsavel(String responsavel) { this.responsavel = responsavel; }
    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    public String getRegiaoOrigem() { return regiaoOrigem; }
    public void setRegiaoOrigem(String regiaoOrigem) { this.regiaoOrigem = regiaoOrigem; }
    public String getRegiaoDestino() { return regiaoDestino; }
    public void setRegiaoDestino(String regiaoDestino) { this.regiaoDestino = regiaoDestino; }
    public LocalDate getDataPedido() { return dataPedido; }
    public void setDataPedido(LocalDate dataPedido) { this.dataPedido = dataPedido; }
    public LocalDate getPrevisaoEntrega() { return previsaoEntrega; }
    public void setPrevisaoEntrega(LocalDate previsaoEntrega) { this.previsaoEntrega = previsaoEntrega; }
    public StatusPedido getStatus() { return status; }
    public void setStatus(StatusPedido status) { this.status = status; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public String getUltimaLocalizacao() { return ultimaLocalizacao; }
    public void setUltimaLocalizacao(String ultimaLocalizacao) { this.ultimaLocalizacao = ultimaLocalizacao; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
    public List<ItemPedido> getItens() { return itens; }
}
