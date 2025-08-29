package ru.aiivar.tg.yt.downloader.model;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.util.List;

@Data
@Builder
public class VideoMetadataResponse {

    private String title;
    private String description;
    private String uploader;
    private String uploaderId;
    private String channelUrl;
    private String webpageUrl;
    private String thumbnail;
    private Duration duration;
    private Long viewCount;
    private Long likeCount;
    private String uploadDate;
    private String availability;
    private String license;
    private List<String> tags;
    private String category;
    private String language;
    private String ageLimit;
    private Boolean isLive;
    private Boolean wasLive;
    private String liveStatus;
    private String extractor;
    private String extractorKey;
    private String format;
    private String resolution;
    private String fps;
    private String vcodec;
    private String acodec;
    private Long filesize;
    private String error;
}
