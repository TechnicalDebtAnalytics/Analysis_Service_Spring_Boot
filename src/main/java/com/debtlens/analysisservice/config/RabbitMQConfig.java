package com.debtlens.analysisservice.config;

public class RabbitMQConfig {
}
/*RabbitMQConfig.java

Defines the queues/exchanges/listeners used by this service.

This service only needs the queues relevant to it:

Analysis Job Queue
Analysis Result Queue

It doesn't need to configure the ML queues because those belong to the Application Backend ↔ ML service communication.*/