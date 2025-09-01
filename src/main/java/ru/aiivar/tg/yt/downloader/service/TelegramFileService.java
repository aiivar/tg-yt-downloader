package ru.aiivar.tg.yt.downloader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class TelegramFileService {

    private static final Logger logger = LoggerFactory.getLogger(TelegramFileService.class);

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.chat.id}")
    private String chatId;

    @Value("${telegram.api.use.local:false}")
    private boolean useLocalApi;

    @Value("${telegram.api.local.url:http://localhost:8081}")
    private String localApiUrl;

    @Value("${telegram.api.local.credentials.path:/path/to/credentials.json}")
    private String localCredentialsPath;

    private final RestTemplate restTemplate;

    public TelegramFileService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Получает базовый URL для API запросов
     * @return URL для API запросов
     */
    private String getApiBaseUrl() {
        if (useLocalApi) {
            logger.info("Using local Telegram Bot API server: {}", localApiUrl);
            return localApiUrl;
        } else {
            logger.info("Using official Telegram Bot API server");
            return "https://api.telegram.org";
        }
    }

    /**
     * Строит полный URL для API запроса
     * @param endpoint endpoint API (например, "sendVideo", "sendDocument")
     * @return полный URL для запроса
     */
    private String buildApiUrl(String endpoint) {
        String baseUrl = getApiBaseUrl();
        return UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/bot{token}/")
                .path(endpoint)
                .buildAndExpand(botToken)
                .toUriString();
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
            String url = buildApiUrl("sendVideo");
            
            // Создаем заголовки для multipart запроса
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            // Создаем multipart данные
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            // Добавляем chat_id
            body.add("chat_id", chatId);
            
            // Добавляем видео файл
            body.add("video", new org.springframework.core.io.FileSystemResource(file));

            body.add("supports_streaming", true);

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

    public String sendVideoByFileIdToChat(String fileId, String chatId) throws IOException {
        logger.info("Sending video {} to Telegram chat: {}", fileId, chatId);

        try {
            String url = buildApiUrl("sendVideo");

            // Создаем заголовки для multipart запроса
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // Создаем multipart данные
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // Добавляем chat_id
            body.add("chat_id", chatId);

            // Добавляем видео файл
            body.add("video", fileId);

            body.add("supports_streaming", true);

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
            String url = buildApiUrl("getFile");
            
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
     * Проверяет, нужно ли использовать локальный API для файла данного размера
     * @param fileSize размер файла в байтах
     * @return true если нужно использовать локальный API
     */
    private boolean shouldUseLocalApi(long fileSize) {
        long maxOfficialApiSize = 50 * 1024 * 1024; // 50 MB
        return useLocalApi && fileSize > maxOfficialApiSize;
    }

    /**
     * Отправляет большой файл (до 2GB) через локальный Bot API сервер
     * @param file файл для отправки
     * @param caption подпись к файлу
     * @param isVideo true если это видео файл
     * @return fileId полученный от Telegram
     * @throws IOException если произошла ошибка при работе с файлом
     */
    public String uploadLargeFileToTelegram(File file, String caption, boolean isVideo) throws IOException {
        if (!useLocalApi) {
            throw new IOException("Local Bot API server is not configured. Cannot upload large files.");
        }

        logger.info("Uploading large file to local Telegram Bot API: {} ({} MB)", 
                   file.getName(), file.length() / (1024 * 1024));

        try {
            String endpoint = isVideo ? "sendVideo" : "sendDocument";
            String url = buildApiUrl(endpoint);

            // Создаем заголовки для multipart запроса
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // Создаем multipart данные
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // Добавляем chat_id
            body.add("chat_id", chatId);

            if (isVideo) {
                body.add("supports_streaming", true);
            }

            // Добавляем файл
            String fileField = isVideo ? "video" : "document";
            body.add(fileField, new org.springframework.core.io.FileSystemResource(file));

            // Добавляем подпись если она есть
            if (caption != null && !caption.trim().isEmpty()) {
                body.add("caption", caption);
            }

            // Создаем HTTP entity
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            logger.info("Sending large file to local Telegram Bot API: {}", url);

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
                    Map<String, Object> fileData = (Map<String, Object>) result.get(isVideo ? "video" : "document");
                    String fileId = (String) fileData.get("file_id");

                    logger.info("Large file uploaded successfully to local Telegram Bot API. File ID: {}", fileId);
                    return fileId;
                } else {
                    String errorDescription = (String) responseBody.get("description");
                    logger.error("Local Telegram Bot API returned error: {}", errorDescription);
                    throw new IOException("Local Telegram Bot API error: " + errorDescription);
                }
            } else {
                logger.error("Unexpected response from local Telegram Bot API: {}", response.getStatusCode());
                throw new IOException("Unexpected response from local Telegram Bot API: " + response.getStatusCode());
            }

        } catch (Exception e) {
            logger.error("Error uploading large file to local Telegram Bot API: {}", file.getName(), e);
            throw new IOException("Failed to upload large file to local Telegram Bot API: " + e.getMessage(), e);
        }
    }

    /**
     * Проверяет доступность Telegram Bot API
     * @return true если API доступен, false в противном случае
     */
    public boolean checkTelegramApiHealth() {
        logger.info("Checking Telegram Bot API health");
        
        try {
            String url = buildApiUrl("getMe");
            
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

    // Getters for configuration
    public boolean isUseLocalApi() {
        return useLocalApi;
    }

    public String getLocalApiUrl() {
        return localApiUrl;
    }

    public String getLocalCredentialsPath() {
        return localCredentialsPath;
    }
}
