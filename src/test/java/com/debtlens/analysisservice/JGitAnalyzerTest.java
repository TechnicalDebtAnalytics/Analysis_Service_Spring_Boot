package com.debtlens.analysisservice;

import com.debtlens.analysisservice.analyzer.JGitAnalyzer;
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

import static org.junit.jupiter.api.Assertions.*;

class JGitAnalyzerTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldEnrichClassGitMetrics() throws Exception {
        Path repositoryPath = tempDir.resolve("sample-repo");
        Files.createDirectories(repositoryPath);

        Path javaFile = repositoryPath.resolve("src/main/java/com/debtlens/analysisservice/messaging/AnalysisResultPublisher.java");
        Files.createDirectories(javaFile.getParent());

        try (Git git = Git.init().setDirectory(repositoryPath.toFile()).setInitialBranch("main").call()) {
            Files.writeString(javaFile, "package com.debtlens.analysisservice.messaging;\npublic class AnalysisResultPublisher {}\n", StandardCharsets.UTF_8);
            git.add().addFilepattern("src/main/java/com/debtlens/analysisservice/messaging/AnalysisResultPublisher.java").call();
            PersonIdent author1 = new PersonIdent("Author One", "author1@example.com", Instant.now().minusSeconds(3600), ZoneOffset.UTC);
            git.commit().setMessage("Initial commit").setAuthor(author1).setCommitter(author1).call();

            Files.writeString(javaFile, "package com.debtlens.analysisservice.messaging;\npublic class AnalysisResultPublisher {\n public void publish() {}\n}\n", StandardCharsets.UTF_8);
            git.add().addFilepattern("src/main/java/com/debtlens/analysisservice/messaging/AnalysisResultPublisher.java").call();
            PersonIdent author2 = new PersonIdent("Author Two", "author2@example.com", Instant.now(), ZoneOffset.UTC);
            git.commit().setMessage("Add method").setAuthor(author2).setCommitter(author2).call();
        }

        JGitAnalyzer analyzer = new JGitAnalyzer();

        // Create sample class metrics
        ClassMetrics classMetrics = new ClassMetrics();
        classMetrics.setClassName("AnalysisResultPublisher");
        classMetrics.setFilePath(javaFile.toString());

        List<ClassMetrics> metrics = new ArrayList<>();
        metrics.add(classMetrics);

        // Enrich the class with Git metrics
        analyzer.enrichGitMetrics(
                repositoryPath,
                metrics
        );

        ClassMetrics result = metrics.get(0);

        System.out.println("========== FILE GIT ANALYSIS ==========");
        System.out.println("Class             : " + result.getClassName());
        System.out.println("Versions          : " + result.getNumberOfVersionsUntil());
        System.out.println("Authors           : " + result.getNumberOfAuthorsUntil());
        System.out.println("Lines Added       : " + result.getLinesAddedUntil());
        System.out.println("Max Lines Added   : " + result.getMaxLinesAddedUntil());
        System.out.println("Avg Lines Added   : " + result.getAvgLinesAddedUntil());
        System.out.println("Lines Removed     : " + result.getLinesRemovedUntil());
        System.out.println("Max Lines Removed : " + result.getMaxLinesRemovedUntil());
        System.out.println("Avg Lines Removed : " + result.getAvgLinesRemovedUntil());
        System.out.println("Code Churn        : " + result.getCodeChurnUntil());
        System.out.println("Max Code Churn    : " + result.getMaxCodeChurnUntil());
        System.out.println("Avg Code Churn    : " + result.getAvgCodeChurnUntil());
        System.out.println("======================================");

        // Basic validation
        assertNotNull(result);
        assertTrue(result.getNumberOfVersionsUntil() > 0, "Versions should be greater than 0");
        assertTrue(result.getNumberOfAuthorsUntil() > 0, "Authors should be greater than 0");
        assertTrue(result.getLinesAddedUntil() > 0, "Lines added should be greater than 0");
        assertTrue(result.getLinesRemovedUntil() >= 0, "Lines removed cannot be negative");
        assertTrue(result.getCodeChurnUntil() > 0, "Code churn should be greater than 0");
    }
}