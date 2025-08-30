package ru.aiivar.tg.yt.downloader.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.aiivar.tg.yt.downloader.model.VideoMetadataRequest;
import ru.aiivar.tg.yt.downloader.model.VideoMetadataResponse;
import ru.aiivar.tg.yt.downloader.model.VideoFormatsResponse;
import ru.aiivar.tg.yt.downloader.model.VideoDownloadRequest;
import ru.aiivar.tg.yt.downloader.model.VideoDownloadResponse;
import ru.aiivar.tg.yt.downloader.service.VideoMetadataService;
import ru.aiivar.tg.yt.downloader.service.VideoDownloadService;

@RestController
@RequestMapping("/api/video")
@CrossOrigin(origins = "*")
public class VideoController {

    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    @Autowired
    private VideoMetadataService videoMetadataService;

    @Autowired
    private VideoDownloadService videoDownloadService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/metadata")
    public ResponseEntity<VideoMetadataResponse> getMetadata(@RequestBody VideoMetadataRequest request) {
        logger.info("Received metadata request for URL: {}", request.getUrl());
        VideoMetadataResponse response = videoMetadataService.getMetadata(request);
        logger.info("Metadata request completed for URL: {}", request.getUrl());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/download")
    public ResponseEntity<VideoDownloadResponse> downloadVideo(@RequestBody VideoDownloadRequest request) {
        logger.info("Received download request for URL: {} with format: {} and resolution: {}", 
                   request.getUrl(), request.getFormat(), request.getResolution());
        
        VideoDownloadResponse response = videoDownloadService.downloadVideo(request);
        
        if (response.isSuccess()) {
            logger.info("Download completed successfully for URL: {} with download ID: {}", 
                       request.getUrl(), response.getDownloadId());
        } else {
            logger.error("Download failed for URL: {} with error: {}", 
                        request.getUrl(), response.getError());
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/download/{downloadId}/status")
    public ResponseEntity<VideoDownloadResponse> getDownloadStatus(@PathVariable String downloadId) {
        logger.info("Received status request for download ID: {}", downloadId);
        
        VideoDownloadResponse response = videoDownloadService.getDownloadStatus(downloadId);
        
        logger.info("Status request completed for download ID: {}", downloadId);
        return ResponseEntity.ok(response);
    }
}
