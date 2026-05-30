package br.hubpedro.api;

import br.hubpedro.contracts.HttpRequest;
import br.hubpedro.contracts.HttpResponse;
import br.hubpedro.contracts.annotations.Get;
import br.hubpedro.contracts.annotations.Path;
import br.hubpedro.contracts.annotations.Post;
import br.hubpedro.contracts.annotations.Query;
import br.hubpedro.infra.api.dto.Responses;
import br.hubpedro.infra.api.router.RouterManager;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RouterManagerTest {

    static class SampleController {
        @Get("/users/{id}")
        public HttpResponse getUser(@Path("id") int id, @Query("q") String query) {
            return Responses.ok("User ID: " + id + ", Query: " + query);
        }

        @Post("/users")
        public HttpResponse createUser(HttpRequest request) {
            return Responses.builder()
                    .status(201)
                    .body("Created: " + request.getBody())
                    .build();
        }
    }

    @Test
    public void testControllerRouteRegistrationAndResolution() {
        RouterManager routerManager = new RouterManager();
        routerManager.registerController(new SampleController());

        // Test GET with path and query parameters
        HttpRequest getRequest = new br.hubpedro.infra.api.dto.HttpRequest(
                "GET",
                "/users/123",
                Collections.emptyMap(),
                Map.of("q", "hello"),
                Collections.emptyMap(),
                ""
        );

        HttpResponse getResponse = routerManager.resolve(getRequest);
        assertNotNull(getResponse);
        assertEquals(200, getResponse.getStatus());
        assertEquals("User ID: 123, Query: hello", getResponse.getBody());

        // Test POST with request body
        HttpRequest postRequest = new br.hubpedro.infra.api.dto.HttpRequest(
                "POST",
                "/users",
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                "John Doe"
        );

        HttpResponse postResponse = routerManager.resolve(postRequest);
        assertNotNull(postResponse);
        assertEquals(201, postResponse.getStatus());
        assertEquals("Created: John Doe", postResponse.getBody());
    }
}
