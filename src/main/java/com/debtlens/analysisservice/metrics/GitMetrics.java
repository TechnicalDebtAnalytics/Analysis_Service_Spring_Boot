package com.debtlens.analysisservice.metrics;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GitMetrics {
    private int commitCount;    //total commits related to the repository/file
    private int authorCount;    //number of contributors

    private int linesAdded;     //lines added
    private int linesDeleted;   //lines deleted
    private int churn;          //linesAdded + linesDeleted

    private LocalDateTime lastModified;     //most recent modification time
}
