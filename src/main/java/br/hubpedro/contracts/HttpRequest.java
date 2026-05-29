package br.hubpedro.contracts;

import java.util.Map;

/**
 * Representa uma requisição HTTP recebida pelo servidor.
 */
public interface HttpRequest {

    /**
     * Retorna o método HTTP da requisição (ex: GET, POST, PUT, DELETE).
     */
    String getMethod();

    /**
     * Retorna o caminho (URI/path) da requisição (ex: "/users").
     */
    String getPath();

    /**
     * Retorna o corpo (body) da requisição.
     */
    String getBody();

    /**
     * Retorna um mapa contendo os cabeçalhos (headers) da requisição.
     */
    Map<String, String> getHeaders();

    /**
     * Retorna um mapa contendo os parâmetros de busca (query parameters).
     */
    Map<String, String> getQueryParams();

    /**
     * Retorna um mapa contendo os parâmetros capturados na URL (ex: id de "/users/{id}").
     */
    Map<String, String> getPathParams();
}
