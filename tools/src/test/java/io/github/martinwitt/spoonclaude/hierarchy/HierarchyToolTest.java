package io.github.martinwitt.spoonclaude.hierarchy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.martinwitt.spoonclaude.resolve.AmbiguousNameException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HierarchyToolTest {

    @TempDir
    Path tempDir;

    // AC-1, AC-7, AC-10: basic class with no hierarchy emits full structure with empty arrays
    @Test
    void classWithNoHierarchyHasEmptyArrays() throws IOException {
        writeSource("Standalone.java", "public class Standalone {}");

        var result = tool("Standalone").execute();

        assertThat(result.fqn()).isEqualTo("Standalone");
        assertThat(result.simpleName()).isEqualTo("Standalone");
        assertThat(result.kind()).isEqualTo("class");
        assertThat(result.source()).isEqualTo("project");
        assertThat(result.supertypes()).isEmpty();
        assertThat(result.subtypes()).isEmpty();
        assertThat(result.subtypesScope()).isEqualTo("project-local only");
    }

    // AC-1, AC-6: class extending project class shows supertype with source=project
    @Test
    void classExtendingProjectClassShowsProjectSupertype() throws IOException {
        writeSource("Base.java", "public class Base {}");
        writeSource("Child.java", "public class Child extends Base {}");

        var result = tool("Child").execute();

        assertThat(result.supertypes()).hasSize(1);
        var base = result.supertypes().get(0);
        assertThat(base.simpleName()).isEqualTo("Base");
        assertThat(base.source()).isEqualTo("project");
        assertThat(base.kind()).isEqualTo("class");
    }

    // AC-4: interface target has kind=interface; interface supertype also has kind=interface
    @Test
    void interfaceExtendingInterfaceHasCorrectKind() throws IOException {
        writeSource("Animal.java", "public interface Animal {}");
        writeSource("Dog.java", "public interface Dog extends Animal {}");

        var result = tool("Dog").execute();

        assertThat(result.kind()).isEqualTo("interface");
        assertThat(result.supertypes()).hasSize(1);
        assertThat(result.supertypes().get(0).kind()).isEqualTo("interface");
    }

    // AC-1: subtypes found in the project
    @Test
    void findsDirectProjectLocalSubtypes() throws IOException {
        writeSource("Base.java", "public class Base {}");
        writeSource("Sub.java", "public class Sub extends Base {}");

        var result = tool("Base").execute();

        assertThat(result.subtypes()).hasSize(1);
        assertThat(result.subtypes().get(0).simpleName()).isEqualTo("Sub");
        assertThat(result.subtypes().get(0).source()).isEqualTo("project");
    }

    // AC-5: classpath supertype appears as leaf node with source=classpath and no children
    @Test
    void classpathSupertypeIsLeafWithClasspathSource() throws IOException {
        writeSource("MyList.java", "import java.util.ArrayList; public class MyList extends ArrayList {}");

        var result = tool("MyList").execute();

        assertThat(result.supertypes())
                .anyMatch(n -> n.simpleName().equals("ArrayList") && n.source().equals("classpath"));
        var arrayListNode = result.supertypes().stream()
                .filter(n -> n.simpleName().equals("ArrayList"))
                .findFirst()
                .orElseThrow();
        assertThat(arrayListNode.supertypes()).isEmpty();
    }

    // AC-3: --include-object adds Object to the supertype chain
    @Test
    void includeObjectFlagAddsObjectAsSupertypeLeaf() throws IOException {
        writeSource("Base.java", "public class Base {}");
        writeSource("Child.java", "public class Child extends Base {}");

        var result = toolWithFlags("Child", false, false, true).execute();

        var base = result.supertypes().get(0);
        assertThat(base.simpleName()).isEqualTo("Base");
        assertThat(base.supertypes()).anyMatch(n -> n.fqn().equals("java.lang.Object"));
    }

    // AC-3: without --include-object, Object is absent from the chain
    @Test
    void objectExcludedFromChainByDefault() throws IOException {
        writeSource("Foo.java", "public class Foo {}");

        var result = tool("Foo").execute();

        assertThat(result.supertypes()).noneMatch(n -> n.fqn().contains("Object"));
    }

    // AC-2: --supertypes-only produces empty subtypes
    @Test
    void supertypesOnlyFlagOmitsSubtypes() throws IOException {
        writeSource("Base.java", "public class Base {}");
        writeSource("Sub.java", "public class Sub extends Base {}");

        var result = toolWithFlags("Base", true, false, false).execute();

        assertThat(result.subtypes()).isEmpty();
    }

    // AC-2: --subtypes-only produces empty supertypes
    @Test
    void subtypesOnlyFlagOmitsSupertypes() throws IOException {
        writeSource("Base.java", "public class Base {}");
        writeSource("Sub.java", "public class Sub extends Base {}");

        var result = toolWithFlags("Sub", false, true, false).execute();

        assertThat(result.supertypes()).isEmpty();
    }

    // AC-8: ambiguous simple name throws AmbiguousNameException
    @Test
    void ambiguousSimpleNameThrowsAmbiguousNameException() throws IOException {
        Path pkgA = tempDir.resolve("a");
        Path pkgB = tempDir.resolve("b");
        Files.createDirectories(pkgA);
        Files.createDirectories(pkgB);
        Files.writeString(pkgA.resolve("Foo.java"), "package a; public class Foo {}");
        Files.writeString(pkgB.resolve("Foo.java"), "package b; public class Foo {}");

        assertThatThrownBy(() -> tool("Foo").execute()).isInstanceOf(AmbiguousNameException.class);
    }

    // AC-9: unknown type throws IllegalArgumentException
    @Test
    void unknownTypeThrowsIllegalArgumentException() throws IOException {
        writeSource("Existing.java", "public class Existing {}");

        assertThatThrownBy(() -> tool("Missing").execute()).isInstanceOf(IllegalArgumentException.class);
    }

    // AC-6: transitive supertype chain is recursively resolved for project types
    @Test
    void transitiveProjectSupertypesAreRecursivelyResolved() throws IOException {
        writeSource("GrandParent.java", "public class GrandParent {}");
        writeSource("Parent.java", "public class Parent extends GrandParent {}");
        writeSource("Child.java", "public class Child extends Parent {}");

        var result = tool("Child").execute();

        var parent = result.supertypes().get(0);
        assertThat(parent.simpleName()).isEqualTo("Parent");
        assertThat(parent.supertypes()).hasSize(1);
        assertThat(parent.supertypes().get(0).simpleName()).isEqualTo("GrandParent");
    }

    // AC-4: enum target has kind=enum
    @Test
    void enumTypeHasKindEnum() throws IOException {
        writeSource("Color.java", "public enum Color { RED, GREEN, BLUE }");

        var result = tool("Color").execute();

        assertThat(result.kind()).isEqualTo("enum");
    }

    // AC-7: subtypesScope is always "project-local only"
    @Test
    void subtypesScopeIsAlwaysProjectLocalOnly() throws IOException {
        writeSource("Foo.java", "public class Foo {}");

        var result = tool("Foo").execute();

        assertThat(result.subtypesScope()).isEqualTo("project-local only");
    }

    private HierarchyTool tool(String typeName) {
        return new HierarchyTool(tempDir.toString(), typeName, false, false, false);
    }

    private HierarchyTool toolWithFlags(
            String typeName, boolean supertypesOnly, boolean subtypesOnly, boolean includeObject) {
        return new HierarchyTool(tempDir.toString(), typeName, supertypesOnly, subtypesOnly, includeObject);
    }

    private void writeSource(String filename, String content) throws IOException {
        Files.writeString(tempDir.resolve(filename), content);
    }
}
