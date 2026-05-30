package br.hubpedro.infra.api.router;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import br.hubpedro.contracts.HttpRequest;
import br.hubpedro.contracts.HttpResponse;
import br.hubpedro.contracts.RequestHandler;
import br.hubpedro.contracts.annotations.Path;
import br.hubpedro.contracts.annotations.Query;

public class ReflectionMethodHandler implements RequestHandler {
    private final Object controllerInstance;
    private final Method targetMethod;
    private final ParameterConverter parameterConverter;

    public ReflectionMethodHandler(Object controllerInstance, Method targetMethod) {
        this.controllerInstance = controllerInstance;
        this.targetMethod = targetMethod;
        this.targetMethod.setAccessible(true);
        this.parameterConverter = new ParameterConverter();
    }

    @Override
    public HttpResponse handle(HttpRequest request) throws Exception {
        try {
            Parameter[] parameters = targetMethod.getParameters();
            Object[] argsToPass = new Object[parameters.length];

            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                Class<?> parameterType = parameter.getType();

                if (parameterType.isAssignableFrom(request.getClass())) {
                    argsToPass[i] = request;
                    continue;
                }

                if (parameter.isAnnotationPresent(Path.class)) {
                    Path pathVar = parameter.getAnnotation(Path.class);
                    String stringValue = request.getPathParams().get(pathVar.value());
                    argsToPass[i] = parameterConverter.convertType(stringValue, parameterType);
                }
                else if (parameter.isAnnotationPresent(Query.class)) {
                    Query queryVar = parameter.getAnnotation(Query.class);
                    String stringValue = request.getQueryParams().get(queryVar.value());
                    argsToPass[i] = parameterConverter.convertType(stringValue, parameterType);
                } else {
                    argsToPass[i] = null;
                }
            }

            Object result = targetMethod.invoke(controllerInstance, argsToPass);

            if (result instanceof HttpResponse response) {
                return response;
            }

            if (result != null) {
                return br.hubpedro.infra.api.dto.Responses.ok(result.toString());
            }

            return br.hubpedro.infra.api.dto.Responses.status(200);

        } catch (IllegalArgumentException e) {
            return HttpResponse.json(400, "{\"error\": \"" + e.getMessage() + "\"}");
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception exceptionCause) {
                throw exceptionCause;
            }
            throw new RuntimeException(cause);
        } catch (Exception e) {
            return HttpResponse.json(500, "{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
