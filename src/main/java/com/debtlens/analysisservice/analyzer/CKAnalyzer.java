package com.debtlens.analysisservice.analyzer;

import com.debtlens.analysisservice.metrics.ClassMetrics;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public class CKAnalyzer implements CodeAnalyzer {

    @Override
    public List<ClassMetrics> analyze(Path repositoryPath) {
        return List.of();
    }
}