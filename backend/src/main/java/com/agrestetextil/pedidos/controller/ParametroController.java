package com.agrestetextil.pedidos.controller;

import com.agrestetextil.pedidos.dto.AlertasResponse.ParametrosAlerta;
import com.agrestetextil.pedidos.dto.ParametrosRequest;
import com.agrestetextil.pedidos.exception.AcessoNegadoException;
import com.agrestetextil.pedidos.service.ParametroService;
import com.agrestetextil.pedidos.service.Usuario;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** Parametros de alerta ajustaveis pelo administrador. */
@RestController
@RequestMapping("/api/parametros")
public class ParametroController {

    private final ParametroService service;

    public ParametroController(ParametroService service) {
        this.service = service;
    }

    @GetMapping
    public ParametrosAlerta obter() {
        return service.obter();
    }

    @PutMapping
    public ParametrosAlerta atualizar(@Valid @RequestBody ParametrosRequest req,
                                      @RequestHeader(value = "X-Usuario", required = false) String usuario,
                                      @RequestHeader(value = "X-Perfil", required = false) String perfil) {
        Usuario u = UsuarioHeaders.de(usuario, perfil);
        if (!u.admin()) {
            throw new AcessoNegadoException("Somente o administrador (X-Perfil: ADMIN) pode alterar os parâmetros.");
        }
        return service.atualizar(req);
    }
}
