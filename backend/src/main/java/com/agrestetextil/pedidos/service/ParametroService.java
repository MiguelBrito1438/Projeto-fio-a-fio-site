package com.agrestetextil.pedidos.service;

import com.agrestetextil.pedidos.dto.AlertasResponse.ParametrosAlerta;
import com.agrestetextil.pedidos.dto.ParametrosRequest;
import com.agrestetextil.pedidos.model.Parametro;
import com.agrestetextil.pedidos.repository.ParametroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Parametros ajustaveis pela administracao, sem alterar a regra central (documento de requisitos, secao 11). */
@Service
public class ParametroService {

    static final String LIMITE_ATRASOS = "ALERTA_LIMITE_ATRASOS_REGIAO";
    static final String HORAS_SEM_ATUALIZACAO = "ALERTA_HORAS_SEM_ATUALIZACAO";
    static final int PADRAO_LIMITE_ATRASOS = 3;
    static final int PADRAO_HORAS_SEM_ATUALIZACAO = 48;

    private final ParametroRepository repo;

    public ParametroService(ParametroRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public ParametrosAlerta obter() {
        return new ParametrosAlerta(
                lerInt(LIMITE_ATRASOS, PADRAO_LIMITE_ATRASOS),
                lerInt(HORAS_SEM_ATUALIZACAO, PADRAO_HORAS_SEM_ATUALIZACAO));
    }

    @Transactional
    public ParametrosAlerta atualizar(ParametrosRequest req) {
        gravar(LIMITE_ATRASOS, req.limiteAtrasosPorRegiao());
        gravar(HORAS_SEM_ATUALIZACAO, req.horasSemAtualizacao());
        return obter();
    }

    private int lerInt(String chave, int padrao) {
        return repo.findById(chave)
                .map(p -> {
                    try {
                        return Integer.parseInt(p.getValor());
                    } catch (NumberFormatException e) {
                        return padrao;
                    }
                })
                .orElse(padrao);
    }

    private void gravar(String chave, int valor) {
        Parametro p = repo.findById(chave).orElseGet(() -> new Parametro(chave, String.valueOf(valor)));
        p.setValor(String.valueOf(valor));
        repo.save(p);
    }
}
