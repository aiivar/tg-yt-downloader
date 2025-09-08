package ru.aiivar.tg.yt.downloader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.aiivar.tg.yt.downloader.config.ProcessingConfiguration;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * Service for monitoring system memory and providing adaptive processing recommendations
 */
@Service
public class MemoryMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(MemoryMonitoringService.class);

    @Autowired
    private ProcessingConfiguration processingConfig;

    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

    /**
     * Get current memory usage statistics
     */
    public MemoryStats getMemoryStats() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long maxMemory = Runtime.getRuntime().maxMemory();
        long usedMemory = totalMemory - freeMemory;
        
        double usedPercentage = (double) usedMemory / maxMemory * 100;
        double freePercentage = (double) freeMemory / maxMemory * 100;
        
        return new MemoryStats(
                totalMemory,
                freeMemory,
                usedMemory,
                maxMemory,
                usedPercentage,
                freePercentage,
                heapUsage.getUsed(),
                heapUsage.getMax(),
                nonHeapUsage.getUsed(),
                nonHeapUsage.getMax()
        );
    }

    /**
     * Check if system has enough memory for processing
     */
    public boolean hasEnoughMemory() {
        if (!processingConfig.isEnableMemoryMonitoring()) {
            return true;
        }

        MemoryStats stats = getMemoryStats();
        
        // In containerized environments, use max memory instead of free memory for better accuracy
        long availableMemoryMB = stats.getMaxMemory() / (1024 * 1024);
        long usedMemoryMB = stats.getUsedMemory() / (1024 * 1024);
        long freeMemoryMB = availableMemoryMB - usedMemoryMB;
        
        // Check if free memory is above minimum threshold
        if (freeMemoryMB < processingConfig.getMinFreeMemoryMB()) {
            logger.warn("Insufficient free memory: {} MB (minimum required: {} MB, max: {} MB, used: {} MB)", 
                    freeMemoryMB, processingConfig.getMinFreeMemoryMB(), availableMemoryMB, usedMemoryMB);
            return false;
        }

        // Check if memory usage is below threshold percentage
        if (stats.getUsedPercentage() > processingConfig.getMemoryThresholdPercentage()) {
            logger.warn("Memory usage too high: {}% (threshold: {}%)", 
                    stats.getUsedPercentage(), processingConfig.getMemoryThresholdPercentage());
            return false;
        }

        logger.debug("Memory check passed: {} MB free, {}% used", freeMemoryMB, stats.getUsedPercentage());
        return true;
    }

    /**
     * Get recommended number of concurrent tasks based on memory
     */
    public int getRecommendedConcurrentTasks() {
        if (!processingConfig.isEnableAdaptiveProcessing()) {
            return processingConfig.getMaxConcurrentTasks();
        }

        MemoryStats stats = getMemoryStats();
        int maxTasks = processingConfig.getMaxConcurrentTasks();
        
        // Calculate based on available memory (use max memory for containerized environments)
        long availableMemoryMB = stats.getMaxMemory() / (1024 * 1024);
        long usedMemoryMB = stats.getUsedMemory() / (1024 * 1024);
        long freeMemoryMB = availableMemoryMB - usedMemoryMB;
        long estimatedMemoryPerTaskMB = 300; // Conservative estimate for video processing
        
        int memoryBasedTasks = (int) (freeMemoryMB / estimatedMemoryPerTaskMB);
        
        // Use the smaller of the two values
        int recommendedTasks = Math.min(maxTasks, memoryBasedTasks);
        
        // Ensure at least 1 task if memory allows
        if (recommendedTasks < 1 && hasEnoughMemory()) {
            recommendedTasks = 1;
        }
        
        logger.debug("Memory-based task recommendation: {} (max: {}, memory-based: {}, free: {} MB)", 
                recommendedTasks, maxTasks, memoryBasedTasks, freeMemoryMB);
        
        return recommendedTasks;
    }

    /**
     * Check if a file size is acceptable for processing
     */
    public boolean isFileSizeAcceptable(long fileSizeBytes) {
        long maxFileSizeBytes = processingConfig.getMaxFileSizeMB() * 1024 * 1024;
        return fileSizeBytes <= maxFileSizeBytes;
    }

    /**
     * Get memory pressure level
     */
    public MemoryPressureLevel getMemoryPressureLevel() {
        MemoryStats stats = getMemoryStats();
        
        if (stats.getUsedPercentage() >= 90) {
            return MemoryPressureLevel.CRITICAL;
        } else if (stats.getUsedPercentage() >= processingConfig.getMemoryThresholdPercentage()) {
            return MemoryPressureLevel.HIGH;
        } else if (stats.getUsedPercentage() >= 60) {
            return MemoryPressureLevel.MEDIUM;
        } else {
            return MemoryPressureLevel.LOW;
        }
    }

    /**
     * Force garbage collection if memory pressure is high
     */
    public void performGarbageCollectionIfNeeded() {
        MemoryPressureLevel pressure = getMemoryPressureLevel();
        
        if (pressure == MemoryPressureLevel.CRITICAL || pressure == MemoryPressureLevel.HIGH) {
            logger.info("High memory pressure detected ({}), performing garbage collection", pressure);
            System.gc();
            
            // Wait a bit for GC to complete
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Log memory stats after GC
            MemoryStats statsAfterGC = getMemoryStats();
            logger.info("Memory after GC: {} MB free, {}% used", 
                    (statsAfterGC.getMaxMemory() - statsAfterGC.getUsedMemory()) / (1024 * 1024),
                    statsAfterGC.getUsedPercentage());
        }
    }

    /**
     * Temporarily disable memory monitoring for emergency processing
     * This should only be used in extreme cases where tasks are stuck
     */
    public void temporarilyDisableMemoryMonitoring(int durationMinutes) {
        logger.warn("Temporarily disabling memory monitoring for {} minutes", durationMinutes);
        processingConfig.setEnableMemoryMonitoring(false);
        
        // Schedule re-enabling of memory monitoring
        new Thread(() -> {
            try {
                Thread.sleep(durationMinutes * 60 * 1000);
                processingConfig.setEnableMemoryMonitoring(true);
                logger.info("Re-enabled memory monitoring after {} minutes", durationMinutes);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                processingConfig.setEnableMemoryMonitoring(true);
                logger.info("Re-enabled memory monitoring due to interruption");
            }
        }).start();
    }

    /**
     * Memory statistics inner class
     */
    public static class MemoryStats {
        private final long totalMemory;
        private final long freeMemory;
        private final long usedMemory;
        private final long maxMemory;
        private final double usedPercentage;
        private final double freePercentage;
        private final long heapUsed;
        private final long heapMax;
        private final long nonHeapUsed;
        private final long nonHeapMax;

        public MemoryStats(long totalMemory, long freeMemory, long usedMemory, long maxMemory,
                          double usedPercentage, double freePercentage,
                          long heapUsed, long heapMax, long nonHeapUsed, long nonHeapMax) {
            this.totalMemory = totalMemory;
            this.freeMemory = freeMemory;
            this.usedMemory = usedMemory;
            this.maxMemory = maxMemory;
            this.usedPercentage = usedPercentage;
            this.freePercentage = freePercentage;
            this.heapUsed = heapUsed;
            this.heapMax = heapMax;
            this.nonHeapUsed = nonHeapUsed;
            this.nonHeapMax = nonHeapMax;
        }

        // Getters
        public long getTotalMemory() { return totalMemory; }
        public long getFreeMemory() { return freeMemory; }
        public long getUsedMemory() { return usedMemory; }
        public long getMaxMemory() { return maxMemory; }
        public double getUsedPercentage() { return usedPercentage; }
        public double getFreePercentage() { return freePercentage; }
        public long getHeapUsed() { return heapUsed; }
        public long getHeapMax() { return heapMax; }
        public long getNonHeapUsed() { return nonHeapUsed; }
        public long getNonHeapMax() { return nonHeapMax; }

        public String getFormattedTotalMemory() {
            return formatBytes(totalMemory);
        }

        public String getFormattedFreeMemory() {
            return formatBytes(freeMemory);
        }

        public String getFormattedUsedMemory() {
            return formatBytes(usedMemory);
        }

        public String getFormattedMaxMemory() {
            return formatBytes(maxMemory);
        }

        private String formatBytes(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * Memory pressure levels
     */
    public enum MemoryPressureLevel {
        LOW,    // < 60% memory usage
        MEDIUM, // 60-80% memory usage
        HIGH,   // 80-90% memory usage
        CRITICAL // > 90% memory usage
    }
}
