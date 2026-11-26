package com.agrestetextil.pedidos.service;

import com.agrestetextil.pedidos.dto.ClienteRequest;
import com.agrestetextil.pedidos.dto.ClienteResponse;
import com.agrestetextil.pedidos.exception.NaoEncontradoException;
import com.agrestetextil.pedidos.model.Cliente;
import com.agrestetextil.pedidos.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository repo;

    public ClienteService(ClienteRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public ClienteResponse criar(ClienteRequest req) {
        Cliente c = new Cliente();
        c.setNome(req.nome().trim());
        c.setDocumento(vazioParaNulo(req.documento()));
        c.setTelefone(vazioParaNulo(req.telefone()));
        c.setCidade(vazioParaNulo(req.cidade()));
        return ClienteResponse.de(repo.save(c));
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar(String nome) {
        List<Cliente> lista = (nome == null || nome.isBlank())
                ? repo.findAll(org.springframework.data.domain.Sort.by("nome"))
                : repo.findByNomeContainingIgnoreCaseOrderByNomeAsc(nome.trim());
        return lista.stream().map(ClienteResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscar(Long id) {
        return ClienteResponse.de(repo.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Cliente não encontrado: " + id)));
    }

    private String vazioParaNulo(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
