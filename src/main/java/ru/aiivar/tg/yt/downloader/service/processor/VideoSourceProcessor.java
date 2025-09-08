package ru.aiivar.tg.yt.downloader.service.processor;

import ru.aiivar.tg.yt.downloader.entity.VideoDownloadTask;
import ru.aiivar.tg.yt.downloader.entity.enums.SourceType;

import java.io.File;
import java.util.Map;

/**
 * Interface for processing videos from different sources
 * Implementations handle downloading videos from specific platforms
 */
public interface VideoSourceProcessor {

    /**
     * Get the source type this processor handles
     */
    SourceType getSupportedSourceType();

    /**
     * Check if this processor can handle the given URL
     */
    boolean canProcess(String url);

    /**
     * Download video from the source
     */
    File downloadVideo(VideoDownloadTask task) throws Exception;

    /**
     * Get video metadata without downloading
     */
    VideoMetadata getVideoMetadata(String url) throws Exception;

    /**
     * Get available formats for the video
     */
    Map<String, Object> getAvailableFormats(String url) throws Exception;

    /**
     * Validate the download request
     */
    void validateRequest(VideoDownloadTask task) throws Exception;

    /**
     * Get processor-specific configuration
     */
    Map<String, Object> getProcessorConfig();

    /**
     * Set processor-specific configuration
     */
    void setProcessorConfig(Map<String, Object> config);

    /**
     * Video metadata inner class
     */
    class VideoMetadata {
        private String title;
        private String description;
        private String author;
        private String thumbnailUrl;
        private Long durationSeconds;
        private Long fileSizeBytes;
        private String format;
        private String resolution;
        private Long bitrate;
        private Double fps;
        private String codec;

        // Constructors
        public VideoMetadata() {}

        public VideoMetadata(String title, String description, String author, String thumbnailUrl,
                           Long durationSeconds, Long fileSizeBytes, String format, String resolution) {
            this.title = title;
            this.description = description;
            this.author = author;
            this.thumbnailUrl = thumbnailUrl;
            this.durationSeconds = durationSeconds;
            this.fileSizeBytes = fileSizeBytes;
            this.format = format;
            this.resolution = resolution;
        }

        // Getters and setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }

        public String getThumbnailUrl() { return thumbnailUrl; }
        public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

        public Long getDurationSeconds() { return durationSeconds; }
        public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }

        public Long getFileSizeBytes() { return fileSizeBytes; }
        public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }

        public String getResolution() { return resolution; }
        public void setResolution(String resolution) { this.resolution = resolution; }

        public Long getBitrate() { return bitrate; }
        public void setBitrate(Long bitrate) { this.bitrate = bitrate; }

        public Double getFps() { return fps; }
        public void setFps(Double fps) { this.fps = fps; }

        public String getCodec() { return codec; }
        public void setCodec(String codec) { this.codec = codec; }
    }
}
