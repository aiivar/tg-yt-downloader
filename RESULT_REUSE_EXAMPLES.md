# Result Reuse Examples

This document provides practical examples of how to use the result reuse feature.

## Basic Usage

### 1. Check if a video has already been downloaded

```bash
curl "http://localhost:8080/api/v1/tasks/results/exists?sourceUrl=https://www.youtube.com/watch?v=dQw4w9WgXcQ&destinationType=TELEGRAM"
```

**Response if exists:**
```json
{
  "exists": true,
  "destinationId": "BAADBAADrwADBREAAYag8VZgAAEiAg",
  "fileName": "Rick Astley - Never Gonna Give You Up.mp4",
  "fileSizeBytes": 15728640,
  "createdAt": "2024-01-15T10:30:00"
}
```

**Response if not exists:**
```json
{
  "exists": false
}
```

### 2. Create a task (automatically checks for reuse)

```bash
curl -X POST "http://localhost:8080/api/v1/tasks" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
    "chatId": "123456789",
    "format": "mp4",
    "quality": "720p"
  }'
```

**Response for new video:**
```json
{
  "success": true,
  "message": "Task created successfully",
  "downloadId": "task-123"
}
```

**Response for existing video (reused):**
```json
{
  "success": true,
  "message": "Task completed by reusing existing result",
  "downloadId": "task-124"
}
```

### 3. Send existing result to a chat

```bash
curl -X POST "http://localhost:8080/api/v1/tasks/results/send-existing?sourceUrl=https://www.youtube.com/watch?v=dQw4w9WgXcQ&chatId=987654321&destinationType=TELEGRAM"
```

**Response:**
```json
{
  "success": true,
  "message": "Existing result sent successfully",
  "downloadId": "task-125",
  "telegramFileId": "BAADBAADrwADBREAAYag8VZgAAEiAg",
  "fileName": "Rick Astley - Never Gonna Give You Up.mp4",
  "fileSize": 15728640
}
```

## Python Bot Integration

```python
import requests
import logging

class VideoDownloadBot:
    def __init__(self, api_base_url):
        self.api_base_url = api_base_url
        self.logger = logging.getLogger(__name__)
    
    def handle_video_request(self, url, chat_id):
        """Handle a video download request with result reuse"""
        try:
            # Check if video already exists
            exists_response = requests.get(
                f"{self.api_base_url}/api/v1/tasks/results/exists",
                params={"sourceUrl": url, "destinationType": "TELEGRAM"}
            )
            
            if exists_response.status_code == 200:
                exists_data = exists_response.json()
                
                if exists_data["exists"]:
                    self.logger.info(f"Found existing result for {url}, reusing...")
                    
                    # Send existing result
                    send_response = requests.post(
                        f"{self.api_base_url}/api/v1/tasks/results/send-existing",
                        params={
                            "sourceUrl": url,
                            "chatId": chat_id,
                            "destinationType": "TELEGRAM"
                        }
                    )
                    
                    if send_response.status_code == 200:
                        result = send_response.json()
                        self.logger.info(f"Reused existing result: {result['telegramFileId']}")
                        return {
                            "success": True,
                            "message": "Video sent (reused existing download)",
                            "telegramFileId": result["telegramFileId"]
                        }
                    else:
                        self.logger.error(f"Failed to send existing result: {send_response.text}")
                        # Fall back to normal processing
                
                # Create new task (will automatically check for reuse)
                task_response = requests.post(
                    f"{self.api_base_url}/api/v1/tasks",
                    json={
                        "url": url,
                        "chatId": chat_id,
                        "format": "mp4",
                        "quality": "720p"
                    }
                )
                
                if task_response.status_code == 200:
                    result = task_response.json()
                    self.logger.info(f"Created task: {result['downloadId']}")
                    return {
                        "success": True,
                        "message": result["message"],
                        "taskId": result["downloadId"]
                    }
                else:
                    self.logger.error(f"Failed to create task: {task_response.text}")
                    return {"success": False, "error": "Failed to create download task"}
            
        except Exception as e:
            self.logger.error(f"Error handling video request: {e}")
            return {"success": False, "error": str(e)}

# Usage
bot = VideoDownloadBot("http://localhost:8080")
result = bot.handle_video_request("https://www.youtube.com/watch?v=dQw4w9WgXcQ", "123456789")
print(result)
```

## JavaScript/Node.js Integration

```javascript
const axios = require('axios');

class VideoDownloadService {
    constructor(apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    async handleVideoRequest(url, chatId) {
        try {
            // Check if video already exists
            const existsResponse = await axios.get(`${this.apiBaseUrl}/api/v1/tasks/results/exists`, {
                params: {
                    sourceUrl: url,
                    destinationType: 'TELEGRAM'
                }
            });

            if (existsResponse.data.exists) {
                console.log(`Found existing result for ${url}, reusing...`);
                
                // Send existing result
                const sendResponse = await axios.post(`${this.apiBaseUrl}/api/v1/tasks/results/send-existing`, null, {
                    params: {
                        sourceUrl: url,
                        chatId: chatId,
                        destinationType: 'TELEGRAM'
                    }
                });

                if (sendResponse.status === 200) {
                    console.log(`Reused existing result: ${sendResponse.data.telegramFileId}`);
                    return {
                        success: true,
                        message: 'Video sent (reused existing download)',
                        telegramFileId: sendResponse.data.telegramFileId
                    };
                } else {
                    console.error('Failed to send existing result');
                    // Fall back to normal processing
                }
            }

            // Create new task (will automatically check for reuse)
            const taskResponse = await axios.post(`${this.apiBaseUrl}/api/v1/tasks`, {
                url: url,
                chatId: chatId,
                format: 'mp4',
                quality: '720p'
            });

            if (taskResponse.status === 200) {
                console.log(`Created task: ${taskResponse.data.downloadId}`);
                return {
                    success: true,
                    message: taskResponse.data.message,
                    taskId: taskResponse.data.downloadId
                };
            } else {
                console.error('Failed to create task');
                return { success: false, error: 'Failed to create download task' };
            }

        } catch (error) {
            console.error('Error handling video request:', error);
            return { success: false, error: error.message };
        }
    }
}

// Usage
const service = new VideoDownloadService('http://localhost:8080');
service.handleVideoRequest('https://www.youtube.com/watch?v=dQw4w9WgXcQ', '123456789')
    .then(result => console.log(result))
    .catch(error => console.error(error));
```

## Telegram Bot Integration

```python
from telegram import Update
from telegram.ext import Application, CommandHandler, MessageHandler, filters, ContextTypes
import requests

class TelegramVideoBot:
    def __init__(self, token, api_base_url):
        self.api_base_url = api_base_url
        self.application = Application.builder().token(token).build()
        self.setup_handlers()
    
    def setup_handlers(self):
        self.application.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, self.handle_message))
        self.application.add_handler(CommandHandler("start", self.start))
    
    async def start(self, update: Update, context: ContextTypes.DEFAULT_TYPE):
        await update.message.reply_text("Send me a YouTube URL to download!")
    
    async def handle_message(self, update: Update, context: ContextTypes.DEFAULT_TYPE):
        message_text = update.message.text
        chat_id = str(update.message.chat_id)
        
        # Check if it's a YouTube URL
        if "youtube.com" in message_text or "youtu.be" in message_text:
            await update.message.reply_text("Processing your video request...")
            
            try:
                # Check if video already exists
                exists_response = requests.get(
                    f"{self.api_base_url}/api/v1/tasks/results/exists",
                    params={"sourceUrl": message_text, "destinationType": "TELEGRAM"}
                )
                
                if exists_response.status_code == 200 and exists_response.json()["exists"]:
                    # Send existing result
                    send_response = requests.post(
                        f"{self.api_base_url}/api/v1/tasks/results/send-existing",
                        params={
                            "sourceUrl": message_text,
                            "chatId": chat_id,
                            "destinationType": "TELEGRAM"
                        }
                    )
                    
                    if send_response.status_code == 200:
                        await update.message.reply_text("Video sent! (This was a previously downloaded video)")
                    else:
                        await update.message.reply_text("Error sending existing video. Creating new download...")
                        await self.create_new_task(message_text, chat_id, update)
                else:
                    # Create new task
                    await self.create_new_task(message_text, chat_id, update)
                    
            except Exception as e:
                await update.message.reply_text(f"Error processing video: {str(e)}")
        else:
            await update.message.reply_text("Please send a valid YouTube URL")
    
    async def create_new_task(self, url, chat_id, update):
        try:
            task_response = requests.post(
                f"{self.api_base_url}/api/v1/tasks",
                json={
                    "url": url,
                    "chatId": chat_id,
                    "format": "mp4",
                    "quality": "720p"
                }
            )
            
            if task_response.status_code == 200:
                result = task_response.json()
                if "reusing existing result" in result["message"]:
                    await update.message.reply_text("Video sent! (Reused existing download)")
                else:
                    await update.message.reply_text(f"Download started! Task ID: {result['downloadId']}")
            else:
                await update.message.reply_text("Error creating download task")
                
        except Exception as e:
            await update.message.reply_text(f"Error creating task: {str(e)}")

# Usage
bot = TelegramVideoBot("YOUR_BOT_TOKEN", "http://localhost:8080")
bot.application.run_polling()
```

## Monitoring and Statistics

### Check processing statistics

```bash
curl "http://localhost:8080/api/v1/tasks/processing/statistics"
```

**Response:**
```json
{
  "totalTasks": 150,
  "pendingTasks": 5,
  "processingTasks": 1,
  "completedTasks": 140,
  "failedTasks": 4,
  "retryableTasks": 2,
  "maxConcurrentTasks": 1,
  "availableSlots": 1,
  "memoryUsagePercentage": 45.2,
  "freeMemoryMB": 1024
}
```

### Check processing status

```bash
curl "http://localhost:8080/api/v1/tasks/processing/status"
```

**Response:**
```json
{
  "maxConcurrentTasks": 1,
  "availableSlots": 1,
  "currentlyProcessing": 0,
  "memoryPressure": "LOW",
  "hasEnoughMemory": true
}
```

## Benefits in Practice

1. **First request**: Downloads and uploads video (takes time)
2. **Subsequent requests**: Instantly reuses existing Telegram file ID
3. **Bandwidth savings**: No re-downloading of the same video
4. **Server efficiency**: No re-processing of the same content
5. **User experience**: Faster response times for popular videos

This feature is particularly useful for:
- Popular videos that get requested multiple times
- Bot users who might request the same video again
- Reducing server load and bandwidth costs
- Improving response times for users
