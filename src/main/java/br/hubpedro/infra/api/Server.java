package br.hubpedro.infra.api;

import br.hubpedro.contracts.HttpServer;
import br.hubpedro.contracts.Router;
import br.hubpedro.contracts.HttpRequest;
import br.hubpedro.contracts.HttpResponse;
import br.hubpedro.infra.api.dto.Responses;
import br.hubpedro.infra.api.parser.HttpParser;

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

/**
 * Servidor HTTP robusto e thread-safe alimentado por Virtual Threads (Project Loom).
 * Gerencia o ciclo de vida do ServerSocket de forma sincronizada com suporte a Graceful Shutdown.
 */
public class Server implements HttpServer {

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

            System.out.println("Servidor FastAPI-Java iniciado com Virtual Threads na porta " + port + "...");

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
                            System.err.println("Erro ao aceitar nova conexão: " + e.getMessage());
                        }
                    }
                }
            });

        } catch (IOException e) {
            System.err.println("Falha fatal ao inicializar o servidor HTTP: " + e.getMessage());
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
            
            System.out.println("Iniciando desligamento gracioso do servidor...");

            // 1. Fecha o ServerSocket para parar de aceitar novas conexões
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    System.err.println("Erro ao fechar o ServerSocket: " + e.getMessage());
                }
            }

            // 2. Desliga o executor de Virtual Threads aguardando as requisições ativas
            if (executor != null) {
                executor.shutdown();
                try {
                    // Aguarda até 10 segundos para a finalização das tarefas pendentes
                    if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                        System.err.println("Algumas requisições não terminaram a tempo. Forçando desligamento...");
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            System.out.println("Servidor HTTP finalizado com sucesso.");
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
                request = HttpParser.parse(clientSocket.getInputStream());
            } catch (Exception e) {
                // Loga internamente o erro de parser, mas não vaza e.getMessage() para o cliente
                System.err.println("Erro de parsing HTTP: " + e.getMessage());
                writeResponse(out, Responses.builder().status(400).body("Bad Request").build());
                return;
            }

            // 2. Resolve a rota correspondente e obtém a resposta do handler
            HttpResponse response;
            try {
                response = router.resolve(request);
            } catch (Exception e) {
                System.err.println("Erro na execução do RequestHandler: " + e.getMessage());
                response = Responses.builder().status(500).body("Internal Server Error").build();
            }

            // 3. Devolve a resposta estruturada para o cliente
            writeResponse(out, response);

        } catch (IOException e) {
            System.err.println("Erro de comunicação de rede com o cliente: " + e.getMessage());
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
