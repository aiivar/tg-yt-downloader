package ru.aiivar.tg.yt.downloader.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.aiivar.tg.yt.downloader.model.VideoMetadataRequest;
import ru.aiivar.tg.yt.downloader.model.VideoFormatsResponse;
import ru.aiivar.tg.yt.downloader.model.VideoFormat;

import java.lang.reflect.Method;
import java.util.List;

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

    @Test
    public void testParseFormatLine() throws Exception {
        // Test the parseFormatLine method directly using reflection
        Method parseFormatLineMethod = VideoMetadataService.class.getDeclaredMethod("parseFormatLine", String.class);
        parseFormatLineMethod.setAccessible(true);

        // Test with a video format line
        String videoLine = "18      mp4   640x268     30  2 |     8.69MiB   718k https | avc1.42001E          mp4a.40.2       44k 360p";
        VideoFormat videoFormat = (VideoFormat) parseFormatLineMethod.invoke(videoMetadataService, videoLine);
        
        assertNotNull(videoFormat);
        assertEquals("18", videoFormat.getFormatId());
        assertEquals("mp4", videoFormat.getExtension());
        assertEquals("640x268", videoFormat.getResolution());
        assertEquals("30", videoFormat.getFps());
        assertEquals("2", videoFormat.getChannels());
        assertEquals("8.69MiB", videoFormat.getFilesize());
        assertEquals("718k", videoFormat.getTbr());
        assertEquals("avc1.42001E", videoFormat.getVcodec());
        assertEquals("mp4a.40.2", videoFormat.getAcodec());
        assertEquals("44k", videoFormat.getAbr());
        assertEquals("360p", videoFormat.getMoreInfo());

        // Test with an audio-only format line
        String audioLine = "249     webm  audio only      2 |   564.02KiB    46k https | audio only           opus        46k 48k low, webm_dash";
        VideoFormat audioFormat = (VideoFormat) parseFormatLineMethod.invoke(videoMetadataService, audioLine);
        
        assertNotNull(audioFormat);
        assertEquals("249", audioFormat.getFormatId());
        assertEquals("webm", audioFormat.getExtension());
        assertEquals("audio only", audioFormat.getResolution());
        assertEquals("", audioFormat.getFps());
        assertEquals("2", audioFormat.getChannels());
        assertEquals("564.02KiB", audioFormat.getFilesize());
        assertEquals("46k", audioFormat.getTbr());
        assertEquals("audio only", audioFormat.getVcodec());
        assertEquals("opus", audioFormat.getAcodec());
        assertEquals("46k", audioFormat.getAbr());
        assertEquals("48k", audioFormat.getAsr());
        assertEquals("low, webm_dash", audioFormat.getMoreInfo());
    }
}
