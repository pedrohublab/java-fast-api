package br.hubpedro.contracts;

import java.util.Map;


/**
 * Representa a resposta HTTP a ser enviada ao cliente.
 * Utiliza o padrão Fluent API para facilitar a construção da resposta.
 */
public interface HttpResponse {

    /**
     * Retorna o código de status HTTP (ex: 200, 201, 404, 500).
     */
    int getStatus();

    /**
     * Retorna o corpo da resposta.
     */
    String getBody();

    /**
     * Retorna um mapa contendo os cabeçalhos de resposta.
     */
    Map<String, String> getHeaders();

    /**
     * Cria uma resposta JSON com o status e o corpo especificados.
     */
    static HttpResponse json(int code, String msg) {
        return br.hubpedro.infra.api.dto.Responses.builder()
                .status(code)
                .header("Content-Type", "application/json; charset=UTF-8")
                .body(msg)
                .build();
    }
}
