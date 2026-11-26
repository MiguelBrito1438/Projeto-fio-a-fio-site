package com.agrestetextil.pedidos.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.agrestetextil.pedidos.model.StatusPedido.*;
import static org.junit.jupiter.api.Assertions.*;

class AtrasoTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 17);

    @Test
    void previsaoParaHojeAindaNaoEstaAtrasada() {
        assertFalse(Atraso.estaAtrasado(EM_TRANSPORTE, HOJE, HOJE));
    }

    @Test
    void previsaoVencidaEmEtapaAbertaEstaAtrasada() {
        for (StatusPedido s : new StatusPedido[]{PENDENTE, EM_PRODUCAO, EM_PREPARACAO, AGUARDANDO_COLETA, EM_TRANSPORTE}) {
            assertTrue(Atraso.estaAtrasado(s, HOJE.minusDays(1), HOJE), s.name());
        }
        assertEquals(5, Atraso.diasDeAtraso(PENDENTE, HOJE.minusDays(5), HOJE));
    }

    @Test
    void entregueECanceladoNuncaEstaoAtrasados() {
        assertFalse(Atraso.estaAtrasado(ENTREGUE, HOJE.minusDays(5), HOJE));
        assertFalse(Atraso.estaAtrasado(CANCELADO, HOJE.minusDays(5), HOJE));
        assertEquals(0, Atraso.diasDeAtraso(ENTREGUE, HOJE.minusDays(5), HOJE));
    }

    @Test
    void previsaoFuturaNaoEstaAtrasada() {
        assertFalse(Atraso.estaAtrasado(PENDENTE, HOJE.plusDays(3), HOJE));
    }
}
