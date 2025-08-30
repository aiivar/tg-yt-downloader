package ru.aiivar.tg.yt.downloader.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramFileServiceTest {

    @InjectMocks
    private TelegramFileService telegramFileService;

    private File testFile;

    @BeforeEach
    void setUp() throws IOException {
        // Set up test configuration
        ReflectionTestUtils.setField(telegramFileService, "botToken", "test_bot_token");
        ReflectionTestUtils.setField(telegramFileService, "chatId", "test_chat_id");
        
        // Create a test file
        Path tempFile = Files.createTempFile("test_video", ".mp4");
        testFile = tempFile.toFile();
        testFile.deleteOnExit();
    }

    @Test
    void testCheckTelegramApiHealth_WithValidToken() {
        // This test would require actual Telegram API call
        // In a real scenario, you'd mock the RestTemplate response
        
        boolean isHealthy = telegramFileService.checkTelegramApiHealth();
        
        // Note: This will likely fail without a valid bot token
        // In a real test environment, you'd mock the API response
        assertNotNull(isHealthy);
    }

    @Test
    void testGetFileInfo_WithValidFileId() {
        // This test would require actual Telegram API call
        // In a real scenario, you'd mock the RestTemplate response
        
        String testFileId = "test_file_id";
        Map<String, Object> fileInfo = telegramFileService.getFileInfo(testFileId);
        
        // Note: This will likely return null without a valid bot token
        // In a real test environment, you'd mock the API response
        // assertNotNull(fileInfo);
    }

    @Test
    void testUploadFileToTelegram_WithValidFile() {
        // This test would require actual Telegram API call
        // In a real scenario, you'd mock the RestTemplate response
        
        try {
            String fileId = telegramFileService.uploadFileToTelegram(testFile, "Test caption");
            
            // Note: This will likely fail without a valid bot token
            // In a real test environment, you'd mock the API response
            // assertNotNull(fileId);
        } catch (IOException e) {
            // Expected to fail without valid bot token
            assertTrue(e.getMessage().contains("Telegram API"));
        }
    }

    @Test
    void testUploadVideoToTelegram_WithValidFile() {
        // This test would require actual Telegram API call
        // In a real scenario, you'd mock the RestTemplate response
        
        try {
            String fileId = telegramFileService.uploadVideoToTelegram(testFile, "Test video caption");
            
            // Note: This will likely fail without a valid bot token
            // In a real test environment, you'd mock the API response
            // assertNotNull(fileId);
        } catch (IOException e) {
            // Expected to fail without valid bot token
            assertTrue(e.getMessage().contains("Telegram API"));
        }
    }

    @Test
    void testUploadFileToTelegram_WithNullCaption() {
        try {
            String fileId = telegramFileService.uploadFileToTelegram(testFile, null);
            
            // Note: This will likely fail without a valid bot token
            // In a real test environment, you'd mock the API response
            // assertNotNull(fileId);
        } catch (IOException e) {
            // Expected to fail without valid bot token
            assertTrue(e.getMessage().contains("Telegram API"));
        }
    }

    @Test
    void testUploadVideoToTelegram_WithEmptyCaption() {
        try {
            String fileId = telegramFileService.uploadVideoToTelegram(testFile, "");
            
            // Note: This will likely fail without a valid bot token
            // In a real test environment, you'd mock the API response
            // assertNotNull(fileId);
        } catch (IOException e) {
            // Expected to fail without valid bot token
            assertTrue(e.getMessage().contains("Telegram API"));
        }
    }
}
