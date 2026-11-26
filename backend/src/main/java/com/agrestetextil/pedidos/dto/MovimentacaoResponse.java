package com.agrestetextil.pedidos.dto;

import com.agrestetextil.pedidos.model.Movimentacao;

import java.time.LocalDateTime;

public record MovimentacaoResponse(
        Long id,
        String statusAnterior,
        String statusAnteriorRotulo,
        String statusNovo,
        String statusNovoRotulo,
        String tipo,
        LocalDateTime dataHora,
        String responsavel,
        String localizacao,
        String descricao) {

    public static MovimentacaoResponse de(Movimentacao m) {
        return new MovimentacaoResponse(
                m.getId(),
                m.getStatusAnterior() == null ? null : m.getStatusAnterior().name(),
                m.getStatusAnterior() == null ? "—" : m.getStatusAnterior().getRotulo(),
                m.getStatusNovo().name(),
                m.getStatusNovo().getRotulo(),
                m.getTipo().name(),
                m.getDataHora(),
                m.getResponsavel(),
                m.getLocalizacao(),
                m.getDescricao());
    }
}
