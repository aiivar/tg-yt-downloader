package ru.aiivar.tg.yt.downloader.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.aiivar.tg.yt.downloader.model.VideoMetadataRequest;
import ru.aiivar.tg.yt.downloader.model.VideoMetadataResponse;
import ru.aiivar.tg.yt.downloader.model.VideoFormatsResponse;
import ru.aiivar.tg.yt.downloader.service.VideoMetadataService;

@RestController
@RequestMapping("/api/video")
@CrossOrigin(origins = "*")
public class VideoController {

    @Autowired
    private VideoMetadataService videoMetadataService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/metadata")
    public ResponseEntity<VideoMetadataResponse> getMetadata(@RequestBody VideoMetadataRequest request) {
        VideoMetadataResponse response = videoMetadataService.getMetadata(request);
        return ResponseEntity.ok(response);
    }
}
