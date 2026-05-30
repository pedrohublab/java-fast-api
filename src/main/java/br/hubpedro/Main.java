package br.hubpedro;

import br.hubpedro.contracts.Router;
import br.hubpedro.contracts.HttpServer;
import br.hubpedro.infra.api.FastApi;
import br.hubpedro.contracts.annotations.Get;
import br.hubpedro.contracts.annotations.Path;
import br.hubpedro.contracts.annotations.Query;
import br.hubpedro.contracts.HttpResponse;

class XPTOController {
    @Get("/hello/{nome}")
    public HttpResponse saudar(@Path("nome") String nome, @Query("sobrenome") String sobrenome) {
        String sobrenomeFinal = (sobrenome != null) ? sobrenome : "";
        return HttpResponse.json(200,
                "{\"mensagem\": \"Olá, " + nome + " " + sobrenomeFinal + "! Seu framework funciona!\"}");
    }
}

public class Main {
    public static void main(String[] args) {
        Router router = FastApi.newRouter();

        FastApi.registerController(router, new XPTOController());

        HttpServer server = FastApi.newServer(router);
        server.start(8080);
    }
}
