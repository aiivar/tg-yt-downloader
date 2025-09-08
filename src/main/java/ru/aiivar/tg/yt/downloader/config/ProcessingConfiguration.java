package ru.aiivar.tg.yt.downloader.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for video processing
 */
@Configuration
@ConfigurationProperties(prefix = "video.processing")
public class ProcessingConfiguration {

    /**
     * Maximum number of videos that can be processed concurrently
     */
    private int maxConcurrentTasks = 1;

    /**
     * Maximum number of threads in the processing thread pool
     */
    private int maxThreadPoolSize = 2;

    /**
     * Core number of threads in the processing thread pool
     */
    private int coreThreadPoolSize = 1;

    /**
     * Queue capacity for pending tasks
     */
    private int queueCapacity = 10;

    /**
     * Enable memory monitoring for adaptive processing
     */
    private boolean enableMemoryMonitoring = true;

    /**
     * Memory threshold percentage (0-100) below which processing is reduced
     */
    private double memoryThresholdPercentage = 80.0;

    /**
     * Minimum free memory in MB required for processing
     */
    private long minFreeMemoryMB = 512;

    /**
     * Maximum file size in MB that can be processed
     */
    private long maxFileSizeMB = 2000;

    /**
     * Enable adaptive processing based on system resources
     */
    private boolean enableAdaptiveProcessing = true;

    /**
     * Processing interval in milliseconds
     */
    private long processingIntervalMs = 30000;

    /**
     * Retry interval in milliseconds
     */
    private long retryIntervalMs = 300000;

    /**
     * Stuck task check interval in milliseconds
     */
    private long stuckTaskCheckIntervalMs = 600000;

    /**
     * Cleanup interval in milliseconds
     */
    private long cleanupIntervalMs = 3600000;

    // Getters and Setters
    public int getMaxConcurrentTasks() {
        return maxConcurrentTasks;
    }

    public void setMaxConcurrentTasks(int maxConcurrentTasks) {
        this.maxConcurrentTasks = maxConcurrentTasks;
    }

    public int getMaxThreadPoolSize() {
        return maxThreadPoolSize;
    }

    public void setMaxThreadPoolSize(int maxThreadPoolSize) {
        this.maxThreadPoolSize = maxThreadPoolSize;
    }

    public int getCoreThreadPoolSize() {
        return coreThreadPoolSize;
    }

    public void setCoreThreadPoolSize(int coreThreadPoolSize) {
        this.coreThreadPoolSize = coreThreadPoolSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public boolean isEnableMemoryMonitoring() {
        return enableMemoryMonitoring;
    }

    public void setEnableMemoryMonitoring(boolean enableMemoryMonitoring) {
        this.enableMemoryMonitoring = enableMemoryMonitoring;
    }

    public double getMemoryThresholdPercentage() {
        return memoryThresholdPercentage;
    }

    public void setMemoryThresholdPercentage(double memoryThresholdPercentage) {
        this.memoryThresholdPercentage = memoryThresholdPercentage;
    }

    public long getMinFreeMemoryMB() {
        return minFreeMemoryMB;
    }

    public void setMinFreeMemoryMB(long minFreeMemoryMB) {
        this.minFreeMemoryMB = minFreeMemoryMB;
    }

    public long getMaxFileSizeMB() {
        return maxFileSizeMB;
    }

    public void setMaxFileSizeMB(long maxFileSizeMB) {
        this.maxFileSizeMB = maxFileSizeMB;
    }

    public boolean isEnableAdaptiveProcessing() {
        return enableAdaptiveProcessing;
    }

    public void setEnableAdaptiveProcessing(boolean enableAdaptiveProcessing) {
        this.enableAdaptiveProcessing = enableAdaptiveProcessing;
    }

    public long getProcessingIntervalMs() {
        return processingIntervalMs;
    }

    public void setProcessingIntervalMs(long processingIntervalMs) {
        this.processingIntervalMs = processingIntervalMs;
    }

    public long getRetryIntervalMs() {
        return retryIntervalMs;
    }

    public void setRetryIntervalMs(long retryIntervalMs) {
        this.retryIntervalMs = retryIntervalMs;
    }

    public long getStuckTaskCheckIntervalMs() {
        return stuckTaskCheckIntervalMs;
    }

    public void setStuckTaskCheckIntervalMs(long stuckTaskCheckIntervalMs) {
        this.stuckTaskCheckIntervalMs = stuckTaskCheckIntervalMs;
    }

    public long getCleanupIntervalMs() {
        return cleanupIntervalMs;
    }

    public void setCleanupIntervalMs(long cleanupIntervalMs) {
        this.cleanupIntervalMs = cleanupIntervalMs;
    }
}
