package br.hubpedro.api;

import br.hubpedro.contracts.HttpServer;
import br.hubpedro.contracts.Router;
import br.hubpedro.infra.Server;
import br.hubpedro.infra.api.router.DefaultRouter;

/**
 * Fachada principal pública do framework FastAPI-Java.
 * Permite instanciar servidores e roteadores sem expor pacotes internos de infraestrutura.
 */
public final class FastApi {

    private FastApi() {
        // Construtor privado para evitar instanciação
    }

    /**
     * Instancia o roteador padrão da biblioteca.
     *
     * @return uma nova instância de Router
     */
    public static Router newRouter() {
        return new DefaultRouter();
    }

    /**
     * Instancia o servidor HTTP padrão configurado com o roteador informado.
     *
     * @param router o roteador contendo as rotas registradas
     * @return uma nova instância de HttpServer
     */
    public static HttpServer newServer(Router router) {
        return new Server(router);
    }
}
