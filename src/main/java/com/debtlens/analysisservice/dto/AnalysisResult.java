package com.debtlens.analysisservice.dto;

import com.debtlens.analysisservice.metrics.ClassMetrics;
import com.debtlens.analysisservice.metrics.GitMetrics;
import com.debtlens.analysisservice.metrics.RepositoryMetrics;
import lombok.Data;

@Data
public class AnalysisResult {

    private String jobId;
    private String repositoryId;
    private String status;
    private ClassMetrics classMetrics;
    private RepositoryMetrics repositoryMetrics;
    private GitMetrics gitMetrics;
    private String comments;
    private String error;
}