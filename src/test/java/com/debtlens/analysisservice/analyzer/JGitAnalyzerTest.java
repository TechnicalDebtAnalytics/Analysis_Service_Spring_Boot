package com.debtlens.analysisservice.analyzer;

import com.debtlens.analysisservice.metrics.ClassMetrics;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JGitAnalyzerTest {

    @TempDir
    Path tempDir;

    @Test
    void enrichGitMetrics_shouldAggregateFileHistoryAcrossMultipleCommits() throws Exception {
        Path repositoryPath = tempDir.resolve("repository");
        Files.createDirectories(repositoryPath);

        Path sampleFile = repositoryPath.resolve("src/main/java/com/example/history/Sample.java");
        Files.createDirectories(sampleFile.getParent());

        try (Git git = Git.init().setDirectory(repositoryPath.toFile()).call()) {
            commitRevision(
                    git,
                    repositoryPath,
                    sampleFile,
                    """
                            package com.example.history;

                            public class Sample {
                                int value = 1;
                            }
                            """,
                    "Alice",
                    "alice@example.com",
                    Instant.parse("2024-01-01T00:00:00Z"),
                    "Initial commit"
            );

            commitRevision(
                    git,
                    repositoryPath,
                    sampleFile,
                    """
                            package com.example.history;

                            public class Sample {
                                int value = 1;
                                // first change
                            }
                            """,
                    "Alice",
                    "alice@example.com",
                    Instant.parse("2024-01-02T00:00:00Z"),
                    "Add first change"
            );

            commitRevision(
                    git,
                    repositoryPath,
                    sampleFile,
                    """
                            package com.example.history;

                            public class Sample {
                                int value = 1;
                                // second change
                            }
                            """,
                    "Bob",
                    "bob@example.com",
                    Instant.parse("2024-01-05T00:00:00Z"),
                    "Replace change"
            );
        }

        ClassMetrics metrics = new ClassMetrics();
        metrics.setClassName("Sample");
        metrics.setFilePath(sampleFile.toString());

        List<ClassMetrics> classMetrics = new ArrayList<>();
        classMetrics.add(metrics);

        new JGitAnalyzer().enrichGitMetrics(repositoryPath, classMetrics);

        ClassMetrics result = classMetrics.get(0);

        assertNotNull(result);
        assertEquals(2, result.getNumberOfVersionsUntil());
        assertEquals(2, result.getNumberOfAuthorsUntil());
        assertEquals(2, result.getLinesAddedUntil());
        assertEquals(1, result.getLinesRemovedUntil());
        assertEquals(3, result.getCodeChurnUntil());
        assertEquals(1, result.getMaxLinesAddedUntil());
        assertEquals(1, result.getMaxLinesRemovedUntil());
        assertEquals(2, result.getMaxCodeChurnUntil());
        assertEquals(1.0, result.getAvgLinesAddedUntil(), 0.0001);
        assertEquals(0.5, result.getAvgLinesRemovedUntil(), 0.0001);
        assertEquals(1.5, result.getAvgCodeChurnUntil(), 0.0001);
        assertEquals(3.0, result.getAgeWithRespectTo(), 0.0001);
                assertEquals(6.0, result.getWeightedAgeWithRespectTo(), 0.0001);
    }

    private void commitRevision(
            Git git,
            Path repositoryPath,
            Path file,
            String content,
            String authorName,
            String authorEmail,
            Instant when,
            String message
    ) throws Exception {
        Files.writeString(file, content, StandardCharsets.UTF_8);

        String relativePath = repositoryPath.relativize(file)
                .toString()
                .replace('\\', '/');

        git.add().addFilepattern(relativePath).call();

        PersonIdent ident = new PersonIdent(
                authorName,
                authorEmail,
                when,
                ZoneOffset.UTC
        );

        git.commit()
                .setMessage(message)
                .setAuthor(ident)
                .setCommitter(ident)
                .call();
    }
}