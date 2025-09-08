package ru.aiivar.tg.yt.downloader.service.processor.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.aiivar.tg.yt.downloader.entity.VideoDownloadTask;
import ru.aiivar.tg.yt.downloader.entity.VideoDownloadTaskResult;
import ru.aiivar.tg.yt.downloader.entity.enums.DestinationType;
import ru.aiivar.tg.yt.downloader.service.TelegramFileService;
import ru.aiivar.tg.yt.downloader.service.processor.VideoDestinationProcessor;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Telegram destination processor implementation
 */
@Component
public class TelegramDestinationProcessor implements VideoDestinationProcessor {

    private static final Logger logger = LoggerFactory.getLogger(TelegramDestinationProcessor.class);

    @Autowired
    private TelegramFileService telegramFileService;

    private Map<String, Object> config = new HashMap<>();

    @Override
    public DestinationType getSupportedDestinationType() {
        return DestinationType.TELEGRAM;
    }

    @Override
    public String uploadVideo(File videoFile, VideoDownloadTask task, VideoDownloadTaskResult result) throws Exception {
        logger.info("Starting Telegram video upload for task: {}", task.getId());

        try {
            // Create caption for the video
            String caption = buildVideoCaption(task);

            long fileSize = videoFile.length();
            long maxOfficialApiSize = 50 * 1024 * 1024; // 50 MB

            String telegramFileId;

            // Check if we should use local API for large files
            if (fileSize > maxOfficialApiSize) {
                logger.info("File size {} MB exceeds 50MB limit, using local Bot API server",
                        fileSize / (1024 * 1024));
                telegramFileId = telegramFileService.uploadLargeFileToTelegram(videoFile, caption, true);
            } else {
                logger.info("File size {} MB is within 50MB limit, using official API",
                        fileSize / (1024 * 1024));
                telegramFileId = telegramFileService.uploadVideoToTelegram(videoFile, caption);
            }

            logger.info("Upload to Telegram completed. File ID: {}", telegramFileId);

            // Update result with file information
            result.setDestinationId(telegramFileId);
            result.setFileName(videoFile.getName());
            result.setFileSizeBytes(fileSize);
            result.setFileFormat(getFileExtension(videoFile.getName()));

            return telegramFileId;

        } catch (Exception e) {
            logger.error("Error uploading video to Telegram for task: {}", task.getId(), e);
            throw new Exception("Failed to upload video to Telegram: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendVideoById(String destinationId, VideoDownloadTask task) throws Exception {
        logger.info("Sending video {} to Telegram chat: {}", destinationId, task.getChatId());

        try {
            telegramFileService.sendVideoByFileIdToChat(destinationId, task.getChatId());
            logger.info("Video sent successfully to Telegram chat: {}", task.getChatId());

        } catch (Exception e) {
            logger.error("Error sending video to Telegram chat: {}", task.getChatId(), e);
            throw new Exception("Failed to send video to Telegram: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getVideoInfo(String destinationId) throws Exception {
        logger.info("Getting video info from Telegram for file ID: {}", destinationId);

        try {
            Map<String, Object> fileInfo = telegramFileService.getFileInfo(destinationId);
            logger.info("Retrieved video info from Telegram for file ID: {}", destinationId);
            return fileInfo;

        } catch (Exception e) {
            logger.error("Error getting video info from Telegram for file ID: {}", destinationId, e);
            throw new Exception("Failed to get video info from Telegram: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteVideo(String destinationId) throws Exception {
        logger.info("Deleting video from Telegram with file ID: {}", destinationId);

        try {
            // Note: Telegram Bot API doesn't provide a direct way to delete files
            // Files are automatically deleted after a certain period
            // This method is here for interface compliance
            logger.warn("Telegram Bot API doesn't support file deletion. File will be automatically deleted by Telegram.");
            return true;

        } catch (Exception e) {
            logger.error("Error deleting video from Telegram with file ID: {}", destinationId, e);
            return false;
        }
    }

    @Override
    public void validateRequest(VideoDownloadTask task) throws Exception {
        if (task.getChatId() == null || task.getChatId().trim().isEmpty()) {
            throw new IllegalArgumentException("Chat ID cannot be null or empty for Telegram destination");
        }

        if (task.getUserId() == null || task.getUserId().trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty for Telegram destination");
        }

        // Check if Telegram API is available
        if (!telegramFileService.checkTelegramApiHealth()) {
            throw new Exception("Telegram Bot API is not available");
        }

        logger.info("Telegram request validation passed for task: {}", task.getId());
    }

    @Override
    public Map<String, Object> getProcessorConfig() {
        Map<String, Object> telegramConfig = new HashMap<>(config);
        telegramConfig.put("useLocalApi", telegramFileService.isUseLocalApi());
        telegramConfig.put("localApiUrl", telegramFileService.getLocalApiUrl());
        telegramConfig.put("localCredentialsPath", telegramFileService.getLocalCredentialsPath());
        return telegramConfig;
    }

    @Override
    public void setProcessorConfig(Map<String, Object> config) {
        this.config = new HashMap<>(config);
    }

    @Override
    public boolean isDestinationAvailable() {
        return telegramFileService.checkTelegramApiHealth();
    }

    @Override
    public long getMaxFileSize() {
        // Telegram Bot API supports up to 2GB with local Bot API server
        return telegramFileService.isUseLocalApi() ? 2L * 1024 * 1024 * 1024 : 50L * 1024 * 1024;
    }

    @Override
    public String[] getSupportedFormats() {
        return new String[]{"mp4", "avi", "mov", "wmv", "flv", "webm", "mkv"};
    }

    @Override
    public UploadProgressCallback getUploadProgressCallback() {
        return new UploadProgressCallback() {
            @Override
            public void onProgress(long bytesUploaded, long totalBytes, double percentage) {
                logger.debug("Upload progress: {}% ({} / {} bytes)", 
                        String.format("%.2f", percentage), bytesUploaded, totalBytes);
            }

            @Override
            public void onComplete(String destinationId) {
                logger.info("Upload completed successfully. File ID: {}", destinationId);
            }

            @Override
            public void onError(Exception error) {
                logger.error("Upload failed", error);
            }
        };
    }

    private String buildVideoCaption(VideoDownloadTask task) {
        StringBuilder caption = new StringBuilder();
        
        caption.append("📹 Video Downloaded\n");
        caption.append("🔗 Source: ").append(task.getSourceUrl()).append("\n");
        caption.append("📅 Download ID: ").append(task.getId()).append("\n");
        
        if (task.getSourceType() != null) {
            caption.append("🌐 Platform: ").append(task.getSourceType().getDisplayName()).append("\n");
        }
        
        return caption.toString();
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "unknown";
        }
        
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        
        return "unknown";
    }
}
