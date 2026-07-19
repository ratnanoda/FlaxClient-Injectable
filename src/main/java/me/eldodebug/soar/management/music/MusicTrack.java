package me.eldodebug.soar.management.music;

import java.io.File;

public final class MusicTrack {

    private final File file;
    private final String title;
    private volatile long durationMillis;

    MusicTrack(File file) {
        this.file = file;
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        this.title = dot > 0 ? name.substring(0, dot) : name;
    }

    public File getFile() {
        return file;
    }

    public String getTitle() {
        return title;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    void setDurationMillis(long durationMillis) {
        this.durationMillis = Math.max(0L, durationMillis);
    }
}
