package com.debtlens.analysisservice.repository;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryManagerTest {

    @TempDir
    Path tempDir;

    private final RepositoryManager repositoryManager = new RepositoryManager();

    @Test
    void prepareRepository_shouldCloneRequestedBranchFromLocalRepository() throws Exception {
        Path sourceRepository = tempDir.resolve("source-repository");
        Files.createDirectories(sourceRepository);

        Path file = sourceRepository.resolve("README.txt");

        try (Git git = Git.init().setDirectory(sourceRepository.toFile()).call()) {
            writeAndCommit(
                    git,
                    sourceRepository,
                    file,
                    "main branch content\n",
                    "Main Author",
                    "main@example.com",
                    Instant.parse("2024-01-01T00:00:00Z"),
                    "main commit"
            );

            git.branchCreate().setName("feature").call();
            git.checkout().setName("feature").call();

            writeAndCommit(
                    git,
                    sourceRepository,
                    file,
                    "feature branch content\n",
                    "Feature Author",
                    "feature@example.com",
                    Instant.parse("2024-01-02T00:00:00Z"),
                    "feature commit"
            );
        }

        Path clonedRepository = repositoryManager.prepareRepository(sourceRepository.toUri().toString(), "feature");

        try {
            assertTrue(Files.exists(clonedRepository));
            assertTrue(Files.exists(clonedRepository.resolve(".git")));
            assertTrue(Files.readString(clonedRepository.resolve("README.txt"), StandardCharsets.UTF_8).contains("feature branch content"));

            try (Git clonedGit = Git.open(clonedRepository.toFile())) {
                assertEquals("feature", clonedGit.getRepository().getBranch());
            }
        } finally {
            repositoryManager.cleanupRepository(clonedRepository);
        }

        assertFalse(Files.exists(clonedRepository));
    }

    @Test
    void prepareRepository_shouldThrowForInvalidRepositoryLocation() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> repositoryManager.prepareRepository("file:///definitely/does/not/exist.git", "main")
        );

        assertTrue(exception.getMessage().contains("Failed to prepare repository"));
    }

    @Test
    void cleanupRepository_shouldRemoveDirectoryTree() throws Exception {
        Path repositoryPath = tempDir.resolve("cleanup-target");
        Files.createDirectories(repositoryPath.resolve("nested"));
        Files.writeString(repositoryPath.resolve("nested/file.txt"), "temporary", StandardCharsets.UTF_8);

        assertTrue(Files.exists(repositoryPath));

        repositoryManager.cleanupRepository(repositoryPath);

        assertFalse(Files.exists(repositoryPath));
    }

    private void writeAndCommit(
            Git git,
            Path repositoryRoot,
            Path file,
            String content,
            String authorName,
            String authorEmail,
            Instant when,
            String message
    ) throws Exception {
        Files.writeString(file, content, StandardCharsets.UTF_8);

        String relativePath = repositoryRoot.relativize(file).toString().replace('\\', '/');
        git.add().addFilepattern(relativePath).call();

        PersonIdent ident = new PersonIdent(
            authorName,
            authorEmail,
            when,
            ZoneOffset.UTC
        );

        git.commit()
                .setMessage(message)
                .setAuthor(ident)
                .setCommitter(ident)
                .call();
    }
}