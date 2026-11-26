package com.agrestetextil.pedidos.service;

import com.agrestetextil.pedidos.dto.AlertasResponse;
import com.agrestetextil.pedidos.dto.AlertasResponse.*;
import com.agrestetextil.pedidos.model.Pedido;
import com.agrestetextil.pedidos.model.StatusPedido;
import com.agrestetextil.pedidos.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** Alertas regionais (RF07). Sempre calculados na hora, sobre os dados atuais (RN13). */
@Service
public class AlertaService {

    private final PedidoRepository pedidos;
    private final ParametroService parametros;
    private final Clock clock;

    public AlertaService(PedidoRepository pedidos, ParametroService parametros, Clock clock) {
        this.pedidos = pedidos;
        this.parametros = parametros;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AlertasResponse gerar() {
        LocalDate hoje = LocalDate.now(clock);
        LocalDateTime agora = LocalDateTime.now(clock);
        ParametrosAlerta config = parametros.obter();

        List<StatusPedido> abertos = Arrays.stream(StatusPedido.values())
                .filter(StatusPedido::isAberto).toList();
        List<Pedido> emAberto = pedidos.findByStatusIn(abertos);

        // 1) Atraso individual (somente pedidos abertos; entregues/cancelados nunca entram)
        List<Pedido> atrasados = emAberto.stream()
                .filter(p -> p.estaAtrasado(hoje))
                .sorted(Comparator.comparingLong((Pedido p) -> p.diasDeAtraso(hoje)).reversed()
                        .thenComparing(Pedido::getCodigo))
                .toList();
        List<AlertaAtraso> individuais = atrasados.stream()
                .map(p -> new AlertaAtraso(p.getId(), p.getCodigo(), p.getRegiaoDestino(), p.getPrevisaoEntrega(),
                        p.getStatus().name(), p.getStatus().getRotulo(), p.diasDeAtraso(hoje)))
                .toList();

        // 2) Concentracao regional: N ou mais atrasados na mesma regiao de destino
        Map<String, List<Pedido>> porRegiao = atrasados.stream()
                .collect(Collectors.groupingBy(p -> p.getRegiaoDestino().trim().toLowerCase(),
                        LinkedHashMap::new, Collectors.toList()));
        List<AlertaConcentracao> concentracoes = porRegiao.values().stream()
                .filter(l -> l.size() >= config.limiteAtrasosPorRegiao())
                .map(l -> new AlertaConcentracao(l.get(0).getRegiaoDestino().trim(), l.size(),
                        l.stream().map(Pedido::getCodigo).sorted().toList()))
                .sorted(Comparator.comparingLong(AlertaConcentracao::quantidadeAtrasados).reversed()
                        .thenComparing(AlertaConcentracao::regiaoDestino))
                .toList();

        // 3) Previsao para hoje (area de atencao; ainda NAO e atraso)
        List<AlertaPrazoHoje> prazoHoje = emAberto.stream()
                .filter(p -> p.getPrevisaoEntrega().equals(hoje))
                .sorted(Comparator.comparing(Pedido::getCodigo))
                .map(p -> new AlertaPrazoHoje(p.getId(), p.getCodigo(), p.getRegiaoDestino(),
                        p.getStatus().name(), p.getStatus().getRotulo()))
                .toList();

        // 4) Em transporte sem movimentacao registrada por mais que o periodo configurado
        Duration limite = Duration.ofHours(config.horasSemAtualizacao());
        List<AlertaSemAtualizacao> semAtualizacao = emAberto.stream()
                .filter(p -> p.getStatus() == StatusPedido.EM_TRANSPORTE)
                .filter(p -> Duration.between(p.getAtualizadoEm(), agora).compareTo(limite) > 0)
                .sorted(Comparator.comparing(Pedido::getAtualizadoEm))
                .map(p -> new AlertaSemAtualizacao(p.getId(), p.getCodigo(), p.getRegiaoDestino(),
                        p.getAtualizadoEm(), Duration.between(p.getAtualizadoEm(), agora).toHours()))
                .toList();

        return new AlertasResponse(individuais, concentracoes, prazoHoje, semAtualizacao, config);
    }
}
