package com.debtlens.analysisservice.messaging;

public class AnalysisResultPublisher {
}
/*AnalysisResultPublisher

After analysis:

AnalysisService
      ↓
AnalysisResultPublisher
      ↓
RabbitMQ
      ↓
Application Backend

It publishes:

Analysis job ID
Repository information
Class metrics
Method metrics
Git metrics
Comments
Analysis status
Errors if applicable*/