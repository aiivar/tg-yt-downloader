package ru.aiivar.tg.yt.downloader.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aiivar.tg.yt.downloader.entity.VideoDownloadTask;
import ru.aiivar.tg.yt.downloader.entity.VideoDownloadTaskResult;
import ru.aiivar.tg.yt.downloader.entity.enums.DestinationType;
import ru.aiivar.tg.yt.downloader.entity.enums.TaskStatus;
import ru.aiivar.tg.yt.downloader.repository.VideoDownloadTaskResultRepository;
import ru.aiivar.tg.yt.downloader.service.EntityFactory;
import ru.aiivar.tg.yt.downloader.service.VideoDownloadTaskResultService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of VideoDownloadTaskResultService
 */
@Service
@Transactional
public class VideoDownloadTaskResultServiceImpl implements VideoDownloadTaskResultService {

    private static final Logger logger = LoggerFactory.getLogger(VideoDownloadTaskResultServiceImpl.class);

    @Autowired
    private VideoDownloadTaskResultRepository resultRepository;

    @Autowired
    private EntityFactory entityFactory;

    @Override
    public VideoDownloadTaskResult createResult(VideoDownloadTask task, DestinationType destinationType) {
        logger.info("Creating new result for task: {} and destination: {}", task.getId(), destinationType);

        VideoDownloadTaskResult result = entityFactory.newEntity(VideoDownloadTaskResult.class);
        result.setTask(task);
        result.setDestinationType(destinationType);
        result.setStatus(TaskStatus.PENDING);

        VideoDownloadTaskResult savedResult = resultRepository.save(result);
        logger.info("Created result with ID: {}", savedResult.getId());

        return savedResult;
    }

    @Override
    public VideoDownloadTaskResult createResult(String taskId, DestinationType destinationType, String destinationId) {
        logger.info("Creating new result for task: {}, destination: {}, destinationId: {}", 
                taskId, destinationType, destinationId);

        VideoDownloadTaskResult result = entityFactory.newEntity(VideoDownloadTaskResult.class);
        result.setDestinationType(destinationType);
        result.setDestinationId(destinationId);
        result.setStatus(TaskStatus.COMPLETED);

        VideoDownloadTaskResult savedResult = resultRepository.save(result);
        logger.info("Created result with ID: {}", savedResult.getId());

        return savedResult;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VideoDownloadTaskResult> getResultById(String resultId) {
        return resultRepository.findById(resultId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VideoDownloadTaskResult> getResultsByTask(VideoDownloadTask task) {
        return resultRepository.findByTask(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VideoDownloadTaskResult> getResultsByTaskId(String taskId) {
        return resultRepository.findByTaskId(taskId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VideoDownloadTaskResult> getPrimaryResultByTaskId(String taskId) {
        return resultRepository.findPrimaryResultByTaskId(taskId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VideoDownloadTaskResult> getResultsByDestinationType(DestinationType destinationType) {
        return resultRepository.findByDestinationType(destinationType);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VideoDownloadTaskResult> getResultsByDestinationType(DestinationType destinationType, Pageable pageable) {
        return resultRepository.findByDestinationType(destinationType, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VideoDownloadTaskResult> getResultByDestinationId(String destinationId) {
        return resultRepository.findByDestinationId(destinationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VideoDownloadTaskResult> getResultsByStatus(TaskStatus status) {
        return resultRepository.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VideoDownloadTaskResult> getResultsByStatus(TaskStatus status, Pageable pageable) {
        return resultRepository.findByStatus(status, pageable);
    }

    @Override
    public VideoDownloadTaskResult updateResultStatus(String resultId, TaskStatus status) {
        return updateResultStatus(resultId, status, null);
    }

    @Override
    public VideoDownloadTaskResult updateResultStatus(String resultId, TaskStatus status, String errorMessage) {
        logger.info("Updating result {} status to {} with error: {}", resultId, status, errorMessage);

        Optional<VideoDownloadTaskResult> resultOpt = resultRepository.findById(resultId);
        if (resultOpt.isEmpty()) {
            throw new IllegalArgumentException("Result not found with ID: " + resultId);
        }

        VideoDownloadTaskResult result = resultOpt.get();
        result.setStatus(status);
        result.setErrorMessage(errorMessage);

        VideoDownloadTaskResult updatedResult = resultRepository.save(result);
        logger.info("Updated result {} status to {}", resultId, status);

        return updatedResult;
    }

    @Override
    public VideoDownloadTaskResult markResultAsStarted(String resultId) {
        logger.info("Marking result {} as started", resultId);

        Optional<VideoDownloadTaskResult> resultOpt = resultRepository.findById(resultId);
        if (resultOpt.isEmpty()) {
            throw new IllegalArgumentException("Result not found with ID: " + resultId);
        }

        VideoDownloadTaskResult result = resultOpt.get();
        result.setStatus(TaskStatus.PROCESSING);
        result.setUploadStartedAt(LocalDateTime.now());

        VideoDownloadTaskResult updatedResult = resultRepository.save(result);
        logger.info("Marked result {} as started", resultId);

        return updatedResult;
    }

    @Override
    public VideoDownloadTaskResult markResultAsCompleted(String resultId) {
        logger.info("Marking result {} as completed", resultId);

        Optional<VideoDownloadTaskResult> resultOpt = resultRepository.findById(resultId);
        if (resultOpt.isEmpty()) {
            throw new IllegalArgumentException("Result not found with ID: " + resultId);
        }

        VideoDownloadTaskResult result = resultOpt.get();
        result.setStatus(TaskStatus.COMPLETED);
        result.setUploadCompletedAt(LocalDateTime.now());

        VideoDownloadTaskResult updatedResult = resultRepository.save(result);
        logger.info("Marked result {} as completed", resultId);

        return updatedResult;
    }

    @Override
    public VideoDownloadTaskResult markResultAsFailed(String resultId, String errorMessage) {
        logger.info("Marking result {} as failed with error: {}", resultId, errorMessage);

        Optional<VideoDownloadTaskResult> resultOpt = resultRepository.findById(resultId);
        if (resultOpt.isEmpty()) {
            throw new IllegalArgumentException("Result not found with ID: " + resultId);
        }

        VideoDownloadTaskResult result = resultOpt.get();
        result.setStatus(TaskStatus.FAILED);
        result.setErrorMessage(errorMessage);

        VideoDownloadTaskResult updatedResult = resultRepository.save(result);
        logger.info("Marked result {} as failed", resultId);

        return updatedResult;
    }

    @Override
    public VideoDownloadTaskResult updateResultWithFileInfo(String resultId, String fileName, Long fileSizeBytes,
                                                          String fileFormat, String resolution, Long durationSeconds) {
        logger.info("Updating result {} with file info", resultId);

        Optional<VideoDownloadTaskResult> resultOpt = resultRepository.findById(resultId);
        if (resultOpt.isEmpty()) {
            throw new IllegalArgumentException("Result not found with ID: " + resultId);
        }

        VideoDownloadTaskResult result = resultOpt.get();
        result.setFileName(fileName);
        result.setFileSizeBytes(fileSizeBytes);
        result.setFileFormat(fileFormat);
        result.setResolution(resolution);
        result.setDurationSeconds(durationSeconds);

        VideoDownloadTaskResult updatedResult = resultRepository.save(result);
        logger.info("Updated result {} with file info", resultId);

        return updatedResult;
    }

    @Override
    public VideoDownloadTaskResult updateResultWithVideoMetadata(String resultId, Long bitrate, Double fps,
                                                               String codec, String thumbnailUrl) {
        logger.info("Updating result {} with video metadata", resultId);

        Optional<VideoDownloadTaskResult> resultOpt = resultRepository.findById(resultId);
        if (resultOpt.isEmpty()) {
            throw new IllegalArgumentException("Result not found with ID: " + resultId);
        }

        VideoDownloadTaskResult result = resultOpt.get();
        result.setBitrate(bitrate);
        result.setFps(fps);
        result.setCodec(codec);
        result.setThumbnailUrl(thumbnailUrl);

        VideoDownloadTaskResult updatedResult = resultRepository.save(result);
        logger.info("Updated result {} with video metadata", resultId);

        return updatedResult;
    }

    @Override
    public VideoDownloadTaskResult updateResultWithProcessingTimes(String resultId, Long processingTimeMs,
                                                                Long uploadTimeMs) {
        logger.info("Updating result {} with processing times", resultId);

        Optional<VideoDownloadTaskResult> resultOpt = resultRepository.findById(resultId);
        if (resultOpt.isEmpty()) {
            throw new IllegalArgumentException("Result not found with ID: " + resultId);
        }

        VideoDownloadTaskResult result = resultOpt.get();
        result.setProcessingTimeMs(processingTimeMs);
        result.setUploadTimeMs(uploadTimeMs);

        VideoDownloadTaskResult updatedResult = resultRepository.save(result);
        logger.info("Updated result {} with processing times", resultId);

        return updatedResult;
    }

    @Override
    public VideoDownloadTaskResult setAsPrimaryResult(String resultId) {
        logger.info("Setting result {} as primary", resultId);

        Optional<VideoDownloadTaskResult> resultOpt = resultRepository.findById(resultId);
        if (resultOpt.isEmpty()) {
            throw new IllegalArgumentException("Result not found with ID: " + resultId);
        }

        VideoDownloadTaskResult result = resultOpt.get();
        result.setIsPrimaryResult(true);

        VideoDownloadTaskResult updatedResult = resultRepository.save(result);
        logger.info("Set result {} as primary", resultId);

        return updatedResult;
    }

    @Override
    public VideoDownloadTaskResult setAsSecondaryResult(String resultId) {
        logger.info("Setting result {} as secondary", resultId);

        Optional<VideoDownloadTaskResult> resultOpt = resultRepository.findById(resultId);
        if (resultOpt.isEmpty()) {
            throw new IllegalArgumentException("Result not found with ID: " + resultId);
        }

        VideoDownloadTaskResult result = resultOpt.get();
        result.setIsPrimaryResult(false);

        VideoDownloadTaskResult updatedResult = resultRepository.save(result);
        logger.info("Set result {} as secondary", resultId);

        return updatedResult;
    }

    @Override
    public void deleteResult(String resultId) {
        logger.info("Deleting result: {}", resultId);

        if (!resultRepository.existsById(resultId)) {
            throw new IllegalArgumentException("Result not found with ID: " + resultId);
        }

        resultRepository.deleteById(resultId);
        logger.info("Deleted result: {}", resultId);
    }

    @Override
    public void deleteResultsByTask(String taskId) {
        logger.info("Deleting all results for task: {}", taskId);

        List<VideoDownloadTaskResult> results = resultRepository.findByTaskId(taskId);
        resultRepository.deleteAll(results);
        
        logger.info("Deleted {} results for task: {}", results.size(), taskId);
    }

    @Override
    @Transactional(readOnly = true)
    public ResultStatistics getResultStatistics() {
        long totalResults = resultRepository.count();
        long completedResults = resultRepository.countByStatus(TaskStatus.COMPLETED);
        long failedResults = resultRepository.countByStatus(TaskStatus.FAILED);
        Long totalFileSize = resultRepository.getTotalFileSizeOfCompletedResults();
        
        double averageFileSize = totalFileSize != null ? totalFileSize / Math.max(completedResults, 1) : 0;
        double averageProcessingTime = 0; // This would need to be calculated from actual data

        return new ResultStatistics(totalResults, completedResults, failedResults, 
                totalFileSize != null ? totalFileSize : 0, averageFileSize, averageProcessingTime);
    }

    @Override
    public int cleanupOldCompletedResults(LocalDateTime cutoffDate) {
        logger.info("Cleaning up old completed results before {} (preserving Telegram results)", cutoffDate);
        
        // Find results that can be cleaned up (excluding Telegram results)
        List<VideoDownloadTaskResult> resultsForCleanup = resultRepository.findResultsForCleanup(cutoffDate);
        
        int deletedCount = 0;
        for (VideoDownloadTaskResult result : resultsForCleanup) {
            // Preserve Telegram results to allow reuse
            if (result.getDestinationType() == DestinationType.TELEGRAM) {
                logger.debug("Preserving Telegram result: {} for potential reuse", result.getId());
                continue;
            }
            
            // Delete non-Telegram results
            resultRepository.delete(result);
            deletedCount++;
            logger.debug("Deleted old result: {} (destination: {})", result.getId(), result.getDestinationType());
        }
        
        logger.info("Cleanup completed: {} results deleted, {} Telegram results preserved", 
                deletedCount, resultsForCleanup.size() - deletedCount);
        
        return deletedCount;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VideoDownloadTaskResult> getResultsByCriteria(String taskId, DestinationType destinationType,
                                                            TaskStatus status, String fileFormat, Boolean isPrimary,
                                                            Pageable pageable) {
        return resultRepository.findByMultipleCriteria(taskId, destinationType, status, fileFormat, isPrimary, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getTotalFileSizeOfCompletedResults() {
        return resultRepository.getTotalFileSizeOfCompletedResults();
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageFileSizeByDestinationType(DestinationType destinationType) {
        return resultRepository.getAverageFileSizeByDestinationType(destinationType);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageProcessingTimeByDestinationType(DestinationType destinationType) {
        return resultRepository.getAverageProcessingTimeByDestinationType(destinationType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VideoDownloadTaskResult> findResultsForCleanup(LocalDateTime cutoffDate) {
        return resultRepository.findResultsForCleanup(cutoffDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VideoDownloadTaskResult> findExistingResultsBySourceUrlAndDestination(String sourceUrl, DestinationType destinationType) {
        logger.debug("Finding existing results for source URL: {} and destination: {}", sourceUrl, destinationType);
        return resultRepository.findExistingResultsBySourceUrlAndDestination(sourceUrl, destinationType);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VideoDownloadTaskResult> findMostRecentResultBySourceUrlAndDestination(String sourceUrl, DestinationType destinationType) {
        logger.debug("Finding most recent result for source URL: {} and destination: {}", sourceUrl, destinationType);
        List<VideoDownloadTaskResult> results = resultRepository.findMostRecentResultsBySourceUrlAndDestination(sourceUrl, destinationType);
        
        if (results.size() > 1) {
            logger.warn("Found {} results for source URL: {} and destination: {}, using the most recent one", 
                    results.size(), sourceUrl, destinationType);
        }
        
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasExistingResult(String sourceUrl, DestinationType destinationType) {
        long count = resultRepository.countExistingResultsBySourceUrlAndDestination(sourceUrl, destinationType);
        boolean exists = count > 0;
        logger.debug("Existing result check for {} and {}: {}", sourceUrl, destinationType, exists);
        return exists;
    }

    @Override
    public VideoDownloadTaskResult reuseExistingResult(String sourceUrl, DestinationType destinationType, VideoDownloadTask newTask) {
        logger.info("Reusing existing result for source URL: {} and destination: {} for new task: {}", 
                sourceUrl, destinationType, newTask.getId());

        Optional<VideoDownloadTaskResult> existingResultOpt = findMostRecentResultBySourceUrlAndDestination(sourceUrl, destinationType);
        if (existingResultOpt.isEmpty()) {
            throw new IllegalArgumentException("No existing result found for source URL: " + sourceUrl + " and destination: " + destinationType);
        }

        VideoDownloadTaskResult existingResult = existingResultOpt.get();
        
        // Create a new result that references the existing destination ID
        VideoDownloadTaskResult newResult = entityFactory.newEntity(VideoDownloadTaskResult.class);
        newResult.setTask(newTask);
        newResult.setDestinationType(destinationType);
        newResult.setDestinationId(existingResult.getDestinationId());
        newResult.setFileName(existingResult.getFileName());
        newResult.setFileSizeBytes(existingResult.getFileSizeBytes());
        newResult.setFileFormat(existingResult.getFileFormat());
        newResult.setDurationSeconds(existingResult.getDurationSeconds());
        newResult.setResolution(existingResult.getResolution());
        newResult.setBitrate(existingResult.getBitrate());
        newResult.setFps(existingResult.getFps());
        newResult.setCodec(existingResult.getCodec());
        newResult.setThumbnailUrl(existingResult.getThumbnailUrl());
        newResult.setStatus(TaskStatus.COMPLETED);
        newResult.setIsPrimaryResult(true);
        
        // Set processing times to 0 since we're reusing
        newResult.setProcessingTimeMs(0L);
        newResult.setUploadTimeMs(0L);
        newResult.setUploadStartedAt(LocalDateTime.now());
        newResult.setUploadCompletedAt(LocalDateTime.now());

        VideoDownloadTaskResult savedResult = resultRepository.save(newResult);
        logger.info("Created reused result with ID: {} for task: {}", savedResult.getId(), newTask.getId());

        return savedResult;
    }
}
