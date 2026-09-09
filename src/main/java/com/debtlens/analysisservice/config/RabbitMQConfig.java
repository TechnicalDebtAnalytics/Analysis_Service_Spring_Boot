package com.debtlens.analysisservice.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ANALYSIS_JOB_QUEUE =
            "analysis_job_creation.queue";

    public static final String ANALYSIS_RESULT_QUEUE =
            "analysis_job_results.queue";

    @Bean
    public Queue analysisJobQueue() {
        return new Queue(
                ANALYSIS_JOB_QUEUE,
                true
        );
    }

    @Bean
    public Queue analysisResultQueue() {
        return new Queue(
                ANALYSIS_RESULT_QUEUE,
                true
        );
    }

    @Bean
    public MessageConverter messageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        return converter;
    }
}