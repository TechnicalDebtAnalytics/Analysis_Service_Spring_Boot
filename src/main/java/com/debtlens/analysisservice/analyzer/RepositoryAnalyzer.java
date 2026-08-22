package com.debtlens.analysisservice.analyzer;

import com.debtlens.analysisservice.metrics.ClassMetrics;
import com.debtlens.analysisservice.metrics.GitMetrics;
import com.debtlens.analysisservice.metrics.RepositoryMetrics;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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


    public RepositoryMetrics analyze(Path repositoryPath) {

        // Analyze Java source files
        System.out.println("===== JavaParser START =====");

        List<ClassMetrics> javaParserMetrics =
                javaParserAnalyzer.analyze(repositoryPath);

        System.out.println(
                "===== JavaParser END - Classes: "
                        + javaParserMetrics.size()
                        + " ====="
        );


// Analyze CK metrics
        System.out.println("===== CK START =====");

        List<ClassMetrics> ckMetrics =
                ckAnalyzer.analyze(repositoryPath);

        System.out.println(
                "===== CK END - Classes: "
                        + ckMetrics.size()
                        + " ====="
        );


// Analyze Git history
        System.out.println("===== JGit START =====");

        GitMetrics gitMetrics =
                jGitAnalyzer.analyze(repositoryPath);

        System.out.println("===== JGit END =====");

        /*
         * Use JavaParser results as the base.
         * Then enrich each class with CK metrics.
         */
        Map<String, ClassMetrics> classMetricsMap =
                new HashMap<>();

        for (ClassMetrics metrics : javaParserMetrics) {

            String key = createKey(
                    metrics.getClassName(),
                    metrics.getFilePath()
            );

            classMetricsMap.put(key, metrics);
        }

        /*
         * Merge CK metrics into the corresponding
         * JavaParser class metrics.
         */
        for (ClassMetrics ckMetric : ckMetrics) {

            String key = createKey(
                    ckMetric.getClassName(),
                    ckMetric.getFilePath()
            );

            ClassMetrics existing =
                    classMetricsMap.get(key);

            if (existing != null) {

                existing.setLoc(ckMetric.getLoc());
                existing.setCbo(ckMetric.getCbo());
                existing.setWmc(ckMetric.getWmc());
                existing.setDit(ckMetric.getDit());
                existing.setRfc(ckMetric.getRfc());
                existing.setLcom(ckMetric.getLcom());
                existing.setNoc(ckMetric.getNoc());

                existing.setMethodCount(
                        ckMetric.getMethodCount()
                );

                existing.setFieldCount(
                        ckMetric.getFieldCount()
                );

            } else {
                /*
                 * If CK found a class that JavaParser did not,
                 * keep the CK result instead of losing it.
                 */
                classMetricsMap.put(key, ckMetric);
            }
        }

        RepositoryMetrics repositoryMetrics =
                new RepositoryMetrics();

        repositoryMetrics.setRepositoryId(
                repositoryPath.toAbsolutePath().toString()
        );

        repositoryMetrics.setRepositoryName(
                repositoryPath.getFileName().toString()
        );

        repositoryMetrics.setClassMetrics(
                List.copyOf(classMetricsMap.values())
        );

        repositoryMetrics.setGitMetrics(
                gitMetrics
        );

        return repositoryMetrics;
    }

    private String createKey(
            String className,
            String filePath
    ) {
        return className + "::" +
                Path.of(filePath)
                        .toAbsolutePath()
                        .normalize();
    }
}