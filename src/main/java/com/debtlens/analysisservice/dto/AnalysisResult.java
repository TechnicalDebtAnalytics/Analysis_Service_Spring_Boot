package com.debtlens.analysisservice.dto;

import com.debtlens.analysisservice.metrics.RepositoryMetrics;
import lombok.Data;

@Data
public class AnalysisResult {

    private String jobId;
    private String repositoryId;
    private String status;

    private RepositoryMetrics repositoryMetrics;

    private String comments;
    private String error;
}