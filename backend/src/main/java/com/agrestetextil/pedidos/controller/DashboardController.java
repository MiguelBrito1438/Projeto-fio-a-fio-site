package com.agrestetextil.pedidos.controller;

import com.agrestetextil.pedidos.dto.AlertasResponse;
import com.agrestetextil.pedidos.dto.DashboardResponse;
import com.agrestetextil.pedidos.service.AlertaService;
import com.agrestetextil.pedidos.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardService dashboard;
    private final AlertaService alertas;

    public DashboardController(DashboardService dashboard, AlertaService alertas) {
        this.dashboard = dashboard;
        this.alertas = alertas;
    }

    /** RF06 - Painel geral. Filtros opcionais por periodo (data do pedido) e regiao. */
    @GetMapping("/dashboard")
    public DashboardResponse dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) String regiao) {
        return dashboard.calcular(de, ate, regiao);
    }

    /** RF07 - Alertas regionais. */
    @GetMapping("/alertas")
    public AlertasResponse alertas() {
        return alertas.gerar();
    }
}
