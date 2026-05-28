package io.github.martinwitt.spoonclaude.routes;

final class PathJoiner {

    private PathJoiner() {}

    static String join(String classPath, String methodPath) {
        String combined = stripTrailingSlash(ensureLeadingSlash(classPath)) + ensureLeadingSlash(methodPath);
        combined = combined.replaceAll("/{2,}", "/");
        combined = stripTrailingSlash(combined);
        return combined.isEmpty() ? "/" : combined;
    }

    private static String ensureLeadingSlash(String p) {
        if (p == null || p.isEmpty()) return "";
        return p.startsWith("/") ? p : "/" + p;
    }

    private static String stripTrailingSlash(String p) {
        if (p.length() > 1 && p.endsWith("/")) return p.substring(0, p.length() - 1);
        return p;
    }
}
