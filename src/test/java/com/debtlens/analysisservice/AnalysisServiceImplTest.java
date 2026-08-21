package com.debtlens.analysisservice;

import com.debtlens.analysisservice.analyzer.RepositoryAnalyzer;
import com.debtlens.analysisservice.dto.AnalysisJobMessage;
import com.debtlens.analysisservice.dto.AnalysisResult;
import com.debtlens.analysisservice.metrics.RepositoryMetrics;
import com.debtlens.analysisservice.repository.RepositoryManager;
import com.debtlens.analysisservice.service.impl.AnalysisServiceImpl;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AnalysisServiceImplTest {

    @Test
    void shouldAnalyzeRepositorySuccessfully() {

        RepositoryAnalyzer repositoryAnalyzer =
                mock(RepositoryAnalyzer.class);

        RepositoryManager repositoryManager =
                mock(RepositoryManager.class);

        AnalysisServiceImpl service =
                new AnalysisServiceImpl(
                        repositoryAnalyzer,
                        repositoryManager
                );

        AnalysisJobMessage job =
                new AnalysisJobMessage();

        job.setJobId("job-001");
        job.setRepositoryId("repo-001");
        job.setRepositoryUrl(
                "https://github.com/example/repository.git"
        );
        job.setBranch("main");

        Path repositoryPath =
                Path.of("/tmp/test-repository");

        RepositoryMetrics repositoryMetrics =
                new RepositoryMetrics();

        when(repositoryManager.prepareRepository(
                job.getRepositoryUrl(),
                job.getBranch()
        )).thenReturn(repositoryPath);

        when(repositoryAnalyzer.analyze(repositoryPath))
                .thenReturn(repositoryMetrics);

        AnalysisResult result =
                service.analyze(job);

        assertEquals("job-001", result.getJobId());
        assertEquals("repo-001", result.getRepositoryId());
        assertEquals("SUCCESS", result.getStatus());

        assertSame(
                repositoryMetrics,
                result.getRepositoryMetrics()
        );

        verify(repositoryManager)
                .prepareRepository(
                        job.getRepositoryUrl(),
                        job.getBranch()
                );

        verify(repositoryAnalyzer)
                .analyze(repositoryPath);

        verify(repositoryManager)
                .cleanupRepository(repositoryPath);
    }

    @Test
    void shouldReturnFailedResultWhenAnalysisFails() {

        RepositoryAnalyzer repositoryAnalyzer =
                mock(RepositoryAnalyzer.class);

        RepositoryManager repositoryManager =
                mock(RepositoryManager.class);

        AnalysisServiceImpl service =
                new AnalysisServiceImpl(
                        repositoryAnalyzer,
                        repositoryManager
                );

        AnalysisJobMessage job =
                new AnalysisJobMessage();

        job.setJobId("job-002");
        job.setRepositoryId("repo-002");
        job.setRepositoryUrl(
                "https://github.com/example/repository.git"
        );
        job.setBranch("main");

        Path repositoryPath =
                Path.of("/tmp/test-repository");

        when(repositoryManager.prepareRepository(
                job.getRepositoryUrl(),
                job.getBranch()
        )).thenReturn(repositoryPath);

        when(repositoryAnalyzer.analyze(repositoryPath))
                .thenThrow(new RuntimeException("Analysis failed"));

        AnalysisResult result =
                service.analyze(job);

        assertEquals("job-002", result.getJobId());
        assertEquals("repo-002", result.getRepositoryId());
        assertEquals("FAILED", result.getStatus());
        assertEquals("Analysis failed", result.getError());

        verify(repositoryManager)
                .cleanupRepository(repositoryPath);
    }
}