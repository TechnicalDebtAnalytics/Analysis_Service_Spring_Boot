package com.debtlens.analysisservice.metrics;

import lombok.Data;

@Data
public class ClassMetrics {
    private String className;   //name of the Java class
    private String packageName; //package containing the class
    private String filePath;    //source file location

    private int loc;    //Lines of Code

    private int cbo;    //Coupling Between Objects
    private int wmc;    //Weighted Methods per Class
    private int dit;    //Depth of Inheritance Tree
    private int rfc;    //Response For a Class
    private int lcom;   //Lack of Cohesion of Methods
    private int noc;    //Number of Children

    private int methodCount;    //number of methods
    private int fieldCount;     //number of fields
}

