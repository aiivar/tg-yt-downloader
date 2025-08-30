# TG YouTube Downloader

A Spring Boot application that provides video metadata retrieval functionality using yt-dlp-java library.

## Features

- Retrieve comprehensive video metadata from YouTube and other supported platforms
- Download videos in MP4 format with 720p resolution
- Real upload to Telegram file server with file ID retrieval
- **Support for large files up to 2GB using local Bot API server**
- Automatic temp directory management and cleanup
- Database storage of video URLs and Telegram file IDs
- RESTful API endpoints for metadata retrieval and video downloading
- Telegram Bot API integration with health checks
- Docker support for easy deployment
- Comprehensive error handling and logging

## API Endpoints

### Health Check
```
GET /api/video/health
```
Returns the health status of the application.

### Get Video Metadata
```
POST /api/video/metadata
Content-Type: application/json

{
  "url": "https://www.youtube.com/watch?v=VIDEO_ID"
}
```

Returns comprehensive video metadata including:
- Title, description, uploader information
- Duration, view count, like count
- Upload date, availability, license
- Tags, category, language
- Format information (resolution, fps, codecs)
- File size and other technical details

### Get Video Formats
```
POST /api/video/formats
Content-Type: application/json

{
  "url": "https://www.youtube.com/watch?v=VIDEO_ID"
}
```

Returns a list of available video formats for the given URL, including:
- Format ID, extension, resolution
- File size, total bitrate
- Video codec, video bitrate
- Audio codec, audio bitrate, audio sample rate
- FPS (frames per second)
- Additional format information

### Download Video
```
POST /api/video/download
Content-Type: application/json

{
  "url": "https://www.youtube.com/watch?v=VIDEO_ID",
  "format": "mp4",
  "resolution": "720p",
  "quality": "best"
}
```

Downloads a video in MP4 format with 720p resolution and uploads it to Telegram file server. Returns:
- Download ID for tracking
- File name and size
- Telegram file ID
- Success/error status

### Get Download Status
```
GET /api/video/download/{downloadId}/status
```

Returns the current status of a download operation using the download ID.

### Check Telegram API Health
```
GET /api/video/telegram/health
```

Returns the health status of the Telegram Bot API connection.

### Get Telegram File Info
```
GET /api/video/telegram/file/{fileId}
```

Returns information about a file stored in Telegram using its file ID.

### Get Telegram Configuration
```
GET /api/video/telegram/config
```

Returns the current Telegram Bot API configuration including local server settings.

## Response Format

### Video Download Response
```json
{
  "success": true,
  "message": "Video downloaded and uploaded successfully",
  "downloadId": "550e8400-e29b-41d4-a716-446655440000",
  "fileName": "video_title.mp4",
  "fileSize": 12345678,
  "telegramFileId": "mock_tg_550e8400-e29b-41d4-a716-446655440000_1234567890",
  "error": null
}
```

### Video Metadata Response
```json
{
  "title": "Video Title",
  "description": "Video description...",
  "uploader": "Channel Name",
  "uploaderId": "channel_id",
  "channelUrl": "https://www.youtube.com/channel/...",
  "webpageUrl": "https://www.youtube.com/watch?v=...",
  "thumbnail": "https://i.ytimg.com/vi/.../default.jpg",
  "duration": "PT3M45S",
  "viewCount": 1234567,
  "likeCount": 12345,
  "uploadDate": "20231201",
  "availability": "public",
  "license": "Standard YouTube License",
  "tags": ["tag1", "tag2", "tag3"],
  "category": "Entertainment",
  "language": "en",
  "ageLimit": "0",
  "isLive": false,
  "wasLive": false,
  "liveStatus": "not_live",
  "extractor": "Youtube",
  "extractorKey": "Youtube",
  "format": "video/webm",
  "resolution": "1080p",
  "fps": "30",
  "vcodec": "vp9",
  "acodec": "opus",
  "filesize": 12345678,
  "error": null
}
```

### Video Formats Response
```json
{
  "formats": [
    {
      "formatId": "22",
      "extension": "mp4",
      "resolution": "1280x720",
      "note": "",
      "filesize": "50.12MiB",
      "tbr": "1.2MiB/s",
      "vcodec": "avc1.4d401f",
      "acodec": "mp4a.40.2",
      "fps": "30",
      "vbr": "1.2MiB/s",
      "abr": "128k",
      "asr": "44kHz",
      "moreInfo": "720p, mp4_dash"
    },
    {
      "formatId": "18",
      "extension": "mp4",
      "resolution": "640x360",
      "note": "",
      "filesize": "25.06MiB",
      "tbr": "600KiB/s",
      "vcodec": "avc1.42001E",
      "acodec": "mp4a.40.2",
      "fps": "30",
      "vbr": "600KiB/s",
      "abr": "96k",
      "asr": "44kHz",
      "moreInfo": "360p, mp4_dash"
    }
  ],
  "error": null,
  "url": "https://www.youtube.com/watch?v=VIDEO_ID"
}
```

## Error Handling

If an error occurs during metadata retrieval, the response will include an `error` field with a descriptive message:

```json
{
  "error": "Error retrieving metadata: Video not found",
  "title": null,
  "description": null,
  ...
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
# Development environment (includes Bot API server)
docker-compose -f docker-compose.dev.yml up --build

# Production environment (includes Bot API server)
docker-compose -f docker-compose.prod.yml up --build

# Standard environment (includes Bot API server)
docker-compose up --build

# Bot API server only (if you want to run it separately)
docker-compose -f docker-compose.bot-api.yml up -d
```

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

The application includes comprehensive tests for the VideoMetadataService covering:
- Valid YouTube URLs
- Invalid URLs
- Null and empty URLs
- Error handling scenarios

## Dependencies

- Spring Boot 3.5.5
- yt-dlp-java 2.0.3
- PostgreSQL (for data persistence)
- Lombok (for reducing boilerplate code)

## Configuration

The application uses Spring Boot's auto-configuration. Key configuration files:
- `application.properties` - Main configuration
- `application-docker.properties` - Docker-specific configuration

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
