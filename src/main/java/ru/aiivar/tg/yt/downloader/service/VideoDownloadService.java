package ru.aiivar.tg.yt.downloader.service;

import com.jfposton.ytdlp.YtDlp;
import com.jfposton.ytdlp.YtDlpException;
import com.jfposton.ytdlp.YtDlpRequest;
import com.jfposton.ytdlp.YtDlpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.aiivar.tg.yt.downloader.model.VideoDownloadRequest;
import ru.aiivar.tg.yt.downloader.model.VideoDownloadResponse;
import ru.aiivar.tg.yt.downloader.repository.VideoRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class VideoDownloadService {

    private static final Logger logger = LoggerFactory.getLogger(VideoDownloadService.class);
    
    private static final String TEMP_DIR = "temp_downloads";
    private static final String MP4_FORMAT = "mp4";
    private static final String RESOLUTION_720P = "720p";

    @Autowired
    private VideoRepository videoRepository;

    public VideoDownloadResponse downloadVideo(VideoDownloadRequest request) {
        String downloadId = UUID.randomUUID().toString();
        logger.info("Starting video download with ID: {} for URL: {}", downloadId, request.getUrl());
        
        Path tempDir = null;
        File downloadedFile = null;
        
        try {
            // Create temp directory
            tempDir = createTempDirectory(downloadId);
            logger.info("Created temp directory: {}", tempDir);
            
            // Download video
            downloadedFile = downloadVideoFile(request, tempDir, downloadId);
            logger.info("Video downloaded successfully: {}", downloadedFile.getAbsolutePath());
            
            // Mock upload to Telegram
            String telegramFileId = mockUploadToTelegram(downloadedFile, downloadId);
            logger.info("Mock upload to Telegram completed with file ID: {}", telegramFileId);
            
            // Get file size
            long fileSize = downloadedFile.length();
            logger.info("File size: {} bytes", fileSize);
            
            return VideoDownloadResponse.builder()
                    .success(true)
                    .message("Video downloaded and uploaded successfully")
                    .downloadId(downloadId)
                    .fileName(downloadedFile.getName())
                    .fileSize(fileSize)
                    .telegramFileId(telegramFileId)
                    .build();
                    
        } catch (Exception e) {
            logger.error("Error during video download process for ID: {}", downloadId, e);
            return VideoDownloadResponse.builder()
                    .success(false)
                    .downloadId(downloadId)
                    .error("Download failed: " + e.getMessage())
                    .build();
        } finally {
            // Clean up temp directory
            cleanupTempDirectory(tempDir, downloadId);
        }
    }

    private Path createTempDirectory(String downloadId) throws IOException {
        logger.info("Creating temp directory for download ID: {}", downloadId);
        
        Path tempDir = Paths.get(TEMP_DIR, downloadId);
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
            logger.info("Created temp directory: {}", tempDir.toAbsolutePath());
        }
        
        return tempDir;
    }

    private File downloadVideoFile(VideoDownloadRequest request, Path tempDir, String downloadId) throws YtDlpException {
        logger.info("Starting video download for URL: {} with format: {} and resolution: {}", 
                   request.getUrl(), request.getFormat(), request.getResolution());
        
        YtDlpRequest ytRequest = new YtDlpRequest(request.getUrl());
        
        // Set output path
        String outputTemplate = tempDir.resolve("%(title)s.%(ext)s").toString();
        ytRequest.setOption("output", outputTemplate);
        
        // Set format for MP4 with 720p resolution
        String format = String.format("best[height<=720][ext=%s]/best[height<=720]/best[ext=%s]/best", 
                                    MP4_FORMAT, MP4_FORMAT);
        ytRequest.setOption("format", format);
        
        // Additional options for better quality
        ytRequest.setOption("merge-output-format", MP4_FORMAT);
        ytRequest.setOption("prefer-free-formats");
        ytRequest.setOption("no-playlist");
        
        logger.info("Executing yt-dlp with format: {}", format);
        
        YtDlpResponse response = YtDlp.execute(ytRequest);
        
        if (response.getExitCode() != 0) {
            logger.error("yt-dlp failed with exit code: {} for download ID: {}", response.getExitCode(), downloadId);
            throw new YtDlpException("yt-dlp failed: " + response.getErr());
        }
        
        logger.info("yt-dlp completed successfully for download ID: {}", downloadId);
        
        // Find the downloaded file
        File[] files = tempDir.toFile().listFiles();
        if (files == null || files.length == 0) {
            throw new YtDlpException("No file was downloaded");
        }
        
        File downloadedFile = files[0];
        logger.info("Found downloaded file: {}", downloadedFile.getName());
        
        return downloadedFile;
    }

    private String mockUploadToTelegram(File file, String downloadId) {
        logger.info("Starting mock upload to Telegram for file: {} with download ID: {}", 
                   file.getName(), downloadId);
        
        try {
            // Simulate upload delay
            Thread.sleep(1000);
            
            // Generate mock Telegram file ID
            String telegramFileId = "mock_tg_" + downloadId + "_" + System.currentTimeMillis();
            
            logger.info("Mock upload completed. Generated Telegram file ID: {}", telegramFileId);
            
            return telegramFileId;
            
        } catch (InterruptedException e) {
            logger.error("Mock upload interrupted for download ID: {}", downloadId, e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Upload interrupted", e);
        }
    }

    private void cleanupTempDirectory(Path tempDir, String downloadId) {
        if (tempDir == null) {
            logger.warn("No temp directory to clean up for download ID: {}", downloadId);
            return;
        }
        
        try {
            logger.info("Cleaning up temp directory: {} for download ID: {}", tempDir, downloadId);
            
            // Delete all files in the temp directory
            File[] files = tempDir.toFile().listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.delete()) {
                        logger.info("Deleted file: {}", file.getName());
                    } else {
                        logger.warn("Failed to delete file: {}", file.getName());
                    }
                }
            }
            
            // Delete the temp directory itself
            if (Files.deleteIfExists(tempDir)) {
                logger.info("Deleted temp directory: {}", tempDir);
            } else {
                logger.warn("Failed to delete temp directory: {}", tempDir);
            }
            
        } catch (Exception e) {
            logger.error("Error cleaning up temp directory for download ID: {}", downloadId, e);
        }
    }

    public VideoDownloadResponse getDownloadStatus(String downloadId) {
        logger.info("Checking download status for ID: {}", downloadId);
        
        // This is a mock implementation - in a real scenario, you'd check the actual status
        // from a database or cache
        return VideoDownloadResponse.builder()
                .success(true)
                .downloadId(downloadId)
                .message("Download completed")
                .build();
    }
}
