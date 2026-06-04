package io.github.martinwitt.spoonclaude.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AnnotationUsageToolTest {

    @TempDir
    Path tempDir;

    @Test
    void findsClassLevelAnnotation() throws IOException {
        writeSource("Transactional.java", "public @interface Transactional {}");
        writeSource("OrderService.java", """
                @Transactional
                public class OrderService {}
                """);

        var result = new AnnotationUsageTool(tempDir.toString(), "Transactional").execute();

        assertThat(result.usages()).hasSize(1);
        assertThat(result.usages().get(0).elementKind()).isEqualTo("class");
        assertThat(result.usages().get(0).elementFqn()).isEqualTo("OrderService");
    }

    @Test
    void findsMethodLevelAnnotation() throws IOException {
        writeSource("Cacheable.java", "public @interface Cacheable {}");
        writeSource("ProductService.java", """
                public class ProductService {
                  @Cacheable public String find(Long id) { return ""; }
                  public void save(String s) {}
                }
                """);

        var result = new AnnotationUsageTool(tempDir.toString(), "Cacheable").execute();

        assertThat(result.usages()).hasSize(1);
        assertThat(result.usages().get(0).elementKind()).isEqualTo("method");
        assertThat(result.usages().get(0).elementFqn()).contains("find");
        assertThat(result.usages().get(0).declaringClass()).isEqualTo("ProductService");
    }

    @Test
    void findsFieldLevelAnnotation() throws IOException {
        writeSource("Inject.java", "public @interface Inject {}");
        writeSource("MyService.java", """
                public class MyService {
                  @Inject private Object repo;
                  private Object other;
                }
                """);

        var result = new AnnotationUsageTool(tempDir.toString(), "Inject").execute();

        assertThat(result.usages()).hasSize(1);
        assertThat(result.usages().get(0).elementKind()).isEqualTo("field");
        assertThat(result.usages().get(0).elementFqn()).contains("repo");
    }

    @Test
    void findsAllKindsInOneClass() throws IOException {
        writeSource("Ann.java", "public @interface Ann {}");
        writeSource("Multi.java", """
                @Ann
                public class Multi {
                  @Ann private String name;
                  @Ann public void process() {}
                }
                """);

        var result = new AnnotationUsageTool(tempDir.toString(), "Ann").execute();

        assertThat(result.usages()).hasSize(3);
        assertThat(result.usages()).extracting("elementKind").containsExactlyInAnyOrder("class", "field", "method");
    }

    @Test
    void simpleNameLookupEquatesFqnLookup() throws IOException {
        Files.createDirectories(tempDir.resolve("com/example"));
        writeSource("com/example/Transactional.java", """
                package com.example;
                public @interface Transactional {}
                """);
        writeSource("Service.java", """
                @com.example.Transactional
                public class Service {}
                """);

        var bySimple = new AnnotationUsageTool(tempDir.toString(), "Transactional").execute();
        var byFqn = new AnnotationUsageTool(tempDir.toString(), "com.example.Transactional").execute();

        assertThat(bySimple.usages()).hasSize(1);
        assertThat(byFqn.usages()).hasSize(1);
        assertThat(bySimple.usages().get(0).elementFqn())
                .isEqualTo(byFqn.usages().get(0).elementFqn());
    }

    private void writeSource(String filename, String content) throws IOException {
        Path target = tempDir.resolve(filename);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content);
    }
}
