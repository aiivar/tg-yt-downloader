package ru.aiivar.tg.yt.downloader.service.processor;

import ru.aiivar.tg.yt.downloader.entity.VideoDownloadTask;
import ru.aiivar.tg.yt.downloader.entity.VideoDownloadTaskResult;
import ru.aiivar.tg.yt.downloader.entity.enums.DestinationType;

import java.io.File;
import java.util.Map;

/**
 * Interface for uploading videos to different destinations
 * Implementations handle uploading videos to specific platforms
 */
public interface VideoDestinationProcessor {

    /**
     * Get the destination type this processor handles
     */
    DestinationType getSupportedDestinationType();

    /**
     * Upload video file to the destination
     */
    String uploadVideo(File videoFile, VideoDownloadTask task, VideoDownloadTaskResult result) throws Exception;

    /**
     * Send video by ID to a specific recipient
     */
    void sendVideoById(String destinationId, VideoDownloadTask task) throws Exception;

    /**
     * Get video information from destination
     */
    Map<String, Object> getVideoInfo(String destinationId) throws Exception;

    /**
     * Delete video from destination
     */
    boolean deleteVideo(String destinationId) throws Exception;

    /**
     * Validate the upload request
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
     * Check if the destination is available
     */
    boolean isDestinationAvailable();

    /**
     * Get maximum file size supported by this destination
     */
    long getMaxFileSize();

    /**
     * Get supported file formats
     */
    String[] getSupportedFormats();

    /**
     * Get upload progress callback (optional)
     */
    default UploadProgressCallback getUploadProgressCallback() {
        return null;
    }

    /**
     * Upload progress callback interface
     */
    interface UploadProgressCallback {
        void onProgress(long bytesUploaded, long totalBytes, double percentage);
        void onComplete(String destinationId);
        void onError(Exception error);
    }
}
