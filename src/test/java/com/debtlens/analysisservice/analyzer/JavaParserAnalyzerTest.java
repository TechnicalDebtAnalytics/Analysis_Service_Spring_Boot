package com.debtlens.analysisservice.analyzer;

import com.debtlens.analysisservice.metrics.ClassMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaParserAnalyzerTest {

    @TempDir
    Path tempDir;

    @Test
    void analyze_shouldExtractClassesMethodsFieldsCommentsAndIgnoreTestSources() throws Exception {
        Path repositoryPath = tempDir.resolve("repository");
        Path mainSource = repositoryPath.resolve("src/main/java/com/example/analysis/Outer.java");
        Path ignoredTestSource = repositoryPath.resolve("src/test/java/com/example/analysis/IgnoredTest.java");

        Files.createDirectories(mainSource.getParent());
        Files.createDirectories(ignoredTestSource.getParent());

        Files.writeString(mainSource, """
                package com.example.analysis;

                /**
                 * Outer class comment.
                 */
                public class Outer {

                    public String publicField;
                    private int firstPrivateField, secondPrivateField;
                    int packageField;

                    // outer method comment
                    public void doWork() {
                    }

                    private int helper() {
                        return 42;
                    }

                    void packageMethod() {
                    }

                    /**
                     * Inner class comment.
                     */
                    static class Inner {

                        private String innerValue;

                        // inner method comment
                        public String getInnerValue() {
                            return innerValue;
                        }
                    }
                }
                """);

        Files.writeString(ignoredTestSource, """
                package com.example.analysis;

                public class IgnoredTest {
                    public void shouldNotBeSeen() {
                    }
                }
                """);

        JavaParserAnalyzer analyzer = new JavaParserAnalyzer();

        List<ClassMetrics> results = analyzer.analyze(repositoryPath);

        assertEquals(2, results.size());

        Map<String, ClassMetrics> byClassName = results.stream()
                .collect(Collectors.toMap(ClassMetrics::getClassName, metrics -> metrics));

        assertTrue(byClassName.containsKey("Outer"));
        assertTrue(byClassName.containsKey("Inner"));

        ClassMetrics outer = byClassName.get("Outer");
        assertNotNull(outer);
        assertEquals(mainSource.toString(), outer.getFilePath());
        assertTrue(outer.getStartLine() > 0);
        assertTrue(outer.getEndLine() >= outer.getStartLine());
        assertTrue(outer.getNumberOfLinesOfCode() > 0);
        assertEquals(3, outer.getNumberOfMethods());
        assertEquals(1, outer.getNumberOfPublicMethods());
        assertEquals(1, outer.getNumberOfPrivateMethods());
        assertEquals(4, outer.getNumberOfAttributes());
        assertEquals(1, outer.getNumberOfPublicAttributes());
        assertEquals(2, outer.getNumberOfPrivateAttributes());
        ClassMetrics inner = byClassName.get("Inner");
        assertNotNull(inner);
        assertEquals(mainSource.toString(), inner.getFilePath());
        assertTrue(inner.getStartLine() > 0);
        assertTrue(inner.getEndLine() >= inner.getStartLine());
        assertTrue(inner.getNumberOfLinesOfCode() > 0);
        assertEquals(1, inner.getNumberOfMethods());
        assertEquals(1, inner.getNumberOfPublicMethods());
        assertEquals(0, inner.getNumberOfPrivateMethods());
        assertEquals(1, inner.getNumberOfAttributes());
        assertEquals(0, inner.getNumberOfPublicAttributes());
        assertEquals(1, inner.getNumberOfPrivateAttributes());
        assertTrue(results.stream().noneMatch(metrics -> metrics.getClassName().equals("IgnoredTest")));
    }
}