package br.hubpedro.infra.api.router;

import java.util.Map;
import java.util.function.Function;

public class ParameterConverter {

    private static final Map<Class<?>, Function<String, Object>> CONVERTERS = Map.of(
            String.class, value -> value,
            int.class, value -> Integer.parseInt(value.trim()),
            Integer.class, value -> Integer.parseInt(value.trim()),
            long.class, value -> Long.parseLong(value.trim()),
            Long.class, value -> Long.parseLong(value.trim()));

    public Object convertType(String value, Class<?> targetType) {
        if (value == null || value.isBlank()) {
            return targetType == String.class ? "" : null;
        }

        Function<String, Object> converter = CONVERTERS.get(targetType);

        if (converter == null) {
            throw new UnsupportedOperationException("Tipo não suportado pelo framework: " + targetType.getName());
        }

        try {
            return converter.apply(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "O valor '" + value + "' não é válido para o tipo " + targetType.getSimpleName());
        }
    }
}