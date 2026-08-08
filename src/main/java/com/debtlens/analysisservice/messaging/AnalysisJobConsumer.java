package com.debtlens.analysisservice.messaging;

public class AnalysisJobConsumer {
}
/*AnalysisJobConsumer

Consumes:

Application Backend
       ↓
RabbitMQ
       ↓
AnalysisJobConsumer

It receives something like:

analysisJobId
repositoryId
repositoryUrl
branch

Then it calls:

AnalysisService

It should not contain the actual analysis logic.*/