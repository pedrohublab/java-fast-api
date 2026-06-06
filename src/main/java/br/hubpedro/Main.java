package br.hubpedro;

import br.hubpedro.contracts.Router;

import java.util.logging.Logger;

import br.hubpedro.contracts.HttpServer;
import br.hubpedro.infra.api.FastApi;
import br.hubpedro.infra.api.dto.Responses;


/**
 * Exemplo de uso da biblioteca FastAPI-like em Java.
 * Esta classe utiliza estritamente a API pública sem vazar pacotes internos de infraestrutura.
 */
public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        LOGGER.info("=== Inicializando o FastAPI-Java (Loom Sandbox) ===");

        // 1. Instanciamos o roteador dinâmico através da fachada pública
        Router router = FastApi.newRouter();

        HttpServer server = FastApi.newServer(router);
        
        // Gancho para desligar o servidor graciosamente no encerramento da JVM
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Encerramento solicitado. Desligando o servidor...");
            server.stop();
        }));

        server.start(8080);
    }
}
