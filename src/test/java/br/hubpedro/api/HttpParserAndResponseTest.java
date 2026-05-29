package br.hubpedro.api;

import br.hubpedro.contracts.HttpRequest;
import br.hubpedro.contracts.HttpResponse;
import br.hubpedro.infra.api.parser.HttpParser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class HttpParserAndResponseTest {

    @Test
    public void testParseUtf8BodyWithExactBytes() throws IOException {
        // Corpo contendo caracteres multi-bytes (acentos e emoji)
        String body = "Olá Mundo! 🚀 FastAPI com Java é incrível.";
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        int contentLength = bodyBytes.length; // Comprimento em bytes, não caracteres!

        String rawRequest = "POST /items HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "Content-Length: " + contentLength + "\r\n" +
                "Content-Type: text/plain; charset=UTF-8\r\n" +
                "\r\n";

        byte[] requestBytes = concatenate(rawRequest.getBytes(StandardCharsets.UTF_8), bodyBytes);
        ByteArrayInputStream input = new ByteArrayInputStream(requestBytes);

        HttpRequest request = HttpParser.parse(input);

        assertEquals("POST", request.getMethod());
        assertEquals("/items", request.getPath());
        assertEquals(String.valueOf(contentLength), request.getHeaders().get("content-length"));
        assertEquals(body, request.getBody());
    }

    @Test
    public void testParseHeaderSizeLimitExceeded() {
        // Gera cabeçalhos gigantescos (> 8KB) para estourar o limite DoS
        StringBuilder massiveHeaders = new StringBuilder("GET / HTTP/1.1\r\n");
        for (int i = 0; i < 500; i++) {
            massiveHeaders.append("X-Header-").append(i).append(": some-very-long-dummy-value-that-takes-space\r\n");
        }
        massiveHeaders.append("\r\n");

        ByteArrayInputStream input = new ByteArrayInputStream(massiveHeaders.toString().getBytes(StandardCharsets.UTF_8));

        assertThrows(IOException.class, () -> {
            HttpParser.parse(input);
        });
    }

    @Test
    public void testParsePayloadSizeLimitExceeded() {
        // Content-Length anunciando 20MB (limite é 10MB)
        String rawRequest = "POST /upload HTTP/1.1\r\n" +
                "Content-Length: 20971520\r\n" + // 20 MB
                "\r\n";

        ByteArrayInputStream input = new ByteArrayInputStream(rawRequest.getBytes(StandardCharsets.UTF_8));

        assertThrows(IOException.class, () -> {
            HttpParser.parse(input);
        });
    }

    @Test
    public void testResponseImmutability() {
        HttpResponse response = Responses.builder()
                .status(201)
                .header("Custom-Header", "value1")
                .body("Created")
                .build();

        assertEquals(201, response.getStatus());
        assertEquals("Created", response.getBody());
        assertEquals("value1", response.getHeaders().get("Custom-Header"));

        // O mapa de cabeçalhos retornado deve ser estritamente imutável
        assertThrows(UnsupportedOperationException.class, () -> {
            response.getHeaders().put("Another-Header", "fails");
        });
    }

    private byte[] concatenate(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}
