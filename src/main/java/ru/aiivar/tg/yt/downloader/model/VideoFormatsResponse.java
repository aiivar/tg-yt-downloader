package ru.aiivar.tg.yt.downloader.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VideoFormatsResponse {
    private List<VideoFormat> formats;
    private String error;
    private String url;
}
