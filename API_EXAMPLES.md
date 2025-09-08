# API Usage Examples

This document provides practical examples of how to use the TG YouTube Downloader task-based API endpoints.

## Task Management Examples

### 1. Create a Video Download Task

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
    "chatId": "123456789",
    "format": "mp4",
    "resolution": "720p",
    "quality": "best"
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "Task created successfully",
  "downloadId": "550e8400-e29b-41d4-a716-446655440000",
  "fileName": null,
  "fileSize": null,
  "telegramFileId": null,
  "error": null
}
```

### 2. Check Task Status

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/tasks/550e8400-e29b-41d4-a716-446655440000
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PROCESSING",
  "sourceUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
  "sourceType": "YOUTUBE",
  "destinationType": "TELEGRAM",
  "userId": "123456789",
  "chatId": "123456789",
  "requestedFormat": "mp4",
  "requestedQuality": "720p",
  "requestedResolution": "720p",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:15",
  "downloadStartedAt": "2024-01-15T10:30:10",
  "downloadCompletedAt": null,
  "errorMessage": null
}
```

### 3. Get Task Results

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/tasks/550e8400-e29b-41d4-a716-446655440000/results
```

**Response:**
```json
{
  "results": [
    {
      "id": "result-123",
      "taskId": "550e8400-e29b-41d4-a716-446655440000",
      "status": "COMPLETED",
      "destinationType": "TELEGRAM",
      "destinationId": "BAADBAADrwADBREAAYag8VZgAAEC",
      "fileName": "Rick Astley - Never Gonna Give You Up.mp4",
      "fileSizeBytes": 45678901,
      "fileFormat": "mp4",
      "durationSeconds": 225,
      "resolution": "720p",
      "bitrate": 1000000,
      "fps": 30.0,
      "codec": "h264",
      "thumbnailUrl": "https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
      "uploadStartedAt": "2024-01-15T10:30:20",
      "uploadCompletedAt": "2024-01-15T10:30:45",
      "processingTimeMs": 25000,
      "uploadTimeMs": 25000,
      "isPrimaryResult": true
    }
  ]
}
```

## Processing Management Examples

### 1. Get Processing Status

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/tasks/processing/status
```

**Response:**
```json
{
  "availableSlots": 0,
  "currentlyProcessing": 1,
  "maxConcurrentTasks": 1,
  "memoryPressure": "LOW",
  "freeMemoryMB": 512,
  "totalMemoryMB": 1024,
  "memoryUsagePercentage": 50.0
}
```

### 2. Get Processing Statistics

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/tasks/processing/statistics
```

**Response:**
```json
{
  "totalTasksProcessed": 150,
  "successfulTasks": 145,
  "failedTasks": 5,
  "averageProcessingTimeMs": 25000,
  "averageUploadTimeMs": 15000,
  "successRate": 96.67
}
```

## Result Reuse Examples

### 1. Check Existing Results

**Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/tasks/results/exists?url=https://www.youtube.com/watch?v=dQw4w9WgXcQ&destinationType=TELEGRAM"
```

**Response:**
```json
{
  "exists": true,
  "resultCount": 1,
  "mostRecentResult": {
    "id": "result-123",
    "destinationId": "BAADBAADrwADBREAAYag8VZgAAEC",
    "fileName": "Rick Astley - Never Gonna Give You Up.mp4",
    "fileSizeBytes": 45678901,
    "createdAt": "2024-01-15T10:30:00"
  }
}
```

### 2. Send Existing Result

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/tasks/results/send-existing \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
    "destinationType": "TELEGRAM",
    "chatId": "123456789"
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "Existing result sent successfully",
  "resultId": "result-123",
  "destinationId": "BAADBAADrwADBREAAYag8VZgAAEC"
}
```

## Error Handling Examples

### 1. Invalid URL

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://invalid-url.com/video",
    "chatId": "123456789",
    "format": "mp4",
    "resolution": "720p"
  }'
```

**Response:**
```json
{
  "success": false,
  "error": "Failed to create task: Video not found or unavailable",
  "downloadId": null
}
```

### 2. Task Processing Error

**Task Status Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "FAILED",
  "errorMessage": "Video is private or unavailable",
  "retryCount": 1,
  "maxRetries": 3
}
```

## JavaScript/Node.js Examples

### Using fetch API

```javascript
async function createVideoTask(url, chatId) {
  try {
    const response = await fetch('http://localhost:8080/api/v1/tasks', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        url: url,
        chatId: chatId,
        format: 'mp4',
        resolution: '720p',
        quality: 'best'
      })
    });

    const result = await response.json();
    
    if (result.success) {
      console.log('Task created successfully!');
      console.log('Task ID:', result.downloadId);
      
      // Poll for task completion
      await pollTaskStatus(result.downloadId);
    } else {
      console.error('Task creation failed:', result.error);
    }
  } catch (error) {
    console.error('Request failed:', error);
  }
}

async function pollTaskStatus(taskId) {
  const maxAttempts = 120; // 10 minutes with 5-second intervals
  let attempts = 0;
  
  while (attempts < maxAttempts) {
    try {
      const response = await fetch(`http://localhost:8080/api/v1/tasks/${taskId}`);
      const task = await response.json();
      
      if (task.status === 'COMPLETED') {
        console.log('Task completed successfully!');
        
        // Get task results
        const resultsResponse = await fetch(`http://localhost:8080/api/v1/tasks/${taskId}/results`);
        const results = await resultsResponse.json();
        
        if (results.results && results.results.length > 0) {
          const result = results.results[0];
          console.log('File name:', result.fileName);
          console.log('File size:', result.fileSizeBytes);
          console.log('Telegram file ID:', result.destinationId);
        }
        return;
      } else if (task.status === 'FAILED') {
        console.error('Task failed:', task.errorMessage);
        return;
      } else {
        console.log(`Task status: ${task.status}`);
        await new Promise(resolve => setTimeout(resolve, 5000)); // Wait 5 seconds
        attempts++;
      }
    } catch (error) {
      console.error('Error polling task status:', error);
      attempts++;
    }
  }
  
  console.log('Task polling timeout reached');
}

// Usage
createVideoTask('https://www.youtube.com/watch?v=dQw4w9WgXcQ', '123456789');
```

### Using axios

```javascript
const axios = require('axios');

async function createVideoTask(url, chatId) {
  try {
    const response = await axios.post('http://localhost:8080/api/v1/tasks', {
      url: url,
      chatId: chatId,
      format: 'mp4',
      resolution: '720p',
      quality: 'best'
    });

    const result = response.data;
    
    if (result.success) {
      console.log('Task created successfully!');
      console.log('Task ID:', result.downloadId);
      
      // Poll for task completion
      await pollTaskStatus(result.downloadId);
    } else {
      console.error('Task creation failed:', result.error);
    }
  } catch (error) {
    console.error('Request failed:', error.response?.data || error.message);
  }
}

async function pollTaskStatus(taskId) {
  const maxAttempts = 120; // 10 minutes with 5-second intervals
  let attempts = 0;
  
  while (attempts < maxAttempts) {
    try {
      const response = await axios.get(`http://localhost:8080/api/v1/tasks/${taskId}`);
      const task = response.data;
      
      if (task.status === 'COMPLETED') {
        console.log('Task completed successfully!');
        
        // Get task results
        const resultsResponse = await axios.get(`http://localhost:8080/api/v1/tasks/${taskId}/results`);
        const results = resultsResponse.data;
        
        if (results.results && results.results.length > 0) {
          const result = results.results[0];
          console.log('File name:', result.fileName);
          console.log('File size:', result.fileSizeBytes);
          console.log('Telegram file ID:', result.destinationId);
        }
        return;
      } else if (task.status === 'FAILED') {
        console.error('Task failed:', task.errorMessage);
        return;
      } else {
        console.log(`Task status: ${task.status}`);
        await new Promise(resolve => setTimeout(resolve, 5000)); // Wait 5 seconds
        attempts++;
      }
    } catch (error) {
      console.error('Error polling task status:', error);
      attempts++;
    }
  }
  
  console.log('Task polling timeout reached');
}

// Usage
createVideoTask('https://www.youtube.com/watch?v=dQw4w9WgXcQ', '123456789');
```

## Python Examples

### Using requests

```python
import requests
import json
import time

def create_video_task(url, chat_id):
    try:
        response = requests.post(
            'http://localhost:8080/api/v1/tasks',
            headers={'Content-Type': 'application/json'},
            json={
                'url': url,
                'chatId': chat_id,
                'format': 'mp4',
                'resolution': '720p',
                'quality': 'best'
            }
        )
        
        result = response.json()
        
        if result['success']:
            print('Task created successfully!')
            print('Task ID:', result['downloadId'])
            
            # Poll for task completion
            poll_task_status(result['downloadId'])
        else:
            print('Task creation failed:', result['error'])
            
    except Exception as e:
        print('Request failed:', str(e))

def poll_task_status(task_id):
    max_attempts = 120  # 10 minutes with 5-second intervals
    attempts = 0
    
    while attempts < max_attempts:
        try:
            response = requests.get(f'http://localhost:8080/api/v1/tasks/{task_id}')
            task = response.json()
            
            if task['status'] == 'COMPLETED':
                print('Task completed successfully!')
                
                # Get task results
                results_response = requests.get(f'http://localhost:8080/api/v1/tasks/{task_id}/results')
                results = results_response.json()
                
                if results['results'] and len(results['results']) > 0:
                    result = results['results'][0]
                    print('File name:', result['fileName'])
                    print('File size:', result['fileSizeBytes'])
                    print('Telegram file ID:', result['destinationId'])
                return
            elif task['status'] == 'FAILED':
                print('Task failed:', task['errorMessage'])
                return
            else:
                print(f'Task status: {task["status"]}')
                time.sleep(5)  # Wait 5 seconds
                attempts += 1
                
        except Exception as e:
            print('Error polling task status:', str(e))
            attempts += 1
    
    print('Task polling timeout reached')

# Usage
create_video_task('https://www.youtube.com/watch?v=dQw4w9WgXcQ', '123456789')
```

## Telegram Bot Integration

The application includes a Python Telegram bot that provides real-time status updates. The bot automatically:

- Detects YouTube URLs in messages
- Creates tasks using the new API
- Polls task status and updates messages in real-time
- Shows progress indicators and completion status
- Handles result reuse for instant delivery

### Bot Commands

- `/start` - Show welcome message and instructions
- `/status` - Check bot and backend status
- `/help` - Show detailed help information

### Bot Usage

Simply send a YouTube URL to the bot, and it will:
1. Create a task and show "Processing started..."
2. Update the message with task ID and progress
3. Show completion status with file information
4. Handle errors gracefully with helpful messages

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

The application provides comprehensive logging for all task operations. You can monitor the logs to track:

- Task creation and management
- Processing status and progress
- Memory monitoring and adaptive processing
- Result reuse and caching
- Error handling and retry logic
- Database operations and cleanup

Example log output:
```
2024-01-15 10:30:15.123 INFO  - Creating new video download task for URL: https://www.youtube.com/watch?v=dQw4w9WgXcQ
2024-01-15 10:30:15.124 INFO  - Created video download task with ID: 550e8400-e29b-41d4-a716-446655440000
2024-01-15 10:30:15.125 INFO  - Processing task: 550e8400-e29b-41d4-a716-446655440000
2024-01-15 10:30:20.456 INFO  - Task completed successfully: 550e8400-e29b-41d4-a716-446655440000
2024-01-15 10:30:20.457 INFO  - Result saved with destination ID: BAADBAADrwADBREAAYag8VZgAAEC
```
