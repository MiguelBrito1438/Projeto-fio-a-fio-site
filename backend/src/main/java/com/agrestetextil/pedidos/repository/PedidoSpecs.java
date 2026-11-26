package com.agrestetextil.pedidos.repository;

import com.agrestetextil.pedidos.model.Pedido;
import com.agrestetextil.pedidos.model.StatusPedido;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/** Filtros dinamicos de consulta (RF02). Todos sao opcionais: valor nulo/vazio = sem filtro. */
public final class PedidoSpecs {

    private PedidoSpecs() {
    }

    public static Specification<Pedido> filtrar(String codigo, String cliente, StatusPedido status,
                                                LocalDate de, LocalDate ate, String regiao,
                                                Boolean atrasado, LocalDate hoje) {
        return (root, query, cb) -> {
            List<Predicate> p = new java.util.ArrayList<>();

            if (temTexto(codigo)) {
                p.add(cb.like(cb.lower(root.get("codigo")), "%" + codigo.trim().toLowerCase() + "%"));
            }
            if (temTexto(cliente)) {
                p.add(cb.like(cb.lower(root.get("cliente").get("nome")), "%" + cliente.trim().toLowerCase() + "%"));
            }
            if (status != null) {
                p.add(cb.equal(root.get("status"), status));
            }
            if (de != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("dataPedido"), de));
            }
            if (ate != null) {
                p.add(cb.lessThanOrEqualTo(root.get("dataPedido"), ate));
            }
            if (temTexto(regiao)) {
                String r = regiao.trim().toLowerCase();
                p.add(cb.or(
                        cb.equal(cb.lower(root.get("regiaoDestino")), r),
                        cb.equal(cb.lower(root.get("regiaoOrigem")), r)));
            }
            if (atrasado != null) {
                Predicate vencido = cb.and(
                        cb.lessThan(root.get("previsaoEntrega"), hoje),
                        root.get("status").in(abertos()));
                p.add(atrasado ? vencido : cb.not(vencido));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    /** Filtros do painel: periodo (data do pedido) e regiao de destino/origem. */
    public static Specification<Pedido> periodoERegiao(LocalDate de, LocalDate ate, String regiao) {
        return filtrar(null, null, null, de, ate, regiao, null, null);
    }

    private static List<StatusPedido> abertos() {
        return Arrays.stream(StatusPedido.values()).filter(StatusPedido::isAberto).toList();
    }

    private static boolean temTexto(String s) {
        return s != null && !s.isBlank();
    }
}
