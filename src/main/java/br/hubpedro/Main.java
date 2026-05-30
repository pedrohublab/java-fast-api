package br.hubpedro;

import br.hubpedro.contracts.HttpServer;
import br.hubpedro.contracts.Router;
import br.hubpedro.infra.api.FastApi;
import br.hubpedro.infra.api.dto.Responses;

/**
 * Exemplo de uso da biblioteca FastAPI-like em Java.
 * Esta classe utiliza estritamente a API pública sem vazar pacotes internos de infraestrutura.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("=== Inicializando o FastAPI-Java (Loom Sandbox) ===");

        // 1. Instanciamos o roteador dinâmico através da fachada pública
        Router router = FastApi.newRouter();

        // 2. Registramos os endpoints no roteador utilizando respostas imutáveis
        
        // Rota Estática: Home
        router.get("/", request -> Responses.builder()
                .status(200)
                .header("Content-Type", "text/html; charset=UTF-8")
                .body("<h1>🚀 FastAPI Java com Virtual Threads está rodando!</h1>" +
                      "<p>Experimente chamar o endpoint dinâmico: <a href=\"/items/42?q=computador\">/items/42?q=computador</a></p>")
                .build()
        );

        // Rota Dinâmica com Path Parameters e Query Parameters: /items/{id}
        router.get("/items/{id}", request -> {
            String id = request.getPathParams().get("id");
            String q = request.getQueryParams().getOrDefault("q", "nenhum");
            
            // Geramos uma resposta JSON contendo dados da URL, Query e da Virtual Thread atual!
            String jsonResponse = String.format(
                    "{\n  \"item_id\": \"%s\",\n  \"query\": \"%s\",\n  \"executed_by_thread\": \"%s\"\n}",
                    id, q, Thread.currentThread()
            );

            return Responses.json(jsonResponse);
        });

        // Rota POST com leitura do Corpo (Body): /items
        router.post("/items", request -> {
            String body = request.getBody();
            
            String jsonResponse = String.format(
                    "{\n  \"message\": \"Item recebido e persistido com sucesso!\",\n  \"data\": %s,\n  \"executed_by_thread\": \"%s\"\n}",
                    body.isBlank() ? "\"{}\"" : body, Thread.currentThread()
            );

            return Responses.builder()
                    .status(201)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body(jsonResponse)
                    .build();
        });

        // 3. Inicializamos o servidor HTTP rodando com Virtual Threads na porta 8080
        HttpServer server = FastApi.newServer(router);
        
        // Gancho para desligar o servidor graciosamente no encerramento da JVM
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nEncerramento solicitado. Desligando o servidor...");
            server.stop();
        }));

        server.start(8080);
        
        // Aguarda a terminação do servidor de forma limpa para manter a JVM viva
        server.awaitTermination();
    }
}