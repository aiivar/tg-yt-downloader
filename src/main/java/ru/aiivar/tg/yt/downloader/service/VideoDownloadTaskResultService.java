package ru.aiivar.tg.yt.downloader.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.aiivar.tg.yt.downloader.entity.VideoDownloadTask;
import ru.aiivar.tg.yt.downloader.entity.VideoDownloadTaskResult;
import ru.aiivar.tg.yt.downloader.entity.enums.DestinationType;
import ru.aiivar.tg.yt.downloader.entity.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing video download task results
 */
public interface VideoDownloadTaskResultService {

    /**
     * Create a new result for a task
     */
    VideoDownloadTaskResult createResult(VideoDownloadTask task, DestinationType destinationType);

    /**
     * Create a new result with explicit parameters
     */
    VideoDownloadTaskResult createResult(String taskId, DestinationType destinationType, String destinationId);

    /**
     * Get a result by ID
     */
    Optional<VideoDownloadTaskResult> getResultById(String resultId);

    /**
     * Get all results for a task
     */
    List<VideoDownloadTaskResult> getResultsByTask(VideoDownloadTask task);

    /**
     * Get all results for a task by task ID
     */
    List<VideoDownloadTaskResult> getResultsByTaskId(String taskId);

    /**
     * Get primary result for a task
     */
    Optional<VideoDownloadTaskResult> getPrimaryResultByTaskId(String taskId);

    /**
     * Get results by destination type
     */
    List<VideoDownloadTaskResult> getResultsByDestinationType(DestinationType destinationType);

    /**
     * Get results by destination type with pagination
     */
    Page<VideoDownloadTaskResult> getResultsByDestinationType(DestinationType destinationType, Pageable pageable);

    /**
     * Get result by destination ID
     */
    Optional<VideoDownloadTaskResult> getResultByDestinationId(String destinationId);

    /**
     * Get results by status
     */
    List<VideoDownloadTaskResult> getResultsByStatus(TaskStatus status);

    /**
     * Get results by status with pagination
     */
    Page<VideoDownloadTaskResult> getResultsByStatus(TaskStatus status, Pageable pageable);

    /**
     * Update result status
     */
    VideoDownloadTaskResult updateResultStatus(String resultId, TaskStatus status);

    /**
     * Update result status with error message
     */
    VideoDownloadTaskResult updateResultStatus(String resultId, TaskStatus status, String errorMessage);

    /**
     * Mark result as started
     */
    VideoDownloadTaskResult markResultAsStarted(String resultId);

    /**
     * Mark result as completed
     */
    VideoDownloadTaskResult markResultAsCompleted(String resultId);

    /**
     * Mark result as failed
     */
    VideoDownloadTaskResult markResultAsFailed(String resultId, String errorMessage);

    /**
     * Update result with file information
     */
    VideoDownloadTaskResult updateResultWithFileInfo(String resultId, String fileName, Long fileSizeBytes,
                                                   String fileFormat, String resolution, Long durationSeconds);

    /**
     * Update result with video metadata
     */
    VideoDownloadTaskResult updateResultWithVideoMetadata(String resultId, Long bitrate, Double fps,
                                                        String codec, String thumbnailUrl);

    /**
     * Update result with processing times
     */
    VideoDownloadTaskResult updateResultWithProcessingTimes(String resultId, Long processingTimeMs,
                                                          Long uploadTimeMs);

    /**
     * Set result as primary
     */
    VideoDownloadTaskResult setAsPrimaryResult(String resultId);

    /**
     * Set result as secondary
     */
    VideoDownloadTaskResult setAsSecondaryResult(String resultId);

    /**
     * Delete a result
     */
    void deleteResult(String resultId);

    /**
     * Delete all results for a task
     */
    void deleteResultsByTask(String taskId);

    /**
     * Get result statistics
     */
    ResultStatistics getResultStatistics();

    /**
     * Clean up old completed results
     */
    int cleanupOldCompletedResults(LocalDateTime cutoffDate);

    /**
     * Get results by multiple criteria with pagination
     */
    Page<VideoDownloadTaskResult> getResultsByCriteria(String taskId, DestinationType destinationType,
                                                      TaskStatus status, String fileFormat, Boolean isPrimary,
                                                      Pageable pageable);

    /**
     * Get total file size of all completed results
     */
    Long getTotalFileSizeOfCompletedResults();

    /**
     * Get average file size by destination type
     */
    Double getAverageFileSizeByDestinationType(DestinationType destinationType);

    /**
     * Get average processing time by destination type
     */
    Double getAverageProcessingTimeByDestinationType(DestinationType destinationType);

    /**
     * Find results that need cleanup
     */
    List<VideoDownloadTaskResult> findResultsForCleanup(LocalDateTime cutoffDate);

    /**
     * Find existing completed results by source URL and destination type
     */
    List<VideoDownloadTaskResult> findExistingResultsBySourceUrlAndDestination(String sourceUrl, DestinationType destinationType);

    /**
     * Find the most recent completed result by source URL and destination type
     */
    Optional<VideoDownloadTaskResult> findMostRecentResultBySourceUrlAndDestination(String sourceUrl, DestinationType destinationType);

    /**
     * Check if a result exists for the given source URL and destination type
     */
    boolean hasExistingResult(String sourceUrl, DestinationType destinationType);

    /**
     * Reuse an existing result for a new task
     */
    VideoDownloadTaskResult reuseExistingResult(String sourceUrl, DestinationType destinationType, VideoDownloadTask newTask);

    /**
     * Result statistics inner class
     */
    class ResultStatistics {
        private long totalResults;
        private long completedResults;
        private long failedResults;
        private long totalFileSize;
        private double averageFileSize;
        private double averageProcessingTime;

        // Constructors, getters, and setters
        public ResultStatistics() {}

        public ResultStatistics(long totalResults, long completedResults, long failedResults,
                              long totalFileSize, double averageFileSize, double averageProcessingTime) {
            this.totalResults = totalResults;
            this.completedResults = completedResults;
            this.failedResults = failedResults;
            this.totalFileSize = totalFileSize;
            this.averageFileSize = averageFileSize;
            this.averageProcessingTime = averageProcessingTime;
        }

        // Getters and setters
        public long getTotalResults() { return totalResults; }
        public void setTotalResults(long totalResults) { this.totalResults = totalResults; }

        public long getCompletedResults() { return completedResults; }
        public void setCompletedResults(long completedResults) { this.completedResults = completedResults; }

        public long getFailedResults() { return failedResults; }
        public void setFailedResults(long failedResults) { this.failedResults = failedResults; }

        public long getTotalFileSize() { return totalFileSize; }
        public void setTotalFileSize(long totalFileSize) { this.totalFileSize = totalFileSize; }

        public double getAverageFileSize() { return averageFileSize; }
        public void setAverageFileSize(double averageFileSize) { this.averageFileSize = averageFileSize; }

        public double getAverageProcessingTime() { return averageProcessingTime; }
        public void setAverageProcessingTime(double averageProcessingTime) { this.averageProcessingTime = averageProcessingTime; }
    }
}
