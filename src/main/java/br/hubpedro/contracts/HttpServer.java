package br.hubpedro.contracts;

public interface HttpServer {

    void start(int port);

    void stop();

    /**
     * Aguarda até que o servidor seja encerrado (bloqueia a thread atual).
     */
    void awaitTermination();
}