package com.debtlens.analysisservice;

import com.debtlens.analysisservice.analyzer.CKAnalyzer;
import com.debtlens.analysisservice.analyzer.JGitAnalyzer;
import com.debtlens.analysisservice.analyzer.JavaParserAnalyzer;
import com.debtlens.analysisservice.analyzer.RepositoryAnalyzer;
import com.debtlens.analysisservice.metrics.ClassMetrics;
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


        // Create RepositoryAnalyzer
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


        // Class metrics validation
        assertNotNull(metrics.getClassMetrics());

        assertFalse(
                metrics.getClassMetrics().isEmpty()
        );


        // Check first class contains analysis metrics
        ClassMetrics firstClass =
                metrics.getClassMetrics().get(0);


        assertNotNull(firstClass);

        assertNotNull(
                firstClass.getClassName()
        );

        assertNotNull(
                firstClass.getFilePath()
        );


        // Git metrics are now inside ClassMetrics
        assertTrue(
                firstClass.getNumberOfVersionsUntil() >= 0
        );

        assertTrue(
                firstClass.getNumberOfAuthorsUntil() >= 0
        );


        // Print results
        System.out.println(
                "========== REPOSITORY ANALYSIS =========="
        );

        System.out.println(
                "Repository : "
                        + metrics.getRepositoryName()
        );

        System.out.println(
                "Classes    : "
                        + metrics.getClassMetrics().size()
        );

        System.out.println(
                "First Class: "
                        + firstClass.getClassName()
        );

        System.out.println(
                "Versions   : "
                        + firstClass.getNumberOfVersionsUntil()
        );

        System.out.println(
                "Authors    : "
                        + firstClass.getNumberOfAuthorsUntil()
        );

        System.out.println(
                "========================================="
        );
    }
}