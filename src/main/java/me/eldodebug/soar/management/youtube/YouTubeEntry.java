package me.eldodebug.soar.management.youtube;

import java.io.File;

public final class YouTubeEntry {

    private final String url;
    private volatile String title;
    private volatile long durationMillis;
    private volatile File mediaFile;

    YouTubeEntry(String url, String title, long durationMillis) {
        this.url = url;
        this.title = title == null || title.trim().isEmpty() ? url : title;
        this.durationMillis = Math.max(0L, durationMillis);
    }

    public String getUrl() { return url; }
    public String getTitle() { return title; }
    public long getDurationMillis() { return durationMillis; }
    public File getMediaFile() { return mediaFile; }
    void setTitle(String title) { if(title != null && !title.trim().isEmpty()) this.title = title.trim(); }
    void setDurationMillis(long durationMillis) { this.durationMillis = Math.max(0L, durationMillis); }
    void setMediaFile(File mediaFile) { this.mediaFile = mediaFile; }
}
