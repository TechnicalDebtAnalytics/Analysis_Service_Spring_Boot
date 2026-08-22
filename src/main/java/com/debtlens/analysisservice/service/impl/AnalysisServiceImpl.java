package com.debtlens.analysisservice.service.impl;

import com.debtlens.analysisservice.analyzer.RepositoryAnalyzer;
import com.debtlens.analysisservice.dto.AnalysisJobMessage;
import com.debtlens.analysisservice.dto.AnalysisResult;
import com.debtlens.analysisservice.metrics.RepositoryMetrics;
import com.debtlens.analysisservice.repository.RepositoryManager;
import com.debtlens.analysisservice.service.AnalysisService;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class AnalysisServiceImpl implements AnalysisService {

    private final RepositoryAnalyzer repositoryAnalyzer;
    private final RepositoryManager repositoryManager;

    public AnalysisServiceImpl(
            RepositoryAnalyzer repositoryAnalyzer,
            RepositoryManager repositoryManager
    ) {
        this.repositoryAnalyzer = repositoryAnalyzer;
        this.repositoryManager = repositoryManager;
    }

    @Override
    public AnalysisResult analyze(AnalysisJobMessage job) {

        Path repositoryPath = null;

        try {
            // 1. Clone repository
            repositoryPath = repositoryManager.prepareRepository(
                    job.getRepositoryUrl(),
                    job.getBranch()
            );

            // 2. Analyze repository
            RepositoryMetrics repositoryMetrics =
                    repositoryAnalyzer.analyze(repositoryPath);

            // 3. Create successful result
            AnalysisResult result = new AnalysisResult();

            result.setJobId(job.getJobId());
            result.setRepositoryId(job.getRepositoryId());
            result.setStatus("SUCCESS");
            result.setRepositoryMetrics(repositoryMetrics);

            return result;

        } catch (Exception e) {

            // 4. Return failed result
            AnalysisResult result = new AnalysisResult();

            result.setJobId(job.getJobId());
            result.setRepositoryId(job.getRepositoryId());
            result.setStatus("FAILED");
            result.setError(e.getMessage());

            return result;

        } finally {

            // 5. Always remove temporary repository
            repositoryManager.cleanupRepository(repositoryPath);
        }
    }
}