package io.github.martinwitt.spoonclaude.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImplementationFinderTest {

    @TempDir
    Path tempDir;

    @Test
    void findsDirectImplementersOfInterface() throws IOException {
        writeSource("Greeter.java", "public interface Greeter { String greet(); }");
        writeSource(
                "English.java", "public class English implements Greeter { public String greet() { return \"hi\"; } }");
        writeSource(
                "German.java",
                "public class German implements Greeter { public String greet() { return \"hallo\"; } }");

        var result = new ImplementationFinder(tempDir.toString(), "Greeter").execute();

        assertThat(result.target()).isEqualTo("Greeter");
        assertThat(result.implementations()).hasSize(2);
        assertThat(result.implementations()).anyMatch(i -> i.classFqn().equals("English"));
        assertThat(result.implementations()).anyMatch(i -> i.classFqn().equals("German"));
    }

    @Test
    void findsConcreteExtendersOfAbstractClass() throws IOException {
        writeSource("Shape.java", """
                public abstract class Shape {
                  public abstract double area();
                }
                """);
        writeSource("Circle.java", """
                public class Circle extends Shape {
                  public double area() { return 3.14; }
                }
                """);
        writeSource("Square.java", """
                public class Square extends Shape {
                  public double area() { return 4.0; }
                }
                """);

        var result = new ImplementationFinder(tempDir.toString(), "Shape").execute();

        assertThat(result.implementations()).hasSize(2);
        assertThat(result.implementations()).anyMatch(i -> i.classFqn().equals("Circle"));
        assertThat(result.implementations()).anyMatch(i -> i.classFqn().equals("Square"));
    }

    @Test
    void findsTransitiveImplementersThroughSubclass() throws IOException {
        writeSource("Drawable.java", "public interface Drawable { void draw(); }");
        writeSource("AbstractShape.java", """
                public abstract class AbstractShape implements Drawable {
                  public abstract double area();
                }
                """);
        writeSource("Triangle.java", """
                public class Triangle extends AbstractShape {
                  public void draw() {}
                  public double area() { return 0.5; }
                }
                """);

        var result = new ImplementationFinder(tempDir.toString(), "Drawable").execute();

        assertThat(result.implementations()).anyMatch(i -> i.classFqn().equals("AbstractShape"));
        assertThat(result.implementations()).anyMatch(i -> i.classFqn().equals("Triangle"));
    }

    @Test
    void listsOverriddenAbstractMethods() throws IOException {
        writeSource("Repo.java", """
                public interface Repo {
                  void save(String s);
                  String load(Long id);
                }
                """);
        writeSource("MemoryRepo.java", """
                public class MemoryRepo implements Repo {
                  public void save(String s) {}
                  public String load(Long id) { return ""; }
                }
                """);
        writeSource("PartialRepo.java", """
                public abstract class PartialRepo implements Repo {
                  public void save(String s) {}
                }
                """);

        var result = new ImplementationFinder(tempDir.toString(), "Repo").execute();

        var memory = result.implementations().stream()
                .filter(i -> i.classFqn().equals("MemoryRepo"))
                .findFirst()
                .orElseThrow();
        assertThat(memory.overriddenMethods()).containsExactlyInAnyOrder("save(String)", "load(Long)");

        var partial = result.implementations().stream()
                .filter(i -> i.classFqn().equals("PartialRepo"))
                .findFirst()
                .orElseThrow();
        assertThat(partial.overriddenMethods()).containsExactly("save(String)");
    }

    @Test
    void returnsEmptyImplementationsForUnimplementedInterface() throws IOException {
        writeSource("Lonely.java", "public interface Lonely { void cry(); }");

        var result = new ImplementationFinder(tempDir.toString(), "Lonely").execute();

        assertThat(result.implementations()).isEmpty();
    }

    @Test
    void throwsWhenTargetTypeNotFound() throws IOException {
        writeSource("Existing.java", "public class Existing {}");

        assertThatThrownBy(() -> new ImplementationFinder(tempDir.toString(), "Missing").execute())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing");
    }

    private void writeSource(String filename, String content) throws IOException {
        Files.writeString(tempDir.resolve(filename), content);
    }
}
