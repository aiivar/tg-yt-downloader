package ru.aiivar.tg.yt.downloader.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.aiivar.tg.yt.downloader.model.VideoMetadataRequest;
import ru.aiivar.tg.yt.downloader.model.VideoMetadataResponse;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class VideoMetadataServiceTest {

    @Autowired
    private VideoMetadataService videoMetadataService;

    @Test
    void testGetMetadataWithValidUrl() {
        // Test with a valid YouTube URL
        VideoMetadataRequest request = new VideoMetadataRequest();
        request.setUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"); // Rick Roll video
        
        VideoMetadataResponse response = videoMetadataService.getMetadata(request);
        
        assertNotNull(response);
        assertNull(response.getError());
        assertNotNull(response.getTitle());
        assertNotNull(response.getUploader());
        assertNotNull(response.getWebpageUrl());
    }

    @Test
    void testGetMetadataWithInvalidUrl() {
        // Test with an invalid URL
        VideoMetadataRequest request = new VideoMetadataRequest();
        request.setUrl("https://invalid-url-that-does-not-exist.com");
        
        VideoMetadataResponse response = videoMetadataService.getMetadata(request);
        
        assertNotNull(response);
        assertNotNull(response.getError());
        assertTrue(response.getError().contains("Error"));
    }

    @Test
    void testGetMetadataWithNullUrl() {
        // Test with null URL
        VideoMetadataRequest request = new VideoMetadataRequest();
        request.setUrl(null);
        
        VideoMetadataResponse response = videoMetadataService.getMetadata(request);
        
        assertNotNull(response);
        assertNotNull(response.getError());
    }

    @Test
    void testGetMetadataWithEmptyUrl() {
        // Test with empty URL
        VideoMetadataRequest request = new VideoMetadataRequest();
        request.setUrl("");
        
        VideoMetadataResponse response = videoMetadataService.getMetadata(request);
        
        assertNotNull(response);
        assertNotNull(response.getError());
    }
}
