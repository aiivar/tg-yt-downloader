package ru.aiivar.tg.yt.downloader.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VideoFormat {
    private String formatId;
    private String extension;
    private String resolution;
    private String note;
    private String filesize;
    private String tbr; // Total bitrate
    private String vcodec;
    private String acodec;
    private String fps;
    private String vbr; // Video bitrate
    private String abr; // Audio bitrate
    private String asr; // Audio sample rate
    private String moreInfo;
}
