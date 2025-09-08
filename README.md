# TG YouTube Downloader

A Spring Boot application that provides extensible video download functionality using yt-dlp-java library with task-based processing and result reuse.

## Features

- **Extensible Architecture**: Support for multiple video sources (YouTube, Vimeo, TikTok, etc.) and destinations (Telegram, Discord, etc.)
- **Task-based Processing**: Asynchronous video download and upload with real-time status tracking
- **Result Reuse**: Intelligent caching to avoid re-downloading previously processed videos
- **Memory-aware Processing**: Configurable concurrency limits with adaptive processing based on available memory
- **Real-time Status Updates**: Live progress tracking and status monitoring
- **Telegram Bot Integration**: Complete bot with real-time status updates and progress indicators
- **Database Persistence**: Comprehensive task and result storage with cleanup management
- **Docker Support**: Easy deployment with multiple environment configurations
- **Comprehensive Monitoring**: Processing statistics, memory monitoring, and health checks

## API Endpoints

### Task Management

#### Create Video Download Task
```
POST /api/v1/tasks
Content-Type: application/json

{
  "url": "https://www.youtube.com/watch?v=VIDEO_ID",
  "chatId": "123456789",
  "format": "mp4",
  "resolution": "720p",
  "quality": "best"
}
```

Creates a new video download task. Returns task ID for tracking and status monitoring.

#### Get Task Status
```
GET /api/v1/tasks/{taskId}
```

Returns the current status of a task including progress information.

#### Get Task Results
```
GET /api/v1/tasks/{taskId}/results
```

Returns the results of a completed task including file information and destination IDs.

#### Get All Tasks
```
GET /api/v1/tasks?page=0&size=20&status=PENDING
```

Returns paginated list of tasks with optional filtering by status.

### Processing Management

#### Get Processing Status
```
GET /api/v1/tasks/processing/status
```

Returns current processing status including available slots and memory pressure.

#### Get Processing Statistics
```
GET /api/v1/tasks/processing/statistics
```

Returns processing statistics including success rates and performance metrics.

#### Update Processing Configuration
```
PUT /api/v1/tasks/processing/config
Content-Type: application/json

{
  "maxConcurrentTasks": 2,
  "memoryThresholdPercentage": 80
}
```

Updates processing configuration parameters.

### Result Reuse

#### Check Existing Results
```
GET /api/v1/tasks/results/exists?url={url}&destinationType=TELEGRAM
```

Checks if a result already exists for the given URL and destination type.

#### Send Existing Result
```
POST /api/v1/tasks/results/send-existing
Content-Type: application/json

{
  "url": "https://www.youtube.com/watch?v=VIDEO_ID",
  "destinationType": "TELEGRAM",
  "chatId": "123456789"
}
```

Sends an existing result to the specified destination without re-processing.

## Response Format

### Task Creation Response
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

### Task Status Response
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PROCESSING",
  "sourceUrl": "https://www.youtube.com/watch?v=VIDEO_ID",
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

### Task Results Response
```json
{
  "results": [
    {
      "id": "result-123",
      "taskId": "550e8400-e29b-41d4-a716-446655440000",
      "status": "COMPLETED",
      "destinationType": "TELEGRAM",
      "destinationId": "BAADBAADrwADBREAAYag8VZgAAEC",
      "fileName": "video_title.mp4",
      "fileSizeBytes": 12345678,
      "fileFormat": "mp4",
      "durationSeconds": 225,
      "resolution": "720p",
      "bitrate": 1000000,
      "fps": 30.0,
      "codec": "h264",
      "thumbnailUrl": "https://i.ytimg.com/vi/VIDEO_ID/maxresdefault.jpg",
      "uploadStartedAt": "2024-01-15T10:30:20",
      "uploadCompletedAt": "2024-01-15T10:30:45",
      "processingTimeMs": 25000,
      "uploadTimeMs": 25000,
      "isPrimaryResult": true
    }
  ]
}
```

### Processing Status Response
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

## Error Handling

If an error occurs during task processing, the response will include an `error` field with a descriptive message:

```json
{
  "success": false,
  "error": "Failed to create task: Video not found or unavailable",
  "downloadId": null
}
```

Task status responses include error information:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "FAILED",
  "errorMessage": "Video is private or unavailable",
  "retryCount": 1,
  "maxRetries": 3
}
```

## Running the Application

### Using Docker (Recommended)

1. Ensure you have the `yt-dlp-java-2.0.3.jar` file in the project root directory
2. Set up environment variables:

```bash
# Option A: Export environment variables
export TELEGRAM_BOT_TOKEN=1234567890:ABCdefGHIjklMNOpqrsTUVwxyz
export TELEGRAM_CHAT_ID=123456789
export TELEGRAM_API_ID=your_api_id_here
export TELEGRAM_API_HASH=your_api_hash_here

# Option B: Use .env file
cp env.example .env
# Edit .env file with your actual values
```

3. Build and run using Docker Compose:

```bash
# Development environment (includes Bot API server and Telegram bot)
docker-compose -f docker-compose.dev.yml up --build

# Production environment (includes Bot API server and Telegram bot)
docker-compose -f docker-compose.prod.yml up --build

# Standard environment (includes Bot API server and Telegram bot)
docker-compose up --build

# Bot API server only (if you want to run it separately)
docker-compose -f docker-compose.bot-api.yml up -d
```

### Telegram Bot

The application includes a Python Telegram bot that provides real-time status updates:

- **Real-time Processing**: Shows live progress with task IDs and status updates
- **Result Reuse**: Instantly delivers previously downloaded videos
- **Commands**: `/start`, `/status`, `/help` for user guidance
- **Error Handling**: Graceful error messages and recovery

The bot automatically polls task status and updates messages in real-time, providing a seamless user experience.

### Using Maven

1. Install the yt-dlp-java JAR locally:
```bash
mvn install:install-file \
  -Dfile=yt-dlp-java-2.0.3.jar \
  -DgroupId=com.jfposton \
  -DartifactId=yt-dlp-java \
  -Dversion=2.0.3 \
  -Dpackaging=jar
```

2. Run the application:
```bash
mvn spring-boot:run
```

## Testing

Run the tests using Maven:

```bash
mvn test
```

The application includes comprehensive tests for the task-based system covering:
- Task creation and management
- Result reuse functionality
- Error handling scenarios
- Memory-aware processing
- Extensible architecture components

## Dependencies

- Spring Boot 3.5.5
- yt-dlp-java 2.0.3
- PostgreSQL (for data persistence)
- Lombok (for reducing boilerplate code)
- Python 3.9+ (for Telegram bot)
- python-telegram-bot (for bot functionality)

## Configuration

The application uses Spring Boot's auto-configuration. Key configuration files:
- `application.properties` - Main configuration
- `application-docker.properties` - Docker-specific configuration
- `application-processing.properties` - Processing and memory management configuration

### Temp Directory Configuration

The application creates temporary directories for video downloads in the system's temp directory:
- **Local development**: Uses `java.io.tmpdir` system property
- **Docker**: Uses `/tmp/temp_downloads` directory with proper permissions
- **Fallback**: Defaults to `/tmp` if system temp directory is not available

The temp directories are automatically cleaned up after each download operation.

### Telegram Bot Configuration

The application requires Telegram Bot configuration for file uploads:

1. **Create a Telegram Bot**:
   - Message @BotFather on Telegram
   - Use `/newbot` command
   - Follow instructions to create your bot
   - Save the bot token

2. **Get Chat ID**:
   - Start a chat with your bot
   - Send a message to the bot
   - Access: `https://api.telegram.org/bot<YOUR_BOT_TOKEN>/getUpdates`
   - Find your chat ID in the response

3. **Configure Application**:

   **Option A: Environment Variables (Recommended)**
   ```bash
   export TELEGRAM_BOT_TOKEN=1234567890:ABCdefGHIjklMNOpqrsTUVwxyz
   export TELEGRAM_CHAT_ID=123456789
   ```

   **Option B: .env file**
   ```bash
   cp env.example .env
   # Edit .env file with your actual values
   ```

   **Option C: application.properties (for local development only)**
   ```properties
   telegram.bot.token=1234567890:ABCdefGHIjklMNOpqrsTUVwxyz
   telegram.chat.id=123456789
   ```

### Large File Support (Up to 2GB)

For files larger than 50MB, the application can use a local Telegram Bot API server:

1. **Set up Local Bot API Server** (see `LOCAL_BOT_API_SETUP.md`)
2. **Configure Environment Variables**:
   ```bash
   export TELEGRAM_API_USE_LOCAL=true
   export TELEGRAM_API_LOCAL_URL=http://localhost:8081
   ```
3. **Automatic Fallback**: Files > 50MB automatically use local API if configured

## Logging

The application uses SLF4J with Logback for logging. Log levels can be configured in `application.properties`:

```properties
logging.level.ru.aiivar.tg.yt.downloader=INFO
logging.level.com.jfposton.ytdlp=DEBUG
```
