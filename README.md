# TG YouTube Downloader

A Spring Boot application that provides video metadata retrieval functionality using yt-dlp-java library.

## Features

- Retrieve comprehensive video metadata from YouTube and other supported platforms
- RESTful API endpoints for metadata retrieval
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

## Response Format

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
2. Build and run using Docker Compose:

```bash
docker-compose up --build
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

## Logging

The application uses SLF4J with Logback for logging. Log levels can be configured in `application.properties`:

```properties
logging.level.ru.aiivar.tg.yt.downloader=INFO
logging.level.com.jfposton.ytdlp=DEBUG
```
