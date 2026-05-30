package br.hubpedro.infra.api.dto;

import br.hubpedro.contracts.HttpResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Fábrica de conveniência e Builder para construção segura e imutável de respostas HTTP.
 */
public final class Responses {

    private Responses() {
        // Construtor privado para evitar instanciação
    }

    /**
     * Retorna um novo construtor (builder) de respostas HTTP.
     *
     * @return uma nova instância de ResponseBuilder
     */
    public static ResponseBuilder builder() {
        return new ResponseBuilder();
    }

    /**
     * Atalho para criar uma resposta 200 OK de texto puro.
     *
     * @param body o corpo da resposta
     * @return a resposta HTTP imutável
     */
    public static HttpResponse ok(String body) {
        return builder().status(200).body(body).build();
    }

    /**
     * Atalho para criar uma resposta 200 OK no formato JSON.
     *
     * @param json o corpo JSON da resposta
     * @return a resposta HTTP imutável
     */
    public static HttpResponse json(String json) {
        return builder()
                .status(200)
                .header("Content-Type", "application/json; charset=UTF-8")
                .body(json)
                .build();
    }

    /**
     * Atalho para criar uma resposta apenas com código de status e corpo vazio.
     *
     * @param status o código de status HTTP
     * @return a resposta HTTP imutável
     */
    public static HttpResponse status(int status) {
        return builder().status(status).build();
    }

    /**
     * Construtor fluente para personalização refinada das respostas HTTP.
     */
    public static final class ResponseBuilder {
        private int status = 200;
        private String body = "";
        private final Map<String, String> headers = new HashMap<>();

        ResponseBuilder() {
            // Configura cabeçalhos seguros padrão e metadados do servidor
            headers.put("Content-Type", "text/plain; charset=UTF-8");
            headers.put("Server", "FastAPI-Java-VirtualThreads");
            headers.put("X-Content-Type-Options", "nosniff");
            headers.put("X-Frame-Options", "DENY");
            headers.put("X-XSS-Protection", "1; mode=block");
        }

        public ResponseBuilder status(int status) {
            this.status = status;
            return this;
        }

        public ResponseBuilder body(String body) {
            this.body = body == null ? "" : body;
            return this;
        }

        public ResponseBuilder header(String name, String value) {
            if (name != null && value != null) {
                this.headers.put(name, value);
            }
            return this;
        }

        /**
         * Compila as configurações em uma instância de HttpResponse imutável.
         *
         * @return a HttpResponse imutável construída
         */
        public HttpResponse build() {
            return new ImmutableHttpResponse(status, body, headers);
        }
    }

    /**
     * Implementação interna estritamente imutável do contrato HttpResponse.
     */
    private static record ImmutableHttpResponse(int status, String body, Map<String, String> headers) implements HttpResponse {
        public ImmutableHttpResponse {
            // Garante que o mapa interno seja imutável e seguro contra vazamentos de estado
            headers = Map.copyOf(headers);
        }

        @Override
        public int getStatus() {
            return status;
        }

        @Override
        public String getBody() {
            return body;
        }

        @Override
        public Map<String, String> getHeaders() {
            return headers;
        }
    }
}
