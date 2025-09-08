package ru.aiivar.tg.yt.downloader.entity.enums;

/**
 * Enum representing different video source types
 */
public enum SourceType {
    YOUTUBE("YouTube", "youtube.com", "youtu.be")

    private final String displayName;
    private final String[] domains;

    SourceType(String displayName, String... domains) {
        this.displayName = displayName;
        this.domains = domains;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String[] getDomains() {
        return domains;
    }

    /**
     * Determines the source type from a URL
     */
    public static SourceType fromUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return CUSTOM;
        }

        String lowerUrl = url.toLowerCase();
        for (SourceType type : values()) {
            if (type == CUSTOM) continue;
            for (String domain : type.domains) {
                if (lowerUrl.contains(domain.toLowerCase())) {
                    return type;
                }
            }
        }
        return CUSTOM;
    }
}
