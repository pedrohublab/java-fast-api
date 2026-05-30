package br.hubpedro.infra.api.parser;

import br.hubpedro.contracts.HttpRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utilitário responsável por fazer o parsing de um InputStream de socket HTTP
 * bruto
 * e convertê-lo em uma instância de br.hubpedro.infra.api.dto.HttpRequest.
 * Faz a leitura precisa por bytes (e não por caracteres) para evitar
 * truncamento ou
 * travamento ao ler corpos codificados em UTF-8 com acentos/emojis.
 */
public class HttpParser {

    private static final int MAX_HEADER_SIZE = 8192; // Limite de 8KB para cabeçalhos (Prevenção DoS)
    private static final int MAX_BODY_SIZE = 10 * 1024 * 1024; // Limite de 10MB para corpo (Prevenção DoS)

    private static final Pattern NEWLINE_PATTERN = Pattern.compile("\\r?\\n");
    private static final Pattern SPACE_PATTERN = Pattern.compile("\\s+");

    /**
     * Faz o parsing da requisição HTTP vinda do cliente a partir do fluxo de bytes
     * brutos.
     *
     * @param inputStream o fluxo de entrada do socket do cliente
     * @return um objeto HttpRequest totalmente preenchido
     * @throws IOException caso ocorra algum erro na leitura ou a requisição seja
     *                     inválida/exceda limites
     */
    public static HttpRequest parse(InputStream inputStream) throws IOException {
        // 1. Ler os cabeçalhos de forma segura usando o leitor dedicado de bytes
        String headersText = HttpHeaderReader.readHeaders(inputStream, MAX_HEADER_SIZE);
        String[] lines = NEWLINE_PATTERN.split(headersText);

        if (lines.length == 0 || lines[0].isBlank()) {
            throw new IOException("Request Line inválida.");
        }

        // 2. Parsear a Request Line (Ex: GET /items/5?q=teste HTTP/1.1)
        String requestLine = lines[0];
        String[] parts = SPACE_PATTERN.split(requestLine);
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

        // 3. Ler o corpo (body) consumindo exatamente o tamanho em bytes indicado por
        // Content-Length
        String body = "";
        String contentLengthHeader = headers.get("content-length");
        if (contentLengthHeader != null) {
            try {
                int contentLength = Integer.parseInt(contentLengthHeader.trim());
                if (contentLength > MAX_BODY_SIZE) {
                    throw new IOException("Corpo da requisição excede o limite máximo permitido de "
                            + (MAX_BODY_SIZE / 1024 / 1024) + "MB.");
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
                body);
    }
}
