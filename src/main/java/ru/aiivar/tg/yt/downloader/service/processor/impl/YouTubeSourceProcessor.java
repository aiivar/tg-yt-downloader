package ru.aiivar.tg.yt.downloader.service.processor.impl;

import com.jfposton.ytdlp.YtDlp;
import com.jfposton.ytdlp.YtDlpException;
import com.jfposton.ytdlp.YtDlpRequest;
import com.jfposton.ytdlp.YtDlpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.aiivar.tg.yt.downloader.entity.VideoDownloadTask;
import ru.aiivar.tg.yt.downloader.entity.enums.SourceType;
import ru.aiivar.tg.yt.downloader.service.processor.VideoSourceProcessor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * YouTube source processor implementation
 */
@Component
public class YouTubeSourceProcessor implements VideoSourceProcessor {

    private static final Logger logger = LoggerFactory.getLogger(YouTubeSourceProcessor.class);
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/yt_downloads";

    private Map<String, Object> config = new HashMap<>();

    @Override
    public SourceType getSupportedSourceType() {
        return SourceType.YOUTUBE;
    }

    @Override
    public boolean canProcess(String url) {
        return url != null && (url.contains("youtube.com") || url.contains("youtu.be"));
    }

    @Override
    public File downloadVideo(VideoDownloadTask task) throws Exception {
        logger.info("Starting YouTube video download for task: {}", task.getId());

        try {
            // Create temp directory
            Path tempDir = createTempDirectory(task.getId());
            logger.info("Created temp directory: {}", tempDir);

            // Download video
            File downloadedFile = downloadVideoFile(task, tempDir);
            logger.info("Video downloaded successfully: {}", downloadedFile.getAbsolutePath());

            return downloadedFile;

        } catch (Exception e) {
            logger.error("Error during YouTube video download for task: {}", task.getId(), e);
            throw new Exception("Failed to download YouTube video: " + e.getMessage(), e);
        }
    }

    @Override
    public VideoMetadata getVideoMetadata(String url) throws Exception {
        logger.info("Getting YouTube video metadata for URL: {}", url);

        try {
            YtDlpRequest request = new YtDlpRequest(url);
            request.setOption("dump-json");
            request.setOption("no-download");

            YtDlpResponse response = YtDlp.execute(request);

            if (response.getExitCode() != 0) {
                throw new YtDlpException("yt-dlp failed: " + response.getErr());
            }

            // Parse JSON response to extract metadata
            String jsonOutput = response.getOut();
            return parseMetadataFromJson(jsonOutput);

        } catch (Exception e) {
            logger.error("Error getting YouTube video metadata for URL: {}", url, e);
            throw new Exception("Failed to get video metadata: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getAvailableFormats(String url) throws Exception {
        logger.info("Getting available formats for YouTube URL: {}", url);

        try {
            YtDlpRequest request = new YtDlpRequest(url);
            request.setOption("list-formats");

            YtDlpResponse response = YtDlp.execute(request);

            if (response.getExitCode() != 0) {
                throw new YtDlpException("yt-dlp failed: " + response.getErr());
            }

            // Parse format list from output
            return parseFormatsFromOutput(response.getOut());

        } catch (Exception e) {
            logger.error("Error getting available formats for YouTube URL: {}", url, e);
            throw new Exception("Failed to get available formats: " + e.getMessage(), e);
        }
    }

    @Override
    public void validateRequest(VideoDownloadTask task) throws Exception {
        if (task.getSourceUrl() == null || task.getSourceUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("Source URL cannot be null or empty");
        }

        if (!canProcess(task.getSourceUrl())) {
            throw new IllegalArgumentException("URL is not a valid YouTube URL");
        }

        // Additional validation can be added here
        logger.info("YouTube request validation passed for task: {}", task.getId());
    }

    @Override
    public Map<String, Object> getProcessorConfig() {
        return new HashMap<>(config);
    }

    @Override
    public void setProcessorConfig(Map<String, Object> config) {
        this.config = new HashMap<>(config);
    }

    private Path createTempDirectory(String taskId) throws IOException {
        logger.info("Creating temp directory for task: {}", taskId);

        try {
            Path tempDir = Paths.get(TEMP_DIR, taskId);

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
            logger.error("Failed to create temp directory for task: {}", taskId, e);
            throw new IOException("Failed to create temp directory: " + e.getMessage(), e);
        }
    }

    private File downloadVideoFile(VideoDownloadTask task, Path tempDir) throws YtDlpException {
        logger.info("Starting video download for URL: {} with format: {} and resolution: {}",
                task.getSourceUrl(), task.getRequestedFormat(), task.getRequestedResolution());

        YtDlpRequest ytRequest = new YtDlpRequest(task.getSourceUrl());

        // Set output path
        String outputTemplate = tempDir.resolve("%(title)s.%(ext)s").toString();
        ytRequest.setOption("output", outputTemplate);

        // Set format based on task requirements
        String format = buildFormatString(task);
        ytRequest.setOption("format", format);

        // Additional options
        ytRequest.setOption("merge-output-format", task.getRequestedFormat());
        ytRequest.setOption("prefer-free-formats");
        ytRequest.setOption("no-playlist");

        logger.info("Executing yt-dlp with format: {}", format);

        var progressInt = new AtomicInteger(1);
        YtDlpResponse response = YtDlp.execute(
                ytRequest,
                (progress, etaInSeconds) -> {
                    if (progress >= progressInt.get()) {
                        progressInt.addAndGet((int) progress - progressInt.get() + 1);
                        logger.debug("Task:{}. Progress: {}%, time left: {} sec.", task.getId(), progress, etaInSeconds);
                    }
                },
                logger::debug
        );

        if (response.getExitCode() != 0) {
            logger.error("yt-dlp failed with exit code: {} for task: {}", response.getExitCode(), task.getId());
            throw new YtDlpException("yt-dlp failed: " + response.getErr());
        }

        logger.info("yt-dlp completed successfully for task: {}", task.getId());

        // Find the downloaded file
        File[] files = tempDir.toFile().listFiles();
        if (files == null || files.length == 0) {
            throw new YtDlpException("No file was downloaded");
        }

        File downloadedFile = files[0];
        logger.info("Found downloaded file: {}", downloadedFile.getName());

        return downloadedFile;
    }

    private String buildFormatString(VideoDownloadTask task) {
        String format = task.getRequestedFormat();
        String resolution = task.getRequestedResolution();
        String quality = task.getRequestedQuality();

        if ("best".equals(quality)) {
            return String.format("best[height<=%s][ext=%s]", 
                    extractHeightFromResolution(resolution), format);
        } else if ("worst".equals(quality)) {
            return String.format("worst[height<=%s][ext=%s]", 
                    extractHeightFromResolution(resolution), format);
        } else {
            return String.format("best[height<=%s][ext=%s]", 
                    extractHeightFromResolution(resolution), format);
        }
    }

    private String extractHeightFromResolution(String resolution) {
        if (resolution == null) return "720";
        
        // Extract height from resolution string (e.g., "720p" -> "720")
        String height = resolution.replaceAll("[^0-9]", "");
        return height.isEmpty() ? "720" : height;
    }

    private VideoMetadata parseMetadataFromJson(String jsonOutput) {
        // This is a simplified implementation
        // In a real implementation, you would use a JSON parser like Jackson or Gson
        VideoMetadata metadata = new VideoMetadata();
        
        // Extract basic information from JSON
        // This is a placeholder - actual implementation would parse the JSON properly
        metadata.setTitle("YouTube Video");
        metadata.setDescription("Downloaded from YouTube");
        metadata.setAuthor("Unknown");
        
        return metadata;
    }

    private Map<String, Object> parseFormatsFromOutput(String output) {
        // This is a simplified implementation
        // In a real implementation, you would parse the format list properly
        Map<String, Object> formats = new HashMap<>();
        formats.put("formats", "Available formats would be parsed here");
        return formats;
    }
}
