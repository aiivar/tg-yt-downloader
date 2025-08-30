package ru.aiivar.tg.yt.downloader.service;

import com.jfposton.ytdlp.YtDlp;
import com.jfposton.ytdlp.YtDlpException;
import com.jfposton.ytdlp.YtDlpRequest;
import com.jfposton.ytdlp.YtDlpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.aiivar.tg.yt.downloader.model.VideoMetadataRequest;
import ru.aiivar.tg.yt.downloader.model.VideoMetadataResponse;
import ru.aiivar.tg.yt.downloader.model.VideoFormatsResponse;
import ru.aiivar.tg.yt.downloader.model.VideoFormat;
import ru.aiivar.tg.yt.downloader.repository.VideoRepository;

import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VideoMetadataService {

    private static final Logger logger = LoggerFactory.getLogger(VideoMetadataService.class);

    @Autowired
    private VideoRepository videoRepository;

    public VideoMetadataResponse getMetadata(VideoMetadataRequest request) {
        try {
            logger.info("Retrieving metadata for URL: {}", request.getUrl());

            YtDlpRequest ytRequest = new YtDlpRequest(request.getUrl());
            ytRequest.setOption("dump-json");
            ytRequest.setOption("no-playlist");

            YtDlpResponse response = YtDlp.execute(ytRequest);

            if (response.getExitCode() != 0) {
                logger.error("yt-dlp failed with exit code: {} for URL: {}", response.getExitCode(), request.getUrl());
                return VideoMetadataResponse.builder()
                        .error("Failed to retrieve video information: " + response.getErr())
                        .build();
            }

            String jsonOutput = response.getOut();
            return parseVideoMetadata(jsonOutput, request.getUrl());

        } catch (YtDlpException e) {
            logger.error("YoutubeDLException for URL: {}", request.getUrl(), e);
            return VideoMetadataResponse.builder()
                    .error("Error retrieving metadata: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("Unexpected error for URL: {}", request.getUrl(), e);
            return VideoMetadataResponse.builder()
                    .error("Unexpected error: " + e.getMessage())
                    .build();
        }
    }

    public VideoFormatsResponse getFormats(VideoMetadataRequest request) {
        try {
            logger.info("Retrieving formats for URL: {}", request.getUrl());

            YtDlpRequest ytRequest = new YtDlpRequest(request.getUrl());
            ytRequest.setOption("list-formats");
            ytRequest.setOption("no-playlist");

            YtDlpResponse response = YtDlp.execute(ytRequest);

            if (response.getExitCode() != 0) {
                logger.error("yt-dlp failed with exit code: {} for URL: {}", response.getExitCode(), request.getUrl());
                return VideoFormatsResponse.builder()
                        .error("Failed to retrieve video formats: " + response.getErr())
                        .url(request.getUrl())
                        .build();
            }

            String formatsOutput = response.getOut();
            return parseVideoFormats(formatsOutput, request.getUrl());

        } catch (YtDlpException e) {
            logger.error("YoutubeDLException for URL: {}", request.getUrl(), e);
            return VideoFormatsResponse.builder()
                    .error("Error retrieving formats: " + e.getMessage())
                    .url(request.getUrl())
                    .build();
        } catch (Exception e) {
            logger.error("Unexpected error for URL: {}", request.getUrl(), e);
            return VideoFormatsResponse.builder()
                    .error("Unexpected error: " + e.getMessage())
                    .url(request.getUrl())
                    .build();
        }
    }

    private VideoFormatsResponse parseVideoFormats(String formatsOutput, String url) {
        try {
            List<VideoFormat> formats = new ArrayList<>();
            String[] lines = formatsOutput.split("\n");
            
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("ID") || line.startsWith("--")) {
                    continue;
                }
                
                VideoFormat format = parseFormatLine(line);
                if (format != null) {
                    formats.add(format);
                }
            }

            return VideoFormatsResponse.builder()
                    .formats(formats)
                    .url(url)
                    .build();

        } catch (Exception e) {
            logger.error("Error parsing video formats output", e);
            return VideoFormatsResponse.builder()
                    .error("Error parsing video formats: " + e.getMessage())
                    .url(url)
                    .build();
        }
    }

    private VideoFormat parseFormatLine(String line) {
        try {
            // yt-dlp -F output format: ID EXT RESOLUTION │ FILESIZE TBR │ VCODEC VBR │ ACODEC ABR ASR │ MORE INFO
            // Example: 22 mp4 1280x720 │ 50.12MiB 1.2MiB/s │ avc1.4d401f 1.2MiB/s │ mp4a.40.2 128k 44kHz │ 720p, mp4_dash
            String[] parts = line.split("\\│");
            if (parts.length < 3) {
                return null;
            }

            // Parse first part: ID EXT RESOLUTION
            String[] firstPart = parts[0].trim().split("\\s+");
            if (firstPart.length < 2) {
                return null;
            }

            String formatId = firstPart[0];
            String extension = firstPart[1];
            String resolution = firstPart.length > 2 ? firstPart[2] : "";

            // Parse second part: FILESIZE TBR
            String[] secondPart = parts[1].trim().split("\\s+");
            String filesize = secondPart.length > 0 ? secondPart[0] : "";
            String tbr = secondPart.length > 1 ? secondPart[1] : "";

            // Parse third part: VCODEC VBR
            String[] thirdPart = parts[2].trim().split("\\s+");
            String vcodec = thirdPart.length > 0 ? thirdPart[0] : "";
            String vbr = thirdPart.length > 1 ? thirdPart[1] : "";

            // Parse fourth part: ACODEC ABR ASR
            String acodec = "";
            String abr = "";
            String asr = "";
            if (parts.length > 3) {
                String[] fourthPart = parts[3].trim().split("\\s+");
                acodec = fourthPart.length > 0 ? fourthPart[0] : "";
                abr = fourthPart.length > 1 ? fourthPart[1] : "";
                asr = fourthPart.length > 2 ? fourthPart[2] : "";
            }

            // Parse more info (last part)
            String moreInfo = "";
            if (parts.length > 4) {
                moreInfo = parts[4].trim();
            }

            // Extract fps from resolution if present (e.g., "1280x720@30fps")
            String fps = "";
            if (resolution.contains("@")) {
                String[] resParts = resolution.split("@");
                resolution = resParts[0];
                fps = resParts[1].replace("fps", "");
            }

            return VideoFormat.builder()
                    .formatId(formatId)
                    .extension(extension)
                    .resolution(resolution)
                    .filesize(filesize)
                    .tbr(tbr)
                    .vcodec(vcodec)
                    .vbr(vbr)
                    .acodec(acodec)
                    .abr(abr)
                    .asr(asr)
                    .fps(fps)
                    .moreInfo(moreInfo)
                    .build();

        } catch (Exception e) {
            logger.warn("Error parsing format line: {}", line, e);
            return null;
        }
    }

    private VideoMetadataResponse parseVideoMetadata(String jsonOutput, String url) {
        try {
            // Extract basic information using regex patterns
            String title = extractValue(jsonOutput, "\"title\":\\s*\"([^\"]+)\"");
            String description = extractValue(jsonOutput, "\"description\":\\s*\"([^\"]+)\"");
            String uploader = extractValue(jsonOutput, "\"uploader\":\\s*\"([^\"]+)\"");
            String uploaderId = extractValue(jsonOutput, "\"uploader_id\":\\s*\"([^\"]+)\"");
            String channelUrl = extractValue(jsonOutput, "\"channel_url\":\\s*\"([^\"]+)\"");
            String webpageUrl = extractValue(jsonOutput, "\"webpage_url\":\\s*\"([^\"]+)\"");
            String thumbnail = extractValue(jsonOutput, "\"thumbnail\":\\s*\"([^\"]+)\"");
            String durationStr = extractValue(jsonOutput, "\"duration\":\\s*(\\d+)");
            String viewCountStr = extractValue(jsonOutput, "\"view_count\":\\s*(\\d+)");
            String likeCountStr = extractValue(jsonOutput, "\"like_count\":\\s*(\\d+)");
            String uploadDate = extractValue(jsonOutput, "\"upload_date\":\\s*\"([^\"]+)\"");
            String availability = extractValue(jsonOutput, "\"availability\":\\s*\"([^\"]+)\"");
            String license = extractValue(jsonOutput, "\"license\":\\s*\"([^\"]+)\"");
            String tags = extractValue(jsonOutput, "\"tags\":\\s*\\[([^\\]]+)\\]");
            String category = extractValue(jsonOutput, "\"category\":\\s*\"([^\"]+)\"");
            String language = extractValue(jsonOutput, "\"language\":\\s*\"([^\"]+)\"");
            String ageLimitStr = extractValue(jsonOutput, "\"age_limit\":\\s*(\\d+)");
            String isLiveStr = extractValue(jsonOutput, "\"is_live\":\\s*(true|false)");
            String wasLiveStr = extractValue(jsonOutput, "\"was_live\":\\s*(true|false)");
            String liveStatus = extractValue(jsonOutput, "\"live_status\":\\s*\"([^\"]+)\"");
            String extractor = extractValue(jsonOutput, "\"extractor\":\\s*\"([^\"]+)\"");
            String extractorKey = extractValue(jsonOutput, "\"extractor_key\":\\s*\"([^\"]+)\"");
            String format = extractValue(jsonOutput, "\"format\":\\s*\"([^\"]+)\"");
            String resolution = extractValue(jsonOutput, "\"resolution\":\\s*\"([^\"]+)\"");
            String fps = extractValue(jsonOutput, "\"fps\":\\s*(\\d+)");
            String vcodec = extractValue(jsonOutput, "\"vcodec\":\\s*\"([^\"]+)\"");
            String acodec = extractValue(jsonOutput, "\"acodec\":\\s*\"([^\"]+)\"");
            String filesizeStr = extractValue(jsonOutput, "\"filesize\":\\s*(\\d+)");

            // Parse numeric values
            Duration duration = null;
            if (durationStr != null) {
                duration = Duration.ofSeconds(Long.parseLong(durationStr));
            }

            Long viewCount = null;
            if (viewCountStr != null) {
                viewCount = Long.parseLong(viewCountStr);
            }

            Long likeCount = null;
            if (likeCountStr != null) {
                likeCount = Long.parseLong(likeCountStr);
            }

            Long filesize = null;
            if (filesizeStr != null) {
                filesize = Long.parseLong(filesizeStr);
            }

            Boolean isLive = null;
            if (isLiveStr != null) {
                isLive = Boolean.parseBoolean(isLiveStr);
            }

            Boolean wasLive = null;
            if (wasLiveStr != null) {
                wasLive = Boolean.parseBoolean(wasLiveStr);
            }

            List<String> tagsList = parseTags(tags);

            return VideoMetadataResponse.builder()
                    .title(title)
                    .description(description)
                    .uploader(uploader)
                    .uploaderId(uploaderId)
                    .channelUrl(channelUrl)
                    .webpageUrl(webpageUrl != null ? webpageUrl : url)
                    .thumbnail(thumbnail)
                    .duration(duration)
                    .viewCount(viewCount)
                    .likeCount(likeCount)
                    .uploadDate(uploadDate)
                    .availability(availability)
                    .license(license)
                    .tags(tagsList)
                    .category(category)
                    .language(language)
                    .ageLimit(ageLimitStr)
                    .isLive(isLive)
                    .wasLive(wasLive)
                    .liveStatus(liveStatus)
                    .extractor(extractor)
                    .extractorKey(extractorKey)
                    .format(format)
                    .resolution(resolution)
                    .fps(fps)
                    .vcodec(vcodec)
                    .acodec(acodec)
                    .filesize(filesize)
                    .build();

        } catch (Exception e) {
            logger.error("Error parsing video metadata JSON", e);
            return VideoMetadataResponse.builder()
                    .error("Error parsing video metadata: " + e.getMessage())
                    .build();
        }
    }

    private String extractValue(String json, String pattern) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private List<String> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.trim().isEmpty()) {
            return List.of();
        }

        try {
            // Remove quotes and split by comma
            String cleanTags = tagsJson.replaceAll("\"", "").trim();
            if (cleanTags.isEmpty()) {
                return List.of();
            }

            return List.of(cleanTags.split(","))
                    .stream()
                    .map(String::trim)
                    .filter(tag -> !tag.isEmpty())
                    .toList();
        } catch (Exception e) {
            logger.warn("Error parsing tags: {}", tagsJson, e);
            return List.of();
        }
    }
}
