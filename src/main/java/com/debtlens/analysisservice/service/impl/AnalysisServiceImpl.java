package com.debtlens.analysisservice.service.impl;

import com.debtlens.analysisservice.analyzer.RepositoryAnalyzer;
import com.debtlens.analysisservice.dto.AnalysisJobMessage;
import com.debtlens.analysisservice.dto.AnalysisResult;
import com.debtlens.analysisservice.service.AnalysisService;
import org.springframework.stereotype.Service;

@Service
public class AnalysisServiceImpl implements AnalysisService {

    private final RepositoryAnalyzer repositoryAnalyzer;

    public AnalysisServiceImpl(RepositoryAnalyzer repositoryAnalyzer) {
        this.repositoryAnalyzer = repositoryAnalyzer;
    }

    @Override
    public AnalysisResult analyze(AnalysisJobMessage job) {
        return null;
    }
}