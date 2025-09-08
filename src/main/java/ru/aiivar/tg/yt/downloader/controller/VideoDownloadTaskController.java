package ru.aiivar.tg.yt.downloader.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.aiivar.tg.yt.downloader.entity.VideoDownloadTask;
import ru.aiivar.tg.yt.downloader.entity.VideoDownloadTaskResult;
import ru.aiivar.tg.yt.downloader.entity.enums.DestinationType;
import ru.aiivar.tg.yt.downloader.entity.enums.SourceType;
import ru.aiivar.tg.yt.downloader.entity.enums.TaskStatus;
import ru.aiivar.tg.yt.downloader.model.VideoDownloadRequest;
import ru.aiivar.tg.yt.downloader.model.VideoDownloadResponse;
import ru.aiivar.tg.yt.downloader.service.VideoDownloadTaskExecutor;
import ru.aiivar.tg.yt.downloader.service.VideoDownloadTaskResultService;
import ru.aiivar.tg.yt.downloader.service.VideoDownloadTaskService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for managing video download tasks
 */
@RestController
@RequestMapping("/api/v1/tasks")
@CrossOrigin(origins = "*")
public class VideoDownloadTaskController {

    private static final Logger logger = LoggerFactory.getLogger(VideoDownloadTaskController.class);

    @Autowired
    private VideoDownloadTaskService taskService;

    @Autowired
    private VideoDownloadTaskResultService resultService;

    @Autowired
    private VideoDownloadTaskExecutor taskExecutor;

    /**
     * Create a new video download task
     */
    @PostMapping
    public ResponseEntity<VideoDownloadResponse> createTask(@RequestBody VideoDownloadRequest request) {
        logger.info("Creating new video download task for URL: {}", request.getUrl());

        try {
            VideoDownloadTask task = taskService.createTaskWithReuseCheck(request);
            
            String message = task.getStatus() == TaskStatus.COMPLETED ? 
                    "Task completed by reusing existing result" : "Task created successfully";
            
            VideoDownloadResponse response = VideoDownloadResponse.builder()
                    .success(true)
                    .message(message)
                    .downloadId(task.getId())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error creating video download task", e);
            
            VideoDownloadResponse response = VideoDownloadResponse.builder()
                    .success(false)
                    .error("Failed to create task: " + e.getMessage())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get a task by ID
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<VideoDownloadTask> getTask(@PathVariable String taskId) {
        logger.info("Getting task: {}", taskId);

        Optional<VideoDownloadTask> task = taskService.getTaskById(taskId);
        if (task.isPresent()) {
            return ResponseEntity.ok(task.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all tasks with pagination
     */
    @GetMapping
    public ResponseEntity<Page<VideoDownloadTask>> getAllTasks(Pageable pageable) {
        logger.info("Getting all tasks with pagination: {}", pageable);

        Page<VideoDownloadTask> tasks = taskService.getAllTasks(pageable);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Get tasks by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<VideoDownloadTask>> getTasksByStatus(@PathVariable TaskStatus status) {
        logger.info("Getting tasks by status: {}", status);

        List<VideoDownloadTask> tasks = taskService.getTasksByStatus(status);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Get tasks by user ID
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<VideoDownloadTask>> getTasksByUserId(@PathVariable String userId) {
        logger.info("Getting tasks by user ID: {}", userId);

        List<VideoDownloadTask> tasks = taskService.getTasksByUserId(userId);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Get tasks by chat ID
     */
    @GetMapping("/chat/{chatId}")
    public ResponseEntity<List<VideoDownloadTask>> getTasksByChatId(@PathVariable String chatId) {
        logger.info("Getting tasks by chat ID: {}", chatId);

        List<VideoDownloadTask> tasks = taskService.getTasksByChatId(chatId);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Get pending tasks
     */
    @GetMapping("/pending")
    public ResponseEntity<List<VideoDownloadTask>> getPendingTasks() {
        logger.info("Getting pending tasks");

        List<VideoDownloadTask> tasks = taskService.getPendingTasksOrderedByPriority();
        return ResponseEntity.ok(tasks);
    }

    /**
     * Get retryable tasks
     */
    @GetMapping("/retryable")
    public ResponseEntity<List<VideoDownloadTask>> getRetryableTasks() {
        logger.info("Getting retryable tasks");

        List<VideoDownloadTask> tasks = taskService.getRetryableTasks();
        return ResponseEntity.ok(tasks);
    }

    /**
     * Get stuck processing tasks
     */
    @GetMapping("/stuck")
    public ResponseEntity<List<VideoDownloadTask>> getStuckProcessingTasks() {
        logger.info("Getting stuck processing tasks");

        List<VideoDownloadTask> tasks = taskService.getStuckProcessingTasks();
        return ResponseEntity.ok(tasks);
    }

    /**
     * Update task status
     */
    @PutMapping("/{taskId}/status")
    public ResponseEntity<VideoDownloadTask> updateTaskStatus(
            @PathVariable String taskId,
            @RequestParam TaskStatus status,
            @RequestParam(required = false) String errorMessage) {
        
        logger.info("Updating task {} status to {}", taskId, status);

        try {
            VideoDownloadTask task;
            if (errorMessage != null) {
                task = taskService.updateTaskStatus(taskId, status, errorMessage);
            } else {
                task = taskService.updateTaskStatus(taskId, status);
            }
            return ResponseEntity.ok(task);

        } catch (Exception e) {
            logger.error("Error updating task status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Retry a failed task
     */
    @PostMapping("/{taskId}/retry")
    public ResponseEntity<VideoDownloadTask> retryTask(@PathVariable String taskId) {
        logger.info("Retrying task: {}", taskId);

        try {
            VideoDownloadTask task = taskService.retryTask(taskId);
            return ResponseEntity.ok(task);

        } catch (Exception e) {
            logger.error("Error retrying task", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Cancel a task
     */
    @PostMapping("/{taskId}/cancel")
    public ResponseEntity<VideoDownloadTask> cancelTask(@PathVariable String taskId) {
        logger.info("Cancelling task: {}", taskId);

        try {
            VideoDownloadTask task = taskService.cancelTask(taskId);
            return ResponseEntity.ok(task);

        } catch (Exception e) {
            logger.error("Error cancelling task", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete a task
     */
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable String taskId) {
        logger.info("Deleting task: {}", taskId);

        try {
            taskService.deleteTask(taskId);
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            logger.error("Error deleting task", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Process a task
     */
    @PostMapping("/{taskId}/process")
    public ResponseEntity<VideoDownloadTaskResult> processTask(@PathVariable String taskId) {
        logger.info("Processing task: {}", taskId);

        try {
            VideoDownloadTaskResult result = taskService.processTask(taskId);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error processing task", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Process multiple tasks
     */
    @PostMapping("/process")
    public ResponseEntity<List<VideoDownloadTaskResult>> processTasks(@RequestBody List<String> taskIds) {
        logger.info("Processing {} tasks", taskIds.size());

        try {
            List<VideoDownloadTaskResult> results = taskService.processTasks(taskIds);
            return ResponseEntity.ok(results);

        } catch (Exception e) {
            logger.error("Error processing tasks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get task results
     */
    @GetMapping("/{taskId}/results")
    public ResponseEntity<List<VideoDownloadTaskResult>> getTaskResults(@PathVariable String taskId) {
        logger.info("Getting results for task: {}", taskId);

        List<VideoDownloadTaskResult> results = taskService.getTaskResults(taskId);
        return ResponseEntity.ok(results);
    }

    /**
     * Get primary task result
     */
    @GetMapping("/{taskId}/results/primary")
    public ResponseEntity<VideoDownloadTaskResult> getPrimaryTaskResult(@PathVariable String taskId) {
        logger.info("Getting primary result for task: {}", taskId);

        Optional<VideoDownloadTaskResult> result = taskService.getPrimaryTaskResult(taskId);
        if (result.isPresent()) {
            return ResponseEntity.ok(result.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get task statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<VideoDownloadTaskService.TaskStatistics> getTaskStatistics() {
        logger.info("Getting task statistics");

        VideoDownloadTaskService.TaskStatistics statistics = taskService.getTaskStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * Clean up old completed tasks
     */
    @DeleteMapping("/cleanup/completed")
    public ResponseEntity<Integer> cleanupOldCompletedTasks(@RequestParam(defaultValue = "30") int daysOld) {
        logger.info("Cleaning up old completed tasks older than {} days", daysOld);

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        int deletedCount = taskService.cleanupOldCompletedTasks(cutoffDate);
        return ResponseEntity.ok(deletedCount);
    }

    /**
     * Clean up old failed tasks
     */
    @DeleteMapping("/cleanup/failed")
    public ResponseEntity<Integer> cleanupOldFailedTasks(@RequestParam(defaultValue = "7") int daysOld) {
        logger.info("Cleaning up old failed tasks older than {} days", daysOld);

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        int deletedCount = taskService.cleanupOldFailedTasks(cutoffDate);
        return ResponseEntity.ok(deletedCount);
    }

    /**
     * Get tasks by multiple criteria
     */
    @GetMapping("/search")
    public ResponseEntity<Page<VideoDownloadTask>> searchTasks(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String chatId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) SourceType sourceType,
            @RequestParam(required = false) DestinationType destinationType,
            Pageable pageable) {
        
        logger.info("Searching tasks with criteria: userId={}, chatId={}, status={}, sourceType={}, destinationType={}",
                userId, chatId, status, sourceType, destinationType);

        Page<VideoDownloadTask> tasks = taskService.getTasksByCriteria(
                userId, chatId, status, sourceType, destinationType, pageable);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Get processing status and statistics
     */
    @GetMapping("/processing/status")
    public ResponseEntity<VideoDownloadTaskExecutor.ProcessingStatus> getProcessingStatus() {
        logger.info("Getting processing status");

        VideoDownloadTaskExecutor.ProcessingStatus status = taskExecutor.getProcessingStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * Get execution statistics with memory information
     */
    @GetMapping("/processing/statistics")
    public ResponseEntity<VideoDownloadTaskExecutor.TaskExecutionStatistics> getExecutionStatistics() {
        logger.info("Getting execution statistics");

        VideoDownloadTaskExecutor.TaskExecutionStatistics statistics = taskExecutor.getExecutionStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * Update processing configuration
     */
    @PutMapping("/processing/config")
    public ResponseEntity<String> updateProcessingConfiguration(@RequestParam int maxConcurrentTasks) {
        logger.info("Updating processing configuration: maxConcurrentTasks={}", maxConcurrentTasks);

        try {
            if (maxConcurrentTasks < 1) {
                return ResponseEntity.badRequest().body("Max concurrent tasks must be at least 1");
            }

            taskExecutor.updateProcessingConfiguration(maxConcurrentTasks);
            return ResponseEntity.ok("Processing configuration updated successfully");

        } catch (Exception e) {
            logger.error("Error updating processing configuration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update processing configuration: " + e.getMessage());
        }
    }

    /**
     * Check if a result exists for the given source URL and destination type
     */
    @GetMapping("/results/exists")
    public ResponseEntity<Map<String, Object>> checkExistingResult(
            @RequestParam String sourceUrl,
            @RequestParam(required = false, defaultValue = "TELEGRAM") DestinationType destinationType) {
        
        logger.info("Checking for existing result: sourceUrl={}, destinationType={}", sourceUrl, destinationType);

        try {
            boolean exists = resultService.hasExistingResult(sourceUrl, destinationType);
            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);
            
            if (exists) {
                Optional<VideoDownloadTaskResult> result = resultService.findMostRecentResultBySourceUrlAndDestination(sourceUrl, destinationType);
                if (result.isPresent()) {
                    response.put("destinationId", result.get().getDestinationId());
                    response.put("fileName", result.get().getFileName());
                    response.put("fileSizeBytes", result.get().getFileSizeBytes());
                    response.put("createdAt", result.get().getCreatedAt());
                }
            }
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error checking for existing result", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Send existing result to a chat (reuse existing Telegram file ID)
     */
    @PostMapping("/results/send-existing")
    public ResponseEntity<VideoDownloadResponse> sendExistingResult(
            @RequestParam String sourceUrl,
            @RequestParam String chatId,
            @RequestParam(required = false, defaultValue = "TELEGRAM") DestinationType destinationType) {
        
        logger.info("Sending existing result: sourceUrl={}, chatId={}, destinationType={}", sourceUrl, chatId, destinationType);

        try {
            Optional<VideoDownloadTaskResult> existingResult = resultService.findMostRecentResultBySourceUrlAndDestination(sourceUrl, destinationType);
            
            if (existingResult.isEmpty()) {
                VideoDownloadResponse response = VideoDownloadResponse.builder()
                        .success(false)
                        .error("No existing result found for the given source URL and destination type")
                        .build();
                return ResponseEntity.notFound().build();
            }

            VideoDownloadTaskResult result = existingResult.get();
            
            // Create a new task for tracking
            VideoDownloadTask task = taskService.createTask(sourceUrl, SourceType.fromUrl(sourceUrl), destinationType, chatId, chatId, null);
            
            // Send the existing file to the chat
            // This would need to be implemented in the destination processor
            // For now, we'll just return the existing result info
            
            VideoDownloadResponse response = VideoDownloadResponse.builder()
                    .success(true)
                    .message("Existing result sent successfully")
                    .downloadId(task.getId())
                    .telegramFileId(result.getDestinationId())
                    .fileName(result.getFileName())
                    .fileSize(result.getFileSizeBytes())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error sending existing result", e);
            
            VideoDownloadResponse response = VideoDownloadResponse.builder()
                    .success(false)
                    .error("Failed to send existing result: " + e.getMessage())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
