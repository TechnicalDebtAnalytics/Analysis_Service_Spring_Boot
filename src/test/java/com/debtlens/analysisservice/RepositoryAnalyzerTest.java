package com.debtlens.analysisservice;

import com.debtlens.analysisservice.analyzer.CKAnalyzer;
import com.debtlens.analysisservice.analyzer.JGitAnalyzer;
import com.debtlens.analysisservice.analyzer.JavaParserAnalyzer;
import com.debtlens.analysisservice.analyzer.RepositoryAnalyzer;
import com.debtlens.analysisservice.metrics.RepositoryMetrics;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryAnalyzerTest {

    @Test
    void shouldAnalyzeRepository() {

        Path repositoryPath = Path.of(
                "/home/nalina/Desktop/Technical Debt/Analysis_Service_Spring_Boot"
        );

        // Create individual analyzers
        JavaParserAnalyzer javaParserAnalyzer =
                new JavaParserAnalyzer();

        CKAnalyzer ckAnalyzer =
                new CKAnalyzer();

        JGitAnalyzer jGitAnalyzer =
                new JGitAnalyzer();

        // Create RepositoryAnalyzer with its dependencies
        RepositoryAnalyzer analyzer =
                new RepositoryAnalyzer(
                        javaParserAnalyzer,
                        ckAnalyzer,
                        jGitAnalyzer
                );

        // Analyze repository
        RepositoryMetrics metrics =
                analyzer.analyze(repositoryPath);

        // Basic validation
        assertNotNull(metrics);

        // Repository information
        assertNotNull(metrics.getRepositoryId());
        assertNotNull(metrics.getRepositoryName());

        // Java + CK metrics
        assertNotNull(metrics.getClassMetrics());
        assertFalse(metrics.getClassMetrics().isEmpty());

        // Git metrics
        assertNotNull(metrics.getGitMetrics());
        assertTrue(
                metrics.getGitMetrics().getCommitCount() > 0
        );

        // Print results
        System.out.println("========== REPOSITORY ANALYSIS ==========");
        System.out.println(
                "Repository : " +
                        metrics.getRepositoryName()
        );

        System.out.println(
                "Classes    : " +
                        metrics.getClassMetrics().size()
        );

        System.out.println(
                "Commits    : " +
                        metrics.getGitMetrics().getCommitCount()
        );

        System.out.println(
                "Authors    : " +
                        metrics.getGitMetrics().getAuthorCount()
        );

        System.out.println("=========================================");
    }
}