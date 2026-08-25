package com.debtlens.analysisservice.analyzer;

import com.debtlens.analysisservice.metrics.ClassMetrics;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;


@Component
public class JavaParserAnalyzer implements CodeAnalyzer {


    @Override
    public List<ClassMetrics> analyze(Path repositoryPath) {

        List<ClassMetrics> results = new ArrayList<>();

        try (Stream<Path> files = Files.walk(repositoryPath)) {


            files
                    .filter(path -> {
                        String normalized = path.toString().replace('\\', '/');
                        return normalized.endsWith(".java")
                                && !normalized.contains("/.git/")
                                && !normalized.contains("/target/")
                                && !normalized.contains("/build/")
                                && !normalized.contains("/.gradle/")
                                && !normalized.contains("/src/test/");
                    })
                    .forEach(path ->
                            analyzeFile(path, results)
                    );


        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to analyze Java files in: "
                            + repositoryPath,
                    e
            );
        }


        return results;
    }



    private void analyzeFile(
            Path filePath,
            List<ClassMetrics> results
    ) {


        System.out.println(
                "PARSING START: "
                        + filePath
        );


        try {


            CompilationUnit compilationUnit =
                    StaticJavaParser.parse(filePath);


            System.out.println(
                    "PARSING DONE: "
                            + filePath
            );



            compilationUnit
                    .findAll(ClassOrInterfaceDeclaration.class)
                    .forEach(classDeclaration -> {


                        System.out.println(
                                "PROCESSING CLASS: "
                                        + classDeclaration.getNameAsString()
                        );



                        ClassMetrics metrics =
                                new ClassMetrics();



                        // -------------------------
                        // Class information
                        // -------------------------

                        metrics.setClassName(
                                classDeclaration.getNameAsString()
                        );


                        metrics.setFilePath(
                                filePath.toString()
                        );



                        // -------------------------
                        // Line numbers
                        // -------------------------

                        classDeclaration.getBegin()
                                .ifPresent(position ->
                                        metrics.setStartLine(
                                                position.line
                                        )
                                );


                        classDeclaration.getEnd()
                                .ifPresent(position ->
                                        metrics.setEndLine(
                                                position.line
                                        )
                                );



                        if(metrics.getStartLine() > 0 &&
                                metrics.getEndLine() > 0) {


                            metrics.setNumberOfLinesOfCode(
                                    metrics.getEndLine()
                                            -
                                            metrics.getStartLine()
                                            + 1
                            );

                        }




                        // -------------------------
                        // Methods
                        // -------------------------

                        List<MethodDeclaration> methods =
                                classDeclaration.getMethods();



                        metrics.setNumberOfMethods(
                                methods.size()
                        );



                        int publicMethods = 0;
                        int privateMethods = 0;



                        for(MethodDeclaration method : methods){


                            if(method.hasModifier(
                                    Modifier.Keyword.PUBLIC)) {


                                publicMethods++;


                            }
                            else if(method.hasModifier(
                                    Modifier.Keyword.PRIVATE)) {


                                privateMethods++;

                            }

                        }



                        metrics.setNumberOfPublicMethods(
                                publicMethods
                        );


                        metrics.setNumberOfPrivateMethods(
                                privateMethods
                        );





                        // -------------------------
                        // Attributes
                        // -------------------------

                        List<FieldDeclaration> fields =
                                classDeclaration.getFields();



                        int totalAttributes = 0;
                        int publicAttributes = 0;
                        int privateAttributes = 0;



                        for(FieldDeclaration field : fields){


                            int count =
                                    field.getVariables().size();


                            totalAttributes += count;



                            if(field.hasModifier(
                                    Modifier.Keyword.PUBLIC)){


                                publicAttributes += count;


                            }
                            else if(field.hasModifier(
                                    Modifier.Keyword.PRIVATE)){


                                privateAttributes += count;

                            }

                        }




                        metrics.setNumberOfAttributes(
                                totalAttributes
                        );


                        metrics.setNumberOfPublicAttributes(
                                publicAttributes
                        );


                        metrics.setNumberOfPrivateAttributes(
                                privateAttributes
                        );

                        // -------------------------
                        // Comments extraction
                        // -------------------------
                        List<String> extractedComments = new ArrayList<>();

                        // 1. Comments inside the class body
                        classDeclaration.getAllContainedComments().forEach(comment -> {
                            String content = comment.getContent();
                            if (content != null && !content.trim().isBlank()) {
                                extractedComments.add(content.trim());
                            }
                        });

                        // 2. Class declaration comment / Javadoc header
                        classDeclaration.getComment().ifPresent(comment -> {
                            String content = comment.getContent();
                            if (content != null && !content.trim().isBlank() && !extractedComments.contains(content.trim())) {
                                extractedComments.add(0, content.trim());
                            }
                        });

                        // 3. Entire file comments (if single class file)
                        compilationUnit.getAllContainedComments().forEach(comment -> {
                            String content = comment.getContent();
                            if (content != null && !content.trim().isBlank() && !extractedComments.contains(content.trim())) {
                                extractedComments.add(content.trim());
                            }
                        });

                        metrics.setComments(extractedComments);

                        results.add(metrics);



                        System.out.println(
                                "CLASS FINISHED: "
                                        + metrics.getClassName()
                        );


                    });



        }
        catch(Exception e){


            throw new RuntimeException(
                    "Failed to parse Java file: "
                            + filePath,
                    e
            );

        }

    }




}