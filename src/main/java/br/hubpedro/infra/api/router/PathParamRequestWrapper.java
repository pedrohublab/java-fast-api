package br.hubpedro.infra.api.router;

import br.hubpedro.contracts.HttpRequest;
import java.util.Map;

/**
 * Um wrapper delegante para qualquer implementação de HttpRequest.
 * Permite interceptar e substituir os parâmetros de caminho (path parameters) de forma limpa,
 * sem depender de nenhum DTO específico da infraestrutura.
 */
public class PathParamRequestWrapper implements HttpRequest {

    private final HttpRequest delegate;
    private final Map<String, String> pathParams;

    public PathParamRequestWrapper(HttpRequest delegate, Map<String, String> pathParams) {
        this.delegate = delegate;
        this.pathParams = Map.copyOf(pathParams);
    }

    @Override
    public String getMethod() {
        return delegate.getMethod();
    }

    @Override
    public String getPath() {
        return delegate.getPath();
    }

    @Override
    public String getBody() {
        return delegate.getBody();
    }

    @Override
    public Map<String, String> getHeaders() {
        return delegate.getHeaders();
    }

    @Override
    public Map<String, String> getQueryParams() {
        return delegate.getQueryParams();
    }

    @Override
    public Map<String, String> getPathParams() {
        return pathParams;
    }
}
