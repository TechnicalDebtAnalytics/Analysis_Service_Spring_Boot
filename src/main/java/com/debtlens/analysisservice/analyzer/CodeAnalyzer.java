package com.debtlens.analysisservice.analyzer;

import com.debtlens.analysisservice.metrics.ClassMetrics;

import java.nio.file.Path;
import java.util.List;

public interface CodeAnalyzer {
    List<ClassMetrics> analyze(Path repositoryPath);
}
