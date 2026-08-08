package br.hubpedro.infra.api;

import br.hubpedro.contracts.HttpServer;
import br.hubpedro.contracts.Router;
import br.hubpedro.contracts.HttpRequest;
import br.hubpedro.contracts.HttpResponse;
import br.hubpedro.infra.api.dto.Responses;
import br.hubpedro.infra.api.parser.HttpParser;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servidor HTTP robusto e thread-safe alimentado por Virtual Threads (Project Loom).
 * Gerencia o ciclo de vida do ServerSocket de forma sincronizada com suporte a Graceful Shutdown.
 */
public class Server implements HttpServer {

    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());

    private final Router router;
    private ServerSocket serverSocket;
    private ExecutorService executor;
    
    private final ReentrantLock lifecycleLock = new ReentrantLock();
    private final CountDownLatch terminationLatch = new CountDownLatch(1);
    
    private volatile boolean running = false;

    public Server(Router router) {
        if (router == null) {
            throw new IllegalArgumentException("O roteador não pode ser nulo.");
        }
        this.router = router;
    }

    @Override
    public void start(int port) {
        lifecycleLock.lock();
        try {
            if (running) {
                throw new IllegalStateException("O servidor já está rodando.");
            }

            this.executor = Executors.newVirtualThreadPerTaskExecutor();
            this.serverSocket = new ServerSocket(port);
            this.running = true;

            LOGGER.info("Servidor FastAPI-Java iniciado com Virtual Threads na porta " + port + "...");

            // Loop de accept rodando de forma assíncrona sob uma Virtual Thread dedicada
            Thread.ofVirtual().name("http-accept-loop").start(() -> {
                while (running && !serverSocket.isClosed()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        
                        // Configura timeout de leitura de 30 segundos no socket do cliente
                        clientSocket.setSoTimeout(30000);

                        try {
                            if (running && !executor.isShutdown()) {
                                // Submete a conexão para processamento concorrente no executor de Virtual Threads
                                executor.submit(() -> handleClient(clientSocket));
                            } else {
                                clientSocket.close();
                            }
                        } catch (RejectedExecutionException e) {
                            // Executor já está rejeitando tarefas por estar em desligamento
                            try {
                                clientSocket.close();
                            } catch (IOException ignored) {}
                        }
                    } catch (IOException e) {
                        if (running) {
                            LOGGER.log(Level.SEVERE, "Erro ao aceitar nova conexão", e);
                        }
                    }
                }
            });

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Falha fatal ao inicializar o servidor HTTP", e);
            stop();
        } finally {
            lifecycleLock.unlock();
        }
    }

    @Override
    public void stop() {
        lifecycleLock.lock();
        try {
            if (!running) {
                return;
            }
            this.running = false;
            
            LOGGER.info("Iniciando desligamento gracioso do servidor...");

            // 1. Fecha o ServerSocket para parar de aceitar novas conexões
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Erro ao fechar o ServerSocket", e);
                }
            }

            // 2. Desliga o executor de Virtual Threads aguardando as requisições ativas
            if (executor != null) {
                executor.shutdown();
                try {
                    // Aguarda até 10 segundos para a finalização das tarefas pendentes
                    if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                        LOGGER.warning("Algumas requisições não terminaram a tempo. Forçando desligamento...");
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            LOGGER.info("Servidor HTTP finalizado com sucesso.");
        } finally {
            terminationLatch.countDown();
            lifecycleLock.unlock();
        }
    }

    @Override
    public void awaitTermination() {
        try {
            terminationLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (clientSocket;
             OutputStream out = clientSocket.getOutputStream()) {

            // 1. Faz o parsing do fluxo de bytes da requisição
            HttpRequest request;
            try {
                // Wrap the input stream in a BufferedInputStream to greatly improve reading performance
                // since the HttpParser reads byte by byte.
                request = HttpParser.parse(new BufferedInputStream(clientSocket.getInputStream()));
            } catch (Exception e) {
                // Loga internamente o erro de parser, mas não vaza a stack trace para o cliente
                LOGGER.log(Level.SEVERE, "Erro de parsing HTTP", e);
                writeResponse(out, Responses.builder().status(400).body("Bad Request").build());
                return;
            }

            // 2. Resolve a rota correspondente e obtém a resposta do handler
            HttpResponse response;
            try {
                response = router.resolve(request);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erro na execução do RequestHandler", e);
                response = Responses.builder().status(500).body("Internal Server Error").build();
            }

            // 3. Devolve a resposta estruturada para o cliente
            writeResponse(out, response);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erro de comunicação de rede com o cliente", e);
        }
    }

    private void writeResponse(OutputStream out, HttpResponse response) throws IOException {
        PrintWriter writer = new PrintWriter(out, false, StandardCharsets.UTF_8);

        // Escreve a HTTP Status Line
        writer.print("HTTP/1.1 " + response.getStatus() + "\r\n");

        // Escreve todos os cabeçalhos
        if (response.getHeaders() != null) {
            for (var entry : response.getHeaders().entrySet()) {
                writer.print(entry.getKey() + ": " + entry.getValue() + "\r\n");
            }
        }

        // Calcula o tamanho preciso de bytes do body codificado em UTF-8
        byte[] bodyBytes = response.getBody().getBytes(StandardCharsets.UTF_8);
        writer.print("Content-Length: " + bodyBytes.length + "\r\n");

        // Linha em branco obrigatória divisória
        writer.print("\r\n");
        writer.flush();

        // Escreve o payload bruto em bytes
        out.write(bodyBytes);
        out.flush();
    }
}
