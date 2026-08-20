package com.debtlens.analysisservice.analyzer;

import com.debtlens.analysisservice.metrics.ClassMetrics;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class JavaParserAnalyzer implements CodeAnalyzer {

    @Override
    public List<ClassMetrics> analyze(Path repositoryPath) {

        List<ClassMetrics> results = new ArrayList<>();

        try (Stream<Path> files = Files.walk(repositoryPath)) {

            files.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> analyzeFile(path, results));

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to analyze Java files in: " + repositoryPath,
                    e
            );
        }

        return results;
    }

    private void analyzeFile(
            Path filePath,
            List<ClassMetrics> results
    ) {
        try {
            CompilationUnit compilationUnit =
                    StaticJavaParser.parse(filePath);

            compilationUnit.findAll(ClassOrInterfaceDeclaration.class)
                    .forEach(classDeclaration -> {

                        ClassMetrics metrics = new ClassMetrics();

                        metrics.setClassName(
                                classDeclaration.getNameAsString()
                        );

                        metrics.setFilePath(
                                filePath.toString()
                        );

                        metrics.setMethodCount(
                                classDeclaration
                                        .findAll(MethodDeclaration.class)
                                        .size()
                        );

                        results.add(metrics);
                    });

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to parse Java file: " + filePath,
                    e
            );
        }
    }
}