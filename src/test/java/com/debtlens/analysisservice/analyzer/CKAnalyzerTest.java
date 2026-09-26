package com.debtlens.analysisservice.analyzer;

import com.debtlens.analysisservice.metrics.ClassMetrics;
import com.github.mauricioaniche.ck.CK;
import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.CKNotifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CKAnalyzerTest {

    @TempDir
    Path tempDir;

    @Test
    void analyze_shouldMapActualCKResultsIntoClassMetrics() throws Exception {
        Path repositoryPath = tempDir.resolve("repository");
        Path sourceFile = repositoryPath.resolve("src/main/java/com/example/metrics/SampleService.java");

        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, """
                package com.example.metrics;

                import java.util.ArrayList;
                import java.util.List;

                public class SampleService {

                    private final List<String> names = new ArrayList<>();

                    public SampleService() {
                    }

                    public void addName(String name) {
                        names.add(name);
                    }

                    public int size() {
                        return names.size();
                    }
                }
                """);

        List<CKClassResult> expectedResults = new ArrayList<>();
        CK ck = new CK();
        ck.calculate(repositoryPath, new CKNotifier() {
            @Override
            public void notify(CKClassResult result) {
                expectedResults.add(result);
            }

            @Override
            public void notifyError(String sourceFile, Exception e) {
                throw new AssertionError("CK analysis failed for " + sourceFile, e);
            }
        });

        CKAnalyzer analyzer = new CKAnalyzer();
        List<ClassMetrics> actualResults = analyzer.analyze(repositoryPath);

        assertFalse(expectedResults.isEmpty());
        assertEquals(expectedResults.size(), actualResults.size());

        for (CKClassResult expected : expectedResults) {
            ClassMetrics actual = actualResults.stream()
                    .filter(metrics -> metrics.getClassName().equals(expected.getClassName())
                            && metrics.getFilePath().equals(expected.getFile()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "Missing metrics for CK class " + expected.getClassName()
                    ));

            assertEquals(expected.getClassName(), actual.getClassName());
            assertEquals(expected.getFile(), actual.getFilePath());
            assertEquals(expected.getCbo(), actual.getCbo());
            assertEquals(expected.getWmc(), actual.getWmc());
            assertEquals(expected.getDit(), actual.getDit());
            assertEquals(expected.getRfc(), actual.getRfc());
            assertEquals(expected.getLcom(), actual.getLcom());
            assertEquals(expected.getNoc(), actual.getNoc());
            assertEquals(expected.getFanin(), actual.getFanin());
            assertEquals(expected.getFanout(), actual.getFanout());
            assertEquals(expected.getNumberOfMethods(), actual.getNumberOfMethods());
            assertEquals(expected.getNumberOfFields(), actual.getNumberOfAttributes());

            assertNotNull(actual);
            assertTrue(actual.getNumberOfMethods() >= 0);
            assertTrue(actual.getNumberOfAttributes() >= 0);
        }
    }
}