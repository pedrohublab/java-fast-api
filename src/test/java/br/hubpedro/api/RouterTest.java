package br.hubpedro.api;

import br.hubpedro.contracts.HttpRequest;
import br.hubpedro.contracts.HttpResponse;
import br.hubpedro.contracts.Router;
import br.hubpedro.infra.api.FastApi;
import br.hubpedro.infra.api.dto.Responses;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RouterTest {

    @Test
    public void testRoutePrecedence() {
        Router router = FastApi.newRouter();

        // Registrar rota dinâmica genérica
        router.get("/items/{id}", request -> Responses.ok("dynamic: " + request.getPathParams().get("id")));

        // Registrar rota estática específica (registrada depois, mas deve ter precedência!)
        router.get("/items/search", request -> Responses.ok("static"));

        // Registrar outra rota dinâmica mais específica
        router.get("/items/{id}/details", request -> Responses.ok("details: " + request.getPathParams().get("id")));

        // Testar casamento de rota estática (deve bater em static mesmo registrada depois)
        HttpResponse res1 = router.resolve(createMockRequest("GET", "/items/search"));
        assertEquals(200, res1.getStatus());
        assertEquals("static", res1.getBody());

        // Testar casamento de rota dinâmica genérica
        HttpResponse res2 = router.resolve(createMockRequest("GET", "/items/123"));
        assertEquals(200, res2.getStatus());
        assertEquals("dynamic: 123", res2.getBody());

        // Testar casamento de rota dinâmica com sufixo
        HttpResponse res3 = router.resolve(createMockRequest("GET", "/items/456/details"));
        assertEquals(200, res3.getStatus());
        assertEquals("details: 456", res3.getBody());
    }

    @Test
    public void testRegexLiteralEscaping() {
        Router router = FastApi.newRouter();

        // Rota contendo caracteres especiais que seriam curingas se não fossem escapados (. e +)
        router.get("/v1.0/add+items", request -> Responses.ok("success"));

        // Deve bater com o caminho exato
        HttpResponse res1 = router.resolve(createMockRequest("GET", "/v1.0/add+items"));
        assertEquals(200, res1.getStatus());
        assertEquals("success", res1.getBody());

        // Não deve bater com /v1a0/adddditems (se o ponto e o mais fossem interpretados como regex literal)
        HttpResponse res2 = router.resolve(createMockRequest("GET", "/v1a0/adddditems"));
        assertEquals(404, res2.getStatus());
    }

    @Test
    public void testDuplicateRouteThrowsException() {
        Router router = FastApi.newRouter();

        router.get("/users", request -> Responses.ok("ok"));

        // Tentar registrar a mesma rota novamente deve lançar IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            router.get("/users", request -> Responses.ok("duplicate"));
        });
    }

    @Test
    public void testMethodNotAllowed405() {
        Router router = FastApi.newRouter();

        router.get("/users", request -> Responses.ok("get"));
        router.post("/users", request -> Responses.ok("post"));

        // PUT não está mapeado no caminho /users, deve retornar 405 com cabeçalho Allow
        HttpResponse response = router.resolve(createMockRequest("PUT", "/users"));
        assertEquals(405, response.getStatus());
        
        String allowHeader = response.getHeaders().get("Allow");
        assertNotNull(allowHeader);
        assertTrue(allowHeader.contains("GET"));
        assertTrue(allowHeader.contains("POST"));
        assertFalse(allowHeader.contains("PUT"));
    }

    @Test
    public void testUniversalPathParamInjection() {
        Router router = FastApi.newRouter();
        router.get("/users/{username}/posts/{postId}", request -> {
            // Verifica se os parâmetros capturados estão no request
            assertEquals("pedro", request.getPathParams().get("username"));
            assertEquals("42", request.getPathParams().get("postId"));
            return Responses.ok("checked");
        });

        // Simulamos o request passando uma implementação customizada qualquer da interface HttpRequest
        HttpRequest customRequest = new HttpRequest() {
            @Override public String getMethod() { return "GET"; }
            @Override public String getPath() { return "/users/pedro/posts/42"; }
            @Override public String getBody() { return ""; }
            @Override public Map<String, String> getHeaders() { return Collections.emptyMap(); }
            @Override public Map<String, String> getQueryParams() { return Collections.emptyMap(); }
            @Override public Map<String, String> getPathParams() { return Collections.emptyMap(); } // Retorna vazio inicialmente
        };

        HttpResponse res = router.resolve(customRequest);
        assertEquals(200, res.getStatus());
        assertEquals("checked", res.getBody());
    }

    private HttpRequest createMockRequest(String method, String path) {
        return new br.hubpedro.infra.api.dto.HttpRequest(
                method, path, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), ""
        );
    }
}
