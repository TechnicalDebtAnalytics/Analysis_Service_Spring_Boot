package com.debtlens.analysisservice.dto;

import lombok.Data;

@Data
public class AnalysisJobMessage {

    private String jobId;
    private String repositoryId;
    private String repositoryUrl;
    private String branch;
}