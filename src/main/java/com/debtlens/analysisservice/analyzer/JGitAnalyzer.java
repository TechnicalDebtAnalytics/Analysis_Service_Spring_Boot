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


        try (var files = java.nio.file.Files.walk(repositoryPath)) {

            files
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> {

                        String relativePath =
                                repositoryPath
                                        .toAbsolutePath()
                                        .normalize()
                                        .relativize(
                                                path.toAbsolutePath()
                                        )
                                        .toString();


                        historyMap.put(
                                normalizePath(relativePath),
                                new FileHistory()
                        );
                    });


        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to scan Java files for Git analysis",
                    e
            );
        }



        try (Repository repository =
                     new FileRepositoryBuilder()
                             .setGitDir(
                                     repositoryPath
                                             .resolve(".git")
                                             .toFile()
                             )
                             .setWorkTree(
                                     repositoryPath.toFile()
                             )
                             .build()) {


            ObjectId head =
                    repository.resolve("HEAD");


            if (head == null) {
                return;
            }


            try (RevWalk revWalk =
                         new RevWalk(repository)) {


                RevCommit headCommit =
                        revWalk.parseCommit(head);


                revWalk.markStart(headCommit);



                for (RevCommit commit : revWalk) {


                    if (commit.getParentCount() == 0) {
                        continue;
                    }


                    RevCommit parent =
                            revWalk.parseCommit(
                                    commit.getParent(0)
                                            .getId()
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
                            .toString();



            FileHistory history =
                    historyMap.get(
                            normalizePath(relativePath)
                    );


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



        try (
                ObjectReader reader =
                        repository.newObjectReader();

                DiffFormatter formatter =
                        new DiffFormatter(
                                new ByteArrayOutputStream()
                        )
        ) {



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



            formatter.setRepository(repository);

            formatter.setDetectRenames(true);



            var entries =
                    formatter.scan(
                            parentTree,
                            commitTree
                    );



            for (var entry : entries) {


                String filePath;


                if (!entry.getNewPath()
                        .equals("/dev/null")) {


                    filePath =
                            entry.getNewPath();


                } else {


                    filePath =
                            entry.getOldPath();

                }



                if (!filePath.endsWith(".java")) {
                    continue;
                }



                FileHistory history =
                        historyMap.get(
                                normalizePath(filePath)
                        );



                if (history == null) {
                    continue;
                }




                int[] changes =
                        calculateLineChanges(
                                repository,
                                entry
                        );



                history.versionCount++;



                if (commit.getAuthorIdent() != null) {


                    history.authors.add(
                            commit.getAuthorIdent()
                                    .getEmailAddress()
                    );
                }



                LocalDateTime commitDate =
                        Instant.ofEpochSecond(
                                        commit.getCommitTime()
                                )
                                .atZone(
                                        ZoneId.systemDefault()
                                )
                                .toLocalDateTime();




                if(history.firstModificationDate == null ||
                        commitDate.isBefore(
                                history.firstModificationDate
                        )) {

                    history.firstModificationDate =
                            commitDate;
                }




                if(history.lastModificationDate == null ||
                        commitDate.isAfter(
                                history.lastModificationDate
                        )) {

                    history.lastModificationDate =
                            commitDate;
                }




                int added =
                        changes[0];


                int removed =
                        changes[1];


                int churn =
                        added + removed;



                history.linesAdded += added;

                history.linesRemoved += removed;

                history.codeChurn += churn;



                history.maxLinesAdded =
                        Math.max(
                                history.maxLinesAdded,
                                added
                        );


                history.maxLinesRemoved =
                        Math.max(
                                history.maxLinesRemoved,
                                removed
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
            org.eclipse.jgit.diff.DiffEntry entry

    ) throws Exception {


        ByteArrayOutputStream output =
                new ByteArrayOutputStream();



        try(DiffFormatter formatter =
                    new DiffFormatter(output)) {


            formatter.setRepository(repository);

            formatter.format(entry);

        }



        int added = 0;

        int removed = 0;



        for(String line :
                output.toString().split("\n")) {


            if(line.startsWith("+")
                    && !line.startsWith("+++")) {

                added++;
            }



            if(line.startsWith("-")
                    && !line.startsWith("---")) {

                removed++;
            }
        }



        return new int[]{
                added,
                removed
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


        metrics.setLinesRemovedUntil(
                history.linesRemoved
        );


        metrics.setCodeChurnUntil(
                history.codeChurn
        );



        metrics.setMaxLinesAddedUntil(
                history.maxLinesAdded
        );


        metrics.setMaxLinesRemovedUntil(
                history.maxLinesRemoved
        );


        metrics.setMaxCodeChurnUntil(
                history.maxCodeChurn
        );



        metrics.setAvgLinesAddedUntil(
                average(
                        history.linesAdded,
                        history.versionCount
                )
        );


        metrics.setAvgLinesRemovedUntil(
                average(
                        history.linesRemoved,
                        history.versionCount
                )
        );


        metrics.setAvgCodeChurnUntil(
                average(
                        history.codeChurn,
                        history.versionCount
                )
        );



        if(history.firstModificationDate != null &&
                history.lastModificationDate != null) {


            long age =
                    java.time.Duration.between(
                                    history.firstModificationDate,
                                    history.lastModificationDate
                            )
                            .toDays();



            metrics.setAgeWithRespectTo(age);


            metrics.setWeightedAgeWithRespectTo(
                    age * history.versionCount
            );


        } else {

            metrics.setAgeWithRespectTo(0);

            metrics.setWeightedAgeWithRespectTo(0);

        }

    }





    private String normalizePath(String path) {

        return path
                .replace("\\","/")
                .replace("./","")
                .trim();
    }





    private double average(
            int total,
            int count
    ) {

        if(count == 0) {
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



        LocalDateTime firstModificationDate;

        LocalDateTime lastModificationDate;

    }

}