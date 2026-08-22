package com.debtlens.analysisservice.metrics;

import lombok.Data;

@Data
public class ClassMetrics {

    private String className;
    private String filePath;

    private int startLine;
    private int endLine;

    private int loc;
    private double cyclomaticComplexity;

    private int dit;
    private int cbo;
    private int fanin;
    private int fanout;
    private double lcom;
    private int noc;

    private int numberOfAttributes;
    private int numberOfLinesOfCode;
    private int numberOfMethods;
    private int numberOfPrivateAttributes;
    private int numberOfPrivateMethods;
    private int numberOfPublicAttributes;
    private int numberOfPublicMethods;

    private int rfc;
    private double wmc;

    // Git history metrics
    private int numberOfVersionsUntil;
    private int numberOfAuthorsUntil;

    private int linesAddedUntil;
    private int maxLinesAddedUntil;
    private double avgLinesAddedUntil;

    private int linesRemovedUntil;
    private int maxLinesRemovedUntil;
    private double avgLinesRemovedUntil;

    private int codeChurnUntil;
    private int maxCodeChurnUntil;
    private double avgCodeChurnUntil;

    private double ageWithRespectTo;
    private double weightedAgeWithRespectTo;
}