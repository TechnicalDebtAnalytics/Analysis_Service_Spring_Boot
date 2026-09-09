package com.debtlens.analysisservice.metrics;

import lombok.Data;

@Data
public class ClassMetrics {

    private String className;
    private String filePath;

    private int startLine;
    private int endLine;

    private int dit;    //Depth of Inheritance Tree
    private int cbo;   //Coupling Between Objects
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

    private int rfc;    //Response For Class
    private double wmc;     //Weighted Methods per Class

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

    private java.util.List<String> comments = new java.util.ArrayList<>();
}