package ru.aiivar.tg.yt.downloader.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.aiivar.tg.yt.downloader.entity.VideoDownloadTask;
import ru.aiivar.tg.yt.downloader.entity.enums.DestinationType;
import ru.aiivar.tg.yt.downloader.entity.enums.SourceType;
import ru.aiivar.tg.yt.downloader.entity.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for VideoDownloadTask entity
 */
@Repository
public interface VideoDownloadTaskRepository extends JpaRepository<VideoDownloadTask, String> {

    /**
     * Find tasks by status
     */
    List<VideoDownloadTask> findByStatus(TaskStatus status);

    /**
     * Find tasks by status with pagination
     */
    Page<VideoDownloadTask> findByStatus(TaskStatus status, Pageable pageable);

    /**
     * Find tasks by user ID
     */
    List<VideoDownloadTask> findByUserId(String userId);

    /**
     * Find tasks by user ID with pagination
     */
    Page<VideoDownloadTask> findByUserId(String userId, Pageable pageable);

    /**
     * Find tasks by chat ID
     */
    List<VideoDownloadTask> findByChatId(String chatId);

    /**
     * Find tasks by source type
     */
    List<VideoDownloadTask> findBySourceType(SourceType sourceType);

    /**
     * Find tasks by destination type
     */
    List<VideoDownloadTask> findByDestinationType(DestinationType destinationType);

    /**
     * Find tasks by source URL
     */
    Optional<VideoDownloadTask> findBySourceUrl(String sourceUrl);

    /**
     * Find tasks that can be retried (failed or cancelled with retry count < max retries)
     */
    @Query("SELECT t FROM VideoDownloadTask t WHERE t.status IN :statuses AND t.retryCount < t.maxRetries")
    List<VideoDownloadTask> findRetryableTasks(@Param("statuses") List<TaskStatus> statuses);

    /**
     * Find pending tasks ordered by priority and creation time
     */
    @Query("SELECT t FROM VideoDownloadTask t WHERE t.status = 'PENDING' ORDER BY t.priority DESC, t.createdAt ASC")
    List<VideoDownloadTask> findPendingTasksOrderedByPriority();

    /**
     * Find tasks created after a specific date
     */
    List<VideoDownloadTask> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find tasks created between two dates
     */
    List<VideoDownloadTask> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find tasks by user ID and status
     */
    List<VideoDownloadTask> findByUserIdAndStatus(String userId, TaskStatus status);

    /**
     * Find tasks by chat ID and status
     */
    List<VideoDownloadTask> findByChatIdAndStatus(String chatId, TaskStatus status);

    /**
     * Count tasks by status
     */
    long countByStatus(TaskStatus status);

    /**
     * Count tasks by user ID
     */
    long countByUserId(String userId);

    /**
     * Count tasks by source type
     */
    long countBySourceType(SourceType sourceType);

    /**
     * Count tasks by destination type
     */
    long countByDestinationType(DestinationType destinationType);

    /**
     * Find tasks that are stuck in processing state for too long
     */
    @Query("SELECT t FROM VideoDownloadTask t WHERE t.status = 'PROCESSING' AND t.updatedAt < :cutoffTime")
    List<VideoDownloadTask> findStuckProcessingTasks(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Find tasks by multiple criteria with pagination
     */
    @Query("SELECT t FROM VideoDownloadTask t WHERE " +
           "(:userId IS NULL OR t.userId = :userId) AND " +
           "(:chatId IS NULL OR t.chatId = :chatId) AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:sourceType IS NULL OR t.sourceType = :sourceType) AND " +
           "(:destinationType IS NULL OR t.destinationType = :destinationType)")
    Page<VideoDownloadTask> findByMultipleCriteria(
            @Param("userId") String userId,
            @Param("chatId") String chatId,
            @Param("status") TaskStatus status,
            @Param("sourceType") SourceType sourceType,
            @Param("destinationType") DestinationType destinationType,
            Pageable pageable);

    /**
     * Delete old completed tasks
     */
    @Query("DELETE FROM VideoDownloadTask t WHERE t.status = 'COMPLETED' AND t.createdAt < :cutoffDate")
    int deleteOldCompletedTasks(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Delete old failed tasks
     */
    @Query("DELETE FROM VideoDownloadTask t WHERE t.status = 'FAILED' AND t.retryCount >= t.maxRetries AND t.createdAt < :cutoffDate")
    int deleteOldFailedTasks(@Param("cutoffDate") LocalDateTime cutoffDate);
}
