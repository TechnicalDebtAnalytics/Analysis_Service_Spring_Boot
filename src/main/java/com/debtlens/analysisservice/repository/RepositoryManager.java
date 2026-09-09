package com.debtlens.analysisservice.repository;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.WindowCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

@Component
public class RepositoryManager {

    private static final Logger log = LoggerFactory.getLogger(RepositoryManager.class);

    public Path prepareRepository(
            String repositoryUrl,
            String branch
    ) {
        try {
            Path repositoryPath = Files.createTempDirectory("analysis-repository-");

            Git git = Git.cloneRepository()
                    .setURI(repositoryUrl)
                    .setDirectory(repositoryPath.toFile())
                    .setBranch(branch)
                    .call();

            git.getRepository().close();
            git.close();

            log.info("Cloned repository '{}' (branch '{}') to {}", repositoryUrl, branch, repositoryPath);
            return repositoryPath;

        } catch (Exception e) {
            log.error("Failed to clone repository: {}", repositoryUrl, e);
            throw new RuntimeException(
                    "Failed to prepare repository: " + repositoryUrl,
                    e
            );
        }
    }

    public void cleanupRepository(Path repositoryPath) {
        if (repositoryPath == null || !Files.exists(repositoryPath)) {
            return;
        }

        try {
            // Release memory-mapped file handles on packfiles
            org.eclipse.jgit.storage.file.WindowCacheConfig config = new org.eclipse.jgit.storage.file.WindowCacheConfig();
            org.eclipse.jgit.internal.storage.file.WindowCache.reconfigure(config);
        } catch (Throwable ignored) {
        }

        try (var walk = Files.walk(repositoryPath)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            File file = path.toFile();
                            if (!file.canWrite()) {
                                file.setWritable(true, false);
                            }
                            Files.deleteIfExists(path);
                        } catch (Exception e) {
                            File file = path.toFile();
                            file.setWritable(true, false);
                            boolean deleted = file.delete();
                            if (!deleted) {
                                file.deleteOnExit();
                            }
                        }
                    });
            log.info("Cleaned up temporary repository at {}", repositoryPath);
        } catch (Exception e) {
            log.warn("Notice during cleanup of temp repo {}: {}", repositoryPath, e.getMessage());
        }
    }
}