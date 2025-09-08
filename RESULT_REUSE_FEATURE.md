# Result Reuse Feature

This document explains the result reuse feature that allows you to avoid re-downloading and re-uploading the same video by reusing existing Telegram file IDs.

## Overview

The system now automatically checks for existing results when creating new tasks and reuses them when possible. This is particularly useful for Telegram destinations where the same video might be requested multiple times by different users.

## Key Features

### 1. Automatic Result Reuse
- When creating a new task, the system checks if the same video has already been downloaded for the same destination
- If a result exists, it's immediately reused instead of re-downloading
- The task is marked as completed instantly

### 2. Telegram Result Preservation
- Telegram results are never deleted during cleanup operations
- This allows indefinite reuse of Telegram file IDs
- Saves bandwidth and processing time

### 3. Result Lookup
- Find existing results by source URL and destination type
- Get the most recent result for a given URL
- Check if a result exists before creating a new task

## How It Works

### Task Creation with Reuse Check

When you create a new task using the API:

```http
POST /api/v1/tasks
{
  "url": "https://www.youtube.com/watch?v=example",
  "chatId": "123456789",
  "format": "mp4",
  "quality": "720p"
}
```

The system will:

1. **Check for existing results** for the same URL and destination type (Telegram)
2. **If found**: Create a new task and immediately mark it as completed with the existing result
3. **If not found**: Create a new task for normal processing

### Response Examples

**New video (needs download):**
```json
{
  "success": true,
  "message": "Task created successfully",
  "downloadId": "task-123"
}
```

**Existing video (reused):**
```json
{
  "success": true,
  "message": "Task completed by reusing existing result",
  "downloadId": "task-124"
}
```

## API Endpoints

### 1. Check for Existing Results

```http
GET /api/v1/tasks/results/exists?sourceUrl=https://www.youtube.com/watch?v=example&destinationType=TELEGRAM
```

**Response:**
```json
{
  "exists": true,
  "destinationId": "BAADBAADrwADBREAAYag8VZgAAEiAg",
  "fileName": "example_video.mp4",
  "fileSizeBytes": 15728640,
  "createdAt": "2024-01-15T10:30:00"
}
```

### 2. Send Existing Result

```http
POST /api/v1/tasks/results/send-existing?sourceUrl=https://www.youtube.com/watch?v=example&chatId=123456789&destinationType=TELEGRAM
```

**Response:**
```json
{
  "success": true,
  "message": "Existing result sent successfully",
  "downloadId": "task-125",
  "telegramFileId": "BAADBAADrwADBREAAYag8VZgAAEiAg",
  "fileName": "example_video.mp4",
  "fileSize": 15728640
}
```

### 3. Get Task Results

```http
GET /api/v1/tasks/{taskId}/results
```

**Response:**
```json
[
  {
    "id": "result-456",
    "destinationType": "TELEGRAM",
    "destinationId": "BAADBAADrwADBREAAYag8VZgAAEiAg",
    "fileName": "example_video.mp4",
    "fileSizeBytes": 15728640,
    "status": "COMPLETED",
    "isPrimaryResult": true,
    "createdAt": "2024-01-15T10:30:00"
  }
]
```

## Database Schema

### New Repository Methods

The `VideoDownloadTaskResultRepository` now includes:

```java
// Find existing completed results by source URL and destination type
List<VideoDownloadTaskResult> findExistingResultsBySourceUrlAndDestination(
    String sourceUrl, DestinationType destinationType);

// Find the most recent completed result
Optional<VideoDownloadTaskResult> findMostRecentResultBySourceUrlAndDestination(
    String sourceUrl, DestinationType destinationType);

// Count existing results
long countExistingResultsBySourceUrlAndDestination(
    String sourceUrl, DestinationType destinationType);
```

### Query Logic

The system uses JOIN queries to find results:

```sql
SELECT r FROM VideoDownloadTaskResult r 
JOIN r.task t 
WHERE t.sourceUrl = :sourceUrl 
AND r.destinationType = :destinationType 
AND r.status = 'COMPLETED' 
AND r.destinationId IS NOT NULL 
ORDER BY r.createdAt DESC
```

## Benefits

### 1. Performance Improvements
- **Instant response** for previously downloaded videos
- **No bandwidth usage** for re-downloads
- **No processing time** for re-uploads
- **Reduced server load**

### 2. Cost Savings
- **Bandwidth savings** - no re-downloading
- **Processing savings** - no re-uploading
- **Storage efficiency** - reuse existing file IDs

### 3. User Experience
- **Faster response times** for popular videos
- **Consistent file IDs** for the same video
- **Reliable delivery** using proven file IDs

## Configuration

### Cleanup Behavior

The system is configured to preserve Telegram results:

```java
// In cleanupOldCompletedResults method
if (result.getDestinationType() == DestinationType.TELEGRAM) {
    logger.debug("Preserving Telegram result: {} for potential reuse", result.getId());
    continue; // Skip deletion
}
```

### Memory Considerations

- **Result storage**: Each result takes minimal database space
- **Index optimization**: Queries are optimized with proper indexes
- **Cleanup strategy**: Only non-Telegram results are cleaned up

## Usage Examples

### 1. Bot Integration

```python
# Python bot example
def handle_video_request(url, chat_id):
    # Check if video already exists
    response = requests.get(f"{API_BASE}/api/v1/tasks/results/exists", 
                          params={"sourceUrl": url})
    
    if response.json()["exists"]:
        # Send existing result
        send_response = requests.post(f"{API_BASE}/api/v1/tasks/results/send-existing",
                                    params={"sourceUrl": url, "chatId": chat_id})
        return send_response.json()
    else:
        # Create new task
        task_response = requests.post(f"{API_BASE}/api/v1/tasks",
                                    json={"url": url, "chatId": chat_id})
        return task_response.json()
```

### 2. Web Application

```javascript
// JavaScript example
async function requestVideo(url, chatId) {
    // Check for existing result
    const existsResponse = await fetch(`/api/v1/tasks/results/exists?sourceUrl=${encodeURIComponent(url)}`);
    const existsData = await existsResponse.json();
    
    if (existsData.exists) {
        // Send existing result
        const sendResponse = await fetch('/api/v1/tasks/results/send-existing', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: `sourceUrl=${encodeURIComponent(url)}&chatId=${chatId}`
        });
        return await sendResponse.json();
    } else {
        // Create new task
        const taskResponse = await fetch('/api/v1/tasks', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ url, chatId })
        });
        return await taskResponse.json();
    }
}
```

## Monitoring

### Log Messages

**Result reuse:**
```
INFO - Found existing result for URL: https://www.youtube.com/watch?v=example and destination: TELEGRAM, creating task for reuse
INFO - Created reused result with ID: result-789 for task: task-456
INFO - Task task-456 completed immediately by reusing existing result
```

**Cleanup preservation:**
```
INFO - Cleanup completed: 5 results deleted, 12 Telegram results preserved
DEBUG - Preserving Telegram result: result-123 for potential reuse
```

### Statistics

You can monitor reuse statistics through the existing statistics endpoints:

```http
GET /api/v1/tasks/statistics
GET /api/v1/tasks/processing/statistics
```

## Best Practices

### 1. URL Normalization
- Ensure consistent URL formatting for better matching
- Consider URL parameters that might affect the video content

### 2. Result Management
- Monitor result storage growth
- Consider periodic cleanup of very old results if needed
- Use the statistics endpoints to track usage

### 3. Error Handling
- Always handle cases where reuse fails
- Fall back to normal processing if reuse encounters errors
- Log reuse attempts for debugging

## Troubleshooting

### Common Issues

1. **Results not being found**
   - Check URL format consistency
   - Verify destination type matches
   - Ensure results are marked as COMPLETED

2. **Reuse not working**
   - Check logs for error messages
   - Verify database queries are working
   - Ensure proper transaction handling

3. **Storage concerns**
   - Monitor database size
   - Consider result archival strategies
   - Use cleanup endpoints if needed

This feature significantly improves the efficiency of your video download system by avoiding redundant downloads and uploads while maintaining full functionality.
