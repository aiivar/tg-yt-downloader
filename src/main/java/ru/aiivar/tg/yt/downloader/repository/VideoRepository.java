package ru.aiivar.tg.yt.downloader.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.aiivar.tg.yt.downloader.entity.Video;

public interface VideoRepository extends JpaRepository<Video, String> {
}
