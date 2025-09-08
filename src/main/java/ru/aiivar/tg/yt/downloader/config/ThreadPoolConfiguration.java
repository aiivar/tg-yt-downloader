package ru.aiivar.tg.yt.downloader.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for thread pools used in video processing
 */
@Configuration
@EnableAsync
public class ThreadPoolConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolConfiguration.class);

    @Autowired
    private ProcessingConfiguration processingConfig;

    /**
     * Thread pool executor for video processing tasks
     */
    @Bean(name = "videoProcessingExecutor")
    public Executor videoProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(processingConfig.getCoreThreadPoolSize());
        executor.setMaxPoolSize(processingConfig.getMaxThreadPoolSize());
        executor.setQueueCapacity(processingConfig.getQueueCapacity());
        executor.setThreadNamePrefix("VideoProcessing-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        // Rejection policy: Caller runs if queue is full
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.initialize();
        
        logger.info("Configured video processing thread pool: core={}, max={}, queue={}", 
                processingConfig.getCoreThreadPoolSize(),
                processingConfig.getMaxThreadPoolSize(),
                processingConfig.getQueueCapacity());
        
        return executor;
    }

    /**
     * Thread pool executor for scheduled tasks
     */
    @Bean(name = "scheduledTaskExecutor")
    public Executor scheduledTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Smaller pool for scheduled tasks
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("ScheduledTask-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.initialize();
        
        logger.info("Configured scheduled task thread pool: core=2, max=4, queue=20");
        
        return executor;
    }
}
