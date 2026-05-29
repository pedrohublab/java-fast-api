package br.hubpedro.infra;

import br.hubpedro.api.FastApi;
import br.hubpedro.api.Responses;
import br.hubpedro.contracts.HttpServer;
import br.hubpedro.contracts.Router;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class ServerIntegrationTest {

    private HttpServer server;
    private int port;

    @BeforeEach
    public void setup() throws IOException {
        // Encontra uma porta livre dinâmica na máquina local
        try (ServerSocket s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }

        Router router = FastApi.newRouter();

        router.get("/hello", request -> Responses.ok("Hello World!"));
        
        router.get("/users/{name}", request -> {
            String name = request.getPathParams().get("name");
            String age = request.getQueryParams().getOrDefault("age", "unknown");
            return Responses.ok("User: " + name + ", Age: " + age);
        });

        router.post("/echo", request -> {
            String body = request.getBody();
            return Responses.builder()
                    .status(200)
                    .header("Content-Type", "text/plain; charset=UTF-8")
                    .body(body)
                    .build();
        });

        server = FastApi.newServer(router);
        server.start(port);
    }

    @AfterEach
    public void teardown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void testGetHelloRoute() throws IOException {
        String request = "GET /hello HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "\r\n";

        String response = sendHttpRequest(request);

        assertTrue(response.contains("HTTP/1.1 200"));
        assertTrue(response.contains("Content-Length: 12"));
        assertTrue(response.contains("Hello World!"));
    }

    @Test
    public void testGetDynamicRouteWithQueryParams() throws IOException {
        String request = "GET /users/pedro?age=25 HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "\r\n";

        String response = sendHttpRequest(request);

        assertTrue(response.contains("HTTP/1.1 200"));
        assertTrue(response.contains("User: pedro, Age: 25"));
    }

    @Test
    public void testPostUtf8Body() throws IOException {
        String body = "Mensagem com acentuação e emoji: 🚀🌟";
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

        // Prepara requisição manual escrevendo os cabeçalhos em bytes
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        pw.print("POST /echo HTTP/1.1\r\n");
        pw.print("Host: localhost\r\n");
        pw.print("Content-Length: " + bodyBytes.length + "\r\n");
        pw.print("\r\n");
        pw.flush();
        out.write(bodyBytes);
        out.flush();

        byte[] requestBytes = out.toByteArray();

        // Envia os bytes brutos pelo socket
        byte[] responseBytes;
        try (Socket socket = new Socket("localhost", port);
             OutputStream socketOut = socket.getOutputStream();
             InputStream socketIn = socket.getInputStream()) {
            socketOut.write(requestBytes);
            socketOut.flush();

            ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int read;
            while ((read = socketIn.read(buf)) != -1) {
                responseBuffer.write(buf, 0, read);
            }
            responseBytes = responseBuffer.toByteArray();
        }

        String responseStr = new String(responseBytes, StandardCharsets.UTF_8);
        assertTrue(responseStr.contains("HTTP/1.1 200"));
        assertTrue(responseStr.contains(body));
    }

    @Test
    public void test404NotFound() throws IOException {
        String request = "GET /non-existent HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "\r\n";

        String response = sendHttpRequest(request);

        assertTrue(response.contains("HTTP/1.1 404"));
    }

    @Test
    public void test405MethodNotAllowed() throws IOException {
        String request = "POST /hello HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "\r\n";

        String response = sendHttpRequest(request);

        assertTrue(response.contains("HTTP/1.1 405"));
        assertTrue(response.contains("Allow: GET"));
    }

    private String sendHttpRequest(String request) throws IOException {
        try (Socket socket = new Socket("localhost", port);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            out.print(request);
            out.flush();

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }
}
