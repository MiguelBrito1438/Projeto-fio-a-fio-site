package com.agrestetextil.pedidos;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integracao: percorre o "teste principal de demonstracao" do Plano (secao 8.1)
 * e as regras do documento de requisitos. Data fixa do sistema: 17/11/2026.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PedidosApiIntegrationTest.RelogioFixo.class)
class PedidosApiIntegrationTest {

    @TestConfiguration
    static class RelogioFixo {
        @Bean
        @Primary
        Clock relogioDeTeste() {
            return Clock.fixed(Instant.parse("2026-11-17T15:00:00Z"), ZoneId.of("America/Fortaleza"));
        }
    }

    @Autowired
    MockMvc mvc;

    // ------------------------------------------------------------------ fluxo de demonstracao

    @Test
    void fluxoCompletoDeDemonstracao() throws Exception {
        long cliente = criarCliente("Atacado Demo");
        long pedido = criarPedido("DEMO-001", cliente, "Regiao-Demo", "2026-11-01", "2026-11-10");

        // cadastrado como Pendente e ja atrasado (previsao vencida, status aberto)
        mvc.perform(get("/api/pedidos/" + pedido))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.atrasado").value(true))
                .andExpect(jsonPath("$.diasAtraso").value(7));

        // localiza por codigo e por filtro
        mvc.perform(get("/api/pedidos/codigo/demo-001")).andExpect(status().isOk());
        mvc.perform(get("/api/pedidos").param("codigo", "DEMO-001").param("atrasado", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        // altera status e confere historico automatico
        alterarStatus(pedido, "EM_PRODUCAO", "Joao", null, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_PRODUCAO"));

        mvc.perform(get("/api/pedidos/" + pedido + "/historico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].tipo").value("CADASTRO"))
                .andExpect(jsonPath("$[0].statusAnteriorRotulo").value("—"))
                .andExpect(jsonPath("$[1].statusAnterior").value("PENDENTE"))
                .andExpect(jsonPath("$[1].statusNovo").value("EM_PRODUCAO"))
                .andExpect(jsonPath("$[1].responsavel").value("Joao"));
    }

    // ------------------------------------------------------------------ validacoes de cadastro

    @Test
    void rejeitaCodigoDuplicadoQuantidadeInvalidaEPrevisaoAnterior() throws Exception {
        long cliente = criarCliente("Cliente Validacao");
        criarPedido("VAL-001", cliente, "Regiao-Val", "2026-11-01", "2026-11-20");

        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content(json("val-001", cliente, "Regiao-Val", "2026-11-01", "2026-11-20", "5")))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content(json("VAL-002", cliente, "Regiao-Val", "2026-11-01", "2026-11-20", "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos").exists());

        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content(json("VAL-003", cliente, "Regiao-Val", "2026-11-10", "2026-11-05", "5")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.campos.previsaoEntrega").exists());

        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ transicoes

    @Test
    void bloqueiaTransicoesNaoPermitidasEExigeJustificativaEPerfil() throws Exception {
        long cliente = criarCliente("Cliente Transicao");
        long p = criarPedido("TRA-001", cliente, "Regiao-Tra", "2026-11-01", "2026-11-30");

        // sem identificar o usuario
        alterarStatus(p, "EM_PRODUCAO", null, null, null).andExpect(status().isUnprocessableEntity());
        // pular etapa a partir de Pendente: operador nao pode; admin precisa justificar
        alterarStatus(p, "EM_TRANSPORTE", "Joao", null, "urgente").andExpect(status().isForbidden());
        alterarStatus(p, "EM_TRANSPORTE", "Admin", "ADMIN", null).andExpect(status().isUnprocessableEntity());
        alterarStatus(p, "EM_TRANSPORTE", "Admin", "ADMIN", "Pedido urgente retirado direto")
                .andExpect(status().isOk());
        // retroceder e proibido
        alterarStatus(p, "EM_PREPARACAO", "Joao", null, null).andExpect(status().isUnprocessableEntity());
        // cancelar exige justificativa
        alterarStatus(p, "CANCELADO", "Joao", null, null).andExpect(status().isUnprocessableEntity());
        alterarStatus(p, "CANCELADO", "Joao", null, "Cliente desistiu").andExpect(status().isOk())
                .andExpect(jsonPath("$.atrasado").value(false));
        // cancelado nao volta ao fluxo
        alterarStatus(p, "EM_PRODUCAO", "Admin", "ADMIN", "tentativa").andExpect(status().isUnprocessableEntity());

        mvc.perform(get("/api/pedidos/" + p + "/historico"))
                .andExpect(jsonPath("$[*].tipo", hasItem("CANCELAMENTO")));
    }

    @Test
    void entregaEExplicitaECorrecaoExigeAdmin() throws Exception {
        long cliente = criarCliente("Cliente Entrega");
        long p = criarPedido("ENT-001", cliente, "Regiao-Ent", "2026-11-01", "2026-11-05");
        avancarAteEntregue(p);

        // entregue nunca aparece como atrasado, mesmo com previsao vencida
        mvc.perform(get("/api/pedidos/" + p)).andExpect(jsonPath("$.atrasado").value(false));
        mvc.perform(get("/api/pedidos/" + p + "/historico"))
                .andExpect(jsonPath("$[*].tipo", hasItem("CONFIRMACAO_ENTREGA")));

        alterarStatus(p, "CANCELADO", "Admin", "ADMIN", "x").andExpect(status().isUnprocessableEntity());
        alterarStatus(p, "EM_TRANSPORTE", "Joao", null, "erro de digitacao").andExpect(status().isForbidden());
        alterarStatus(p, "EM_TRANSPORTE", "Admin", "ADMIN", "Entrega marcada por engano")
                .andExpect(status().isOk());
        mvc.perform(get("/api/pedidos/" + p + "/historico"))
                .andExpect(jsonPath("$[*].tipo", hasItem("CORRECAO_EXCEPCIONAL")));
    }

    @Test
    void atualizaLocalizacaoManualERegistraNoHistorico() throws Exception {
        long cliente = criarCliente("Cliente Local");
        long p = criarPedido("LOC-001", cliente, "Regiao-Loc", "2026-11-16", "2026-11-30");

        mvc.perform(patch("/api/pedidos/" + p + "/localizacao")
                        .header("X-Usuario", "Joao").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"localizacao\":\"Santa Cruz do Capibaribe\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ultimaLocalizacao").value("Santa Cruz do Capibaribe"));

        mvc.perform(get("/api/pedidos/" + p + "/historico"))
                .andExpect(jsonPath("$[*].tipo", hasItem("ATUALIZACAO_LOCALIZACAO")));
    }

    // ------------------------------------------------------------------ painel e alertas

    @Test
    void painelEAlertasRegionaisRefletemOsDadosCadastrados() throws Exception {
        long cliente = criarCliente("Cliente Painel");
        String r = "Regiao-Painel";

        criarPedido("PNL-A", cliente, r, "2026-11-01", "2026-11-10");               // pendente, atrasado
        criarPedido("PNL-F", cliente, r, "2026-11-01", "2026-11-12");               // pendente, atrasado
        long b = criarPedido("PNL-B", cliente, r, "2026-11-01", "2026-11-10");     // em producao, atrasado
        alterarStatus(b, "EM_PRODUCAO", "Joao", null, null).andExpect(status().isOk());
        long c = criarPedido("PNL-C", cliente, r, "2026-11-01", "2026-11-20");     // em transporte, no prazo
        alterarStatus(c, "EM_PRODUCAO", "Joao", null, null);
        alterarStatus(c, "EM_PREPARACAO", "Joao", null, null);
        alterarStatus(c, "AGUARDANDO_COLETA", "Joao", null, null);
        alterarStatus(c, "EM_TRANSPORTE", "Joao", null, null).andExpect(status().isOk());
        long d = criarPedido("PNL-D", cliente, r, "2026-11-01", "2026-11-10");     // entregue
        avancarAteEntregue(d);
        long e = criarPedido("PNL-E", cliente, r, "2026-11-01", "2026-11-10");     // cancelado
        alterarStatus(e, "CANCELADO", "Joao", null, "Sem estoque").andExpect(status().isOk());
        criarPedido("PNL-G", cliente, r, "2026-11-01", "2026-11-17");               // vence hoje: NAO atrasado

        mvc.perform(get("/api/dashboard").param("regiao", r))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPedidos").value(7))
                .andExpect(jsonPath("$.pendentes").value(3))     // A, F, G
                .andExpect(jsonPath("$.emAndamento").value(2))   // B, C
                .andExpect(jsonPath("$.entregues").value(1))
                .andExpect(jsonPath("$.cancelados").value(1))
                .andExpect(jsonPath("$.atrasados").value(3))     // A, F, B
                .andExpect(jsonPath("$.porRegiao[0].total").value(7))
                .andExpect(jsonPath("$.porRegiao[0].atrasados").value(3));

        // filtro de periodo (data do pedido) fora do intervalo => zero
        mvc.perform(get("/api/dashboard").param("regiao", r).param("de", "2026-12-01"))
                .andExpect(jsonPath("$.totalPedidos").value(0));

        mvc.perform(get("/api/alertas"))
                .andExpect(status().isOk())
                // 3 atrasados na mesma regiao => alerta agrupado (limite padrao = 3)
                .andExpect(jsonPath("$.concentracoesRegionais[?(@.regiaoDestino=='" + r + "')].quantidadeAtrasados",
                        contains(3)))
                // previsao para hoje aparece na area de atencao, sem ser atraso
                .andExpect(jsonPath("$.previsaoHoje[?(@.codigo=='PNL-G')]", hasSize(1)))
                .andExpect(jsonPath("$.atrasosIndividuais[?(@.codigo=='PNL-G')]", hasSize(0)))
                // entregue e cancelado nunca geram alerta de atraso
                .andExpect(jsonPath("$.atrasosIndividuais[?(@.codigo=='PNL-D')]", hasSize(0)))
                .andExpect(jsonPath("$.atrasosIndividuais[?(@.codigo=='PNL-E')]", hasSize(0)));
    }

    @Test
    void parametrosDeAlertaSoSaoAlteradosPorAdmin() throws Exception {
        mvc.perform(put("/api/parametros").header("X-Usuario", "Joao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"limiteAtrasosPorRegiao\":5,\"horasSemAtualizacao\":24}"))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/parametros").header("X-Usuario", "Admin").header("X-Perfil", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"limiteAtrasosPorRegiao\":3,\"horasSemAtualizacao\":48}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limiteAtrasosPorRegiao").value(3));
    }

    // ------------------------------------------------------------------ auxiliares

    private long criarCliente(String nome) throws Exception {
        String corpo = mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\",\"cidade\":\"Caruaru\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(corpo, "$.id")).longValue();
    }

    private long criarPedido(String codigo, long clienteId, String regiao, String data, String previsao) throws Exception {
        String corpo = mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content(json(codigo, clienteId, regiao, data, previsao, "10")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(corpo, "$.id")).longValue();
    }

    private ResultActions alterarStatus(long id, String novo, String usuario, String perfil, String descricao) throws Exception {
        var req = patch("/api/pedidos/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"novoStatus\":\"" + novo + "\""
                        + (descricao == null ? "" : ",\"descricao\":\"" + descricao + "\"") + "}");
        if (usuario != null) req.header("X-Usuario", usuario);
        if (perfil != null) req.header("X-Perfil", perfil);
        return mvc.perform(req);
    }

    private void avancarAteEntregue(long id) throws Exception {
        for (String s : new String[]{"EM_PRODUCAO", "EM_PREPARACAO", "AGUARDANDO_COLETA", "EM_TRANSPORTE", "ENTREGUE"}) {
            alterarStatus(id, s, "Joao", null, null).andExpect(status().isOk());
        }
    }

    private String json(String codigo, long clienteId, String regiao, String data, String previsao, String qtd) {
        return """
                {"codigo":"%s","clienteId":%d,"responsavel":"Maria","origem":"Toritama","destino":"Recife",
                 "regiaoDestino":"%s","regiaoOrigem":"Agreste",
                 "itens":[{"descricao":"Camisas polo","quantidade":%s,"unidade":"un"}],
                 "dataPedido":"%s","previsaoEntrega":"%s","observacoes":"teste"}
                """.formatted(codigo, clienteId, regiao, qtd, data, previsao);
    }
}
