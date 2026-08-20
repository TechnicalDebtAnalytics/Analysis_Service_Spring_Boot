package com.debtlens.analysisservice.messaging;

import com.debtlens.analysisservice.config.RabbitMQConfig;
import com.debtlens.analysisservice.dto.AnalysisJobMessage;
import com.debtlens.analysisservice.dto.AnalysisResult;
import com.debtlens.analysisservice.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnalysisJobConsumer {

    private final AnalysisService analysisService;
    private final AnalysisResultPublisher analysisResultPublisher;

    @RabbitListener(queues = RabbitMQConfig.ANALYSIS_JOB_QUEUE)
    public void consume(AnalysisJobMessage job) {

        AnalysisResult result = analysisService.analyze(job);

        analysisResultPublisher.publish(result);
    }
}
