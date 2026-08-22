package com.debtlens.analysisservice.analyzer;

import com.debtlens.analysisservice.metrics.ClassMetrics;
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

        // =====================================================
        // 1. JavaParser analysis
        // =====================================================

        System.out.println("===== JavaParser START =====");

        List<ClassMetrics> javaParserMetrics =
                javaParserAnalyzer.analyze(repositoryPath);

        System.out.println(
                "===== JavaParser END - Classes: "
                        + javaParserMetrics.size()
                        + " ====="
        );


        // =====================================================
        // 2. CK analysis
        // =====================================================

        System.out.println("===== CK START =====");

        List<ClassMetrics> ckMetrics =
                ckAnalyzer.analyze(repositoryPath);

        System.out.println(
                "===== CK END - Classes: "
                        + ckMetrics.size()
                        + " ====="
        );


        // =====================================================
        // 3. Use JavaParser results as the base
        // =====================================================

        Map<String, ClassMetrics> classMetricsMap =
                new HashMap<>();

        for (ClassMetrics metrics : javaParserMetrics) {

            String key = createKey(
                    metrics.getFilePath()
            );

            classMetricsMap.put(key, metrics);
        }


        // =====================================================
        // 4. Merge CK metrics into JavaParser metrics
        // =====================================================

        for (ClassMetrics ckMetric : ckMetrics) {

            String key = createKey(
                    ckMetric.getFilePath()
            );

            ClassMetrics existing =
                    classMetricsMap.get(key);

            if (existing != null) {

                existing.setLoc(
                        ckMetric.getLoc()
                );

                existing.setCbo(
                        ckMetric.getCbo()
                );

                existing.setWmc(
                        ckMetric.getWmc()
                );

                existing.setDit(
                        ckMetric.getDit()
                );

                existing.setRfc(
                        ckMetric.getRfc()
                );

                existing.setLcom(
                        ckMetric.getLcom()
                );

                existing.setNoc(
                        ckMetric.getNoc()
                );

                existing.setFanin(
                        ckMetric.getFanin()
                );

                existing.setFanout(
                        ckMetric.getFanout()
                );

                existing.setNumberOfMethods(
                        ckMetric.getNumberOfMethods()
                );

                existing.setNumberOfAttributes(
                        ckMetric.getNumberOfAttributes()
                );

            } else {

                /*
                 * If CK found a class that JavaParser
                 * did not find, keep the CK result.
                 */
                classMetricsMap.put(
                        key,
                        ckMetric
                );
            }
        }


        // =====================================================
        // 5. JGit file-level metrics
        // =====================================================

        System.out.println("===== JGit START =====");

        List<ClassMetrics> finalClassMetrics =
                List.copyOf(classMetricsMap.values());

        jGitAnalyzer.enrichGitMetrics(
                repositoryPath,
                finalClassMetrics
        );

        System.out.println("===== JGit END =====");


        // =====================================================
        // 6. Build RepositoryMetrics
        // =====================================================

        RepositoryMetrics repositoryMetrics =
                new RepositoryMetrics();

        repositoryMetrics.setRepositoryId(
                repositoryPath.toAbsolutePath().toString()
        );

        repositoryMetrics.setRepositoryName(
                repositoryPath.getFileName().toString()
        );

        repositoryMetrics.setClassMetrics(
                finalClassMetrics
        );

        return repositoryMetrics;
    }

    private String createKey(String filePath) {

        return Path.of(filePath)
                .toAbsolutePath()
                .normalize()
                .toString();
    }
}