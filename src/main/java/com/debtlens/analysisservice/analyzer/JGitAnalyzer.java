package com.debtlens.analysisservice.analyzer;

import com.debtlens.analysisservice.metrics.GitMetrics;
import org.eclipse.jgit.internal.storage.file.Pack;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class JGitAnalyzer implements GitAnalyzer{
    @Override
    public GitMetrics analyze(Path repositoryPath){
        return new GitMetrics();
    }

}
