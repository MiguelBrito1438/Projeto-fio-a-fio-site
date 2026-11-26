package com.agrestetextil.pedidos.service;

import com.agrestetextil.pedidos.dto.DashboardResponse;
import com.agrestetextil.pedidos.dto.DashboardResponse.RegiaoResumo;
import com.agrestetextil.pedidos.model.Pedido;
import com.agrestetextil.pedidos.model.StatusPedido;
import com.agrestetextil.pedidos.repository.PedidoRepository;
import com.agrestetextil.pedidos.repository.PedidoSpecs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Painel geral (RF06): indicadores calculados a partir dos pedidos gravados no banco. */
@Service
public class DashboardService {

    private final PedidoRepository pedidos;
    private final Clock clock;

    public DashboardService(PedidoRepository pedidos, Clock clock) {
        this.pedidos = pedidos;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardResponse calcular(LocalDate de, LocalDate ate, String regiao) {
        LocalDate hoje = LocalDate.now(clock);
        List<Pedido> lista = pedidos.findAll(PedidoSpecs.periodoERegiao(de, ate, regiao));

        long pendentes = 0, andamento = 0, entregues = 0, cancelados = 0, atrasados = 0;
        Map<String, long[]> porRegiao = new LinkedHashMap<>();
        Map<String, String> rotulos = new LinkedHashMap<>();

        for (Pedido p : lista) {
            StatusPedido s = p.getStatus();
            if (s == StatusPedido.PENDENTE) pendentes++;
            else if (s.isEmAndamento()) andamento++;
            else if (s == StatusPedido.ENTREGUE) entregues++;
            else if (s == StatusPedido.CANCELADO) cancelados++;

            boolean atrasado = p.estaAtrasado(hoje);
            if (atrasado) atrasados++;

            String chave = p.getRegiaoDestino().trim().toLowerCase();
            rotulos.putIfAbsent(chave, p.getRegiaoDestino().trim());
            long[] c = porRegiao.computeIfAbsent(chave, k -> new long[2]);
            c[0]++;
            if (atrasado) c[1]++;
        }

        List<RegiaoResumo> regioes = porRegiao.entrySet().stream()
                .map(e -> new RegiaoResumo(rotulos.get(e.getKey()), e.getValue()[0], e.getValue()[1]))
                .sorted(Comparator.comparingLong(RegiaoResumo::total).reversed()
                        .thenComparing(RegiaoResumo::regiao))
                .toList();

        return new DashboardResponse(lista.size(), pendentes, andamento, entregues, cancelados, atrasados, regioes);
    }
}
