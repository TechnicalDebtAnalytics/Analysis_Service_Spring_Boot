package com.debtlens.analysisservice.metrics;

import lombok.Data;

import java.util.List;

@Data
public class RepositoryMetrics {
    private String repositoryId;
    private String repositoryName;

    private List<ClassMetrics> classMetrics;

    private GitMetrics gitMetrics;
}
