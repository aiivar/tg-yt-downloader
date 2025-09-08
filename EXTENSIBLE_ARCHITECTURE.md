# Extensible Video Download Task Architecture

This document describes the new extensible architecture for video download tasks that supports multiple sources and destinations.

## Overview

The new architecture is designed to be highly extensible, allowing you to easily add support for new video sources (YouTube, Vimeo, TikTok, etc.) and destinations (Telegram, Discord, Slack, etc.) without modifying the core system.

## Core Components

### 1. Entities

#### BaseTaskEntity
- Abstract base class for all task-related entities
- Provides common fields: `id`, `createdAt`, `updatedAt`, `status`, `priority`, `retryCount`, `maxRetries`, `errorMessage`, `metadata`

#### VideoDownloadTask
- Main entity representing a video download task
- Contains source information (URL, type), destination information (type, config), user information, and download parameters
- Supports multiple formats, qualities, and resolutions
- Tracks processing times and file information

#### VideoDownloadTaskResult
- Entity representing the result of a video download task
- Stores destination-specific information (file IDs, URLs)
- Contains video metadata (duration, resolution, bitrate, etc.)
- Supports multiple results per task (e.g., different formats or destinations)

### 2. Enums

#### TaskStatus
- `PENDING`: Task is created and waiting to be processed
- `PROCESSING`: Task is currently being processed
- `COMPLETED`: Task completed successfully
- `FAILED`: Task failed with an error
- `CANCELLED`: Task was cancelled

#### SourceType
- `YOUTUBE`: YouTube videos

#### DestinationType
- `TELEGRAM`: Telegram Bot API

### 3. Processors

#### VideoSourceProcessor Interface
Handles downloading videos from different sources:

```java
public interface VideoSourceProcessor {
    SourceType getSupportedSourceType();
    boolean canProcess(String url);
    File downloadVideo(VideoDownloadTask task) throws Exception;
    VideoMetadata getVideoMetadata(String url) throws Exception;
    Map<String, Object> getAvailableFormats(String url) throws Exception;
    void validateRequest(VideoDownloadTask task) throws Exception;
}
```

#### VideoDestinationProcessor Interface
Handles uploading videos to different destinations:

```java
public interface VideoDestinationProcessor {
    DestinationType getSupportedDestinationType();
    String uploadVideo(File videoFile, VideoDownloadTask task, VideoDownloadTaskResult result) throws Exception;
    void sendVideoById(String destinationId, VideoDownloadTask task) throws Exception;
    Map<String, Object> getVideoInfo(String destinationId) throws Exception;
    boolean deleteVideo(String destinationId) throws Exception;
    void validateRequest(VideoDownloadTask task) throws Exception;
}
```

### 4. Services

#### VideoDownloadTaskService
- Manages video download tasks
- Provides CRUD operations for tasks
- Handles task status updates and retries
- Supports batch processing and cleanup

#### VideoDownloadTaskResultService
- Manages task results
- Handles result creation and updates
- Provides statistics and analytics

#### VideoDownloadTaskExecutor
- Scheduled task executor for processing tasks
- Handles pending tasks, retries, and stuck task cleanup
- Provides asynchronous processing capabilities

## Usage Examples

### Creating a New Source Processor

To add support for a new video source (e.g., Vimeo):

```java
@Component
public class VimeoSourceProcessor implements VideoSourceProcessor {
    
    @Override
    public SourceType getSupportedSourceType() {
        return SourceType.VIMEO;
    }
    
    @Override
    public boolean canProcess(String url) {
        return url != null && url.contains("vimeo.com");
    }
    
    @Override
    public File downloadVideo(VideoDownloadTask task) throws Exception {
        // Implementation for downloading from Vimeo
        // Use Vimeo API or yt-dlp with Vimeo support
    }
    
    // Implement other required methods...
}
```

### Creating a New Destination Processor

To add support for a new destination (e.g., Discord):

```java
@Component
public class DiscordDestinationProcessor implements VideoDestinationProcessor {
    
    @Override
    public DestinationType getSupportedDestinationType() {
        return DestinationType.DISCORD;
    }
    
    @Override
    public String uploadVideo(File videoFile, VideoDownloadTask task, VideoDownloadTaskResult result) throws Exception {
        // Implementation for uploading to Discord
        // Use Discord webhook API
    }
    
    // Implement other required methods...
}
```

### Creating a Task

```java
// Create a task from a request
VideoDownloadRequest request = new VideoDownloadRequest();
request.setUrl("https://www.youtube.com/watch?v=example");
request.setChatId("123456789");
request.setFormat("mp4");
request.setQuality("720p");

VideoDownloadTask task = taskService.createTask(request);
```

### Processing a Task

```java
// Process a task (download and upload)
VideoDownloadTaskResult result = taskService.processTask(task.getId());
```

## API Endpoints

### Task Management
- `POST /api/v1/tasks` - Create a new task
- `GET /api/v1/tasks/{taskId}` - Get task by ID
- `GET /api/v1/tasks` - Get all tasks (paginated)
- `PUT /api/v1/tasks/{taskId}/status` - Update task status
- `POST /api/v1/tasks/{taskId}/retry` - Retry a failed task
- `POST /api/v1/tasks/{taskId}/cancel` - Cancel a task
- `DELETE /api/v1/tasks/{taskId}` - Delete a task

### Task Processing
- `POST /api/v1/tasks/{taskId}/process` - Process a single task
- `POST /api/v1/tasks/process` - Process multiple tasks
- `GET /api/v1/tasks/pending` - Get pending tasks
- `GET /api/v1/tasks/retryable` - Get retryable tasks

### Results
- `GET /api/v1/tasks/{taskId}/results` - Get task results
- `GET /api/v1/tasks/{taskId}/results/primary` - Get primary result

### Statistics and Cleanup
- `GET /api/v1/tasks/statistics` - Get task statistics
- `DELETE /api/v1/tasks/cleanup/completed` - Clean up old completed tasks
- `DELETE /api/v1/tasks/cleanup/failed` - Clean up old failed tasks

## Database Schema

### video_download_tasks
- `id` (VARCHAR, Primary Key)
- `source_url` (VARCHAR)
- `source_type` (ENUM)
- `destination_type` (ENUM)
- `destination_config` (TEXT)
- `user_id` (VARCHAR)
- `chat_id` (VARCHAR)
- `requested_format` (VARCHAR)
- `requested_quality` (VARCHAR)
- `requested_resolution` (VARCHAR)
- `status` (ENUM)
- `priority` (INTEGER)
- `retry_count` (INTEGER)
- `max_retries` (INTEGER)
- `error_message` (VARCHAR)
- `metadata` (TEXT)
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)
- `download_started_at` (TIMESTAMP)
- `download_completed_at` (TIMESTAMP)
- `estimated_duration_seconds` (BIGINT)
- `file_size_bytes` (BIGINT)
- `temp_file_path` (VARCHAR)

### video_download_task_results
- `id` (VARCHAR, Primary Key)
- `task_id` (VARCHAR, Foreign Key)
- `destination_type` (ENUM)
- `destination_id` (VARCHAR)
- `file_name` (VARCHAR)
- `file_size_bytes` (BIGINT)
- `file_format` (VARCHAR)
- `duration_seconds` (BIGINT)
- `resolution` (VARCHAR)
- `bitrate` (BIGINT)
- `fps` (DOUBLE)
- `codec` (VARCHAR)
- `thumbnail_url` (VARCHAR)
- `download_url` (VARCHAR)
- `status` (ENUM)
- `is_primary_result` (BOOLEAN)
- `processing_time_ms` (BIGINT)
- `upload_time_ms` (BIGINT)
- `destination_metadata` (TEXT)
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)
- `upload_started_at` (TIMESTAMP)
- `upload_completed_at` (TIMESTAMP)

## Configuration

### Processor Registration
Processors are automatically registered via Spring's component scanning. The `ProcessorConfiguration` class handles the registration process.

### Scheduled Tasks
The `VideoDownloadTaskExecutor` runs several scheduled tasks:
- Process pending tasks every 30 seconds
- Retry failed tasks every 5 minutes
- Handle stuck tasks every 10 minutes
- Clean up old tasks every hour

## Extensibility Benefits

1. **Easy to Add New Sources**: Just implement `VideoSourceProcessor` and add the source type to the enum
2. **Easy to Add New Destinations**: Just implement `VideoDestinationProcessor` and add the destination type to the enum
3. **Pluggable Architecture**: Processors are automatically discovered and registered
4. **Consistent API**: All processors follow the same interface, making the system predictable
5. **Rich Metadata**: Tasks and results store comprehensive metadata for analytics and debugging
6. **Retry Logic**: Built-in retry mechanism for failed tasks
7. **Monitoring**: Comprehensive statistics and monitoring capabilities
8. **Cleanup**: Automatic cleanup of old tasks and results

## Migration from Existing System

The new system is designed to work alongside the existing `VideoDownloadService`. You can:

1. Keep using the existing service for backward compatibility
2. Gradually migrate to the new task-based system
3. Use both systems simultaneously for different use cases

The new system provides better tracking, monitoring, and extensibility while maintaining the same core functionality.
