package com.debtlens.analysisservice;

import com.debtlens.analysisservice.analyzer.JGitAnalyzer;
import com.debtlens.analysisservice.metrics.GitMetrics;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JGitAnalyzerTest {

    @Test
    void shouldAnalyzeGitRepository() {

        // Change this to the path of a real Git repository
        Path repositoryPath = Path.of(
                "/home/nalina/Desktop/Technical Debt/Analysis_Service_Spring_Boot"
        );

        JGitAnalyzer analyzer = new JGitAnalyzer();

        GitMetrics metrics = analyzer.analyze(repositoryPath);

        System.out.println("========== GIT ANALYSIS ==========");
        System.out.println("Commits       : " + metrics.getCommitCount());
        System.out.println("Authors       : " + metrics.getAuthorCount());
        System.out.println("Lines Added   : " + metrics.getLinesAdded());
        System.out.println("Lines Deleted : " + metrics.getLinesDeleted());
        System.out.println("Churn         : " + metrics.getChurn());
        System.out.println("Last Modified : " + metrics.getLastModified());
        System.out.println("==================================");

        assertNotNull(metrics);
        assertTrue(metrics.getCommitCount() > 0);
        assertTrue(metrics.getAuthorCount() > 0);
        assertNotNull(metrics.getLastModified());
    }
}