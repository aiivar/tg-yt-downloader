package ru.aiivar.tg.yt.downloader.entity.enums;

/**
 * Enum representing different destination types for downloaded videos
 */
public enum DestinationType {
    TELEGRAM("Telegram", "tg")

    private final String displayName;
    private final String code;

    DestinationType(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return code;
    }
}
