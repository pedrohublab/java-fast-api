package br.hubpedro.infra.api.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class HttpHeaderReader {
    private HttpHeaderReader() {
        // Classe utilitária
    }

    /**
     * Lê os cabeçalhos HTTP do InputStream até encontrar o divisor \r\n\r\n ou \n\n,
     * respeitando o limite máximo especificado em bytes para prevenir ataques DoS.
     *
     * @param inputStream o fluxo de entrada do socket
     * @param maxHeaderSize o tamanho máximo permitido para os cabeçalhos em bytes
     * @return os cabeçalhos lidos como String decodificada em UTF-8
     * @throws IOException se o limite for excedido ou a requisição for vazia/inválida
     */
    public static String readHeaders(InputStream inputStream, int maxHeaderSize) throws IOException {
        ByteArrayOutputStream headerStream = new ByteArrayOutputStream();
        int b;
        int state = 0; // Estado: 0 = normal, 1 = \r, 2 = \r\n, 3 = \r\n\r

        while ((b = inputStream.read()) != -1) {
            headerStream.write(b);
            if (state == 0) {
                if (b == '\r')
                    state = 1;
                else if (b == '\n')
                    state = 2;
            } else if (state == 1) {
                if (b == '\n')
                    state = 2;
                else if (b == '\r')
                    state = 1;
                else
                    state = 0;
            } else if (state == 2) {
                if (b == '\r')
                    state = 3;
                else if (b == '\n')
                    break; // Encontrou \n\n
                else
                    state = 0;
            } else if (state == 3) {
                if (b == '\n')
                    break; // Encontrou \r\n\r\n
                else if (b == '\r')
                    state = 1;
                else
                    state = 0;
            }

            if (headerStream.size() > maxHeaderSize) {
                throw new IOException("Cabeçalhos excederam o limite máximo de " + (maxHeaderSize / 1024) + "KB.");
            }
        }

        if (headerStream.size() == 0) {
            throw new IOException("Requisição HTTP vazia ou fluxo encerrado abruptamente.");
        }

        return headerStream.toString(StandardCharsets.UTF_8);
    }
}

