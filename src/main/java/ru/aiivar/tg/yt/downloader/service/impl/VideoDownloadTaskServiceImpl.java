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
import ru.aiivar.tg.yt.downloader.entity.enums.SourceType;
import ru.aiivar.tg.yt.downloader.entity.enums.TaskStatus;
import ru.aiivar.tg.yt.downloader.model.VideoDownloadRequest;
import ru.aiivar.tg.yt.downloader.repository.VideoDownloadTaskRepository;
import ru.aiivar.tg.yt.downloader.service.EntityFactory;
import ru.aiivar.tg.yt.downloader.service.VideoDownloadTaskResultService;
import ru.aiivar.tg.yt.downloader.service.VideoDownloadTaskService;
import ru.aiivar.tg.yt.downloader.service.processor.VideoDestinationProcessor;
import ru.aiivar.tg.yt.downloader.service.processor.VideoSourceProcessor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of VideoDownloadTaskService
 */
@Service
@Transactional
public class VideoDownloadTaskServiceImpl implements VideoDownloadTaskService {

    private static final Logger logger = LoggerFactory.getLogger(VideoDownloadTaskServiceImpl.class);

    @Autowired
    private VideoDownloadTaskRepository taskRepository;

    @Autowired
    private VideoDownloadTaskResultService resultService;

    @Autowired
    private EntityFactory entityFactory;

    // Processor registry
    private final Map<SourceType, VideoSourceProcessor> sourceProcessors = new ConcurrentHashMap<>();
    private final Map<DestinationType, VideoDestinationProcessor> destinationProcessors = new ConcurrentHashMap<>();

    @Override
    public VideoDownloadTask createTask(VideoDownloadRequest request) {
        logger.info("Creating new video download task for URL: {}", request.getUrl());

        // Determine source type from URL
        SourceType sourceType = SourceType.fromUrl(request.getUrl());
        
        // For now, default to Telegram destination
        DestinationType destinationType = DestinationType.TELEGRAM;

        VideoDownloadTask task = entityFactory.newEntity(VideoDownloadTask.class);
        task.setSourceUrl(request.getUrl());
        task.setSourceType(sourceType);
        task.setDestinationType(destinationType);
        task.setUserId(request.getChatId()); // Using chatId as userId for now
        task.setChatId(request.getChatId());
        task.setRequestedFormat(request.getFormat());
        task.setRequestedQuality(request.getQuality());
        task.setRequestedResolution(request.getResolution());
        task.setStatus(TaskStatus.PENDING);

        VideoDownloadTask savedTask = taskRepository.save(task);
        logger.info("Created video download task with ID: {}", savedTask.getId());

        return savedTask;
    }

    @Override
    public VideoDownloadTask createTaskWithReuseCheck(VideoDownloadRequest request) {
        logger.info("Creating new video download task with reuse check for URL: {}", request.getUrl());

        // Determine source type from URL
        SourceType sourceType = SourceType.fromUrl(request.getUrl());
        
        // For now, default to Telegram destination
        DestinationType destinationType = DestinationType.TELEGRAM;

        // Check if we already have a completed result for this URL and destination
        if (resultService.hasExistingResult(request.getUrl(), destinationType)) {
            logger.info("Found existing result for URL: {} and destination: {}, creating task for reuse", 
                    request.getUrl(), destinationType);

            VideoDownloadTask task = entityFactory.newEntity(VideoDownloadTask.class);
            task.setSourceUrl(request.getUrl());
            task.setSourceType(sourceType);
            task.setDestinationType(destinationType);
            task.setUserId(request.getChatId());
            task.setChatId(request.getChatId());
            task.setRequestedFormat(request.getFormat());
            task.setRequestedQuality(request.getQuality());
            task.setRequestedResolution(request.getResolution());
            task.setStatus(TaskStatus.PENDING);

            VideoDownloadTask savedTask = taskRepository.save(task);
            
            // Create a reused result immediately
            try {
                VideoDownloadTaskResult reusedResult = resultService.reuseExistingResult(request.getUrl(), destinationType, savedTask);
                logger.info("Created reused result with ID: {} for task: {}", reusedResult.getId(), savedTask.getId());
                
                // Send the video to the user since we're reusing an existing result
                try {
                    VideoDestinationProcessor destinationProcessor = getDestinationProcessor(destinationType);
                    destinationProcessor.sendVideoById(reusedResult.getDestinationId(), savedTask);
                    logger.info("Successfully sent reused video to user for task: {}", savedTask.getId());
                } catch (Exception sendError) {
                    logger.error("Error sending reused video to user for task: {}", savedTask.getId(), sendError);
                    // Don't fail the task, just log the error
                }
                
                // Mark task as completed since we're reusing an existing result
                savedTask.setStatus(TaskStatus.COMPLETED);
                savedTask.setDownloadCompletedAt(LocalDateTime.now());
                taskRepository.save(savedTask);
                
                logger.info("Task {} completed immediately by reusing existing result", savedTask.getId());
                
            } catch (Exception e) {
                logger.error("Error reusing existing result for task: {}", savedTask.getId(), e);
                // Task remains in PENDING status for normal processing
            }

            return savedTask;
        } else {
            logger.info("No existing result found for URL: {} and destination: {}, creating new task", 
                    request.getUrl(), destinationType);
            return createTask(request);
        }
    }

    @Override
    public VideoDownloadTask createTask(String sourceUrl, SourceType sourceType, DestinationType destinationType,
                                      String userId, String chatId, String destinationConfig) {
        logger.info("Creating new video download task for URL: {}", sourceUrl);

        VideoDownloadTask task = entityFactory.newEntity(VideoDownloadTask.class);
        task.setSourceUrl(sourceUrl);
        task.setSourceType(sourceType);
        task.setDestinationType(destinationType);
        task.setUserId(userId);
        task.setChatId(chatId);
        task.setDestinationConfig(destinationConfig);
        task.setStatus(TaskStatus.PENDING);

        VideoDownloadTask savedTask = taskRepository.save(task);
        logger.info("Created video download task with ID: {}", savedTask.getId());

        return savedTask;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VideoDownloadTask> getTaskById(String taskId) {
        return taskRepository.findById(taskId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VideoDownloadTask> getAllTasks(Pageable pageable) {
        return taskRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VideoDownloadTask> getTasksByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VideoDownloadTask> getTasksByStatus(TaskStatus status, Pageable pageable) {
        return taskRepository.findByStatus(status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VideoDownloadTask> getTasksByUserId(String userId) {
        return taskRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VideoDownloadTask> getTasksByUserId(String userId, Pageable pageable) {
        return taskRepository.findByUserId(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VideoDownloadTask> getTasksByChatId(String chatId) {
        return taskRepository.findByChatId(chatId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VideoDownloadTask> getPendingTasksOrderedByPriority() {
        return taskRepository.findPendingTasksOrderedByPriority();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VideoDownloadTask> getRetryableTasks() {
        return taskRepository.findRetryableTasks(List.of(TaskStatus.FAILED, TaskStatus.CANCELLED));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VideoDownloadTask> getStuckProcessingTasks() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(2); // Tasks stuck for more than 2 hours
        return taskRepository.findStuckProcessingTasks(cutoffTime);
    }

    @Override
    public VideoDownloadTask updateTaskStatus(String taskId, TaskStatus status) {
        return updateTaskStatus(taskId, status, null);
    }

    @Override
    public VideoDownloadTask updateTaskStatus(String taskId, TaskStatus status, String errorMessage) {
        logger.info("Updating task {} status to {} with error: {}", taskId, status, errorMessage);

        Optional<VideoDownloadTask> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            throw new IllegalArgumentException("Task not found with ID: " + taskId);
        }

        VideoDownloadTask task = taskOpt.get();
        task.setStatus(status);
        task.setErrorMessage(errorMessage);

        VideoDownloadTask updatedTask = taskRepository.save(task);
        logger.info("Updated task {} status to {}", taskId, status);

        return updatedTask;
    }

    @Override
    public VideoDownloadTask markTaskAsStarted(String taskId) {
        logger.info("Marking task {} as started", taskId);

        Optional<VideoDownloadTask> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            throw new IllegalArgumentException("Task not found with ID: " + taskId);
        }

        VideoDownloadTask task = taskOpt.get();
        task.setStatus(TaskStatus.PROCESSING);
        task.setDownloadStartedAt(LocalDateTime.now());

        VideoDownloadTask updatedTask = taskRepository.save(task);
        logger.info("Marked task {} as started", taskId);

        return updatedTask;
    }

    @Override
    public VideoDownloadTask markTaskAsCompleted(String taskId) {
        logger.info("Marking task {} as completed", taskId);

        Optional<VideoDownloadTask> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            throw new IllegalArgumentException("Task not found with ID: " + taskId);
        }

        VideoDownloadTask task = taskOpt.get();
        task.setStatus(TaskStatus.COMPLETED);
        task.setDownloadCompletedAt(LocalDateTime.now());

        VideoDownloadTask updatedTask = taskRepository.save(task);
        logger.info("Marked task {} as completed", taskId);

        return updatedTask;
    }

    @Override
    public VideoDownloadTask markTaskAsFailed(String taskId, String errorMessage) {
        logger.info("Marking task {} as failed with error: {}", taskId, errorMessage);

        Optional<VideoDownloadTask> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            throw new IllegalArgumentException("Task not found with ID: " + taskId);
        }

        VideoDownloadTask task = taskOpt.get();
        task.setStatus(TaskStatus.FAILED);
        task.setErrorMessage(errorMessage);

        VideoDownloadTask updatedTask = taskRepository.save(task);
        logger.info("Marked task {} as failed", taskId);

        return updatedTask;
    }

    @Override
    public VideoDownloadTask retryTask(String taskId) {
        logger.info("Retrying task {}", taskId);

        Optional<VideoDownloadTask> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            throw new IllegalArgumentException("Task not found with ID: " + taskId);
        }

        VideoDownloadTask task = taskOpt.get();
        if (!task.canRetry()) {
            throw new IllegalStateException("Task cannot be retried: " + taskId);
        }

        task.incrementRetryCount();
        task.setStatus(TaskStatus.PENDING);
        task.setErrorMessage(null);

        VideoDownloadTask updatedTask = taskRepository.save(task);
        logger.info("Retried task {} (attempt {})", taskId, task.getRetryCount());

        return updatedTask;
    }

    @Override
    public VideoDownloadTask cancelTask(String taskId) {
        logger.info("Cancelling task {}", taskId);

        Optional<VideoDownloadTask> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            throw new IllegalArgumentException("Task not found with ID: " + taskId);
        }

        VideoDownloadTask task = taskOpt.get();
        task.setStatus(TaskStatus.CANCELLED);

        VideoDownloadTask updatedTask = taskRepository.save(task);
        logger.info("Cancelled task {}", taskId);

        return updatedTask;
    }

    @Override
    public void deleteTask(String taskId) {
        logger.info("Deleting task {}", taskId);

        if (!taskRepository.existsById(taskId)) {
            throw new IllegalArgumentException("Task not found with ID: " + taskId);
        }

        // Delete associated results first
        resultService.deleteResultsByTask(taskId);

        // Delete the task
        taskRepository.deleteById(taskId);
        logger.info("Deleted task {}", taskId);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskStatistics getTaskStatistics() {
        long totalTasks = taskRepository.count();
        long pendingTasks = taskRepository.countByStatus(TaskStatus.PENDING);
        long processingTasks = taskRepository.countByStatus(TaskStatus.PROCESSING);
        long completedTasks = taskRepository.countByStatus(TaskStatus.COMPLETED);
        long failedTasks = taskRepository.countByStatus(TaskStatus.FAILED);
        long cancelledTasks = taskRepository.countByStatus(TaskStatus.CANCELLED);
        long retryableTasks = getRetryableTasks().size();

        return new TaskStatistics(totalTasks, pendingTasks, processingTasks, 
                                completedTasks, failedTasks, cancelledTasks, retryableTasks);
    }

    @Override
    public int cleanupOldCompletedTasks(LocalDateTime cutoffDate) {
        logger.info("Cleaning up old completed tasks before {}", cutoffDate);
        return taskRepository.deleteOldCompletedTasks(cutoffDate);
    }

    @Override
    public int cleanupOldFailedTasks(LocalDateTime cutoffDate) {
        logger.info("Cleaning up old failed tasks before {}", cutoffDate);
        return taskRepository.deleteOldFailedTasks(cutoffDate);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VideoDownloadTask> getTasksByCriteria(String userId, String chatId, TaskStatus status,
                                                    SourceType sourceType, DestinationType destinationType,
                                                    Pageable pageable) {
        return taskRepository.findByMultipleCriteria(userId, chatId, status, sourceType, destinationType, pageable);
    }

    @Override
    public VideoDownloadTaskResult processTask(String taskId) {
        logger.info("Processing task {}", taskId);

        Optional<VideoDownloadTask> taskOpt = getTaskById(taskId);
        if (taskOpt.isEmpty()) {
            throw new IllegalArgumentException("Task not found with ID: " + taskId);
        }

        VideoDownloadTask task = taskOpt.get();
        
        try {
            // Mark task as started
            markTaskAsStarted(taskId);

            // Get processors
            VideoSourceProcessor sourceProcessor = getSourceProcessor(task.getSourceType());
            VideoDestinationProcessor destinationProcessor = getDestinationProcessor(task.getDestinationType());

            // Validate request
            sourceProcessor.validateRequest(task);
            destinationProcessor.validateRequest(task);

            // Download video
            java.io.File downloadedFile = sourceProcessor.downloadVideo(task);

            // Create result
            VideoDownloadTaskResult result = resultService.createResult(task, task.getDestinationType());
            result.setStatus(TaskStatus.PROCESSING);

            // Upload video
            String destinationId = destinationProcessor.uploadVideo(downloadedFile, task, result);

            // Mark result as completed
            result.setStatus(TaskStatus.COMPLETED);
            result.setDestinationId(destinationId);
            resultService.updateResultStatus(result.getId(), TaskStatus.COMPLETED);

            // Send video to destination
            destinationProcessor.sendVideoById(destinationId, task);

            // Mark task as completed
            markTaskAsCompleted(taskId);

            logger.info("Successfully processed task {}", taskId);
            return result;

        } catch (Exception e) {
            logger.error("Error processing task {}", taskId, e);
            markTaskAsFailed(taskId, e.getMessage());
            throw new RuntimeException("Failed to process task: " + e.getMessage(), e);
        }
    }

    @Override
    public List<VideoDownloadTaskResult> processTasks(List<String> taskIds) {
        logger.info("Processing {} tasks", taskIds.size());

        List<VideoDownloadTaskResult> results = new java.util.ArrayList<>();
        for (String taskId : taskIds) {
            try {
                VideoDownloadTaskResult result = processTask(taskId);
                results.add(result);
            } catch (Exception e) {
                logger.error("Failed to process task {}", taskId, e);
                // Continue with other tasks
            }
        }

        logger.info("Processed {} out of {} tasks successfully", results.size(), taskIds.size());
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VideoDownloadTaskResult> getTaskResults(String taskId) {
        return resultService.getResultsByTaskId(taskId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VideoDownloadTaskResult> getPrimaryTaskResult(String taskId) {
        return resultService.getPrimaryResultByTaskId(taskId);
    }

    // Helper methods for processor management
    public void registerSourceProcessor(VideoSourceProcessor processor) {
        sourceProcessors.put(processor.getSupportedSourceType(), processor);
        logger.info("Registered source processor for type: {}", processor.getSupportedSourceType());
    }

    public void registerDestinationProcessor(VideoDestinationProcessor processor) {
        destinationProcessors.put(processor.getSupportedDestinationType(), processor);
        logger.info("Registered destination processor for type: {}", processor.getSupportedDestinationType());
    }

    private VideoSourceProcessor getSourceProcessor(SourceType sourceType) {
        VideoSourceProcessor processor = sourceProcessors.get(sourceType);
        if (processor == null) {
            throw new UnsupportedOperationException("No processor found for source type: " + sourceType);
        }
        return processor;
    }

    private VideoDestinationProcessor getDestinationProcessor(DestinationType destinationType) {
        VideoDestinationProcessor processor = destinationProcessors.get(destinationType);
        if (processor == null) {
            throw new UnsupportedOperationException("No processor found for destination type: " + destinationType);
        }
        return processor;
    }
}
