package ru.aiivar.tg.yt.downloader.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.aiivar.tg.yt.downloader.entity.VideoDownloadTask;
import ru.aiivar.tg.yt.downloader.entity.VideoDownloadTaskResult;
import ru.aiivar.tg.yt.downloader.entity.enums.DestinationType;
import ru.aiivar.tg.yt.downloader.entity.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for VideoDownloadTaskResult entity
 */
@Repository
public interface VideoDownloadTaskResultRepository extends JpaRepository<VideoDownloadTaskResult, String> {

    /**
     * Find results by task
     */
    List<VideoDownloadTaskResult> findByTask(VideoDownloadTask task);

    /**
     * Find results by task ID
     */
    List<VideoDownloadTaskResult> findByTaskId(String taskId);

    /**
     * Find primary result for a task
     */
    @Query("SELECT r FROM VideoDownloadTaskResult r WHERE r.task.id = :taskId AND r.isPrimaryResult = true")
    Optional<VideoDownloadTaskResult> findPrimaryResultByTaskId(@Param("taskId") String taskId);

    /**
     * Find results by destination type
     */
    List<VideoDownloadTaskResult> findByDestinationType(DestinationType destinationType);

    /**
     * Find results by destination type with pagination
     */
    Page<VideoDownloadTaskResult> findByDestinationType(DestinationType destinationType, Pageable pageable);

    /**
     * Find results by destination ID
     */
    Optional<VideoDownloadTaskResult> findByDestinationId(String destinationId);

    /**
     * Find results by status
     */
    List<VideoDownloadTaskResult> findByStatus(TaskStatus status);

    /**
     * Find results by status with pagination
     */
    Page<VideoDownloadTaskResult> findByStatus(TaskStatus status, Pageable pageable);

    /**
     * Find results created after a specific date
     */
    List<VideoDownloadTaskResult> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find results created between two dates
     */
    List<VideoDownloadTaskResult> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find results by file format
     */
    List<VideoDownloadTaskResult> findByFileFormat(String fileFormat);

    /**
     * Find results by resolution
     */
    List<VideoDownloadTaskResult> findByResolution(String resolution);

    /**
     * Find results with file size greater than specified
     */
    List<VideoDownloadTaskResult> findByFileSizeBytesGreaterThan(Long fileSizeBytes);

    /**
     * Find results with file size less than specified
     */
    List<VideoDownloadTaskResult> findByFileSizeBytesLessThan(Long fileSizeBytes);

    /**
     * Find results by multiple criteria with pagination
     */
    @Query("SELECT r FROM VideoDownloadTaskResult r WHERE " +
           "(:taskId IS NULL OR r.task.id = :taskId) AND " +
           "(:destinationType IS NULL OR r.destinationType = :destinationType) AND " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:fileFormat IS NULL OR r.fileFormat = :fileFormat) AND " +
           "(:isPrimary IS NULL OR r.isPrimaryResult = :isPrimary)")
    Page<VideoDownloadTaskResult> findByMultipleCriteria(
            @Param("taskId") String taskId,
            @Param("destinationType") DestinationType destinationType,
            @Param("status") TaskStatus status,
            @Param("fileFormat") String fileFormat,
            @Param("isPrimary") Boolean isPrimary,
            Pageable pageable);

    /**
     * Count results by task
     */
    long countByTask(VideoDownloadTask task);

    /**
     * Count results by task ID
     */
    long countByTaskId(String taskId);

    /**
     * Count results by destination type
     */
    long countByDestinationType(DestinationType destinationType);

    /**
     * Count results by status
     */
    long countByStatus(TaskStatus status);

    /**
     * Get total file size of all completed results
     */
    @Query("SELECT COALESCE(SUM(r.fileSizeBytes), 0) FROM VideoDownloadTaskResult r WHERE r.status = ru.aiivar.tg.yt.downloader.entity.enums.TaskStatus.COMPLETED")
    Long getTotalFileSizeOfCompletedResults();

    /**
     * Get average file size by destination type
     */
    @Query("SELECT AVG(r.fileSizeBytes) FROM VideoDownloadTaskResult r WHERE r.destinationType = :destinationType AND r.fileSizeBytes IS NOT NULL")
    Double getAverageFileSizeByDestinationType(@Param("destinationType") DestinationType destinationType);

    /**
     * Get average processing time by destination type
     */
    @Query("SELECT AVG(r.processingTimeMs) FROM VideoDownloadTaskResult r WHERE r.destinationType = :destinationType AND r.processingTimeMs IS NOT NULL")
    Double getAverageProcessingTimeByDestinationType(@Param("destinationType") DestinationType destinationType);

    /**
     * Find results that need cleanup (old completed results)
     */
    @Query("SELECT r FROM VideoDownloadTaskResult r WHERE r.status = ru.aiivar.tg.yt.downloader.entity.enums.TaskStatus.COMPLETED AND r.createdAt < :cutoffDate")
    List<VideoDownloadTaskResult> findResultsForCleanup(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Delete old completed results
     */
    @Query("DELETE FROM VideoDownloadTaskResult r WHERE r.status = ru.aiivar.tg.yt.downloader.entity.enums.TaskStatus.COMPLETED AND r.createdAt < :cutoffDate")
    int deleteOldCompletedResults(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find results by task and destination type
     */
    List<VideoDownloadTaskResult> findByTaskAndDestinationType(VideoDownloadTask task, DestinationType destinationType);

    /**
     * Find results by task ID and destination type
     */
    List<VideoDownloadTaskResult> findByTaskIdAndDestinationType(String taskId, DestinationType destinationType);

    /**
     * Find existing completed results by source URL and destination type
     * This is used to reuse existing downloads instead of re-downloading
     */
    @Query("SELECT r FROM VideoDownloadTaskResult r " +
           "JOIN r.task t " +
           "WHERE t.sourceUrl = :sourceUrl " +
           "AND r.destinationType = :destinationType " +
           "AND r.status = ru.aiivar.tg.yt.downloader.entity.enums.TaskStatus.COMPLETED " +
           "AND r.destinationId IS NOT NULL " +
           "ORDER BY r.createdAt DESC")
    List<VideoDownloadTaskResult> findExistingResultsBySourceUrlAndDestination(
            @Param("sourceUrl") String sourceUrl, 
            @Param("destinationType") DestinationType destinationType);

    /**
     * Find the most recent completed result by source URL and destination type
     */
    @Query("SELECT r FROM VideoDownloadTaskResult r " +
           "JOIN r.task t " +
           "WHERE t.sourceUrl = :sourceUrl " +
           "AND r.destinationType = :destinationType " +
           "AND r.status = ru.aiivar.tg.yt.downloader.entity.enums.TaskStatus.COMPLETED " +
           "AND r.destinationId IS NOT NULL " +
           "ORDER BY r.createdAt DESC")
    Optional<VideoDownloadTaskResult> findMostRecentResultBySourceUrlAndDestination(
            @Param("sourceUrl") String sourceUrl, 
            @Param("destinationType") DestinationType destinationType);

    /**
     * Count existing results by source URL and destination type
     */
    @Query("SELECT COUNT(r) FROM VideoDownloadTaskResult r " +
           "JOIN r.task t " +
           "WHERE t.sourceUrl = :sourceUrl " +
           "AND r.destinationType = :destinationType " +
           "AND r.status = ru.aiivar.tg.yt.downloader.entity.enums.TaskStatus.COMPLETED")
    long countExistingResultsBySourceUrlAndDestination(
            @Param("sourceUrl") String sourceUrl, 
            @Param("destinationType") DestinationType destinationType);
}
