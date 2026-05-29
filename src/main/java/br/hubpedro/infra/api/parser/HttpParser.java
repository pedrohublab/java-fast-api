package br.hubpedro.infra.api.parser;

import br.hubpedro.contracts.HttpRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilitário responsável por fazer o parsing de um InputStream de socket HTTP bruto
 * e convertê-lo em uma instância de br.hubpedro.infra.api.dto.HttpRequest.
 * Faz a leitura precisa por bytes (e não por caracteres) para evitar truncamento ou
 * travamento ao ler corpos codificados em UTF-8 com acentos/emojis.
 */
public class HttpParser {

    private static final int MAX_HEADER_SIZE = 8192; // Limite de 8KB para cabeçalhos (Prevenção DoS)
    private static final int MAX_BODY_SIZE = 10 * 1024 * 1024; // Limite de 10MB para corpo (Prevenção DoS)

    /**
     * Faz o parsing da requisição HTTP vinda do cliente a partir do fluxo de bytes brutos.
     *
     * @param inputStream o fluxo de entrada do socket do cliente
     * @return um objeto HttpRequest totalmente preenchido
     * @throws IOException caso ocorra algum erro na leitura ou a requisição seja inválida/exceda limites
     */
    public static HttpRequest parse(InputStream inputStream) throws IOException {
        // 1. Ler os cabeçalhos até o divisor \r\n\r\n ou \n\n baseando-se estritamente em bytes
        ByteArrayOutputStream headerStream = new ByteArrayOutputStream();
        int b;
        int state = 0; // Estado: 0 = normal, 1 = \r, 2 = \r\n, 3 = \r\n\r

        while ((b = inputStream.read()) != -1) {
            headerStream.write(b);
            if (state == 0) {
                if (b == '\r') state = 1;
                else if (b == '\n') state = 2;
            } else if (state == 1) {
                if (b == '\n') state = 2;
                else if (b == '\r') state = 1;
                else state = 0;
            } else if (state == 2) {
                if (b == '\r') state = 3;
                else if (b == '\n') break; // Encontrou \n\n
                else state = 0;
            } else if (state == 3) {
                if (b == '\n') break; // Encontrou \r\n\r\n
                else if (b == '\r') state = 1;
                else state = 0;
            }

            if (headerStream.size() > MAX_HEADER_SIZE) {
                throw new IOException("Cabeçalhos excederam o limite máximo de " + (MAX_HEADER_SIZE / 1024) + "KB.");
            }
        }

        if (headerStream.size() == 0) {
            throw new IOException("Requisição HTTP vazia ou fluxo encerrado abruptamente.");
        }

        String headersText = headerStream.toString(StandardCharsets.UTF_8);
        String[] lines = headersText.split("\\r?\\n");

        if (lines.length == 0 || lines[0].isBlank()) {
            throw new IOException("Request Line inválida.");
        }

        // 2. Parsear a Request Line (Ex: GET /items/5?q=teste HTTP/1.1)
        String requestLine = lines[0];
        String[] parts = requestLine.split("\\s+");
        if (parts.length < 3) {
            throw new IOException("Request Line malformatada: " + requestLine);
        }

        String method = parts[0];
        URI uri;
        try {
            uri = URI.create(parts[1]);
        } catch (Exception e) {
            throw new IOException("URI inválida: " + parts[1]);
        }
        String path = uri.getPath();

        // Mapeia Query Parameters de forma segura decodificando de UTF-8
        Map<String, String> queryParams = new HashMap<>();
        String query = uri.getRawQuery();
        if (query != null && !query.isBlank()) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                String value = keyValue.length > 1 ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8) : "";
                queryParams.put(key, value);
            }
        }

        // Mapeia os cabeçalhos em formato normalizado (nomes em caixa baixa)
        Map<String, String> headers = new HashMap<>();
        for (int i = 1; i < lines.length; i++) {
            String headerLine = lines[i];
            if (headerLine.isBlank()) {
                continue;
            }
            int colonIndex = headerLine.indexOf(':');
            if (colonIndex != -1) {
                String name = headerLine.substring(0, colonIndex).trim().toLowerCase();
                String value = headerLine.substring(colonIndex + 1).trim();
                headers.put(name, value);
            }
        }

        // 3. Ler o corpo (body) consumindo exatamente o tamanho em bytes indicado por Content-Length
        String body = "";
        String contentLengthHeader = headers.get("content-length");
        if (contentLengthHeader != null) {
            try {
                int contentLength = Integer.parseInt(contentLengthHeader.trim());
                if (contentLength > MAX_BODY_SIZE) {
                    throw new IOException("Corpo da requisição excede o limite máximo permitido de " + (MAX_BODY_SIZE / 1024 / 1024) + "MB.");
                }
                if (contentLength > 0) {
                    byte[] bodyBytes = new byte[contentLength];
                    int totalRead = 0;
                    while (totalRead < contentLength) {
                        int read = inputStream.read(bodyBytes, totalRead, contentLength - totalRead);
                        if (read == -1) {
                            break; // Stream encerrado inesperadamente antes de ler todo o corpo anunciado
                        }
                        totalRead += read;
                    }
                    body = new String(bodyBytes, 0, totalRead, StandardCharsets.UTF_8);
                }
            } catch (NumberFormatException e) {
                // Cabeçalho de Content-Length inválido é ignorado
            }
        }

        // Os path params começam vazios e são mapeados dinamicamente pelo Roteador
        Map<String, String> pathParams = new HashMap<>();

        return new br.hubpedro.infra.api.dto.HttpRequest(
                method,
                path,
                headers,
                queryParams,
                pathParams,
                body
        );
    }
}
