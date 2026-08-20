package com.debtlens.analysisservice.messaging;

import com.debtlens.analysisservice.config.RabbitMQConfig;
import com.debtlens.analysisservice.dto.AnalysisResult;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnalysisResultPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(AnalysisResult result) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ANALYSIS_RESULT_QUEUE,
                result
        );
    }
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