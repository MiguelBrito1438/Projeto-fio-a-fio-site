package com.agrestetextil.pedidos.controller;

import com.agrestetextil.pedidos.dto.*;
import com.agrestetextil.pedidos.model.StatusPedido;
import com.agrestetextil.pedidos.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    /** RF01 - Cadastrar pedido. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse criar(@Valid @RequestBody PedidoRequest req) {
        return service.criar(req);
    }

    /** RF02 - Consultar pedidos (filtros opcionais, paginado). */
    @GetMapping
    public Page<PedidoResponse> listar(
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String cliente,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) String regiao,
            @RequestParam(required = false) Boolean atrasado,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.listar(codigo, cliente, status, de, ate, regiao, atrasado, pageable);
    }

    @GetMapping("/{id}")
    public PedidoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @GetMapping("/codigo/{codigo}")
    public PedidoResponse buscarPorCodigo(@PathVariable String codigo) {
        return service.buscarPorCodigo(codigo);
    }

    /** Edita os dados de um pedido ainda aberto (nao altera status). */
    @PutMapping("/{id}")
    public PedidoResponse atualizar(@PathVariable Long id,
                                    @Valid @RequestBody PedidoUpdateRequest req,
                                    @RequestHeader(value = "X-Usuario", required = false) String usuario,
                                    @RequestHeader(value = "X-Perfil", required = false) String perfil) {
        return service.atualizar(id, req, UsuarioHeaders.de(usuario, perfil));
    }

    /** RF03 + RF04 - Atualizar status (gera historico automaticamente). */
    @PatchMapping("/{id}/status")
    public PedidoResponse alterarStatus(@PathVariable Long id,
                                        @Valid @RequestBody StatusRequest req,
                                        @RequestHeader(value = "X-Usuario", required = false) String usuario,
                                        @RequestHeader(value = "X-Perfil", required = false) String perfil) {
        return service.alterarStatus(id, req, UsuarioHeaders.de(usuario, perfil));
    }

    /** RF08 - Atualizar localizacao informada manualmente. */
    @PatchMapping("/{id}/localizacao")
    public PedidoResponse atualizarLocalizacao(@PathVariable Long id,
                                               @Valid @RequestBody LocalizacaoRequest req,
                                               @RequestHeader(value = "X-Usuario", required = false) String usuario,
                                               @RequestHeader(value = "X-Perfil", required = false) String perfil) {
        return service.atualizarLocalizacao(id, req, UsuarioHeaders.de(usuario, perfil));
    }

    /** RF09 - Linha do tempo do pedido (ordem cronologica). */
    @GetMapping("/{id}/historico")
    public List<MovimentacaoResponse> historico(@PathVariable Long id) {
        return service.historico(id);
    }
}
