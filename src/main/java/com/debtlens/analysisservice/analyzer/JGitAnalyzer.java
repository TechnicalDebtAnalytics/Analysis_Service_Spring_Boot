package com.debtlens.analysisservice.analyzer;

import com.debtlens.analysisservice.metrics.GitMetrics;
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
import java.util.HashSet;
import java.util.Set;

@Component
public class JGitAnalyzer implements GitAnalyzer {

    @Override
    public GitMetrics analyze(Path repositoryPath) {

        GitMetrics metrics = new GitMetrics();

        Set<String> authors = new HashSet<>();

        try (Repository repository = new FileRepositoryBuilder()
                .setGitDir(repositoryPath.resolve(".git").toFile())
                .setWorkTree(repositoryPath.toFile())
                .build()) {

            ObjectId head = repository.resolve("HEAD");

            if (head == null) {
                return metrics;
            }

            try (RevWalk revWalk = new RevWalk(repository)) {

                RevCommit headCommit = revWalk.parseCommit(head);
                revWalk.markStart(headCommit);

                LocalDateTime latestCommitTime = null;

                for (RevCommit commit : revWalk) {

                    // Commit count
                    metrics.setCommitCount(metrics.getCommitCount() + 1);

                    // Unique authors
                    if (commit.getAuthorIdent() != null) {
                        authors.add(commit.getAuthorIdent().getEmailAddress());
                    }

                    // Latest modification time
                    LocalDateTime commitTime = Instant.ofEpochSecond(
                                    commit.getCommitTime()
                            )
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();

                    if (latestCommitTime == null ||
                            commitTime.isAfter(latestCommitTime)) {

                        latestCommitTime = commitTime;
                    }

                    // Calculate added/deleted lines
                    if (commit.getParentCount() > 0) {

                        RevCommit parent = revWalk.parseCommit(
                                commit.getParent(0).getId()
                        );

                        int[] changes = calculateLineChanges(
                                repository,
                                parent,
                                commit
                        );

                        metrics.setLinesAdded(
                                metrics.getLinesAdded() + changes[0]
                        );

                        metrics.setLinesDeleted(
                                metrics.getLinesDeleted() + changes[1]
                        );
                    }
                }

                metrics.setAuthorCount(authors.size());

                if (latestCommitTime != null) {
                    metrics.setLastModified(latestCommitTime);
                }

                metrics.setChurn(
                        metrics.getLinesAdded()
                                + metrics.getLinesDeleted()
                );
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "Git analysis failed for repository: "
                            + repositoryPath,
                    e
            );
        }

        return metrics;
    }

    private int[] calculateLineChanges(
            Repository repository,
            RevCommit parent,
            RevCommit commit) throws Exception {

        try (ObjectReader reader = repository.newObjectReader();
             DiffFormatter diffFormatter =
                     new DiffFormatter(new ByteArrayOutputStream())) {

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

            var entries = diffFormatter.scan(
                    parentTree,
                    commitTree
            );

            int added = 0;
            int deleted = 0;

            for (var entry : entries) {

                try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                     DiffFormatter formatter = new DiffFormatter(output)) {

                    formatter.setRepository(repository);
                    formatter.setDetectRenames(true);

                    formatter.format(entry);

                    String diff = output.toString();

                    for (String line : diff.split("\n")) {

                        if (line.startsWith("+") && !line.startsWith("+++")) {
                            added++;
                        }

                        if (line.startsWith("-") && !line.startsWith("---")) {
                            deleted++;
                        }
                    }
                }
            }

            return new int[]{added, deleted};
        }
    }
}