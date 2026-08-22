package com.debtlens.analysisservice.analyzer;

import com.debtlens.analysisservice.metrics.ClassMetrics;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class JGitAnalyzer {

    public void enrichGitMetrics(
            Path repositoryPath,
            List<ClassMetrics> classMetrics
    ) {

        Map<String, FileHistory> historyMap =
                new HashMap<>();

        for (ClassMetrics classMetric : classMetrics) {

            Path filePath =
                    Path.of(classMetric.getFilePath())
                            .toAbsolutePath()
                            .normalize();

            String relativePath =
                    repositoryPath
                            .toAbsolutePath()
                            .normalize()
                            .relativize(filePath)
                            .toString()
                            .replace("\\", "/");

            historyMap.put(
                    relativePath,
                    new FileHistory()
            );
        }

        try (Repository repository = new FileRepositoryBuilder()
                .setGitDir(
                        repositoryPath.resolve(".git").toFile()
                )
                .setWorkTree(
                        repositoryPath.toFile()
                )
                .build()) {

            ObjectId head = repository.resolve("HEAD");

            if (head == null) {
                return;
            }

            try (RevWalk revWalk = new RevWalk(repository)) {

                RevCommit headCommit =
                        revWalk.parseCommit(head);

                revWalk.markStart(headCommit);

                for (RevCommit commit : revWalk) {

                    if (commit.getParentCount() == 0) {
                        continue;
                    }

                    RevCommit parent =
                            revWalk.parseCommit(
                                    commit.getParent(0).getId()
                            );

                    processCommit(
                            repository,
                            parent,
                            commit,
                            historyMap
                    );
                }
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Git analysis failed for repository: "
                            + repositoryPath,
                    e
            );
        }

        /*
         * Copy the calculated Git history
         * into each ClassMetrics object.
         */
        for (ClassMetrics classMetric : classMetrics) {

            Path filePath =
                    Path.of(classMetric.getFilePath())
                            .toAbsolutePath()
                            .normalize();

            String relativePath =
                    repositoryPath
                            .toAbsolutePath()
                            .normalize()
                            .relativize(filePath)
                            .toString()
                            .replace("\\", "/");

            FileHistory history =
                    historyMap.get(relativePath);

            if (history != null) {
                applyMetrics(
                        classMetric,
                        history
                );
            }
        }
    }

    private void processCommit(
            Repository repository,
            RevCommit parent,
            RevCommit commit,
            Map<String, FileHistory> historyMap
    ) throws Exception {

        try (ObjectReader reader =
                     repository.newObjectReader();
             DiffFormatter diffFormatter =
                     new DiffFormatter(
                             new ByteArrayOutputStream()
                     )) {

            CanonicalTreeParser parentTree =
                    new CanonicalTreeParser();

            parentTree.reset(
                    reader,
                    parent.getTree()
            );

            CanonicalTreeParser commitTree =
                    new CanonicalTreeParser();

            commitTree.reset(
                    reader,
                    commit.getTree()
            );

            diffFormatter.setRepository(repository);
            diffFormatter.setDetectRenames(true);

            var entries =
                    diffFormatter.scan(
                            parentTree,
                            commitTree
                    );

            for (var entry : entries) {

                String filePath =
                        entry.getNewPath();

                /*
                 * Ignore deleted files because
                 * there is no current file to attach
                 * the metrics to.
                 */
                if ("/dev/null".equals(filePath)) {
                    continue;
                }

                /*
                 * We currently analyze Java files.
                 */
                if (!filePath.endsWith(".java")) {
                    continue;
                }

                FileHistory history =
                        historyMap.get(filePath);

                if (history == null) {
                    continue;
                }

                int[] changes =
                        calculateLineChanges(
                                repository,
                                parentTree,
                                commitTree,
                                entry
                        );

                history.versionCount++;

                if (commit.getAuthorIdent() != null) {

                    history.authors.add(
                            commit.getAuthorIdent()
                                    .getEmailAddress()
                    );
                }

                int added = changes[0];
                int deleted = changes[1];

                int churn =
                        added + deleted;

                history.linesAdded += added;
                history.linesRemoved += deleted;
                history.codeChurn += churn;

                history.maxLinesAdded =
                        Math.max(
                                history.maxLinesAdded,
                                added
                        );

                history.maxLinesRemoved =
                        Math.max(
                                history.maxLinesRemoved,
                                deleted
                        );

                history.maxCodeChurn =
                        Math.max(
                                history.maxCodeChurn,
                                churn
                        );
            }
        }
    }

    private int[] calculateLineChanges(
            Repository repository,
            CanonicalTreeParser parentTree,
            CanonicalTreeParser commitTree,
            org.eclipse.jgit.diff.DiffEntry entry
    ) throws Exception {

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        try (DiffFormatter formatter =
                     new DiffFormatter(output)) {

            formatter.setRepository(repository);
            formatter.setDetectRenames(true);

            formatter.format(entry);
        }

        String diff =
                output.toString();

        int added = 0;
        int deleted = 0;

        for (String line : diff.split("\n")) {

            if (line.startsWith("+")
                    && !line.startsWith("+++")) {

                added++;
            }

            if (line.startsWith("-")
                    && !line.startsWith("---")) {

                deleted++;
            }
        }

        return new int[]{
                added,
                deleted
        };
    }

    private void applyMetrics(
            ClassMetrics metrics,
            FileHistory history
    ) {

        metrics.setNumberOfVersionsUntil(
                history.versionCount
        );

        metrics.setNumberOfAuthorsUntil(
                history.authors.size()
        );

        metrics.setLinesAddedUntil(
                history.linesAdded
        );

        metrics.setMaxLinesAddedUntil(
                history.maxLinesAdded
        );

        metrics.setAvgLinesAddedUntil(
                average(
                        history.linesAdded,
                        history.versionCount
                )
        );

        metrics.setLinesRemovedUntil(
                history.linesRemoved
        );

        metrics.setMaxLinesRemovedUntil(
                history.maxLinesRemoved
        );

        metrics.setAvgLinesRemovedUntil(
                average(
                        history.linesRemoved,
                        history.versionCount
                )
        );

        metrics.setCodeChurnUntil(
                history.codeChurn
        );

        metrics.setMaxCodeChurnUntil(
                history.maxCodeChurn
        );

        metrics.setAvgCodeChurnUntil(
                average(
                        history.codeChurn,
                        history.versionCount
                )
        );

        /*
         * We will implement these after
         * confirming the exact formula.
         */
        metrics.setAgeWithRespectTo(0);
        metrics.setWeightedAgeWithRespectTo(0);
    }

    private double average(
            int total,
            int count
    ) {

        if (count == 0) {
            return 0.0;
        }

        return (double) total / count;
    }

    private static class FileHistory {

        int versionCount;

        Set<String> authors =
                new HashSet<>();

        int linesAdded;
        int maxLinesAdded;

        int linesRemoved;
        int maxLinesRemoved;

        int codeChurn;
        int maxCodeChurn;
    }
}