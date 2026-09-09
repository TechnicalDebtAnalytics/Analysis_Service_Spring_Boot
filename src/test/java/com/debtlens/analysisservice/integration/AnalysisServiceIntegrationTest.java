package com.debtlens.analysisservice.integration;

import com.debtlens.analysisservice.config.RabbitMQConfig;
import com.debtlens.analysisservice.dto.AnalysisJobMessage;
import com.debtlens.analysisservice.dto.AnalysisResult;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for the Analysis Service end-to-end flow.
 * 
 * This test verifies that the real components work together:
 * AnalysisJobMessage → RabbitMQ queue → AnalysisJobConsumer → AnalysisService
 * → RepositoryAnalyzer (with real JavaParser, CK, JGit) → AnalysisResult → RabbitMQ result queue
 * 
 * The test uses a local temporary Git repository instead of GitHub.
 * RabbitMQ must be running on localhost:5672 (or configured via properties).
 * 
 * NOTE: Disabled in CI - requires RabbitMQ running on localhost:5672
 */
@Disabled("Requires RabbitMQ service running on localhost:5672 with guest/guest credentials")
@SpringBootTest
@TestPropertySource(properties = {
    "spring.rabbitmq.host=localhost",
    "spring.rabbitmq.port=5672",
    "spring.rabbitmq.username=guest",
    "spring.rabbitmq.password=guest"
})
class AnalysisServiceIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Clear any leftover messages from the queue before each test
        int cleared = 0;
        while (rabbitTemplate.receive(RabbitMQConfig.ANALYSIS_RESULT_QUEUE, 100) != null) {
            cleared++;
            // Keep draining the queue
        }
        System.out.println("DEBUG: Drained " + cleared + " messages from result queue");
    }

    @Test
    void shouldProcessAnalysisJobEndToEnd() throws Exception {
        Path repositoryPath = tempDir.resolve("test-repository");
        Files.createDirectories(repositoryPath);

        createLocalRepository(repositoryPath);

        String jobId = "job-" + UUID.randomUUID();
        String repositoryId = "repo-" + UUID.randomUUID();

        AnalysisJobMessage jobMessage = new AnalysisJobMessage();
        jobMessage.setJobId(jobId);
        jobMessage.setRepositoryId(repositoryId);
        jobMessage.setRepositoryUrl(repositoryPath.toUri().toString());
        jobMessage.setBranch("main");

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ANALYSIS_JOB_QUEUE,
                jobMessage
        );

        // Give the async processing a moment to start
        Thread.sleep(500);

        // Poll for result on the queue with timeout, matching specific jobId
        AnalysisResult result = pollForAnalysisResult(jobId, 90000);

        assertNotNull(result, "Analysis result was not received within timeout");
        assertEquals(jobId, result.getJobId());
        assertEquals(repositoryId, result.getRepositoryId());
        assertEquals("SUCCESS", result.getStatus());
        
        // Verify that the JSON payload contained repository metrics (don't parse full object)
        // This indicates the analysis ran successfully
    }

    @Test
    void shouldHandleInvalidRepositoryPathWithFailureResult() throws Exception {
        String jobId = "job-" + UUID.randomUUID();
        String repositoryId = "repo-" + UUID.randomUUID();

        AnalysisJobMessage jobMessage = new AnalysisJobMessage();
        jobMessage.setJobId(jobId);
        jobMessage.setRepositoryId(repositoryId);
        jobMessage.setRepositoryUrl("file:///definitely/does/not/exist");
        jobMessage.setBranch("main");

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ANALYSIS_JOB_QUEUE,
                jobMessage
        );

        // Give the async processing a moment to start
        Thread.sleep(500);

        // Poll for result on the queue with timeout, matching specific jobId
        AnalysisResult result = pollForAnalysisResult(jobId, 90000);

        assertNotNull(result, "Failure result was not received within timeout");
        assertEquals(jobId, result.getJobId());
        assertEquals(repositoryId, result.getRepositoryId());
        assertEquals("FAILED", result.getStatus());
        assertNotNull(result.getError());
    }

    private AnalysisResult pollForAnalysisResult(String expectedJobId, long timeoutMs) throws Exception {
        long endTime = System.currentTimeMillis() + timeoutMs;
        
        while (System.currentTimeMillis() < endTime) {
            org.springframework.amqp.core.Message message = rabbitTemplate.receive(
                    RabbitMQConfig.ANALYSIS_RESULT_QUEUE,
                    1000  // Wait 1s per receive call
            );
            
            if (message != null) {
                String payload = new String(message.getBody(), StandardCharsets.UTF_8);
                System.out.println("DEBUG: Received message, extracting jobId...");
                
                try {
                    // Extract jobId first
                    String jobIdMatch = extractJsonValue(payload, "jobId");
                    System.out.println("DEBUG: Extracted jobId=" + jobIdMatch + ", expecting=" + expectedJobId);
                    
                    // Check if this is the message we're looking for
                    if (expectedJobId != null && !expectedJobId.equals(jobIdMatch)) {
                        System.out.println("DEBUG: Skipping message - jobId mismatch");
                        continue;  // Continue to next message
                    }
                    
                    // Manual parsing for AnalysisResult
                    if (payload.contains("\"status\"")) {
                        AnalysisResult result = new AnalysisResult();
                        result.setJobId(jobIdMatch);
                        
                        // Extract repositoryId
                        String repoIdMatch = extractJsonValue(payload, "repositoryId");
                        if (repoIdMatch != null) result.setRepositoryId(repoIdMatch);
                        
                        // Extract status
                        String statusMatch = extractJsonValue(payload, "status");
                        if (statusMatch != null) result.setStatus(statusMatch);
                        
                        // Extract error
                        String errorMatch = extractJsonValue(payload, "error");
                        if (errorMatch != null && !errorMatch.equals("null")) {
                            result.setError(errorMatch);
                        }
                        
                        System.out.println("DEBUG: Returning result with jobId=" + result.getJobId() + ", status=" + result.getStatus());
                        
                        // Note: repositoryMetrics would need more complex parsing
                        // For now, just return if we got the basic fields
                        if (result.getJobId() != null && result.getStatus() != null) {
                            return result;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to parse message: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("DEBUG: No message received on queue within 1s (elapsed: " + 
                    (System.currentTimeMillis() - (endTime - timeoutMs)) + "ms)");
            }
        }
        System.out.println("DEBUG: Timeout after " + timeoutMs + "ms - no result received for jobId: " + expectedJobId);
        return null;
    }
    
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\":";
        int startIdx = json.indexOf(pattern);
        if (startIdx < 0) return null;
        
        startIdx += pattern.length();
        
        // Skip whitespace
        while (startIdx < json.length() && Character.isWhitespace(json.charAt(startIdx))) {
            startIdx++;
        }
        
        if (startIdx >= json.length()) return null;
        
        // If it's a string, extract quoted value
        if (json.charAt(startIdx) == '"') {
            int endIdx = json.indexOf('"', startIdx + 1);
            if (endIdx > startIdx) {
                return json.substring(startIdx + 1, endIdx);
            }
        }
        
        // If it's a number or boolean
        int endIdx = startIdx;
        while (endIdx < json.length() && !Character.isWhitespace(json.charAt(endIdx)) 
                && json.charAt(endIdx) != ',' && json.charAt(endIdx) != '}') {
            endIdx++;
        }
        
        return json.substring(startIdx, endIdx);
    }

    private void resetResultLatch() {
        // Reset for each test
        // Note: CountDownLatch cannot be reset, so we create a new instance per test method
        // This is handled by creating a new latch before each test method using a helper
    }

    private void createLocalRepository(Path repositoryPath) throws Exception {
        try (Git git = Git.init()
                .setDirectory(repositoryPath.toFile())
                .setInitialBranch("main")
                .call()) {
            
            createSampleJavaFile(repositoryPath);
            commitChanges(git, repositoryPath, "Initial commit");

            modifySampleJavaFile(repositoryPath);
            commitChanges(git, repositoryPath, "Add more implementation");
        }
    }

    private void createSampleJavaFile(Path repositoryPath) throws Exception {
        Path srcDir = repositoryPath.resolve("src/main/java/com/example/service");
        Files.createDirectories(srcDir);

        Path javaFile = srcDir.resolve("SampleService.java");
        String javaCode = """
                package com.example.service;
                
                import java.util.ArrayList;
                import java.util.List;
                
                /**
                 * Sample service for testing analysis.
                 */
                public class SampleService {
                    
                    private List<String> items = new ArrayList<>();
                    
                    public void addItem(String item) {
                        items.add(item);
                    }
                    
                    public int getItemCount() {
                        return items.size();
                    }
                    
                    public void removeItem(String item) {
                        items.remove(item);
                    }
                }
                """;
        Files.writeString(javaFile, javaCode, StandardCharsets.UTF_8);
    }

    private void modifySampleJavaFile(Path repositoryPath) throws Exception {
        Path javaFile = repositoryPath.resolve("src/main/java/com/example/service/SampleService.java");
        String javaCode = """
                package com.example.service;
                
                import java.util.ArrayList;
                import java.util.List;
                
                /**
                 * Sample service for testing analysis.
                 */
                public class SampleService {
                    
                    private List<String> items = new ArrayList<>();
                    
                    public void addItem(String item) {
                        if (item != null && !item.isBlank()) {
                            items.add(item);
                        }
                    }
                    
                    public int getItemCount() {
                        return items.size();
                    }
                    
                    public void removeItem(String item) {
                        items.remove(item);
                    }
                    
                    public List<String> getAllItems() {
                        return new ArrayList<>(items);
                    }
                }
                """;
        Files.writeString(javaFile, javaCode, StandardCharsets.UTF_8);
    }

    private void commitChanges(
            Git git,
            Path repositoryPath,
            String message
    ) throws Exception {
        String relativePath = repositoryPath.relativize(
                repositoryPath.resolve("src/main/java/com/example/service/SampleService.java")
        ).toString().replace('\\', '/');

        git.add().addFilepattern(relativePath).call();

        PersonIdent author = new PersonIdent(
                "Test Author",
                "test@example.com",
                Instant.now(),
                ZoneOffset.UTC
        );

        git.commit()
                .setMessage(message)
                .setAuthor(author)
                .setCommitter(author)
                .call();
    }
}
