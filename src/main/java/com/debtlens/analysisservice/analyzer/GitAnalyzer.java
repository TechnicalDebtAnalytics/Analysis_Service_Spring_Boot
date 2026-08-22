package com.debtlens.analysisservice.analyzer;

import com.debtlens.analysisservice.metrics.GitMetrics;

import java.nio.file.Path;

public interface GitAnalyzer {
    GitMetrics analyze(Path repositoryPath);
}
