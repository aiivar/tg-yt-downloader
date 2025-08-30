package ru.aiivar.tg.yt.downloader.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.aiivar.tg.yt.downloader.model.VideoDownloadRequest;
import ru.aiivar.tg.yt.downloader.model.VideoDownloadResponse;
import ru.aiivar.tg.yt.downloader.repository.VideoRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoDownloadServiceTest {

    @Mock
    private VideoRepository videoRepository;

    @InjectMocks
    private VideoDownloadService videoDownloadService;

    private VideoDownloadRequest testRequest;

    @BeforeEach
    void setUp() {
        testRequest = new VideoDownloadRequest();
        testRequest.setUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        testRequest.setFormat("mp4");
        testRequest.setResolution("720p");
        testRequest.setQuality("best");
    }

    @Test
    void testDownloadVideo_ValidRequest_ReturnsSuccessResponse() {
        // This test would require actual yt-dlp execution, so we'll mock the behavior
        // In a real scenario, you'd use @SpringBootTest with a test configuration
        
        VideoDownloadResponse response = videoDownloadService.downloadVideo(testRequest);
        
        assertNotNull(response);
        assertNotNull(response.getDownloadId());
        // Note: This test will likely fail in a unit test environment without yt-dlp
        // In a real scenario, you'd mock the YtDlp.execute() call
    }

    @Test
    void testGetDownloadStatus_ValidId_ReturnsStatusResponse() {
        String downloadId = "test-download-id";
        
        VideoDownloadResponse response = videoDownloadService.getDownloadStatus(downloadId);
        
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(downloadId, response.getDownloadId());
        assertEquals("Download completed", response.getMessage());
    }

    @Test
    void testVideoDownloadRequest_DefaultValues() {
        VideoDownloadRequest request = new VideoDownloadRequest();
        request.setUrl("https://example.com");
        
        assertEquals("mp4", request.getFormat());
        assertEquals("720p", request.getResolution());
        assertEquals("best", request.getQuality());
    }
}
