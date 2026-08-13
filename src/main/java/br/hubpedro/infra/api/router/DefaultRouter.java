package br.hubpedro.infra.api.router;

import br.hubpedro.contracts.HttpRequest;
import br.hubpedro.contracts.HttpResponse;
import br.hubpedro.contracts.RequestHandler;
import br.hubpedro.contracts.Router;
import br.hubpedro.infra.api.dto.Responses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementação thread-safe do roteador dinâmico do framework.
 * Suporta parâmetros de caminho, escape de literais regex, ordenação por
 * especificidade de segmento,
 * tratamento de erros HTTP 405 com o cabeçalho Allow e detecção de
 * conflitos/duplicatas de rotas.
 */
public class DefaultRouter implements Router {

    private static final Logger LOGGER = Logger.getLogger(DefaultRouter.class.getName());

    private final List<Route> routes = new CopyOnWriteArrayList<>();

    @Override
    public synchronized void addRoute(String method, String url, RequestHandler handler) {
        if (method == null || url == null || handler == null) {
            throw new IllegalArgumentException("Método, URL e Handler não podem ser nulos.");
        }

        String normalizedMethod = method.trim().toUpperCase();
        String normalizedUrl = url.trim();

        // Detecção de duplicatas/conflitos
        for (Route route : routes) {
            if (route.getMethod().equalsIgnoreCase(normalizedMethod)
                    && route.getOriginalPattern().equals(normalizedUrl)) {
                throw new IllegalArgumentException(
                        "Rota duplicada detectada: [" + normalizedMethod + "] " + normalizedUrl);
            }
        }

        routes.add(new Route(normalizedMethod, normalizedUrl, handler));

        // Ordena por especificidade (estática antes de dinâmica)
        routes.sort(DefaultRouter::compareRoutes);

        LOGGER.info("Rota registrada e compilada: [" + normalizedMethod + "] " + normalizedUrl);
    }

    @Override
    public HttpResponse resolve(HttpRequest request) {
        String method = request.getMethod();
        String path = request.getPath();

        boolean pathMatchedAnyRoute = false;
        List<String> allowedMethods = new ArrayList<>();

        for (Route route : routes) {
            if (route.matchesPath(path)) {
                pathMatchedAnyRoute = true;
                if (route.getMethod().equalsIgnoreCase(method)) {
                    // 1. Extraímos os parâmetros dinâmicos da URL
                    Map<String, String> pathParams = route.extractPathParams(path);

                    // 2. Injeta universalmente os pathParams através do PathParamRequestWrapper
                    HttpRequest wrappedRequest = new PathParamRequestWrapper(request, pathParams);

                    // 3. Executamos o Handler associado à rota
                    try {
                        return route.getHandler().handle(wrappedRequest);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Erro durante execução da rota [" + method + "] " + path, e);
                        return Responses.builder()
                                .status(500)
                                .body("Internal Server Error")
                                .build();
                    }
                } else {
                    allowedMethods.add(route.getMethod().toUpperCase());
                }
            }
        }

        // Se o caminho existe mas o método está errado, retorna 405 Method Not Allowed
        if (pathMatchedAnyRoute) {
            String allowHeaderValue = String.join(", ", allowedMethods.stream().distinct().toList());
            return Responses.builder()
                    .status(405)
                    .header("Allow", allowHeaderValue)
                    .body("405 Method Not Allowed. Allowed methods: " + allowHeaderValue)
                    .build();
        }

        // Caso nenhuma rota ou caminho seja correspondido, retorna 404 Not Found
        return Responses.builder()
                .status(404)
                .body("404 Not Found - A rota '" + method + " " + path + "' não foi localizada.")
                .build();
    }

    /**
     * Compara duas rotas segment-by-segment para definir a precedência.
     * Rotas estáticas (sem chaves '{}') são priorizadas em relação a rotas
     * dinâmicas.
     */
    private static int compareRoutes(Route r1, Route r2) {
        String[] s1 = r1.getOriginalPattern().split("/");
        String[] s2 = r2.getOriginalPattern().split("/");
        int len = Math.min(s1.length, s2.length);

        for (int i = 0; i < len; i++) {
            boolean isParam1 = s1[i].startsWith("{") && s1[i].endsWith("}");
            boolean isParam2 = s2[i].startsWith("{") && s2[i].endsWith("}");

            if (isParam1 != isParam2) {
                // Segmento estático (false) vem antes de parâmetro dinâmico (true)
                return isParam1 ? 1 : -1;
            }
        }

        // Se são estruturalmente idênticos até o limite, caminhos mais longos vêm
        // primeiro
        return Integer.compare(s2.length, s1.length);
    }

    /**
     * Representação interna de uma rota compilada.
     */
    private static class Route {
        private static final Pattern PARAM_PATTERN = Pattern.compile("\\{([a-zA-Z0-9_]+)\\}");

        private final String method;
        private final String originalPattern;
        private final Pattern pathPattern;
        private final List<String> paramNames;
        private final RequestHandler handler;

        public Route(String method, String urlPattern, RequestHandler handler) {
            this.method = method.toUpperCase();
            this.originalPattern = urlPattern;
            this.handler = handler;
            this.paramNames = new ArrayList<>();

            // Analisa e compila a expressão regular escapando os literais
            StringBuilder regexBuilder = new StringBuilder("^");
            Matcher matcher = PARAM_PATTERN.matcher(urlPattern);
            int lastEnd = 0;

            while (matcher.find()) {
                // Escapa a parte literal anterior ao parâmetro
                String literal = urlPattern.substring(lastEnd, matcher.start());
                regexBuilder.append(Pattern.quote(literal));

                String paramName = matcher.group(1);
                paramNames.add(paramName);
                // Grupo nomeado capturando qualquer caractere que não seja '/'
                regexBuilder.append("(?<").append(paramName).append(">[^/]+)");
                lastEnd = matcher.end();
            }

            // Escapa a parte literal final pós-último parâmetro
            String literalTail = urlPattern.substring(lastEnd);
            regexBuilder.append(Pattern.quote(literalTail));
            regexBuilder.append("$");

            this.pathPattern = Pattern.compile(regexBuilder.toString());
        }

        public String getMethod() {
            return method;
        }

        public String getOriginalPattern() {
            return originalPattern;
        }

        public RequestHandler getHandler() {
            return handler;
        }

        public boolean matchesPath(String requestPath) {
            return this.pathPattern.matcher(requestPath).matches();
        }

        public Map<String, String> extractPathParams(String requestPath) {
            Map<String, String> params = new HashMap<>();
            Matcher matcher = this.pathPattern.matcher(requestPath);
            if (matcher.matches()) {
                for (String paramName : paramNames) {
                    params.put(paramName, matcher.group(paramName));
                }
            }
            return params;
        }
    }
}
