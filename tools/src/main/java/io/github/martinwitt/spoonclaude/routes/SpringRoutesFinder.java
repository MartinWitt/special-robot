package io.github.martinwitt.spoonclaude.routes;

import io.github.martinwitt.spoonclaude.SpoonLoader;
import io.github.martinwitt.spoonclaude.SpoonTool;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.filter.TypeFilter;

public final class SpringRoutesFinder implements SpoonTool<RoutesResult> {

    private static final Map<String, String> SPRING_SHORTCUTS = Map.of(
            "GetMapping", "GET",
            "PostMapping", "POST",
            "PutMapping", "PUT",
            "DeleteMapping", "DELETE",
            "PatchMapping", "PATCH");

    private static final Set<String> HTTP_VERBS = Set.of("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS");

    private final String sourcePath;

    public SpringRoutesFinder(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    @Override
    public RoutesResult execute() {
        var model = SpoonLoader.load(sourcePath);
        List<RoutesResult.RouteEntry> routes = new ArrayList<>();
        for (CtType<?> type : model.getAllTypes()) {
            collectFromType(type, routes);
            collectWebFluxRoutes(type, routes);
        }
        return new RoutesResult(routes);
    }

    private static void collectFromType(CtType<?> type, List<RoutesResult.RouteEntry> out) {
        String springClassPath = annotationPath(type, "RequestMapping");
        String jaxRsClassPath = annotationPath(type, "Path");
        for (CtMethod<?> method : type.getMethods()) {
            for (CtAnnotation<?> annotation : method.getAnnotations()) {
                String name = annotation.getAnnotationType().getSimpleName();
                if (SPRING_SHORTCUTS.containsKey(name)) {
                    String path = PathJoiner.join(springClassPath, AnnotationValues.firstString(annotation, "value"));
                    out.add(route("spring-mvc", SPRING_SHORTCUTS.get(name), path, type, method));
                } else if ("RequestMapping".equals(name)) {
                    String path = PathJoiner.join(springClassPath, AnnotationValues.firstString(annotation, "value"));
                    for (String verb : requestMappingMethods(annotation)) {
                        out.add(route("spring-mvc", verb, path, type, method));
                    }
                } else if (HTTP_VERBS.contains(name)) {
                    String methodPath = annotationPath(method, "Path");
                    String path = PathJoiner.join(jaxRsClassPath, methodPath);
                    out.add(route("jax-rs", name, path, type, method));
                }
            }
        }
    }

    private static void collectWebFluxRoutes(CtType<?> type, List<RoutesResult.RouteEntry> out) {
        if (!isRouterConfigCandidate(type)) return;
        for (CtMethod<?> method : type.getMethods()) {
            for (CtInvocation<?> invocation : method.getElements(new TypeFilter<>(CtInvocation.class))) {
                String name = invocation.getExecutable().getSimpleName();
                if (!HTTP_VERBS.contains(name)) continue;
                String path = firstStringArgument(invocation);
                if (path == null) continue;
                out.add(new RoutesResult.RouteEntry(
                        "spring-webflux-fn",
                        name,
                        normalizePath(path),
                        type.getQualifiedName(),
                        method.getSimpleName()));
            }
        }
    }

    private static boolean isRouterConfigCandidate(CtType<?> type) {
        for (CtAnnotation<?> annotation : type.getAnnotations()) {
            if ("Configuration".equals(annotation.getAnnotationType().getSimpleName())) return true;
        }
        for (CtMethod<?> method : type.getMethods()) {
            for (CtAnnotation<?> annotation : method.getAnnotations()) {
                if ("Bean".equals(annotation.getAnnotationType().getSimpleName())) return true;
            }
        }
        return false;
    }

    private static String firstStringArgument(CtInvocation<?> invocation) {
        var args = invocation.getArguments();
        if (args.isEmpty()) return null;
        var first = args.get(0);
        if (first instanceof CtLiteral<?> literal && literal.getValue() instanceof String s) return s;
        return null;
    }

    private static String normalizePath(String path) {
        return PathJoiner.join("", path);
    }

    private static RoutesResult.RouteEntry route(
            String framework, String verb, String path, CtType<?> type, CtMethod<?> method) {
        return new RoutesResult.RouteEntry(framework, verb, path, type.getQualifiedName(), method.getSimpleName());
    }

    private static List<String> requestMappingMethods(CtAnnotation<?> annotation) {
        List<String> methods = new ArrayList<>(AnnotationValues.enumNames(annotation, "method"));
        if (methods.isEmpty()) methods.add("ANY");
        return methods;
    }

    private static String annotationPath(CtElement element, String annotationSimpleName) {
        for (CtAnnotation<?> annotation : element.getAnnotations()) {
            if (annotationSimpleName.equals(annotation.getAnnotationType().getSimpleName())) {
                return AnnotationValues.firstString(annotation, "value");
            }
        }
        return "";
    }
}
