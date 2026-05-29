package br.hubpedro.contracts;

/**
 * Interface funcional que define o contrato para manipulação de uma requisição HTTP.
 * 
 * Permite que qualquer método Java correspondente (que receba HttpRequest e retorne HttpResponse)
 * possa ser mapeado diretamente como uma rota através de referências de método (method references),
 * como: controller::getUser.
 * 
 * IMPORTANTE: Os manipuladores de requisições (handlers) são chamados concorrentemente por
 * múltiplas Virtual Threads no runtime HTTP. Consequentemente, todas as implementações devem ser
 * totalmente thread-safe ou stateless (sem estado).
 */
@FunctionalInterface
public interface RequestHandler {

    /**
     * Processa a requisição HTTP recebida e gera a resposta correspondente.
     *
     * @param request a requisição HTTP contendo os dados da chamada
     * @return a resposta HTTP a ser devolvida ao cliente
     * @throws Exception caso ocorra algum erro durante o processamento da requisição
     */
    HttpResponse handle(HttpRequest request) throws Exception;
}
