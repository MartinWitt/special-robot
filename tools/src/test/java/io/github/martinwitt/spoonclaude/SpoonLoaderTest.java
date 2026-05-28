package io.github.martinwitt.spoonclaude;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpoonLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsModelFromSourceDirectory() throws IOException {
        writeSource(tempDir, "Foo.java", """
                public class Foo { public void bar() {} }
                """);

        var model = SpoonLoader.load(tempDir.toString());

        assertThat(model.getAllTypes()).hasSize(1);
        assertThat(model.getAllTypes().iterator().next().getSimpleName()).isEqualTo("Foo");
    }

    @Test
    void loadsModelFromSingleFile() throws IOException {
        var src = writeSource(tempDir, "Bar.java", """
                public class Bar { private int x; }
                """);

        var model = SpoonLoader.load(src.toString());

        assertThat(model.getAllTypes()).hasSize(1);
    }

    private static Path writeSource(Path dir, String filename, String content) throws IOException {
        var path = dir.resolve(filename);
        Files.writeString(path, content);
        return path;
    }
}
