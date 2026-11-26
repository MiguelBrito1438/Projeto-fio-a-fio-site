package com.agrestetextil.pedidos.repository;

import com.agrestetextil.pedidos.model.Pedido;
import com.agrestetextil.pedidos.model.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long>, JpaSpecificationExecutor<Pedido> {

    boolean existsByCodigoIgnoreCase(String codigo);

    Optional<Pedido> findByCodigoIgnoreCase(String codigo);

    List<Pedido> findByStatusIn(Collection<StatusPedido> status);
}
