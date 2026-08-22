package com.debtlens.analysisservice.analyzer;

import com.debtlens.analysisservice.metrics.ClassMetrics;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
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
                    // Ignore git files
                    .filter(path ->
                            !path.toString().contains("/.git/")
                    )

                    // Ignore build folders
                    .filter(path ->
                            !path.toString().contains("/target/")
                    )

                    .filter(path ->
                            !path.toString().contains("/build/")
                    )

                    // Ignore test source files
                    .filter(path ->
                            !path.toString().contains("/test/")
                    )

                    // Only Java files
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )

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
                        // Complexity
                        // -------------------------

                        int complexity =
                                calculateCyclomaticComplexity(
                                        methods
                                );


                        metrics.setCyclomaticComplexity(
                                complexity
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




    private int calculateCyclomaticComplexity(
            List<MethodDeclaration> methods
    ){


        int totalComplexity = 0;



        for(MethodDeclaration method : methods){


            int methodComplexity = 1;



            methodComplexity +=
                    method.findAll(IfStmt.class).size();


            methodComplexity +=
                    method.findAll(ForStmt.class).size();


            methodComplexity +=
                    method.findAll(ForEachStmt.class).size();


            methodComplexity +=
                    method.findAll(WhileStmt.class).size();


            methodComplexity +=
                    method.findAll(DoStmt.class).size();


            methodComplexity +=
                    method.findAll(CatchClause.class).size();


            methodComplexity +=
                    method.findAll(ConditionalExpr.class).size();



            for(BinaryExpr expression :
                    method.findAll(BinaryExpr.class)){


                if(expression.getOperator()
                        ==
                        BinaryExpr.Operator.AND
                        ||
                        expression.getOperator()
                                ==
                                BinaryExpr.Operator.OR){


                    methodComplexity++;

                }

            }



            totalComplexity += methodComplexity;

        }



        return totalComplexity;

    }

}