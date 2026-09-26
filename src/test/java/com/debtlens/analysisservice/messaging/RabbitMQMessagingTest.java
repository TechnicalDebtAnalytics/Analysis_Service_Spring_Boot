package com.debtlens.analysisservice.messaging;

import com.debtlens.analysisservice.config.RabbitMQConfig;
import com.debtlens.analysisservice.dto.AnalysisJobMessage;
import com.debtlens.analysisservice.dto.AnalysisResult;
import com.debtlens.analysisservice.service.AnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RabbitMQMessagingTest {

    @Test
    void analysisJobConsumer_shouldAnalyzeAndPublishResult() {
        AnalysisService analysisService = mock(AnalysisService.class);
        AnalysisResultPublisher publisher = mock(AnalysisResultPublisher.class);
        AnalysisJobConsumer consumer = new AnalysisJobConsumer(analysisService, publisher);
        AnalysisJobMessage job = new AnalysisJobMessage();
        job.setJobId("100");
        AnalysisResult result = new AnalysisResult();
        result.setJobId("100");
        result.setStatus("SUCCESS");
        when(analysisService.analyze(job)).thenReturn(result);

        consumer.consume(job);

        verify(analysisService).analyze(job);
        verify(publisher).publish(result);
    }

    @Test
    void analysisJobConsumer_shouldPropagateAnalysisFailureWithoutPublishing() {
        AnalysisService analysisService = mock(AnalysisService.class);
        AnalysisResultPublisher publisher = mock(AnalysisResultPublisher.class);
        AnalysisJobConsumer consumer = new AnalysisJobConsumer(analysisService, publisher);
        AnalysisJobMessage job = new AnalysisJobMessage();
        when(analysisService.analyze(job)).thenThrow(new RuntimeException("analysis failed"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> consumer.consume(job));

        assertEquals("analysis failed", exception.getMessage());
        verify(publisher, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void analysisJobConsumer_shouldPropagateResultPublishingFailure() {
        AnalysisService analysisService = mock(AnalysisService.class);
        AnalysisResultPublisher publisher = mock(AnalysisResultPublisher.class);
        AnalysisJobConsumer consumer = new AnalysisJobConsumer(analysisService, publisher);
        AnalysisJobMessage job = new AnalysisJobMessage();
        AnalysisResult result = new AnalysisResult();
        when(analysisService.analyze(job)).thenReturn(result);
        doThrow(new RuntimeException("publish failed")).when(publisher).publish(result);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> consumer.consume(job));

        assertEquals("publish failed", exception.getMessage());
    }

    @Test
    void analysisResultPublisher_shouldPublishToConfiguredQueue() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        AnalysisResultPublisher publisher = new AnalysisResultPublisher(rabbitTemplate);
        AnalysisResult result = new AnalysisResult();
        result.setJobId("100");

        publisher.publish(result);

        verify(rabbitTemplate).convertAndSend(RabbitMQConfig.ANALYSIS_RESULT_QUEUE, result);
    }

    @Test
    void analysisResultPublisher_shouldPropagateRabbitFailure() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        AnalysisResultPublisher publisher = new AnalysisResultPublisher(rabbitTemplate);
        AnalysisResult result = new AnalysisResult();
        doThrow(new AmqpException("RabbitMQ unavailable"))
                .when(rabbitTemplate).convertAndSend(RabbitMQConfig.ANALYSIS_RESULT_QUEUE, result);

        AmqpException exception = assertThrows(AmqpException.class, () -> publisher.publish(result));

        assertEquals("RabbitMQ unavailable", exception.getMessage());
    }
}
