package com.agrestetextil.pedidos.repository;

import com.agrestetextil.pedidos.model.Movimentacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Long> {

    List<Movimentacao> findByPedidoIdOrderByDataHoraAscIdAsc(Long pedidoId);
}
