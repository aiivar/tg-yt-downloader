package ru.aiivar.tg.yt.downloader.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoDownloadRequest {

    private String url;
    private String chatId;
    private String format = "mp4";
    private String resolution = "720p";
    private String quality = "best";
}
