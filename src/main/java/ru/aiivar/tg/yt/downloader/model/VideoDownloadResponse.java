package ru.aiivar.tg.yt.downloader.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VideoDownloadResponse {

    private boolean success;
    private String message;
    private String downloadId;
    private String fileName;
    private Long fileSize;
    private String telegramFileId;
    private String error;
}
