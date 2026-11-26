package com.agrestetextil.pedidos.repository;

import com.agrestetextil.pedidos.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    List<Cliente> findByNomeContainingIgnoreCaseOrderByNomeAsc(String nome);
}
