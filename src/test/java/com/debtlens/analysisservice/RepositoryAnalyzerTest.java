package com.debtlens.analysisservice;

import com.debtlens.analysisservice.analyzer.CKAnalyzer;
import com.debtlens.analysisservice.analyzer.JGitAnalyzer;
import com.debtlens.analysisservice.analyzer.JavaParserAnalyzer;
import com.debtlens.analysisservice.analyzer.RepositoryAnalyzer;
import com.debtlens.analysisservice.metrics.ClassMetrics;
import com.debtlens.analysisservice.metrics.RepositoryMetrics;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryAnalyzerTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldAnalyzeRepository() throws Exception {
        Path repositoryPath = tempDir.resolve("sample-repo");
        Files.createDirectories(repositoryPath);

        Path javaFile = repositoryPath.resolve("src/main/java/com/example/SampleClass.java");
        Files.createDirectories(javaFile.getParent());

        try (Git git = Git.init().setDirectory(repositoryPath.toFile()).setInitialBranch("main").call()) {
            Files.writeString(javaFile, "package com.example;\n\npublic class SampleClass {\n    public void doSomething() {}\n}\n", StandardCharsets.UTF_8);
            git.add().addFilepattern("src/main/java/com/example/SampleClass.java").call();
            PersonIdent author1 = new PersonIdent("Author One", "author1@example.com", Instant.now().minusSeconds(3600), ZoneOffset.UTC);
            git.commit().setMessage("Initial commit").setAuthor(author1).setCommitter(author1).call();

            Files.writeString(javaFile, "package com.example;\n\npublic class SampleClass {\n    public void doSomething() {}\n    public void anotherMethod() {}\n}\n", StandardCharsets.UTF_8);
            git.add().addFilepattern("src/main/java/com/example/SampleClass.java").call();
            PersonIdent author2 = new PersonIdent("Author Two", "author2@example.com", Instant.now(), ZoneOffset.UTC);
            git.commit().setMessage("Update").setAuthor(author2).setCommitter(author2).call();
        }

        JavaParserAnalyzer javaParserAnalyzer = new JavaParserAnalyzer();
        CKAnalyzer ckAnalyzer = new CKAnalyzer();
        JGitAnalyzer jGitAnalyzer = new JGitAnalyzer();

        RepositoryAnalyzer analyzer = new RepositoryAnalyzer(
                javaParserAnalyzer,
                ckAnalyzer,
                jGitAnalyzer
        );

        RepositoryMetrics metrics = analyzer.analyze(repositoryPath);

        // Validation
        assertNotNull(metrics);
        assertNotNull(metrics.getRepositoryId());
        assertNotNull(metrics.getRepositoryName());
        assertNotNull(metrics.getClassMetrics());
        assertFalse(metrics.getClassMetrics().isEmpty());

        ClassMetrics firstClass = metrics.getClassMetrics()
                .stream()
                .filter(c -> c.getNumberOfMethods() > 0)
                .findFirst()
                .orElse(metrics.getClassMetrics().get(0));

        assertNotNull(firstClass);
        assertNotNull(firstClass.getClassName());
        assertNotNull(firstClass.getFilePath());
        assertTrue(firstClass.getNumberOfVersionsUntil() > 0, "Versions should be greater than 0");
        assertTrue(firstClass.getNumberOfAuthorsUntil() > 0, "Authors should be greater than 0");

        System.out.println("========== REPOSITORY ANALYSIS ==========");
        System.out.println("Repository : " + metrics.getRepositoryName());
        System.out.println("Classes    : " + metrics.getClassMetrics().size());
        System.out.println("\n========== FIRST CLASS METRICS ==========");
        System.out.println("Class Name              : " + firstClass.getClassName());
        System.out.println("File Path               : " + firstClass.getFilePath());
        System.out.println("Number of Lines of Code : " + firstClass.getNumberOfLinesOfCode());
        System.out.println("CBO                     : " + firstClass.getCbo());
        System.out.println("WMC                     : " + firstClass.getWmc());
        System.out.println("DIT                     : " + firstClass.getDit());
        System.out.println("RFC                     : " + firstClass.getRfc());
        System.out.println("LCOM                    : " + firstClass.getLcom());
        System.out.println("NOC                     : " + firstClass.getNoc());
        System.out.println("Fan In                  : " + firstClass.getFanin());
        System.out.println("Fan Out                 : " + firstClass.getFanout());
        System.out.println("Number of Methods       : " + firstClass.getNumberOfMethods());
        System.out.println("Number of Attributes    : " + firstClass.getNumberOfAttributes());
        System.out.println("Public Methods          : " + firstClass.getNumberOfPublicMethods());
        System.out.println("Private Methods         : " + firstClass.getNumberOfPrivateMethods());
        System.out.println("Public Attributes       : " + firstClass.getNumberOfPublicAttributes());
        System.out.println("Private Attributes      : " + firstClass.getNumberOfPrivateAttributes());
        System.out.println("\n========== GIT METRICS ==========");
        System.out.println("Versions                : " + firstClass.getNumberOfVersionsUntil());
        System.out.println("Authors                 : " + firstClass.getNumberOfAuthorsUntil());
        System.out.println("Lines Added             : " + firstClass.getLinesAddedUntil());
        System.out.println("Lines Removed           : " + firstClass.getLinesRemovedUntil());
        System.out.println("Code Churn              : " + firstClass.getCodeChurnUntil());
        System.out.println("Age                     : " + firstClass.getAgeWithRespectTo());
        System.out.println("Weighted Age            : " + firstClass.getWeightedAgeWithRespectTo());
        System.out.println("=========================================");
    }
}