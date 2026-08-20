package com.debtlens.analysisservice.service;

import com.debtlens.analysisservice.dto.AnalysisJobMessage;
import com.debtlens.analysisservice.dto.AnalysisResult;

public interface AnalysisService {

    AnalysisResult analyze(AnalysisJobMessage job);
}