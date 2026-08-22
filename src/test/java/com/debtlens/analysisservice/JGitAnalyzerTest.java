package com.debtlens.analysisservice;

import com.debtlens.analysisservice.analyzer.JGitAnalyzer;
import com.debtlens.analysisservice.metrics.ClassMetrics;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JGitAnalyzerTest {

    @Test
    void shouldEnrichClassGitMetrics() {

        // Change this to the path of a real Git repository
        Path repositoryPath = Path.of(
                "/home/nalina/Desktop/Technical Debt/Analysis_Service_Spring_Boot"
        );

        JGitAnalyzer analyzer = new JGitAnalyzer();

        // Create sample class metrics
        ClassMetrics classMetrics = new ClassMetrics();

        classMetrics.setClassName("AnalysisService");
        classMetrics.setFilePath(
                repositoryPath
                        .resolve("src/main/java/com/debtlens/analysisservice/AnalysisService.java")
                        .toString()
        );

        List<ClassMetrics> metrics =
                new ArrayList<>();

        metrics.add(classMetrics);

        // Enrich the class with Git metrics
        analyzer.enrichGitMetrics(
                repositoryPath,
                metrics
        );

        ClassMetrics result = metrics.get(0);

        System.out.println("========== FILE GIT ANALYSIS ==========");
        System.out.println(
                "Class             : " + result.getClassName()
        );
        System.out.println(
                "Versions          : " + result.getNumberOfVersionsUntil()
        );
        System.out.println(
                "Authors           : " + result.getNumberOfAuthorsUntil()
        );
        System.out.println(
                "Lines Added       : " + result.getLinesAddedUntil()
        );
        System.out.println(
                "Max Lines Added   : " + result.getMaxLinesAddedUntil()
        );
        System.out.println(
                "Avg Lines Added   : " + result.getAvgLinesAddedUntil()
        );
        System.out.println(
                "Lines Removed     : " + result.getLinesRemovedUntil()
        );
        System.out.println(
                "Max Lines Removed : " + result.getMaxLinesRemovedUntil()
        );
        System.out.println(
                "Avg Lines Removed : " + result.getAvgLinesRemovedUntil()
        );
        System.out.println(
                "Code Churn        : " + result.getCodeChurnUntil()
        );
        System.out.println(
                "Max Code Churn    : " + result.getMaxCodeChurnUntil()
        );
        System.out.println(
                "Avg Code Churn    : " + result.getAvgCodeChurnUntil()
        );
        System.out.println("======================================");

        // Basic validation
        assertNotNull(result);

        assertTrue(
                result.getNumberOfVersionsUntil() >= 0
        );

        assertTrue(
                result.getNumberOfAuthorsUntil() >= 0
        );

        assertTrue(
                result.getLinesAddedUntil() >= 0
        );

        assertTrue(
                result.getLinesRemovedUntil() >= 0
        );

        assertTrue(
                result.getCodeChurnUntil() >= 0
        );
    }
}