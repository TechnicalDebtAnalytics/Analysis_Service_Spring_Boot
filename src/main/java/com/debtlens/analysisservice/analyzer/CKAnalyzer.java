package com.debtlens.analysisservice.analyzer;

import com.debtlens.analysisservice.metrics.ClassMetrics;
import com.github.mauricioaniche.ck.CK;
import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.CKNotifier;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class CKAnalyzer implements CodeAnalyzer {

    @Override
    public List<ClassMetrics> analyze(Path repositoryPath) {

        List<ClassMetrics> results = new ArrayList<>();

        CK ck = new CK();

        ck.calculate(repositoryPath, new CKNotifier() {

            @Override
            public void notify(CKClassResult result) {

                ClassMetrics metrics = new ClassMetrics();

                // Basic information
                metrics.setClassName(result.getClassName());
                metrics.setFilePath(result.getFile());

                // CK metrics
                metrics.setLoc(result.getLoc());
                metrics.setCbo(result.getCbo());
                metrics.setWmc(result.getWmc());
                metrics.setDit(result.getDit());
                metrics.setRfc(result.getRfc());
                metrics.setLcom(result.getLcom());
                metrics.setNoc(result.getNoc());

                metrics.setFanin(result.getFanin());
                metrics.setFanout(result.getFanout());

                metrics.setNumberOfMethods(
                        result.getNumberOfMethods()
                );

                metrics.setNumberOfAttributes(
                        result.getNumberOfFields()
                );
                results.add(metrics);
            }

            @Override
            public void notifyError(String sourceFile, Exception e) {
                throw new RuntimeException(
                        "CK analysis failed for: " + sourceFile,
                        e
                );
            }
        });

        return results;
    }
}