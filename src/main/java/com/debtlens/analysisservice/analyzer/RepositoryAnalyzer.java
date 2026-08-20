package com.debtlens.analysisservice.analyzer;

import org.springframework.stereotype.Component;

@Component
public class RepositoryAnalyzer {

    private final JavaParserAnalyzer javaParserAnalyzer;
    private final CKAnalyzer ckAnalyzer;
    private final JGitAnalyzer jGitAnalyzer;

    public RepositoryAnalyzer(
            JavaParserAnalyzer javaParserAnalyzer,
            CKAnalyzer ckAnalyzer,
            JGitAnalyzer jGitAnalyzer
    ) {
        this.javaParserAnalyzer = javaParserAnalyzer;
        this.ckAnalyzer = ckAnalyzer;
        this.jGitAnalyzer = jGitAnalyzer;
    }
}