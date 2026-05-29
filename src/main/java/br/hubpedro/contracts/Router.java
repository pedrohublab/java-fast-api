package br.hubpedro.contracts;

/**
 * Define o contrato para o Roteador da aplicação.
 * O roteador é o "cérebro" que gerencia o mapeamento de métodos HTTP e rotas (URLs)
 * para métodos Java específicos (representados por RequestHandlers) e resolve as requisições recebidas.
 */
public interface Router {

    /**
     * Mapeia um método HTTP e uma URL padrão para um método Java específico (RequestHandler).
     *
     * @param method  o método HTTP (ex: "GET", "POST", "PUT", "DELETE")
     * @param url     o caminho/padrão da URL (ex: "/users", "/users/{id}")
     * @param handler o método Java/Handler específico a ser executado quando a rota for encontrada
     */
    void addRoute(String method, String url, RequestHandler handler);

    /**
     * Encontra a rota correspondente para a requisição informada, executa o método Java associado
     * e retorna a resposta. Caso não encontre, deve tratar adequadamente (ex: retornar 404).
     *
     * @param request a requisição HTTP recebida
     * @return a resposta HTTP resultante da execução do handler correspondente
     */
    HttpResponse resolve(HttpRequest request);

    /**
     * Atalho para registrar uma rota do tipo GET.
     */
    default void get(String url, RequestHandler handler) {
        addRoute("GET", url, handler);
    }

    /**
     * Atalho para registrar uma rota do tipo POST.
     */
    default void post(String url, RequestHandler handler) {
        addRoute("POST", url, handler);
    }

    /**
     * Atalho para registrar uma rota do tipo PUT.
     */
    default void put(String url, RequestHandler handler) {
        addRoute("PUT", url, handler);
    }

    /**
     * Atalho para registrar uma rota do tipo DELETE.
     */
    default void delete(String url, RequestHandler handler) {
        addRoute("DELETE", url, handler);
    }
}
