package io.github.martinwitt.spoonclaude.output;

import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

public final class JsonRenderer {

    private static final JsonMapper MAPPER =
            JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    private JsonRenderer() {}

    public static String render(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JacksonException e) {
            return renderError(e.getMessage());
        }
    }

    public static String renderError(String message) {
        try {
            return MAPPER.writeValueAsString(Map.of("error", message));
        } catch (JacksonException e) {
            return "{\"error\":\"serialization failed\"}";
        }
    }
}
