package com.debtlens.analysisservice.config;

public class ExecutorConfig {
}
/*ExecutorConfig.java

Useful if you want multiple analysis jobs processed concurrently.

For example:

RabbitMQ
   │
   ├── Job 1 ──→ Worker Thread 1
   ├── Job 2 ──→ Worker Thread 2
   └── Job 3 ──→ Worker Thread 3

Be careful with concurrency because repository analysis can be CPU/memory intensive*/