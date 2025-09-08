package ru.aiivar.tg.yt.downloader.entity;

import jakarta.persistence.*;
import ru.aiivar.tg.yt.downloader.entity.enums.DestinationType;

import java.time.LocalDateTime;

/**
 * Entity representing the result of a video download task
 * This stores the outcome of the download process, including file IDs and metadata
 */
@Entity
@Table(name = "video_download_task_results")
public class VideoDownloadTaskResult extends BaseTaskEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private VideoDownloadTask task;

    @Column(name = "destination_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private DestinationType destinationType;

    @Column(name = "destination_id", length = 500)
    private String destinationId; // File ID, URL, or other destination identifier

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "file_format", length = 50)
    private String fileFormat;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Column(name = "resolution", length = 50)
    private String resolution;

    @Column(name = "bitrate")
    private Long bitrate;

    @Column(name = "fps")
    private Double fps;

    @Column(name = "codec", length = 100)
    private String codec;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Column(name = "download_url", length = 1000)
    private String downloadUrl; // Direct download URL if available

    @Column(name = "upload_started_at")
    private LocalDateTime uploadStartedAt;

    @Column(name = "upload_completed_at")
    private LocalDateTime uploadCompletedAt;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "upload_time_ms")
    private Long uploadTimeMs;

    @Column(name = "destination_metadata", columnDefinition = "TEXT")
    private String destinationMetadata; // JSON string for destination-specific metadata

    @Column(name = "is_primary_result", nullable = false)
    private Boolean isPrimaryResult = false; // Indicates if this is the main result for the task

    // Constructors
    public VideoDownloadTaskResult() {
        super();
    }

    public VideoDownloadTaskResult(VideoDownloadTask task, DestinationType destinationType) {
        super();
        this.task = task;
        this.destinationType = destinationType;
    }

    // Getters and Setters
    public VideoDownloadTask getTask() {
        return task;
    }

    public void setTask(VideoDownloadTask task) {
        this.task = task;
    }

    public DestinationType getDestinationType() {
        return destinationType;
    }

    public void setDestinationType(DestinationType destinationType) {
        this.destinationType = destinationType;
    }

    public String getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(String destinationId) {
        this.destinationId = destinationId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    public Long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public Long getBitrate() {
        return bitrate;
    }

    public void setBitrate(Long bitrate) {
        this.bitrate = bitrate;
    }

    public Double getFps() {
        return fps;
    }

    public void setFps(Double fps) {
        this.fps = fps;
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public LocalDateTime getUploadStartedAt() {
        return uploadStartedAt;
    }

    public void setUploadStartedAt(LocalDateTime uploadStartedAt) {
        this.uploadStartedAt = uploadStartedAt;
    }

    public LocalDateTime getUploadCompletedAt() {
        return uploadCompletedAt;
    }

    public void setUploadCompletedAt(LocalDateTime uploadCompletedAt) {
        this.uploadCompletedAt = uploadCompletedAt;
    }

    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public Long getUploadTimeMs() {
        return uploadTimeMs;
    }

    public void setUploadTimeMs(Long uploadTimeMs) {
        this.uploadTimeMs = uploadTimeMs;
    }

    public String getDestinationMetadata() {
        return destinationMetadata;
    }

    public void setDestinationMetadata(String destinationMetadata) {
        this.destinationMetadata = destinationMetadata;
    }

    public Boolean getIsPrimaryResult() {
        return isPrimaryResult;
    }

    public void setIsPrimaryResult(Boolean isPrimaryResult) {
        this.isPrimaryResult = isPrimaryResult;
    }

    // Helper methods
    public boolean isPrimary() {
        return Boolean.TRUE.equals(isPrimaryResult);
    }

    public void markAsPrimary() {
        this.isPrimaryResult = true;
    }

    public void markAsSecondary() {
        this.isPrimaryResult = false;
    }

    public String getFormattedFileSize() {
        if (fileSizeBytes == null) return "Unknown";
        
        long bytes = fileSizeBytes;
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    public String getFormattedDuration() {
        if (durationSeconds == null) return "Unknown";
        
        long seconds = durationSeconds;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%d:%02d", minutes, secs);
        }
    }
}
