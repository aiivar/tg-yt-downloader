# Memory-Aware Video Processing Configuration

This document explains how to configure and use the memory-aware video processing system that allows you to control how many videos are processed simultaneously based on your server's memory capacity.

## Overview

The system now includes:
- **Configurable concurrent processing limits** - Control how many videos process at once
- **Memory monitoring** - Automatically checks available memory before processing
- **Adaptive processing** - Adjusts processing based on system resources
- **Runtime configuration updates** - Change settings without restarting
- **Thread pool management** - Optimized thread pools for different workloads

## Configuration

### Basic Settings

Add these properties to your `application.properties` or `application.yml`:

```properties
# Maximum number of videos that can be processed concurrently
# Start with 1 for low-memory servers, increase as you get more memory
video.processing.max-concurrent-tasks=1

# Thread pool configuration
video.processing.max-thread-pool-size=2
video.processing.core-thread-pool-size=1
video.processing.queue-capacity=10

# Memory monitoring
video.processing.enable-memory-monitoring=true
video.processing.memory-threshold-percentage=80.0
video.processing.min-free-memory-mb=512

# Maximum file size that can be processed (in MB)
video.processing.max-file-size-mb=2000

# Enable adaptive processing
video.processing.enable-adaptive-processing=true
```

### Memory-Based Recommendations

| Server Memory | Recommended max-concurrent-tasks | Notes |
|---------------|----------------------------------|-------|
| 1-2 GB        | 1                               | Very conservative, good for small VPS |
| 4 GB          | 2-3                             | Can handle 2-3 videos simultaneously |
| 8 GB          | 4-6                             | Good for medium workloads |
| 16 GB+        | 8+                              | Can handle high concurrent loads |

### Processing Intervals

```properties
# How often to check for pending tasks (milliseconds)
video.processing.processing-interval-ms=30000

# How often to retry failed tasks (milliseconds)
video.processing.retry-interval-ms=300000

# How often to check for stuck tasks (milliseconds)
video.processing.stuck-task-check-interval-ms=600000

# How often to clean up old tasks (milliseconds)
video.processing.cleanup-interval-ms=3600000
```

## Memory Monitoring

The system continuously monitors memory usage and makes intelligent decisions:

### Memory Pressure Levels
- **LOW** (< 60% usage): Normal processing
- **MEDIUM** (60-80% usage): Slightly reduced processing
- **HIGH** (80-90% usage): Significantly reduced processing
- **CRITICAL** (> 90% usage): Minimal processing, garbage collection

### Automatic Adjustments
- **Memory checks** before each task starts
- **Garbage collection** when memory pressure is high
- **Processing limits** adjusted based on available memory
- **Task queuing** when insufficient memory

## API Endpoints

### Get Processing Status
```http
GET /api/v1/tasks/processing/status
```

Response:
```json
{
  "maxConcurrentTasks": 1,
  "availableSlots": 1,
  "currentlyProcessing": 0,
  "memoryPressure": "LOW",
  "hasEnoughMemory": true
}
```

### Get Execution Statistics
```http
GET /api/v1/tasks/processing/statistics
```

Response:
```json
{
  "totalTasks": 150,
  "pendingTasks": 5,
  "processingTasks": 1,
  "completedTasks": 140,
  "failedTasks": 4,
  "retryableTasks": 2,
  "maxConcurrentTasks": 1,
  "availableSlots": 1,
  "memoryUsagePercentage": 45.2,
  "freeMemoryMB": 1024
}
```

### Update Processing Configuration
```http
PUT /api/v1/tasks/processing/config?maxConcurrentTasks=2
```

## Runtime Configuration Updates

You can change the processing limits without restarting the application:

```bash
# Increase concurrent processing to 2 videos
curl -X PUT "http://localhost:8080/api/v1/tasks/processing/config?maxConcurrentTasks=2"

# Check the new status
curl "http://localhost:8080/api/v1/tasks/processing/status"
```

## Monitoring and Logging

### Key Log Messages

```
INFO  - Found 3 pending tasks, processing 1 (memory-based limit: 1)
WARN  - Insufficient memory for processing tasks, skipping this cycle
INFO  - Acquired processing slot for task: abc123 (available slots: 0)
DEBUG - Released processing slot for task: abc123 (available slots: 1)
```

### Memory Monitoring Logs

```
INFO  - Memory usage: 45.2% (Free: 1024 MB)
WARN  - High memory pressure detected (HIGH), performing garbage collection
INFO  - Memory-based task recommendation: 1 (max: 2, memory-based: 1)
```

## Best Practices

### For Low-Memory Servers (1-2 GB)
```properties
video.processing.max-concurrent-tasks=1
video.processing.memory-threshold-percentage=70.0
video.processing.min-free-memory-mb=256
video.processing.max-file-size-mb=1000
```

### For Medium Servers (4-8 GB)
```properties
video.processing.max-concurrent-tasks=3
video.processing.memory-threshold-percentage=80.0
video.processing.min-free-memory-mb=512
video.processing.max-file-size-mb=2000
```

### For High-Memory Servers (16+ GB)
```properties
video.processing.max-concurrent-tasks=8
video.processing.memory-threshold-percentage=85.0
video.processing.min-free-memory-mb=1024
video.processing.max-file-size-mb=5000
```

## Troubleshooting

### Common Issues

1. **Tasks not processing**
   - Check memory availability: `GET /api/v1/tasks/processing/status`
   - Verify `max-concurrent-tasks` is not 0
   - Check if memory threshold is too low

2. **Out of memory errors**
   - Reduce `max-concurrent-tasks`
   - Increase `min-free-memory-mb`
   - Lower `memory-threshold-percentage`

3. **Slow processing**
   - Increase `max-concurrent-tasks` if memory allows
   - Check memory pressure level
   - Consider increasing server memory

### Performance Tuning

1. **Monitor memory usage patterns**
2. **Adjust thresholds based on your workload**
3. **Use the statistics endpoint to track performance**
4. **Consider file size limits for your use case**

## Migration from Existing System

The new system is backward compatible. To migrate:

1. **Add configuration properties** to your existing setup
2. **Start with conservative settings** (max-concurrent-tasks=1)
3. **Monitor performance** and gradually increase limits
4. **Use the API endpoints** to fine-tune settings

## Example Configuration Files

### application.properties (Low Memory)
```properties
# Conservative settings for 1-2 GB server
video.processing.max-concurrent-tasks=1
video.processing.memory-threshold-percentage=70.0
video.processing.min-free-memory-mb=256
video.processing.max-file-size-mb=1000
video.processing.enable-memory-monitoring=true
video.processing.enable-adaptive-processing=true
```

### application.properties (High Memory)
```properties
# Aggressive settings for 16+ GB server
video.processing.max-concurrent-tasks=8
video.processing.memory-threshold-percentage=85.0
video.processing.min-free-memory-mb=2048
video.processing.max-file-size-mb=5000
video.processing.enable-memory-monitoring=true
video.processing.enable-adaptive-processing=true
```

This system ensures your video processing is always within your server's memory limits while providing the flexibility to scale up as you add more memory.
