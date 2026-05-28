package io.github.martinwitt.spoonclaude.routes;

import java.util.List;

public record RoutesResult(List<RouteEntry> routes) {

    public record RouteEntry(
            String framework, String httpMethod, String path, String handlerClass, String handlerMethod) {}
}
