package ru.aiivar.tg.yt.downloader.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.aiivar.tg.yt.downloader.entity.VideoDownloadTask;
import ru.aiivar.tg.yt.downloader.entity.VideoDownloadTaskResult;
import ru.aiivar.tg.yt.downloader.entity.enums.DestinationType;
import ru.aiivar.tg.yt.downloader.entity.enums.SourceType;
import ru.aiivar.tg.yt.downloader.entity.enums.TaskStatus;
import ru.aiivar.tg.yt.downloader.model.VideoDownloadRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing video download tasks
 */
public interface VideoDownloadTaskService {

    /**
     * Create a new video download task from a request
     */
    VideoDownloadTask createTask(VideoDownloadRequest request);

    /**
     * Create a new video download task from a request, checking for existing results first
     */
    VideoDownloadTask createTaskWithReuseCheck(VideoDownloadRequest request);

    /**
     * Create a new video download task with explicit parameters
     */
    VideoDownloadTask createTask(String sourceUrl, SourceType sourceType, DestinationType destinationType,
                                String userId, String chatId, String destinationConfig);

    /**
     * Get a task by ID
     */
    Optional<VideoDownloadTask> getTaskById(String taskId);

    /**
     * Get all tasks with pagination
     */
    Page<VideoDownloadTask> getAllTasks(Pageable pageable);

    /**
     * Get tasks by status
     */
    List<VideoDownloadTask> getTasksByStatus(TaskStatus status);

    /**
     * Get tasks by status with pagination
     */
    Page<VideoDownloadTask> getTasksByStatus(TaskStatus status, Pageable pageable);

    /**
     * Get tasks by user ID
     */
    List<VideoDownloadTask> getTasksByUserId(String userId);

    /**
     * Get tasks by user ID with pagination
     */
    Page<VideoDownloadTask> getTasksByUserId(String userId, Pageable pageable);

    /**
     * Get tasks by chat ID
     */
    List<VideoDownloadTask> getTasksByChatId(String chatId);

    /**
     * Get pending tasks ordered by priority
     */
    List<VideoDownloadTask> getPendingTasksOrderedByPriority();

    /**
     * Get retryable tasks (failed or cancelled with retry count < max retries)
     */
    List<VideoDownloadTask> getRetryableTasks();

    /**
     * Get stuck processing tasks (processing for too long)
     */
    List<VideoDownloadTask> getStuckProcessingTasks();

    /**
     * Update task status
     */
    VideoDownloadTask updateTaskStatus(String taskId, TaskStatus status);

    /**
     * Update task status with error message
     */
    VideoDownloadTask updateTaskStatus(String taskId, TaskStatus status, String errorMessage);

    /**
     * Mark task as started
     */
    VideoDownloadTask markTaskAsStarted(String taskId);

    /**
     * Mark task as completed
     */
    VideoDownloadTask markTaskAsCompleted(String taskId);

    /**
     * Mark task as failed
     */
    VideoDownloadTask markTaskAsFailed(String taskId, String errorMessage);

    /**
     * Retry a failed task
     */
    VideoDownloadTask retryTask(String taskId);

    /**
     * Cancel a task
     */
    VideoDownloadTask cancelTask(String taskId);

    /**
     * Delete a task
     */
    void deleteTask(String taskId);

    /**
     * Get task statistics
     */
    TaskStatistics getTaskStatistics();

    /**
     * Clean up old completed tasks
     */
    int cleanupOldCompletedTasks(LocalDateTime cutoffDate);

    /**
     * Clean up old failed tasks
     */
    int cleanupOldFailedTasks(LocalDateTime cutoffDate);

    /**
     * Get tasks by multiple criteria with pagination
     */
    Page<VideoDownloadTask> getTasksByCriteria(String userId, String chatId, TaskStatus status,
                                              SourceType sourceType, DestinationType destinationType,
                                              Pageable pageable);

    /**
     * Process a task (download and upload)
     */
    VideoDownloadTaskResult processTask(String taskId);

    /**
     * Process multiple tasks in batch
     */
    List<VideoDownloadTaskResult> processTasks(List<String> taskIds);

    /**
     * Get task results
     */
    List<VideoDownloadTaskResult> getTaskResults(String taskId);

    /**
     * Get primary task result
     */
    Optional<VideoDownloadTaskResult> getPrimaryTaskResult(String taskId);

    /**
     * Task statistics inner class
     */
    class TaskStatistics {
        private long totalTasks;
        private long pendingTasks;
        private long processingTasks;
        private long completedTasks;
        private long failedTasks;
        private long cancelledTasks;
        private long retryableTasks;

        // Constructors, getters, and setters
        public TaskStatistics() {}

        public TaskStatistics(long totalTasks, long pendingTasks, long processingTasks,
                            long completedTasks, long failedTasks, long cancelledTasks, long retryableTasks) {
            this.totalTasks = totalTasks;
            this.pendingTasks = pendingTasks;
            this.processingTasks = processingTasks;
            this.completedTasks = completedTasks;
            this.failedTasks = failedTasks;
            this.cancelledTasks = cancelledTasks;
            this.retryableTasks = retryableTasks;
        }

        // Getters and setters
        public long getTotalTasks() { return totalTasks; }
        public void setTotalTasks(long totalTasks) { this.totalTasks = totalTasks; }

        public long getPendingTasks() { return pendingTasks; }
        public void setPendingTasks(long pendingTasks) { this.pendingTasks = pendingTasks; }

        public long getProcessingTasks() { return processingTasks; }
        public void setProcessingTasks(long processingTasks) { this.processingTasks = processingTasks; }

        public long getCompletedTasks() { return completedTasks; }
        public void setCompletedTasks(long completedTasks) { this.completedTasks = completedTasks; }

        public long getFailedTasks() { return failedTasks; }
        public void setFailedTasks(long failedTasks) { this.failedTasks = failedTasks; }

        public long getCancelledTasks() { return cancelledTasks; }
        public void setCancelledTasks(long cancelledTasks) { this.cancelledTasks = cancelledTasks; }

        public long getRetryableTasks() { return retryableTasks; }
        public void setRetryableTasks(long retryableTasks) { this.retryableTasks = retryableTasks; }
    }
}
