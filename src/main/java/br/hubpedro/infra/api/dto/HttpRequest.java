package br.hubpedro.infra.api.dto;

import java.util.Map;

public record HttpRequest(
        String method,
        String path,
        Map<String, String> headers,
        Map<String, String> queryParams,
        Map<String, String> pathParams,
        String body) implements br.hubpedro.contracts.HttpRequest {

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getBody() {
        return body;
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    @Override
    public Map<String, String> getPathParams() {
        return pathParams;
    }

    /**
     * Retorna uma cópia da requisição com os parâmetros de caminho injetados.
     */
    public HttpRequest withPathParams(Map<String, String> pathParams) {
        return new HttpRequest(method, path, headers, queryParams, pathParams, body);
    }
}
