package ru.aiivar.tg.yt.downloader.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.aiivar.tg.yt.downloader.model.VideoMetadataRequest;
import ru.aiivar.tg.yt.downloader.model.VideoFormatsResponse;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class VideoFormatsServiceTest {

    @Autowired
    private VideoMetadataService videoMetadataService;

    @Test
    public void testGetFormats() {
        // Test with a YouTube URL
        VideoMetadataRequest request = new VideoMetadataRequest();
        request.setUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"); // Rick Roll video for testing

        VideoFormatsResponse response = videoMetadataService.getFormats(request);

        assertNotNull(response);
        assertNull(response.getError(), "Should not have error: " + response.getError());
        assertNotNull(response.getFormats());
        assertFalse(response.getFormats().isEmpty(), "Should have at least one format");
        assertEquals(request.getUrl(), response.getUrl());

        // Verify format structure
        var firstFormat = response.getFormats().get(0);
        assertNotNull(firstFormat.getFormatId());
        assertNotNull(firstFormat.getExtension());
    }

    @Test
    public void testGetFormatsWithInvalidUrl() {
        VideoMetadataRequest request = new VideoMetadataRequest();
        request.setUrl("https://invalid-url-that-does-not-exist.com");

        VideoFormatsResponse response = videoMetadataService.getFormats(request);

        assertNotNull(response);
        assertNotNull(response.getError(), "Should have error for invalid URL");
        assertEquals(request.getUrl(), response.getUrl());
    }
}
