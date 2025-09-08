package ru.aiivar.tg.yt.downloader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.aiivar.tg.yt.downloader.config.ProcessingConfiguration;
import ru.aiivar.tg.yt.downloader.entity.VideoDownloadTask;
import ru.aiivar.tg.yt.downloader.entity.VideoDownloadTaskResult;
import ru.aiivar.tg.yt.downloader.entity.enums.TaskStatus;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;

/**
 * Service for executing video download tasks asynchronously with memory-aware processing
 */
@Service
public class VideoDownloadTaskExecutor {

    private static final Logger logger = LoggerFactory.getLogger(VideoDownloadTaskExecutor.class);

    @Autowired
    private VideoDownloadTaskService taskService;

    @Autowired
    private MemoryMonitoringService memoryMonitoringService;

    @Autowired
    private ProcessingConfiguration processingConfig;

    @Autowired
    @Qualifier("videoProcessingExecutor")
    private Executor videoProcessingExecutor;

    // Semaphore to control concurrent processing
    private final Semaphore processingSemaphore;
    
    // Track currently processing tasks
    private final ConcurrentHashMap<String, CompletableFuture<VideoDownloadTaskResult>> processingTasks = new ConcurrentHashMap<>();

    public VideoDownloadTaskExecutor() {
        // Initialize semaphore with default value, will be updated by configuration
        this.processingSemaphore = new Semaphore(1);
    }

    /**
     * Scheduled task to process pending tasks with memory-aware processing
     */
    @Scheduled(fixedDelayString = "${video.processing.processing-interval-ms:30000}")
    public void processPendingTasks() {
        logger.debug("Checking for pending tasks to process");

        try {
            // Check memory before processing
            if (!memoryMonitoringService.hasEnoughMemory()) {
                logger.warn("Insufficient memory for processing tasks, skipping this cycle");
                memoryMonitoringService.performGarbageCollectionIfNeeded();
                return;
            }

            List<VideoDownloadTask> pendingTasks = taskService.getPendingTasksOrderedByPriority();
            
            if (pendingTasks.isEmpty()) {
                logger.debug("No pending tasks found");
                return;
            }

            // Get recommended number of concurrent tasks based on memory
            int recommendedConcurrentTasks = memoryMonitoringService.getRecommendedConcurrentTasks();
            int availableSlots = Math.min(recommendedConcurrentTasks, processingSemaphore.availablePermits());
            
            if (availableSlots <= 0) {
                logger.debug("No available processing slots, {} tasks currently processing", 
                        processingConfig.getMaxConcurrentTasks() - processingSemaphore.availablePermits());
                return;
            }

            // Process only the number of tasks we can handle
            int tasksToProcess = Math.min(availableSlots, pendingTasks.size());
            logger.info("Found {} pending tasks, processing {} (memory-based limit: {})", 
                    pendingTasks.size(), tasksToProcess, recommendedConcurrentTasks);

            for (int i = 0; i < tasksToProcess; i++) {
                VideoDownloadTask task = pendingTasks.get(i);
                processTaskAsync(task.getId());
            }

        } catch (Exception e) {
            logger.error("Error in scheduled task processing", e);
        }
    }

    /**
     * Scheduled task to retry failed tasks with memory awareness
     */
    @Scheduled(fixedDelayString = "${video.processing.retry-interval-ms:300000}")
    public void retryFailedTasks() {
        logger.debug("Checking for retryable tasks");

        try {
            // Check memory before processing retries
            if (!memoryMonitoringService.hasEnoughMemory()) {
                logger.warn("Insufficient memory for retrying tasks, skipping this cycle");
                return;
            }

            List<VideoDownloadTask> retryableTasks = taskService.getRetryableTasks();
            
            if (retryableTasks.isEmpty()) {
                logger.debug("No retryable tasks found");
                return;
            }

            // Limit retries based on available processing capacity
            int availableSlots = processingSemaphore.availablePermits();
            int tasksToRetry = Math.min(availableSlots, retryableTasks.size());
            
            logger.info("Found {} retryable tasks, retrying {} (available slots: {})", 
                    retryableTasks.size(), tasksToRetry, availableSlots);

            for (int i = 0; i < tasksToRetry; i++) {
                VideoDownloadTask task = retryableTasks.get(i);
                retryTaskAsync(task.getId());
            }

        } catch (Exception e) {
            logger.error("Error in scheduled retry processing", e);
        }
    }

    /**
     * Scheduled task to handle stuck processing tasks
     */
    @Scheduled(fixedDelayString = "${video.processing.stuck-task-check-interval-ms:600000}")
    public void handleStuckTasks() {
        logger.debug("Checking for stuck processing tasks");

        try {
            List<VideoDownloadTask> stuckTasks = taskService.getStuckProcessingTasks();
            
            if (stuckTasks.isEmpty()) {
                logger.debug("No stuck tasks found");
                return;
            }

            logger.warn("Found {} stuck processing tasks", stuckTasks.size());

            // Mark stuck tasks as failed
            for (VideoDownloadTask task : stuckTasks) {
                handleStuckTaskAsync(task.getId());
            }

        } catch (Exception e) {
            logger.error("Error in scheduled stuck task handling", e);
        }
    }

    /**
     * Scheduled task to clean up old tasks
     */
    @Scheduled(fixedDelayString = "${video.processing.cleanup-interval-ms:3600000}")
    public void cleanupOldTasks() {
        logger.debug("Starting cleanup of old tasks");

        try {
            // Clean up completed tasks older than 30 days
            int deletedCompleted = taskService.cleanupOldCompletedTasks(
                    java.time.LocalDateTime.now().minusDays(30));
            
            // Clean up failed tasks older than 7 days
            int deletedFailed = taskService.cleanupOldFailedTasks(
                    java.time.LocalDateTime.now().minusDays(7));

            if (deletedCompleted > 0 || deletedFailed > 0) {
                logger.info("Cleanup completed: {} completed tasks and {} failed tasks deleted", 
                        deletedCompleted, deletedFailed);
            }

        } catch (Exception e) {
            logger.error("Error in scheduled cleanup", e);
        }
    }

    /**
     * Process a single task asynchronously with memory-aware processing
     */
    @Async("videoProcessingExecutor")
    public CompletableFuture<VideoDownloadTaskResult> processTaskAsync(String taskId) {
        logger.info("Processing task asynchronously: {}", taskId);

        // Acquire semaphore permit
        try {
            processingSemaphore.acquire();
            processingTasks.put(taskId, null); // Placeholder to track processing
            
            logger.debug("Acquired processing slot for task: {} (available slots: {})", 
                    taskId, processingSemaphore.availablePermits());

            // Check memory before processing
            if (!memoryMonitoringService.hasEnoughMemory()) {
                logger.warn("Insufficient memory for processing task: {}", taskId);
                throw new RuntimeException("Insufficient memory for processing");
            }

            VideoDownloadTaskResult result = taskService.processTask(taskId);
            logger.info("Successfully processed task: {}", taskId);
            return CompletableFuture.completedFuture(result);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Task processing interrupted: {}", taskId, e);
            return CompletableFuture.failedFuture(e);
        } catch (Exception e) {
            logger.error("Error processing task asynchronously: {}", taskId, e);
            return CompletableFuture.failedFuture(e);
        } finally {
            // Always release semaphore and remove from tracking
            processingSemaphore.release();
            processingTasks.remove(taskId);
            logger.debug("Released processing slot for task: {} (available slots: {})", 
                    taskId, processingSemaphore.availablePermits());
        }
    }

    /**
     * Retry a single task asynchronously
     */
    @Async
    public CompletableFuture<VideoDownloadTask> retryTaskAsync(String taskId) {
        logger.info("Retrying task asynchronously: {}", taskId);

        try {
            VideoDownloadTask task = taskService.retryTask(taskId);
            logger.info("Successfully retried task: {}", taskId);
            return CompletableFuture.completedFuture(task);

        } catch (Exception e) {
            logger.error("Error retrying task asynchronously: {}", taskId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Handle a stuck task asynchronously
     */
    @Async
    public CompletableFuture<Void> handleStuckTaskAsync(String taskId) {
        logger.warn("Handling stuck task: {}", taskId);

        try {
            taskService.updateTaskStatus(taskId, TaskStatus.FAILED, 
                    "Task was stuck in processing state for too long");
            logger.warn("Marked stuck task as failed: {}", taskId);
            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            logger.error("Error handling stuck task: {}", taskId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Process multiple tasks asynchronously
     */
    @Async
    public CompletableFuture<List<VideoDownloadTaskResult>> processTasksAsync(List<String> taskIds) {
        logger.info("Processing {} tasks asynchronously", taskIds.size());

        try {
            List<VideoDownloadTaskResult> results = taskService.processTasks(taskIds);
            logger.info("Successfully processed {} tasks", results.size());
            return CompletableFuture.completedFuture(results);

        } catch (Exception e) {
            logger.error("Error processing tasks asynchronously", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Get task execution statistics with memory information
     */
    public TaskExecutionStatistics getExecutionStatistics() {
        try {
            VideoDownloadTaskService.TaskStatistics taskStats = taskService.getTaskStatistics();
            MemoryMonitoringService.MemoryStats memoryStats = memoryMonitoringService.getMemoryStats();
            
            return new TaskExecutionStatistics(
                    taskStats.getTotalTasks(),
                    taskStats.getPendingTasks(),
                    taskStats.getProcessingTasks(),
                    taskStats.getCompletedTasks(),
                    taskStats.getFailedTasks(),
                    taskStats.getRetryableTasks(),
                    processingConfig.getMaxConcurrentTasks(),
                    processingSemaphore.availablePermits(),
                    memoryStats.getUsedPercentage(),
                    memoryStats.getFreeMemory() / (1024 * 1024) // Convert to MB
            );

        } catch (Exception e) {
            logger.error("Error getting execution statistics", e);
            return new TaskExecutionStatistics(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
    }

    /**
     * Update processing configuration at runtime
     */
    public void updateProcessingConfiguration(int maxConcurrentTasks) {
        logger.info("Updating max concurrent tasks from {} to {}", 
                processingConfig.getMaxConcurrentTasks(), maxConcurrentTasks);
        
        processingConfig.setMaxConcurrentTasks(maxConcurrentTasks);
        
        // Update semaphore permits
        int currentPermits = processingSemaphore.availablePermits();
        int newPermits = maxConcurrentTasks;
        
        if (newPermits > currentPermits) {
            // Add more permits
            processingSemaphore.release(newPermits - currentPermits);
        } else if (newPermits < currentPermits) {
            // Reduce permits (this is more complex, would need to drain permits)
            logger.warn("Reducing concurrent tasks is not fully supported at runtime");
        }
        
        logger.info("Updated processing configuration: max concurrent tasks = {}, available slots = {}", 
                maxConcurrentTasks, processingSemaphore.availablePermits());
    }

    /**
     * Get current processing status
     */
    public ProcessingStatus getProcessingStatus() {
        return new ProcessingStatus(
                processingConfig.getMaxConcurrentTasks(),
                processingSemaphore.availablePermits(),
                processingTasks.size(),
                memoryMonitoringService.getMemoryPressureLevel(),
                memoryMonitoringService.hasEnoughMemory()
        );
    }

    /**
     * Task execution statistics inner class
     */
    public static class TaskExecutionStatistics {
        private final long totalTasks;
        private final long pendingTasks;
        private final long processingTasks;
        private final long completedTasks;
        private final long failedTasks;
        private final long retryableTasks;
        private final int maxConcurrentTasks;
        private final int availableSlots;
        private final double memoryUsagePercentage;
        private final long freeMemoryMB;

        public TaskExecutionStatistics(long totalTasks, long pendingTasks, long processingTasks,
                                     long completedTasks, long failedTasks, long retryableTasks,
                                     int maxConcurrentTasks, int availableSlots, 
                                     double memoryUsagePercentage, long freeMemoryMB) {
            this.totalTasks = totalTasks;
            this.pendingTasks = pendingTasks;
            this.processingTasks = processingTasks;
            this.completedTasks = completedTasks;
            this.failedTasks = failedTasks;
            this.retryableTasks = retryableTasks;
            this.maxConcurrentTasks = maxConcurrentTasks;
            this.availableSlots = availableSlots;
            this.memoryUsagePercentage = memoryUsagePercentage;
            this.freeMemoryMB = freeMemoryMB;
        }

        // Getters
        public long getTotalTasks() { return totalTasks; }
        public long getPendingTasks() { return pendingTasks; }
        public long getProcessingTasks() { return processingTasks; }
        public long getCompletedTasks() { return completedTasks; }
        public long getFailedTasks() { return failedTasks; }
        public long getRetryableTasks() { return retryableTasks; }
        public int getMaxConcurrentTasks() { return maxConcurrentTasks; }
        public int getAvailableSlots() { return availableSlots; }
        public double getMemoryUsagePercentage() { return memoryUsagePercentage; }
        public long getFreeMemoryMB() { return freeMemoryMB; }
    }

    /**
     * Processing status inner class
     */
    public static class ProcessingStatus {
        private final int maxConcurrentTasks;
        private final int availableSlots;
        private final int currentlyProcessing;
        private final MemoryMonitoringService.MemoryPressureLevel memoryPressure;
        private final boolean hasEnoughMemory;

        public ProcessingStatus(int maxConcurrentTasks, int availableSlots, int currentlyProcessing,
                              MemoryMonitoringService.MemoryPressureLevel memoryPressure, boolean hasEnoughMemory) {
            this.maxConcurrentTasks = maxConcurrentTasks;
            this.availableSlots = availableSlots;
            this.currentlyProcessing = currentlyProcessing;
            this.memoryPressure = memoryPressure;
            this.hasEnoughMemory = hasEnoughMemory;
        }

        // Getters
        public int getMaxConcurrentTasks() { return maxConcurrentTasks; }
        public int getAvailableSlots() { return availableSlots; }
        public int getCurrentlyProcessing() { return currentlyProcessing; }
        public MemoryMonitoringService.MemoryPressureLevel getMemoryPressure() { return memoryPressure; }
        public boolean isHasEnoughMemory() { return hasEnoughMemory; }
    }
}
