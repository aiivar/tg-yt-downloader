package ru.aiivar.tg.yt.downloader.service;

import com.jfposton.ytdlp.YtDlp;
import com.jfposton.ytdlp.YtDlpException;
import com.jfposton.ytdlp.YtDlpRequest;
import com.jfposton.ytdlp.YtDlpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.aiivar.tg.yt.downloader.entity.Video;
import ru.aiivar.tg.yt.downloader.model.VideoDownloadRequest;
import ru.aiivar.tg.yt.downloader.model.VideoDownloadResponse;
import ru.aiivar.tg.yt.downloader.repository.VideoRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class VideoDownloadService {

    private static final Logger logger = LoggerFactory.getLogger(VideoDownloadService.class);

    private static final String TEMP_DIR = getTempDirectory();
    private static final String MP4_FORMAT = "mp4";
    private static final String RESOLUTION_720P = "720p";

    private static String getTempDirectory() {
        String tempDir = System.getProperty("java.io.tmpdir");
        if (tempDir == null || tempDir.isEmpty()) {
            tempDir = "/tmp";
        }
        return tempDir + "/temp_downloads";
    }

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private TelegramFileService telegramFileService;

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

            // Upload to Telegram (with automatic API selection for large files)
            String telegramFileId = uploadToTelegram(downloadedFile, downloadId, request.getUrl());
            logger.info("Upload to Telegram completed with file ID: {}", telegramFileId);

            // Get file size
            long fileSize = downloadedFile.length();
            logger.info("File size: {} bytes", fileSize);

            // Save to database
            saveVideoToDatabase(request.getUrl(), telegramFileId, downloadedFile.getName(), fileSize);

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
        logger.info("Base temp directory: {}", TEMP_DIR);

        try {
            Path tempDir = Paths.get(TEMP_DIR, downloadId);

            // Ensure the base temp directory exists
            Path baseTempDir = Paths.get(TEMP_DIR);
            if (!Files.exists(baseTempDir)) {
                logger.info("Creating base temp directory: {}", baseTempDir.toAbsolutePath());
                Files.createDirectories(baseTempDir);
            }

            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
                logger.info("Created temp directory: {}", tempDir.toAbsolutePath());
            }

            return tempDir;
        } catch (IOException e) {
            logger.error("Failed to create temp directory for download ID: {}", downloadId, e);
            throw new IOException("Failed to create temp directory: " + e.getMessage(), e);
        }
    }

    private File downloadVideoFile(VideoDownloadRequest request, Path tempDir, String downloadId) throws YtDlpException {
        logger.info("Starting video download for URL: {} with format: {} and resolution: {}",
                request.getUrl(), request.getFormat(), request.getResolution());

        YtDlpRequest ytRequest = new YtDlpRequest(request.getUrl());

        // Set output path
        String outputTemplate = tempDir.resolve("%(title)s.%(ext)s").toString();
        ytRequest.setOption("output", outputTemplate);

        // Set format for MP4 with 720p resolution
        String format = String.format("best[height<=720][ext=%s]",
                MP4_FORMAT);
        ytRequest.setOption("format", format);

        // Additional options for better quality
        ytRequest.setOption("merge-output-format", MP4_FORMAT);
        ytRequest.setOption("prefer-free-formats");
        ytRequest.setOption("no-playlist");

        logger.info("Executing yt-dlp with format: {}", format);

        var progressInt = new AtomicInteger(1);
        YtDlpResponse response = YtDlp.execute(
                ytRequest,
                (progress, etaInSeconds) -> {
                    if (progress >= progressInt.get()) {
                        progressInt.addAndGet(1);
                        logger.debug("ID:{}. Progress: {}%, time: left {} sec.", downloadId, progress, etaInSeconds);
                    }
                },
                logger::debug
        );

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

    private String uploadToTelegram(File file, String downloadId, String originalUrl) {
        logger.info("Starting upload to Telegram for file: {} with download ID: {}",
                file.getName(), downloadId);

        try {
            // Create caption for the video
            String caption = String.format("Video downloaded from: %s\nDownload ID: %s",
                    originalUrl, downloadId);

            long fileSize = file.length();
            long maxOfficialApiSize = 50 * 1024 * 1024; // 50 MB

            String telegramFileId;

            // Check if we should use local API for large files
            if (fileSize > maxOfficialApiSize) {
                logger.info("File size {} MB exceeds 50MB limit, using local Bot API server",
                        fileSize / (1024 * 1024));
                telegramFileId = telegramFileService.uploadLargeFileToTelegram(file, caption, true);
            } else {
                logger.info("File size {} MB is within 50MB limit, using official API",
                        fileSize / (1024 * 1024));
                telegramFileId = telegramFileService.uploadVideoToTelegram(file, caption);
            }

            logger.info("Upload to Telegram completed. File ID: {}", telegramFileId);

            return telegramFileId;

        } catch (IOException e) {
            logger.error("Failed to upload file to Telegram for download ID: {}", downloadId, e);
            throw new RuntimeException("Failed to upload to Telegram: " + e.getMessage(), e);
        }
    }

    private void saveVideoToDatabase(String url, String telegramFileId, String fileName, long fileSize) {
        logger.info("Saving video to database - URL: {}, File ID: {}, File: {}",
                url, telegramFileId, fileName);

        try {
            Video video = entityFactory.newEntity(Video.class);
            video.setUrl(url);
            video.setFileId(telegramFileId);

            videoRepository.save(video);

            logger.info("Video saved to database successfully. ID: {}", video.getId());

        } catch (Exception e) {
            logger.error("Failed to save video to database", e);
            // Don't throw exception here to avoid breaking the download process
            // The file is already uploaded to Telegram
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
