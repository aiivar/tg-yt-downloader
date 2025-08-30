# API Usage Examples

This document provides practical examples of how to use the TG YouTube Downloader API endpoints.

## Video Download Examples

### 1. Download a YouTube Video

**Request:**
```bash
curl -X POST http://localhost:8080/api/video/download \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
    "format": "mp4",
    "resolution": "720p",
    "quality": "best"
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "Video downloaded and uploaded successfully",
  "downloadId": "550e8400-e29b-41d4-a716-446655440000",
  "fileName": "Rick Astley - Never Gonna Give You Up (Official Music Video).mp4",
  "fileSize": 45678901,
  "telegramFileId": "mock_tg_550e8400-e29b-41d4-a716-446655440000_1234567890",
  "error": null
}
```

### 2. Check Download Status

**Request:**
```bash
curl -X GET http://localhost:8080/api/video/download/550e8400-e29b-41d4-a716-446655440000/status
```

**Response:**
```json
{
  "success": true,
  "downloadId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "Download completed",
  "fileName": null,
  "fileSize": null,
  "telegramFileId": null,
  "error": null
}
```

### 3. Download with Custom Parameters

**Request:**
```bash
curl -X POST http://localhost:8080/api/video/download \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://www.youtube.com/watch?v=example",
    "format": "mp4",
    "resolution": "720p",
    "quality": "best"
  }'
```

## Error Handling Examples

### 1. Invalid URL

**Request:**
```bash
curl -X POST http://localhost:8080/api/video/download \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://invalid-url.com/video",
    "format": "mp4",
    "resolution": "720p"
  }'
```

**Response:**
```json
{
  "success": false,
  "message": null,
  "downloadId": "550e8400-e29b-41d4-a716-446655440001",
  "fileName": null,
  "fileSize": null,
  "telegramFileId": null,
  "error": "Download failed: yt-dlp failed: ERROR: Video not found"
}
```

### 2. Network Error

**Response:**
```json
{
  "success": false,
  "message": null,
  "downloadId": "550e8400-e29b-41d4-a716-446655440002",
  "fileName": null,
  "fileSize": null,
  "telegramFileId": null,
  "error": "Download failed: Network timeout"
}
```

## JavaScript/Node.js Examples

### Using fetch API

```javascript
async function downloadVideo(url) {
  try {
    const response = await fetch('http://localhost:8080/api/video/download', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        url: url,
        format: 'mp4',
        resolution: '720p',
        quality: 'best'
      })
    });

    const result = await response.json();
    
    if (result.success) {
      console.log('Download successful!');
      console.log('Download ID:', result.downloadId);
      console.log('File name:', result.fileName);
      console.log('File size:', result.fileSize);
      console.log('Telegram file ID:', result.telegramFileId);
    } else {
      console.error('Download failed:', result.error);
    }
  } catch (error) {
    console.error('Request failed:', error);
  }
}

// Usage
downloadVideo('https://www.youtube.com/watch?v=dQw4w9WgXcQ');
```

### Using axios

```javascript
const axios = require('axios');

async function downloadVideo(url) {
  try {
    const response = await axios.post('http://localhost:8080/api/video/download', {
      url: url,
      format: 'mp4',
      resolution: '720p',
      quality: 'best'
    });

    const result = response.data;
    
    if (result.success) {
      console.log('Download successful!');
      console.log('Download ID:', result.downloadId);
      console.log('File name:', result.fileName);
      console.log('File size:', result.fileSize);
      console.log('Telegram file ID:', result.telegramFileId);
    } else {
      console.error('Download failed:', result.error);
    }
  } catch (error) {
    console.error('Request failed:', error.response?.data || error.message);
  }
}

// Usage
downloadVideo('https://www.youtube.com/watch?v=dQw4w9WgXcQ');
```

## Python Examples

### Using requests

```python
import requests
import json

def download_video(url):
    try:
        response = requests.post(
            'http://localhost:8080/api/video/download',
            headers={'Content-Type': 'application/json'},
            json={
                'url': url,
                'format': 'mp4',
                'resolution': '720p',
                'quality': 'best'
            }
        )
        
        result = response.json()
        
        if result['success']:
            print('Download successful!')
            print('Download ID:', result['downloadId'])
            print('File name:', result['fileName'])
            print('File size:', result['fileSize'])
            print('Telegram file ID:', result['telegramFileId'])
        else:
            print('Download failed:', result['error'])
            
    except Exception as e:
        print('Request failed:', str(e))

# Usage
download_video('https://www.youtube.com/watch?v=dQw4w9WgXcQ')
```

## Telegram API Examples

### 1. Check Telegram Bot API Health

**Request:**
```bash
curl -X GET http://localhost:8080/api/video/telegram/health
```

**Response:**
```json
{
  "healthy": true,
  "timestamp": 1705312345678
}
```

### 2. Get File Information

**Request:**
```bash
curl -X GET http://localhost:8080/api/video/telegram/file/BAACAgIAAxkBAAIBQ2WX_1234567890
```

**Response:**
```json
{
  "file_id": "BAACAgIAAxkBAAIBQ2WX_1234567890",
  "file_unique_id": "AgADBUNll_1234567890",
  "file_size": 45678901,
  "file_path": "videos/file_123.mp4"
}
```

## Environment Setup

Before using the API, make sure to configure your Telegram Bot credentials:

### Option 1: Environment Variables
```bash
export TELEGRAM_BOT_TOKEN=1234567890:ABCdefGHIjklMNOpqrsTUVwxyz
export TELEGRAM_CHAT_ID=123456789
```

### Option 2: .env File
```bash
cp env.example .env
# Edit .env file with your actual values
```

### Option 3: Docker with Environment Variables
```bash
docker-compose up -e TELEGRAM_BOT_TOKEN=1234567890:ABCdefGHIjklMNOpqrsTUVwxyz -e TELEGRAM_CHAT_ID=123456789
```

## Logging Information

The application provides comprehensive logging for all download operations. You can monitor the logs to track:

- Download initiation
- Temp directory creation
- yt-dlp execution
- File download progress
- Real Telegram upload with file ID retrieval
- Database storage operations
- Temp directory cleanup
- Error handling

Example log output:
```
2024-01-15 10:30:15.123 INFO  - Starting video download with ID: 550e8400-e29b-41d4-a716-446655440000 for URL: https://www.youtube.com/watch?v=dQw4w9WgXcQ
2024-01-15 10:30:15.124 INFO  - Created temp directory: temp_downloads/550e8400-e29b-41d4-a716-446655440000
2024-01-15 10:30:15.125 INFO  - Starting video download for URL: https://www.youtube.com/watch?v=dQw4w9WgXcQ with format: mp4 and resolution: 720p
2024-01-15 10:30:20.456 INFO  - yt-dlp completed successfully for download ID: 550e8400-e29b-41d4-a716-446655440000
2024-01-15 10:30:20.457 INFO  - Found downloaded file: Rick Astley - Never Gonna Give You Up (Official Music Video).mp4
2024-01-15 10:30:20.458 INFO  - Video downloaded successfully: /app/temp_downloads/550e8400-e29b-41d4-a716-446655440000/Rick Astley - Never Gonna Give You Up (Official Music Video).mp4
2024-01-15 10:30:20.459 INFO  - Starting mock upload to Telegram for file: Rick Astley - Never Gonna Give You Up (Official Music Video).mp4 with download ID: 550e8400-e29b-41d4-a716-446655440000
2024-01-15 10:30:21.460 INFO  - Mock upload completed. Generated Telegram file ID: mock_tg_550e8400-e29b-41d4-a716-446655440000_1234567890
2024-01-15 10:30:21.461 INFO  - File size: 45678901 bytes
2024-01-15 10:30:21.462 INFO  - Cleaning up temp directory: temp_downloads/550e8400-e29b-41d4-a716-446655440000 for download ID: 550e8400-e29b-41d4-a716-446655440000
2024-01-15 10:30:21.463 INFO  - Deleted file: Rick Astley - Never Gonna Give You Up (Official Music Video).mp4
2024-01-15 10:30:21.464 INFO  - Deleted temp directory: temp_downloads/550e8400-e29b-41d4-a716-446655440000
```
