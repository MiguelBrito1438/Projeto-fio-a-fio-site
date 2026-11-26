package com.agrestetextil.pedidos.service;

import com.agrestetextil.pedidos.dto.*;
import com.agrestetextil.pedidos.exception.AcessoNegadoException;
import com.agrestetextil.pedidos.exception.ConflitoException;
import com.agrestetextil.pedidos.exception.NaoEncontradoException;
import com.agrestetextil.pedidos.exception.RegraNegocioException;
import com.agrestetextil.pedidos.model.*;
import com.agrestetextil.pedidos.repository.ClienteRepository;
import com.agrestetextil.pedidos.repository.MovimentacaoRepository;
import com.agrestetextil.pedidos.repository.PedidoRepository;
import com.agrestetextil.pedidos.repository.PedidoSpecs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Regras de negocio dos pedidos. Toda mudanca de status/localizacao grava o historico
 * NA MESMA TRANSACAO (se o historico falhar, o pedido nao e alterado).
 */
@Service
public class PedidoService {

    private final PedidoRepository pedidos;
    private final ClienteRepository clientes;
    private final MovimentacaoRepository movimentacoes;
    private final Clock clock;

    public PedidoService(PedidoRepository pedidos, ClienteRepository clientes,
                         MovimentacaoRepository movimentacoes, Clock clock) {
        this.pedidos = pedidos;
        this.clientes = clientes;
        this.movimentacoes = movimentacoes;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- RF01 cadastrar

    @Transactional
    public PedidoResponse criar(PedidoRequest req) {
        String codigo = req.codigo().trim();
        validarDatas(req.dataPedido(), req.previsaoEntrega());

        if (pedidos.existsByCodigoIgnoreCase(codigo)) {
            throw new ConflitoException("codigo", "Já existe um pedido com o código " + codigo + ".");
        }
        Cliente cliente = buscarCliente(req.clienteId());
        LocalDateTime agora = LocalDateTime.now(clock);

        Pedido p = new Pedido();
        p.setCodigo(codigo);
        p.setCliente(cliente);
        p.setResponsavel(req.responsavel().trim());
        p.setOrigem(req.origem().trim());
        p.setDestino(req.destino().trim());
        p.setRegiaoOrigem(vazioParaNulo(req.regiaoOrigem()));
        p.setRegiaoDestino(req.regiaoDestino().trim());
        p.setDataPedido(req.dataPedido());
        p.setPrevisaoEntrega(req.previsaoEntrega());
        p.setStatus(StatusPedido.PENDENTE);
        p.setObservacoes(vazioParaNulo(req.observacoes()));
        p.setUltimaLocalizacao(vazioParaNulo(req.ultimaLocalizacao()));
        p.setAtualizadoEm(agora);
        preencherItens(p, req.itens());

        pedidos.saveAndFlush(p);
        registrar(p, null, StatusPedido.PENDENTE, TipoMovimentacao.CADASTRO, p.getResponsavel(),
                p.getUltimaLocalizacao(), "Pedido cadastrado.", agora);
        return paraResposta(p);
    }

    // ---------------------------------------------------------------- RF02 consultar

    @Transactional(readOnly = true)
    public Page<PedidoResponse> listar(String codigo, String cliente, StatusPedido status,
                                       LocalDate de, LocalDate ate, String regiao,
                                       Boolean atrasado, Pageable pageable) {
        LocalDate hoje = LocalDate.now(clock);
        return pedidos.findAll(PedidoSpecs.filtrar(codigo, cliente, status, de, ate, regiao, atrasado, hoje), pageable)
                .map(p -> PedidoResponse.de(p, hoje));
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscar(Long id) {
        return paraResposta(obter(id));
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscarPorCodigo(String codigo) {
        return paraResposta(pedidos.findByCodigoIgnoreCase(codigo.trim())
                .orElseThrow(() -> new NaoEncontradoException("Pedido não encontrado: " + codigo)));
    }

    // ---------------------------------------------------------------- edicao de dados

    @Transactional
    public PedidoResponse atualizar(Long id, PedidoUpdateRequest req, Usuario usuario) {
        Pedido p = obter(id);
        if (p.getStatus().isFinal()) {
            throw new RegraNegocioException("Pedidos " + p.getStatus().getRotulo().toLowerCase()
                    + "s não podem ter os dados editados.");
        }
        validarDatas(p.getDataPedido(), req.previsaoEntrega());

        LocalDateTime agora = LocalDateTime.now(clock);
        StringBuilder alteracoes = new StringBuilder("Dados do pedido editados.");
        if (!p.getPrevisaoEntrega().equals(req.previsaoEntrega())) {
            alteracoes.append(" Previsão de entrega: ").append(p.getPrevisaoEntrega())
                    .append(" -> ").append(req.previsaoEntrega()).append('.');
        }
        if (!p.getRegiaoDestino().equalsIgnoreCase(req.regiaoDestino().trim())) {
            alteracoes.append(" Região de destino: ").append(p.getRegiaoDestino())
                    .append(" -> ").append(req.regiaoDestino().trim()).append('.');
        }

        p.setCliente(buscarCliente(req.clienteId()));
        p.setResponsavel(req.responsavel().trim());
        p.setOrigem(req.origem().trim());
        p.setDestino(req.destino().trim());
        p.setRegiaoOrigem(vazioParaNulo(req.regiaoOrigem()));
        p.setRegiaoDestino(req.regiaoDestino().trim());
        p.setPrevisaoEntrega(req.previsaoEntrega());
        p.setObservacoes(vazioParaNulo(req.observacoes()));
        p.limparItens();
        preencherItens(p, req.itens());
        p.setAtualizadoEm(agora);

        pedidos.saveAndFlush(p);
        registrar(p, p.getStatus(), p.getStatus(), TipoMovimentacao.EDICAO_DADOS, usuario.nome(),
                p.getUltimaLocalizacao(), alteracoes.toString(), agora);
        return paraResposta(p);
    }

    // ---------------------------------------------------------------- RF03 + RF04 atualizar status

    @Transactional
    public PedidoResponse alterarStatus(Long id, StatusRequest req, Usuario usuario) {
        Pedido p = obter(id);
        StatusPedido anterior = p.getStatus();
        StatusPedido novo = req.novoStatus();

        PoliticaTransicao.Resultado r = PoliticaTransicao.avaliar(anterior, novo);
        if (!r.permitida()) {
            throw new RegraNegocioException("novoStatus", r.motivoRecusa());
        }
        if (r.exigeAdmin() && !usuario.admin()) {
            throw new AcessoNegadoException("Esta transição (" + anterior.getRotulo() + " -> "
                    + novo.getRotulo() + ") exige usuário autorizado (perfil ADMIN).");
        }
        String justificativa = vazioParaNulo(req.descricao());
        if (r.exigeJustificativa() && justificativa == null) {
            throw new RegraNegocioException("descricao", "A justificativa é obrigatória para esta alteração.");
        }

        LocalDateTime agora = LocalDateTime.now(clock);
        String localizacao = vazioParaNulo(req.localizacao());

        p.setStatus(novo);
        if (localizacao != null) {
            p.setUltimaLocalizacao(localizacao);
        }
        p.setAtualizadoEm(agora);
        pedidos.saveAndFlush(p);

        String descricao = justificativa != null ? justificativa
                : "Status alterado de " + anterior.getRotulo() + " para " + novo.getRotulo() + ".";
        registrar(p, anterior, novo, r.tipo(), usuario.nome(),
                localizacao != null ? localizacao : p.getUltimaLocalizacao(), descricao, agora);
        return paraResposta(p);
    }

    // ---------------------------------------------------------------- RF08 localizacao manual

    @Transactional
    public PedidoResponse atualizarLocalizacao(Long id, LocalizacaoRequest req, Usuario usuario) {
        Pedido p = obter(id);
        if (p.getStatus().isFinal()) {
            throw new RegraNegocioException("Não é possível atualizar a localização de um pedido "
                    + p.getStatus().getRotulo().toLowerCase() + ".");
        }
        LocalDateTime agora = LocalDateTime.now(clock);
        String localizacao = req.localizacao().trim();
        p.setUltimaLocalizacao(localizacao);
        p.setAtualizadoEm(agora);
        pedidos.saveAndFlush(p);

        String descricao = vazioParaNulo(req.descricao());
        registrar(p, p.getStatus(), p.getStatus(), TipoMovimentacao.ATUALIZACAO_LOCALIZACAO, usuario.nome(),
                localizacao, descricao != null ? descricao : "Localização atualizada manualmente.", agora);
        return paraResposta(p);
    }

    // ---------------------------------------------------------------- RF09 historico

    @Transactional(readOnly = true)
    public List<MovimentacaoResponse> historico(Long id) {
        obter(id);
        return movimentacoes.findByPedidoIdOrderByDataHoraAscIdAsc(id).stream()
                .map(MovimentacaoResponse::de).toList();
    }

    // ---------------------------------------------------------------- auxiliares

    private Pedido obter(Long id) {
        return pedidos.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Pedido não encontrado: " + id));
    }

    private Cliente buscarCliente(Long id) {
        return clientes.findById(id)
                .orElseThrow(() -> new RegraNegocioException("clienteId", "Cliente não encontrado: " + id));
    }

    private void validarDatas(LocalDate dataPedido, LocalDate previsao) {
        if (previsao.isBefore(dataPedido)) {
            throw new RegraNegocioException("previsaoEntrega",
                    "A previsão de entrega não pode ser anterior à data do pedido.");
        }
    }

    private void preencherItens(Pedido p, List<ItemPedidoRequest> itens) {
        for (ItemPedidoRequest i : itens) {
            ItemPedido item = new ItemPedido();
            item.setDescricao(i.descricao().trim());
            item.setQuantidade(i.quantidade());
            item.setUnidade(vazioParaNulo(i.unidade()));
            p.adicionarItem(item);
        }
    }

    private void registrar(Pedido p, StatusPedido anterior, StatusPedido novo, TipoMovimentacao tipo,
                           String responsavel, String localizacao, String descricao, LocalDateTime quando) {
        Movimentacao m = new Movimentacao();
        m.setPedido(p);
        m.setStatusAnterior(anterior);
        m.setStatusNovo(novo);
        m.setTipo(tipo);
        m.setDataHora(quando);
        m.setResponsavel(responsavel);
        m.setLocalizacao(localizacao);
        m.setDescricao(descricao);
        movimentacoes.save(m);
    }

    private PedidoResponse paraResposta(Pedido p) {
        return PedidoResponse.de(p, LocalDate.now(clock));
    }

    private String vazioParaNulo(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
