package com.agrestetextil.pedidos.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static com.agrestetextil.pedidos.model.StatusPedido.*;
import static org.junit.jupiter.api.Assertions.*;

class PoliticaTransicaoTest {

    @Test
    void fluxoNormalAvancaUmaEtapaSemExigirAdmin() {
        StatusPedido atual = PENDENTE;
        while (atual.proximo() != null) {
            var r = PoliticaTransicao.avaliar(atual, atual.proximo());
            assertTrue(r.permitida(), "deveria permitir " + atual);
            assertFalse(r.exigeAdmin());
            assertFalse(r.exigeJustificativa());
            atual = atual.proximo();
        }
        assertEquals(ENTREGUE, atual);
    }

    @Test
    void entregaEConfirmacaoExplicita() {
        assertEquals(TipoMovimentacao.CONFIRMACAO_ENTREGA,
                PoliticaTransicao.avaliar(EM_TRANSPORTE, ENTREGUE).tipo());
    }

    @ParameterizedTest
    @EnumSource(value = StatusPedido.class, names = "CANCELADO", mode = EnumSource.Mode.EXCLUDE)
    void canceladoNaoVoltaAoFluxo(StatusPedido destino) {
        assertFalse(PoliticaTransicao.avaliar(CANCELADO, destino).permitida());
    }

    @ParameterizedTest
    @EnumSource(value = StatusPedido.class, names = {"ENTREGUE", "CANCELADO"}, mode = EnumSource.Mode.EXCLUDE)
    void cancelamentoPermitidoAntesDaEntregaComJustificativa(StatusPedido atual) {
        var r = PoliticaTransicao.avaliar(atual, CANCELADO);
        assertTrue(r.permitida());
        assertTrue(r.exigeJustificativa());
        assertEquals(TipoMovimentacao.CANCELAMENTO, r.tipo());
    }

    @Test
    void entregueNaoPodeSerCancelado() {
        assertFalse(PoliticaTransicao.avaliar(ENTREGUE, CANCELADO).permitida());
    }

    @Test
    void correcaoDeEntregueExigeAdminEJustificativa() {
        var r = PoliticaTransicao.avaliar(ENTREGUE, EM_TRANSPORTE);
        assertTrue(r.permitida());
        assertTrue(r.exigeAdmin());
        assertTrue(r.exigeJustificativa());
        assertEquals(TipoMovimentacao.CORRECAO_EXCEPCIONAL, r.tipo());
    }

    @Test
    void pendenteParaEtapaPosteriorExigeAdminEJustificativa() {
        var r = PoliticaTransicao.avaliar(PENDENTE, EM_TRANSPORTE);
        assertTrue(r.permitida());
        assertTrue(r.exigeAdmin());
        assertTrue(r.exigeJustificativa());
    }

    @Test
    void naoPermitePularEtapasForaDePendenteNemRetroceder() {
        assertFalse(PoliticaTransicao.avaliar(EM_PRODUCAO, AGUARDANDO_COLETA).permitida());
        assertFalse(PoliticaTransicao.avaliar(EM_PREPARACAO, EM_PRODUCAO).permitida());
        assertFalse(PoliticaTransicao.avaliar(EM_TRANSPORTE, PENDENTE).permitida());
    }

    @Test
    void mesmoStatusNaoEhTransicao() {
        assertFalse(PoliticaTransicao.avaliar(PENDENTE, PENDENTE).permitida());
    }
}
