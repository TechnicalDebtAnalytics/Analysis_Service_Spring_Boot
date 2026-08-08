package com.debtlens.analysisservice.repository;

public class RepositoryManager {
}
/*RepositoryManager.java

This is a good class to have.

Its job is managing the local repository workspace.

For example:

RepositoryManager
        │
        ├── cloneRepository()
        ├── checkoutBranch()
        ├── getRepositoryPath()
        └── cleanupRepository()

So:

RabbitMQ Job
      ↓
RepositoryManager
      ↓
Local repository
      ↓
Analyzers

This keeps Git repository management separate from metric extraction.*/