package com.debtlens.analysisservice.analyzer;

import com.debtlens.analysisservice.metrics.ClassMetrics;
import com.debtlens.analysisservice.metrics.RepositoryMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class RepositoryAnalyzerTest {

    @TempDir
    Path tempDir;

    @Test
    void analyze_shouldMergeJavaParserAndCKMetricsAndEnrichWithGitHistory() {
        Path repositoryPath = tempDir.resolve("repository").toAbsolutePath();
        Path sharedFile = repositoryPath.resolve("src/main/java/com/example/Sample.java");
        Path ckOnlyFile = repositoryPath.resolve("src/main/java/com/example/OnlyCk.java");

        JavaParserAnalyzer javaParserAnalyzer = mock(JavaParserAnalyzer.class);
        CKAnalyzer ckAnalyzer = mock(CKAnalyzer.class);
        JGitAnalyzer jGitAnalyzer = mock(JGitAnalyzer.class);

        ClassMetrics javaParserMetrics = new ClassMetrics();
        javaParserMetrics.setClassName("Sample");
        javaParserMetrics.setFilePath(sharedFile.toString());
        javaParserMetrics.setStartLine(1);
        javaParserMetrics.setEndLine(25);
        javaParserMetrics.setNumberOfLinesOfCode(25);
        javaParserMetrics.setNumberOfMethods(1);
        javaParserMetrics.setNumberOfPublicMethods(1);
        javaParserMetrics.setNumberOfPrivateMethods(0);
        javaParserMetrics.setNumberOfAttributes(1);
        javaParserMetrics.setNumberOfPublicAttributes(1);
        javaParserMetrics.setNumberOfPrivateAttributes(0);
        ClassMetrics ckSharedMetrics = new ClassMetrics();
        ckSharedMetrics.setClassName("Sample");
        ckSharedMetrics.setFilePath(sharedFile.toString());
        ckSharedMetrics.setCbo(7);
        ckSharedMetrics.setWmc(3.5);
        ckSharedMetrics.setDit(2);
        ckSharedMetrics.setRfc(9);
        ckSharedMetrics.setLcom(1.5);
        ckSharedMetrics.setNoc(4);
        ckSharedMetrics.setFanin(6);
        ckSharedMetrics.setFanout(8);
        ckSharedMetrics.setNumberOfMethods(5);
        ckSharedMetrics.setNumberOfAttributes(6);

        ClassMetrics ckOnlyMetrics = new ClassMetrics();
        ckOnlyMetrics.setClassName("OnlyCk");
        ckOnlyMetrics.setFilePath(ckOnlyFile.toString());
        ckOnlyMetrics.setCbo(1);
        ckOnlyMetrics.setWmc(2.0);
        ckOnlyMetrics.setDit(0);
        ckOnlyMetrics.setRfc(3);
        ckOnlyMetrics.setLcom(0.0);
        ckOnlyMetrics.setNoc(0);
        ckOnlyMetrics.setFanin(1);
        ckOnlyMetrics.setFanout(2);
        ckOnlyMetrics.setNumberOfMethods(2);
        ckOnlyMetrics.setNumberOfAttributes(1);

        doAnswer(invocation -> {
            List<ClassMetrics> finalMetrics = invocation.getArgument(1);
            for (ClassMetrics metrics : finalMetrics) {
                if (metrics.getClassName().equals("Sample")) {
                    metrics.setNumberOfVersionsUntil(4);
                    metrics.setNumberOfAuthorsUntil(2);
                    metrics.setLinesAddedUntil(10);
                    metrics.setLinesRemovedUntil(3);
                    metrics.setCodeChurnUntil(13);
                } else if (metrics.getClassName().equals("OnlyCk")) {
                    metrics.setNumberOfVersionsUntil(1);
                    metrics.setNumberOfAuthorsUntil(1);
                    metrics.setLinesAddedUntil(2);
                    metrics.setLinesRemovedUntil(1);
                    metrics.setCodeChurnUntil(3);
                }
            }
            return null;
        }).when(jGitAnalyzer).enrichGitMetrics(eq(repositoryPath), anyList());

        when(javaParserAnalyzer.analyze(repositoryPath)).thenReturn(List.of(javaParserMetrics));
        when(ckAnalyzer.analyze(repositoryPath)).thenReturn(List.of(ckSharedMetrics, ckOnlyMetrics));

        RepositoryAnalyzer analyzer = new RepositoryAnalyzer(javaParserAnalyzer, ckAnalyzer, jGitAnalyzer);
        RepositoryMetrics repositoryMetrics = analyzer.analyze(repositoryPath);

        assertNotNull(repositoryMetrics);
        assertEquals(repositoryPath.toString(), repositoryMetrics.getRepositoryId());
        assertEquals(repositoryPath.getFileName().toString(), repositoryMetrics.getRepositoryName());
        assertEquals(2, repositoryMetrics.getClassMetrics().size());

        ClassMetrics mergedSample = repositoryMetrics.getClassMetrics().stream()
                .filter(metrics -> metrics.getClassName().equals("Sample"))
                .findFirst()
                .orElseThrow();

        assertEquals(sharedFile.toString(), mergedSample.getFilePath());
        assertEquals(25, mergedSample.getNumberOfLinesOfCode());
        assertEquals(7, mergedSample.getCbo());
        assertEquals(3.5, mergedSample.getWmc(), 0.0001);
        assertEquals(2, mergedSample.getDit());
        assertEquals(9, mergedSample.getRfc());
        assertEquals(1.5, mergedSample.getLcom(), 0.0001);
        assertEquals(4, mergedSample.getNoc());
        assertEquals(6, mergedSample.getFanin());
        assertEquals(8, mergedSample.getFanout());
        assertEquals(5, mergedSample.getNumberOfMethods());
        assertEquals(6, mergedSample.getNumberOfAttributes());
        assertEquals(4, mergedSample.getNumberOfVersionsUntil());
        assertEquals(2, mergedSample.getNumberOfAuthorsUntil());
        assertEquals(10, mergedSample.getLinesAddedUntil());
        assertEquals(3, mergedSample.getLinesRemovedUntil());
        assertEquals(13, mergedSample.getCodeChurnUntil());
        ClassMetrics ckOnly = repositoryMetrics.getClassMetrics().stream()
                .filter(metrics -> metrics.getClassName().equals("OnlyCk"))
                .findFirst()
                .orElseThrow();

        assertEquals(ckOnlyFile.toString(), ckOnly.getFilePath());
        assertEquals(1, ckOnly.getCbo());
        assertEquals(2.0, ckOnly.getWmc(), 0.0001);
        assertEquals(0, ckOnly.getDit());
        assertEquals(3, ckOnly.getRfc());
        assertEquals(0.0, ckOnly.getLcom(), 0.0001);
        assertEquals(0, ckOnly.getNoc());
        assertEquals(1, ckOnly.getFanin());
        assertEquals(2, ckOnly.getFanout());
        assertEquals(2, ckOnly.getNumberOfMethods());
        assertEquals(1, ckOnly.getNumberOfAttributes());
        assertEquals(1, ckOnly.getNumberOfVersionsUntil());
        assertEquals(1, ckOnly.getNumberOfAuthorsUntil());
        assertEquals(2, ckOnly.getLinesAddedUntil());
        assertEquals(1, ckOnly.getLinesRemovedUntil());
        assertEquals(3, ckOnly.getCodeChurnUntil());

        verify(javaParserAnalyzer).analyze(repositoryPath);
        verify(ckAnalyzer).analyze(repositoryPath);
        verify(jGitAnalyzer).enrichGitMetrics(eq(repositoryPath), anyList());
    }
}