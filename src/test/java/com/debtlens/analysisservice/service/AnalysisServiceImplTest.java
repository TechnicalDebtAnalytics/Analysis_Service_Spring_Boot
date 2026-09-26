package com.debtlens.analysisservice.service;

import com.debtlens.analysisservice.analyzer.RepositoryAnalyzer;
import com.debtlens.analysisservice.dto.AnalysisJobMessage;
import com.debtlens.analysisservice.dto.AnalysisResult;
import com.debtlens.analysisservice.metrics.RepositoryMetrics;
import com.debtlens.analysisservice.repository.RepositoryManager;
import com.debtlens.analysisservice.service.impl.AnalysisServiceImpl;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisServiceImplTest {

    @Test
    void analyze_shouldReturnSuccessAndAlwaysCleanupOnSuccess() {
        RepositoryAnalyzer repositoryAnalyzer = mock(RepositoryAnalyzer.class);
        RepositoryManager repositoryManager = mock(RepositoryManager.class);
        AnalysisServiceImpl service = new AnalysisServiceImpl(repositoryAnalyzer, repositoryManager);

        AnalysisJobMessage job = new AnalysisJobMessage();
        job.setJobId("job-001");
        job.setRepositoryId("repo-001");
        job.setRepositoryUrl("file:///tmp/source-repository");
        job.setBranch("main");

        Path clonedRepository = Path.of("/tmp/cloned-repository");
        RepositoryMetrics repositoryMetrics = new RepositoryMetrics();

        when(repositoryManager.prepareRepository(job.getRepositoryUrl(), job.getBranch()))
                .thenReturn(clonedRepository);
        when(repositoryAnalyzer.analyze(clonedRepository)).thenReturn(repositoryMetrics);

        AnalysisResult result = service.analyze(job);

        assertEquals("job-001", result.getJobId());
        assertEquals("repo-001", result.getRepositoryId());
        assertEquals("SUCCESS", result.getStatus());
        assertSame(repositoryMetrics, result.getRepositoryMetrics());
        assertNull(result.getError());

        verify(repositoryManager).prepareRepository(job.getRepositoryUrl(), job.getBranch());
        verify(repositoryAnalyzer).analyze(clonedRepository);
        verify(repositoryManager).cleanupRepository(clonedRepository);
    }

    @Test
    void analyze_shouldReturnFailedResultWhenRepositoryPreparationFailsAndStillCleanup() {
        RepositoryAnalyzer repositoryAnalyzer = mock(RepositoryAnalyzer.class);
        RepositoryManager repositoryManager = mock(RepositoryManager.class);
        AnalysisServiceImpl service = new AnalysisServiceImpl(repositoryAnalyzer, repositoryManager);

        AnalysisJobMessage job = new AnalysisJobMessage();
        job.setJobId("job-002");
        job.setRepositoryId("repo-002");
        job.setRepositoryUrl("file:///tmp/missing-repository");
        job.setBranch("main");

        when(repositoryManager.prepareRepository(job.getRepositoryUrl(), job.getBranch()))
                .thenThrow(new RuntimeException("prepare failed"));

        AnalysisResult result = service.analyze(job);

        assertEquals("job-002", result.getJobId());
        assertEquals("repo-002", result.getRepositoryId());
        assertEquals("FAILED", result.getStatus());
        assertEquals("prepare failed", result.getError());
        assertNull(result.getRepositoryMetrics());

        verify(repositoryAnalyzer, never()).analyze(org.mockito.ArgumentMatchers.any());
        verify(repositoryManager).cleanupRepository(null);
    }

    @Test
    void analyze_shouldReturnFailedResultWhenRepositoryAnalysisFailsAndStillCleanup() {
        RepositoryAnalyzer repositoryAnalyzer = mock(RepositoryAnalyzer.class);
        RepositoryManager repositoryManager = mock(RepositoryManager.class);
        AnalysisServiceImpl service = new AnalysisServiceImpl(repositoryAnalyzer, repositoryManager);

        AnalysisJobMessage job = new AnalysisJobMessage();
        job.setJobId("job-003");
        job.setRepositoryId("repo-003");
        job.setRepositoryUrl("file:///tmp/source-repository");
        job.setBranch("main");

        Path clonedRepository = Path.of("/tmp/cloned-repository");

        when(repositoryManager.prepareRepository(job.getRepositoryUrl(), job.getBranch()))
                .thenReturn(clonedRepository);
        when(repositoryAnalyzer.analyze(clonedRepository))
                .thenThrow(new RuntimeException("analysis failed"));

        AnalysisResult result = service.analyze(job);

        assertEquals("job-003", result.getJobId());
        assertEquals("repo-003", result.getRepositoryId());
        assertEquals("FAILED", result.getStatus());
        assertEquals("analysis failed", result.getError());
        assertNull(result.getRepositoryMetrics());

        verify(repositoryManager).prepareRepository(job.getRepositoryUrl(), job.getBranch());
        verify(repositoryAnalyzer).analyze(clonedRepository);
        verify(repositoryManager).cleanupRepository(clonedRepository);
    }
}