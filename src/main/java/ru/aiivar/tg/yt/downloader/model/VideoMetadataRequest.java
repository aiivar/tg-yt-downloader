package ru.aiivar.tg.yt.downloader.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoMetadataRequest {

    private String url;
}
