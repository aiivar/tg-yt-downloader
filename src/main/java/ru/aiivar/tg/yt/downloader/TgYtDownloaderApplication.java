package ru.aiivar.tg.yt.downloader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TgYtDownloaderApplication {

	public static void main(String[] args) {
		SpringApplication.run(TgYtDownloaderApplication.class, args);
	}

}
