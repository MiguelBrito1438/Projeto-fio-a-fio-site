package com.agrestetextil.pedidos.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Converte excecoes em respostas JSON padronizadas: {timestamp, status, erro, mensagem, campos}. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacao(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> campos.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos. Verifique os campos informados.", campos);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> violacao(ConstraintViolationException ex) {
        return resposta(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> jsonInvalido(HttpMessageNotReadableException ex) {
        return resposta(HttpStatus.BAD_REQUEST,
                "Corpo da requisição inválido (JSON malformado, data fora do formato AAAA-MM-DD ou valor de status desconhecido).",
                null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> tipoInvalido(MethodArgumentTypeMismatchException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put(ex.getName(), "Valor inválido: " + ex.getValue());
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetro inválido.", campos);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> cabecalhoAusente(MissingRequestHeaderException ex) {
        return resposta(HttpStatus.BAD_REQUEST,
                "Cabeçalho obrigatório ausente: " + ex.getHeaderName() + ".", null);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Map<String, Object>> regra(RegraNegocioException ex) {
        Map<String, String> campos = ex.getCampo() == null ? null : Map.of(ex.getCampo(), ex.getMessage());
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), campos);
    }

    @ExceptionHandler(NaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> naoEncontrado(NaoEncontradoException ex) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<Map<String, Object>> conflito(ConflitoException ex) {
        Map<String, String> campos = ex.getCampo() == null ? null : Map.of(ex.getCampo(), ex.getMessage());
        return resposta(HttpStatus.CONFLICT, ex.getMessage(), campos);
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<Map<String, Object>> acessoNegado(AcessoNegadoException ex) {
        return resposta(HttpStatus.FORBIDDEN, ex.getMessage(), null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integridade(DataIntegrityViolationException ex) {
        return resposta(HttpStatus.CONFLICT,
                "Operação viola a integridade dos dados (por exemplo, código de pedido já existente).", null);
    }

    private ResponseEntity<Map<String, Object>> resposta(HttpStatus status, String mensagem,
                                                         Map<String, String> campos) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("timestamp", LocalDateTime.now().toString());
        corpo.put("status", status.value());
        corpo.put("erro", status.getReasonPhrase());
        corpo.put("mensagem", mensagem);
        if (campos != null && !campos.isEmpty()) {
            corpo.put("campos", campos);
        }
        return ResponseEntity.status(status).body(corpo);
    }
}
