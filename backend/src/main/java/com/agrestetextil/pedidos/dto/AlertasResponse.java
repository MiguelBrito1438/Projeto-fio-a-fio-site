package com.agrestetextil.pedidos.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Alertas regionais (RF07, secao 8 do documento de requisitos). */
public record AlertasResponse(
        List<AlertaAtraso> atrasosIndividuais,
        List<AlertaConcentracao> concentracoesRegionais,
        List<AlertaPrazoHoje> previsaoHoje,
        List<AlertaSemAtualizacao> semAtualizacao,
        ParametrosAlerta parametros) {

    public record AlertaAtraso(Long pedidoId, String codigo, String regiaoDestino, LocalDate previsaoEntrega,
                               String status, String statusRotulo, long diasAtraso) {
    }

    public record AlertaConcentracao(String regiaoDestino, long quantidadeAtrasados, List<String> codigos) {
    }

    public record AlertaPrazoHoje(Long pedidoId, String codigo, String regiaoDestino, String status,
                                  String statusRotulo) {
    }

    public record AlertaSemAtualizacao(Long pedidoId, String codigo, String regiaoDestino,
                                       LocalDateTime ultimaAtualizacao, long horasSemAtualizacao) {
    }

    public record ParametrosAlerta(int limiteAtrasosPorRegiao, int horasSemAtualizacao) {
    }
}
