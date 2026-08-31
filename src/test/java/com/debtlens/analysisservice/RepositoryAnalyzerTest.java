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

        Path repositoryPath = Path.of("").toAbsolutePath();


        // Create analyzers

        JavaParserAnalyzer javaParserAnalyzer =
                new JavaParserAnalyzer();

        CKAnalyzer ckAnalyzer =
                new CKAnalyzer();

        JGitAnalyzer jGitAnalyzer =
                new JGitAnalyzer();


        // Create repository analyzer

        RepositoryAnalyzer analyzer =
                new RepositoryAnalyzer(
                        javaParserAnalyzer,
                        ckAnalyzer,
                        jGitAnalyzer
                );


        // Analyze repository

        RepositoryMetrics metrics =
                analyzer.analyze(repositoryPath);



        // =========================
        // Validation
        // =========================

        assertNotNull(metrics);

        assertNotNull(
                metrics.getRepositoryId()
        );

        assertNotNull(
                metrics.getRepositoryName()
        );


        assertNotNull(
                metrics.getClassMetrics()
        );

        assertFalse(
                metrics.getClassMetrics().isEmpty()
        );


        ClassMetrics firstClass =
                metrics.getClassMetrics()
                        .stream()
                        .filter(c -> c.getNumberOfMethods() > 0)
                        .findFirst()
                        .orElse(metrics.getClassMetrics().get(0));


        assertNotNull(firstClass);


        assertNotNull(
                firstClass.getClassName()
        );

        assertNotNull(
                firstClass.getFilePath()
        );


        // Git metrics validation

        assertTrue(
                firstClass.getNumberOfVersionsUntil() >= 0
        );

        assertTrue(
                firstClass.getNumberOfAuthorsUntil() >= 0
        );



        // =========================
        // Print Repository Metrics
        // =========================

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
                "\n========== FIRST CLASS METRICS =========="
        );


        System.out.println(
                "Class Name              : "
                        + firstClass.getClassName()
        );


        System.out.println(
                "File Path               : "
                        + firstClass.getFilePath()
        );


        // Code metrics

        System.out.println(
                "Number of Lines of Code : "
                        + firstClass.getNumberOfLinesOfCode()
        );


        // CK metrics

        System.out.println(
                "CBO                     : "
                        + firstClass.getCbo()
        );

        System.out.println(
                "WMC                     : "
                        + firstClass.getWmc()
        );

        System.out.println(
                "DIT                     : "
                        + firstClass.getDit()
        );

        System.out.println(
                "RFC                     : "
                        + firstClass.getRfc()
        );

        System.out.println(
                "LCOM                    : "
                        + firstClass.getLcom()
        );

        System.out.println(
                "NOC                     : "
                        + firstClass.getNoc()
        );


        // Fan metrics

        System.out.println(
                "Fan In                  : "
                        + firstClass.getFanin()
        );

        System.out.println(
                "Fan Out                 : "
                        + firstClass.getFanout()
        );


        // Method / attribute metrics

        System.out.println(
                "Number of Methods       : "
                        + firstClass.getNumberOfMethods()
        );

        System.out.println(
                "Number of Attributes    : "
                        + firstClass.getNumberOfAttributes()
        );

        System.out.println(
                "Public Methods          : "
                        + firstClass.getNumberOfPublicMethods()
        );

        System.out.println(
                "Private Methods         : "
                        + firstClass.getNumberOfPrivateMethods()
        );

        System.out.println(
                "Public Attributes       : "
                        + firstClass.getNumberOfPublicAttributes()
        );

        System.out.println(
                "Private Attributes      : "
                        + firstClass.getNumberOfPrivateAttributes()
        );


        // Git metrics

        System.out.println(
                "\n========== GIT METRICS =========="
        );

        System.out.println(
                "Versions                : "
                        + firstClass.getNumberOfVersionsUntil()
        );

        System.out.println(
                "Authors                 : "
                        + firstClass.getNumberOfAuthorsUntil()
        );

        System.out.println(
                "Lines Added             : "
                        + firstClass.getLinesAddedUntil()
        );

        System.out.println(
                "Lines Removed           : "
                        + firstClass.getLinesRemovedUntil()
        );

        System.out.println(
                "Code Churn              : "
                        + firstClass.getCodeChurnUntil()
        );

        System.out.println(
                "Age                     : "
                        + firstClass.getAgeWithRespectTo()
        );

        System.out.println(
                "Weighted Age            : "
                        + firstClass.getWeightedAgeWithRespectTo()
        );


        System.out.println(
                "========================================="
        );
    }
}