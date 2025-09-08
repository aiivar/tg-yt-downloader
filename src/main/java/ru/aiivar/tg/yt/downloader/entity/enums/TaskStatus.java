package ru.aiivar.tg.yt.downloader.entity.enums;

/**
 * Enum representing the status of a download task
 */
public enum TaskStatus {
    PENDING,        // Task is created and waiting to be processed
    PROCESSING,     // Task is currently being processed
    COMPLETED,      // Task completed successfully
    FAILED,         // Task failed with an error
    CANCELLED       // Task was cancelled
}
