package br.hubpedro.infra.api.router;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import br.hubpedro.contracts.RequestHandler;

public class RouteMetaData {
    private final String method;
    private final Pattern regexPattern;
    private final List<String> paramNames;
    private final RequestHandler handler;

    public RouteMetaData(String method, String originalPath, RequestHandler handler) {
        this.paramNames = new ArrayList<>();
        this.handler = handler;
        this.method = method;

        var matcher = Pattern.compile("\\{([^}]+)\\}").matcher(originalPath);
        while (matcher.find()) {
            this.paramNames.add(matcher.group(1));
        }

        String regexStr = originalPath.replaceAll("\\{[^/]+\\}", "([^/]+)");
        this.regexPattern = Pattern.compile("^" + regexStr + "$");
    }

    public Pattern getRegexPattern() {
        return regexPattern;
    }

    public List<String> getParamNames() {
        return paramNames;
    }

    public RequestHandler getHandler() {
        return handler;
    }

    public String getMethod() {
        return method;
    }

}
