package com.debtlens.analysisservice.repository;

import org.eclipse.jgit.api.Git;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class RepositoryManager {

    public Path prepareRepository(
            String repositoryUrl,
            String branch
    ) {
        try {
            Path repositoryPath = Files.createTempDirectory("analysis-repository-");

            Git.cloneRepository()
                    .setURI(repositoryUrl)
                    .setDirectory(repositoryPath.toFile())
                    .setBranch(branch)
                    .call()
                    .close();

            return repositoryPath;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to prepare repository: " + repositoryUrl,
                    e
            );
        }
    }

    public void cleanupRepository(Path repositoryPath) {
        if (repositoryPath == null) {
            return;
        }

        try {
            Files.walk(repositoryPath)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new RuntimeException(
                                    "Failed to delete: " + path,
                                    e
                            );
                        }
                    });

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to clean repository: " + repositoryPath,
                    e
            );
        }
    }
}