package ru.aiivar.tg.yt.downloader.entity;

import jakarta.persistence.*;
import ru.aiivar.tg.yt.downloader.entity.enums.DestinationType;
import ru.aiivar.tg.yt.downloader.entity.enums.SourceType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a video download task
 * This is the main entity that gets created when a user requests a video download
 */
@Entity
@Table(name = "video_download_tasks")
public class VideoDownloadTask extends BaseTaskEntity {

    @Column(name = "source_url", nullable = false, length = 1000)
    private String sourceUrl;

    @Column(name = "source_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private SourceType sourceType;

    @Column(name = "destination_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private DestinationType destinationType;

    @Column(name = "destination_config", columnDefinition = "TEXT")
    private String destinationConfig; // JSON string for destination-specific configuration

    @Column(name = "user_id", length = 100)
    private String userId; // Telegram user ID or other platform user ID

    @Column(name = "chat_id", length = 100)
    private String chatId; // Telegram chat ID or other platform chat ID

    @Column(name = "requested_format", length = 50)
    private String requestedFormat = "mp4";

    @Column(name = "requested_quality", length = 50)
    private String requestedQuality = "720p";

    @Column(name = "requested_resolution", length = 50)
    private String requestedResolution = "720p";

    @Column(name = "download_started_at")
    private LocalDateTime downloadStartedAt;

    @Column(name = "download_completed_at")
    private LocalDateTime downloadCompletedAt;

    @Column(name = "estimated_duration_seconds")
    private Long estimatedDurationSeconds;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "temp_file_path", length = 500)
    private String tempFilePath;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VideoDownloadTaskResult> results = new ArrayList<>();

    // Constructors
    public VideoDownloadTask() {
        super();
    }

    public VideoDownloadTask(String sourceUrl, SourceType sourceType, DestinationType destinationType) {
        super();
        this.sourceUrl = sourceUrl;
        this.sourceType = sourceType;
        this.destinationType = destinationType;
    }

    // Getters and Setters
    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public DestinationType getDestinationType() {
        return destinationType;
    }

    public void setDestinationType(DestinationType destinationType) {
        this.destinationType = destinationType;
    }

    public String getDestinationConfig() {
        return destinationConfig;
    }

    public void setDestinationConfig(String destinationConfig) {
        this.destinationConfig = destinationConfig;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getRequestedFormat() {
        return requestedFormat;
    }

    public void setRequestedFormat(String requestedFormat) {
        this.requestedFormat = requestedFormat;
    }

    public String getRequestedQuality() {
        return requestedQuality;
    }

    public void setRequestedQuality(String requestedQuality) {
        this.requestedQuality = requestedQuality;
    }

    public String getRequestedResolution() {
        return requestedResolution;
    }

    public void setRequestedResolution(String requestedResolution) {
        this.requestedResolution = requestedResolution;
    }

    public LocalDateTime getDownloadStartedAt() {
        return downloadStartedAt;
    }

    public void setDownloadStartedAt(LocalDateTime downloadStartedAt) {
        this.downloadStartedAt = downloadStartedAt;
    }

    public LocalDateTime getDownloadCompletedAt() {
        return downloadCompletedAt;
    }

    public void setDownloadCompletedAt(LocalDateTime downloadCompletedAt) {
        this.downloadCompletedAt = downloadCompletedAt;
    }

    public Long getEstimatedDurationSeconds() {
        return estimatedDurationSeconds;
    }

    public void setEstimatedDurationSeconds(Long estimatedDurationSeconds) {
        this.estimatedDurationSeconds = estimatedDurationSeconds;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getTempFilePath() {
        return tempFilePath;
    }

    public void setTempFilePath(String tempFilePath) {
        this.tempFilePath = tempFilePath;
    }

    public List<VideoDownloadTaskResult> getResults() {
        return results;
    }

    public void setResults(List<VideoDownloadTaskResult> results) {
        this.results = results;
    }

    public void addResult(VideoDownloadTaskResult result) {
        results.add(result);
        result.setTask(this);
    }

    public void removeResult(VideoDownloadTaskResult result) {
        results.remove(result);
        result.setTask(null);
    }

    // Helper methods
    public boolean isCompleted() {
        return getStatus() == ru.aiivar.tg.yt.downloader.entity.enums.TaskStatus.COMPLETED;
    }

    public boolean isFailed() {
        return getStatus() == ru.aiivar.tg.yt.downloader.entity.enums.TaskStatus.FAILED;
    }

    public boolean canRetry() {
        return getRetryCount() < getMaxRetries() && 
               (isFailed() || getStatus() == ru.aiivar.tg.yt.downloader.entity.enums.TaskStatus.CANCELLED);
    }

    public void incrementRetryCount() {
        this.setRetryCount(this.getRetryCount() + 1);
    }
}
