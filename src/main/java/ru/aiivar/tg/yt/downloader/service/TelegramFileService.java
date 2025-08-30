package ru.aiivar.tg.yt.downloader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Service
public class TelegramFileService {

    private static final Logger logger = LoggerFactory.getLogger(TelegramFileService.class);

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.chat.id}")
    private String chatId;

    private final RestTemplate restTemplate;

    public TelegramFileService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Отправляет файл в Telegram и возвращает fileId
     * @param file файл для отправки
     * @param caption подпись к файлу (опционально)
     * @return fileId полученный от Telegram
     * @throws IOException если произошла ошибка при работе с файлом
     */
    public String uploadFileToTelegram(File file, String caption) throws IOException {
        logger.info("Starting file upload to Telegram: {}", file.getName());
        
        try {
            String url = String.format("https://api.telegram.org/bot%s/sendDocument", botToken);
            
            // Создаем заголовки для multipart запроса
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            // Создаем multipart данные
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            // Добавляем chat_id
            body.add("chat_id", chatId);
            
            // Добавляем файл
            body.add("document", new org.springframework.core.io.FileSystemResource(file));
            
            // Добавляем подпись если она есть
            if (caption != null && !caption.trim().isEmpty()) {
                body.add("caption", caption);
            }
            
            // Создаем HTTP entity
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            logger.info("Sending file to Telegram API: {}", url);
            
            // Отправляем запрос
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                if (Boolean.TRUE.equals(responseBody.get("ok"))) {
                    Map<String, Object> result = (Map<String, Object>) responseBody.get("result");
                    Map<String, Object> document = (Map<String, Object>) result.get("document");
                    String fileId = (String) document.get("file_id");
                    
                    logger.info("File uploaded successfully to Telegram. File ID: {}", fileId);
                    return fileId;
                } else {
                    String errorDescription = (String) responseBody.get("description");
                    logger.error("Telegram API returned error: {}", errorDescription);
                    throw new IOException("Telegram API error: " + errorDescription);
                }
            } else {
                logger.error("Unexpected response from Telegram API: {}", response.getStatusCode());
                throw new IOException("Unexpected response from Telegram API: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("Error uploading file to Telegram: {}", file.getName(), e);
            throw new IOException("Failed to upload file to Telegram: " + e.getMessage(), e);
        }
    }

    /**
     * Отправляет видео файл в Telegram и возвращает fileId
     * @param file видео файл для отправки
     * @param caption подпись к видео (опционально)
     * @return fileId полученный от Telegram
     * @throws IOException если произошла ошибка при работе с файлом
     */
    public String uploadVideoToTelegram(File file, String caption) throws IOException {
        logger.info("Starting video upload to Telegram: {}", file.getName());
        
        try {
            String url = String.format("https://api.telegram.org/bot%s/sendVideo", botToken);
            
            // Создаем заголовки для multipart запроса
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            // Создаем multipart данные
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            // Добавляем chat_id
            body.add("chat_id", chatId);
            
            // Добавляем видео файл
            body.add("video", new org.springframework.core.io.FileSystemResource(file));
            
            // Добавляем подпись если она есть
            if (caption != null && !caption.trim().isEmpty()) {
                body.add("caption", caption);
            }
            
            // Создаем HTTP entity
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            logger.info("Sending video to Telegram API: {}", url);
            
            // Отправляем запрос
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                if (Boolean.TRUE.equals(responseBody.get("ok"))) {
                    Map<String, Object> result = (Map<String, Object>) responseBody.get("result");
                    Map<String, Object> video = (Map<String, Object>) result.get("video");
                    String fileId = (String) video.get("file_id");
                    
                    logger.info("Video uploaded successfully to Telegram. File ID: {}", fileId);
                    return fileId;
                } else {
                    String errorDescription = (String) responseBody.get("description");
                    logger.error("Telegram API returned error: {}", errorDescription);
                    throw new IOException("Telegram API error: " + errorDescription);
                }
            } else {
                logger.error("Unexpected response from Telegram API: {}", response.getStatusCode());
                throw new IOException("Unexpected response from Telegram API: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("Error uploading video to Telegram: {}", file.getName(), e);
            throw new IOException("Failed to upload video to Telegram: " + e.getMessage(), e);
        }
    }

    /**
     * Получает информацию о файле по fileId
     * @param fileId ID файла в Telegram
     * @return информация о файле
     */
    public Map<String, Object> getFileInfo(String fileId) {
        logger.info("Getting file info for file ID: {}", fileId);
        
        try {
            String url = String.format("https://api.telegram.org/bot%s/getFile", botToken);
            
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("file_id", fileId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                if (Boolean.TRUE.equals(responseBody.get("ok"))) {
                    Map<String, Object> fileInfo = (Map<String, Object>) responseBody.get("result");
                    logger.info("File info retrieved successfully for file ID: {}", fileId);
                    return fileInfo;
                } else {
                    String errorDescription = (String) responseBody.get("description");
                    logger.error("Telegram API returned error: {}", errorDescription);
                    return null;
                }
            } else {
                logger.error("Unexpected response from Telegram API: {}", response.getStatusCode());
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Error getting file info for file ID: {}", fileId, e);
            return null;
        }
    }

    /**
     * Проверяет доступность Telegram Bot API
     * @return true если API доступен, false в противном случае
     */
    public boolean checkTelegramApiHealth() {
        logger.info("Checking Telegram Bot API health");
        
        try {
            String url = String.format("https://api.telegram.org/bot%s/getMe", botToken);
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                if (Boolean.TRUE.equals(responseBody.get("ok"))) {
                    Map<String, Object> result = (Map<String, Object>) responseBody.get("result");
                    String botUsername = (String) result.get("username");
                    logger.info("Telegram Bot API is healthy. Bot username: {}", botUsername);
                    return true;
                } else {
                    logger.error("Telegram Bot API health check failed");
                    return false;
                }
            } else {
                logger.error("Telegram Bot API health check failed with status: {}", response.getStatusCode());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error checking Telegram Bot API health", e);
            return false;
        }
    }
}
