package io.github.martinwitt.spoonclaude;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import spoon.Launcher;
import spoon.reflect.CtModel;

public final class SpoonLoader {

    private static final ConcurrentHashMap<String, Entry> CACHE = new ConcurrentHashMap<>();
    private static final boolean CACHE_DISABLED = Boolean.getBoolean("spoon.cache.disable");

    private SpoonLoader() {}

    public static CtModel load(String sourcePath) {
        if (CACHE_DISABLED) return build(sourcePath);
        String key = canonical(sourcePath);
        long mtime = maxMtime(Path.of(sourcePath));
        Entry existing = CACHE.get(key);
        if (existing != null && existing.mtime == mtime) {
            return existing.model;
        }
        CtModel model = build(sourcePath);
        CACHE.put(key, new Entry(mtime, model));
        return model;
    }

    /** Test hook to drop cached models — production code does not need this. */
    public static void clearCache() {
        CACHE.clear();
    }

    private static CtModel build(String sourcePath) {
        Launcher launcher = new Launcher();
        launcher.addInputResource(sourcePath);
        launcher.getEnvironment().setNoClasspath(true);
        launcher.getEnvironment().setComplianceLevel(21);
        launcher.getEnvironment().setCommentEnabled(true);
        launcher.buildModel();
        return launcher.getModel();
    }

    private static String canonical(String path) {
        try {
            return Path.of(path).toRealPath().toString();
        } catch (IOException e) {
            return Path.of(path).toAbsolutePath().toString();
        }
    }

    private static long maxMtime(Path root) {
        if (!Files.exists(root)) return 0L;
        try (var stream = Files.walk(root)) {
            return stream.filter(p -> p.toString().endsWith(".java"))
                    .mapToLong(SpoonLoader::mtime)
                    .max()
                    .orElse(0L);
        } catch (IOException e) {
            return 0L;
        }
    }

    private static long mtime(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private record Entry(long mtime, CtModel model) {}
}
