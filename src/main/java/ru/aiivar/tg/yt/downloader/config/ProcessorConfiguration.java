package ru.aiivar.tg.yt.downloader.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import ru.aiivar.tg.yt.downloader.service.VideoDownloadTaskService;
import ru.aiivar.tg.yt.downloader.service.processor.VideoDestinationProcessor;
import ru.aiivar.tg.yt.downloader.service.processor.VideoSourceProcessor;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Configuration class for registering video processors
 */
@Configuration
public class ProcessorConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ProcessorConfiguration.class);

    @Autowired
    private VideoDownloadTaskService taskService;

    @Autowired
    private List<VideoSourceProcessor> sourceProcessors;

    @Autowired
    private List<VideoDestinationProcessor> destinationProcessors;

    @PostConstruct
    public void registerProcessors() {
        logger.info("Registering video processors...");

        // Register source processors
        for (VideoSourceProcessor processor : sourceProcessors) {
            if (taskService instanceof ru.aiivar.tg.yt.downloader.service.impl.VideoDownloadTaskServiceImpl) {
                ((ru.aiivar.tg.yt.downloader.service.impl.VideoDownloadTaskServiceImpl) taskService)
                        .registerSourceProcessor(processor);
            }
            logger.info("Registered source processor: {} for type: {}", 
                    processor.getClass().getSimpleName(), processor.getSupportedSourceType());
        }

        // Register destination processors
        for (VideoDestinationProcessor processor : destinationProcessors) {
            if (taskService instanceof ru.aiivar.tg.yt.downloader.service.impl.VideoDownloadTaskServiceImpl) {
                ((ru.aiivar.tg.yt.downloader.service.impl.VideoDownloadTaskServiceImpl) taskService)
                        .registerDestinationProcessor(processor);
            }
            logger.info("Registered destination processor: {} for type: {}", 
                    processor.getClass().getSimpleName(), processor.getSupportedDestinationType());
        }

        logger.info("Successfully registered {} source processors and {} destination processors", 
                sourceProcessors.size(), destinationProcessors.size());
    }
}
