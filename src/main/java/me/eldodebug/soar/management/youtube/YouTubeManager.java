package me.eldodebug.soar.management.youtube;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.logger.GlideLogger;
import me.eldodebug.soar.management.media.MediaToolResolver;

/**
 * Unified YouTube download and playback controller. Every playlist item is
 * cached locally through yt-dlp. FFmpeg then exposes either video + audio for
 * PiP playback or audio only for the compact music player.
 */
public final class YouTubeManager {

    public enum PlaybackMode {
        VIDEO,
        MUSIC
    }

    private final List<YouTubeEntry> playlist = new ArrayList<YouTubeEntry>();
    private final File cacheDirectory;
    private final File playlistFile;
    private final File playbackSettingsFile;
    private final String ytDlpCommand;
    private final String ffmpegCommand;

    private volatile YouTubeEntry current;
    private volatile Process videoProcess;
    private volatile Process audioProcess;
    private volatile SourceDataLine audioLine;
    private volatile byte[] latestFrame;
    private volatile long latestFrameSequence;
    private volatile boolean playing;
    private volatile boolean paused;
    private volatile boolean loading;
    private volatile float volume = 1.0F;
    private volatile long pausedPositionMillis;
    private volatile long playbackStartNanos;
    private volatile int generation;
    private volatile String status = "Paste a YouTube link to download";
    private volatile int qualityHeight = 480;
    private volatile RepeatMode repeatMode = RepeatMode.OFF;
    private volatile PlaybackMode playbackMode = PlaybackMode.VIDEO;

    private enum RepeatMode {
        OFF,
        VIDEO,
        PLAYLIST
    }

    public YouTubeManager() {
        cacheDirectory = new File(Glide.getInstance().getFileManager().getCacheDir(), "youtube");
        if(!cacheDirectory.exists()) cacheDirectory.mkdirs();
        playlistFile = new File(cacheDirectory, "playlist.txt");
        playbackSettingsFile = new File(cacheDirectory, "playback-settings.txt");
        ytDlpCommand = MediaToolResolver.resolve("yt-dlp", "FLAX_YTDLP");
        ffmpegCommand = MediaToolResolver.resolve("ffmpeg", "FLAX_FFMPEG");
        loadPlaybackSettings();
        loadPlaylist();
        verifyToolsAsync();
        refreshBrokenMetadataAsync();
    }

    private void verifyToolsAsync() {
        Thread thread = new Thread(() -> {
            if(!canRun(ytDlpCommand, "--version")) status = "Bundled yt-dlp could not be started";
            else if(!canRun(ffmpegCommand, "-version")) status = "Bundled ffmpeg could not be started";
        }, "Flax-YouTube-Tools");
        thread.setDaemon(true);
        thread.start();
    }

    private boolean canRun(String command, String argument) {
        try {
            Process process = new ProcessBuilder(command, argument).redirectErrorStream(true).start();
            drain(process.getInputStream());
            return process.waitFor() == 0;
        } catch(Exception ignored) {
            return false;
        }
    }

    /** Adds an item and immediately downloads a local copy in the background. */
    public void addUrl(String rawUrl) {
        final String url = rawUrl == null ? "" : rawUrl.trim();
        if(!isSupportedUrl(url)) {
            status = "Enter a valid youtube.com or youtu.be link";
            return;
        }
        synchronized(playlist) {
            for(YouTubeEntry entry : playlist) {
                if(entry.getUrl().equals(url)) {
                    status = entry.getMediaFile() != null ? "That item is already downloaded"
                            : "That item is already in the playlist";
                    return;
                }
            }
            playlist.add(new YouTubeEntry(url, url, 0L));
            savePlaylist();
        }
        status = "Reading video information...";
        Thread thread = new Thread(() -> loadMetadata(url, true), "Flax-YouTube-Metadata");
        thread.setDaemon(true);
        thread.start();
    }

    private boolean isSupportedUrl(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        return (lower.startsWith("https://") || lower.startsWith("http://"))
                && (lower.contains("youtube.com/") || lower.contains("youtu.be/"));
    }

    private void loadMetadata(String url, boolean downloadAfter) {
        YouTubeEntry entry = findByUrl(url);
        if(entry == null) return;
        try {
            Process process = new ProcessBuilder(ytDlpCommand, "--encoding", "utf-8",
                    "--no-playlist", "--no-warnings", "--skip-download",
                    "--print", "%(title)s", "--print", "%(duration)s", url)
                    .redirectError(ProcessBuilder.Redirect.INHERIT).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            String title = reader.readLine();
            String seconds = reader.readLine();
            int exit = process.waitFor();
            if(exit != 0) throw new IllegalStateException("yt-dlp exited with " + exit);
            entry.setTitle(title);
            try { entry.setDurationMillis(Math.round(Double.parseDouble(seconds) * 1000.0)); } catch(Exception ignored) {}
            savePlaylist();
            status = downloadAfter ? "Downloading video..." : "Video information updated";
            if(downloadAfter) prefetch(entry);
        } catch(Exception e) {
            status = "Unable to read that YouTube link";
            entry.setDownloadError("Metadata failed");
            GlideLogger.error("Unable to read YouTube metadata", e);
        }
    }

    private void refreshBrokenMetadataAsync() {
        Thread thread = new Thread(() -> {
            List<YouTubeEntry> snapshot = getPlaylist();
            for(YouTubeEntry entry : snapshot) {
                String title = entry.getTitle();
                if(title == null || title.equals(entry.getUrl()) || title.indexOf('\uFFFD') >= 0) {
                    loadMetadata(entry.getUrl(), false);
                }
            }
        }, "Flax-YouTube-Metadata-Repair");
        thread.setDaemon(true);
        thread.start();
    }

    private void prefetch(final YouTubeEntry entry) {
        File cached = findCachedMedia(entry);
        if(cached != null) {
            entry.setMediaFile(cached);
            status = "Downloaded and ready";
            return;
        }
        Thread thread = new Thread(() -> {
            try {
                File media = downloadMedia(entry);
                if(current == null || current == entry) status = "Downloaded and ready";
                entry.setMediaFile(media);
            } catch(Exception e) {
                entry.setDownloadError("Download failed");
                if(current == null || current == entry) status = "Video download failed; check the link";
                GlideLogger.error("Unable to pre-download YouTube video", e);
            }
        }, "Flax-YouTube-Prefetch");
        thread.setDaemon(true);
        thread.start();
    }

    public void play(YouTubeEntry entry) {
        if(entry == null) return;
        generation++;
        stopDecoders();
        current = entry;
        playing = false;
        paused = false;
        loading = false;
        pausedPositionMillis = 0L;
        latestFrame = null;
        File cached = findCachedMedia(entry);
        if(cached != null) {
            entry.setMediaFile(cached);
            startDecoders(entry, 0L, generation);
            return;
        }
        loading = true;
        status = "Downloading video before playback...";
        final int expectedGeneration = generation;
        Thread thread = new Thread(() -> downloadAndPlay(entry, expectedGeneration), "Flax-YouTube-Download");
        thread.setDaemon(true);
        thread.start();
    }

    private void downloadAndPlay(YouTubeEntry entry, int expectedGeneration) {
        try {
            File media = downloadMedia(entry);
            entry.setMediaFile(media);
            if(expectedGeneration == generation && current == entry) {
                startDecoders(entry, 0L, expectedGeneration);
            }
        } catch(Exception e) {
            entry.setDownloadError("Download failed");
            if(expectedGeneration == generation) {
                loading = false;
                status = "Video download failed; check yt-dlp and the link";
            }
            GlideLogger.error("Unable to download YouTube video", e);
        }
    }

    /** Serialises downloads per entry so prefetch and play never download twice. */
    private File downloadMedia(YouTubeEntry entry) throws Exception {
        synchronized(entry) {
            File cached = findCachedMedia(entry);
            if(cached != null) return cached;

            entry.setDownloading(true);
            entry.setDownloadError(null);
            String output = new File(cacheDirectory, cachePrefix(entry) + ".%(ext)s").getAbsolutePath();
            try {
                Process process = new ProcessBuilder(ytDlpCommand, "--encoding", "utf-8",
                        "--no-playlist", "--no-warnings", "--newline",
                        "-f", "bv*[height<=" + qualityHeight + "]+ba/b[height<=" + qualityHeight + "]/b",
                        "--merge-output-format", "mp4", "--ffmpeg-location", ffmpegCommand,
                        "-o", output, "--print", "after_move:filepath", entry.getUrl())
                        .redirectError(ProcessBuilder.Redirect.INHERIT).start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                String line;
                String lastPath = null;
                while((line = reader.readLine()) != null) {
                    if(!line.trim().isEmpty()) lastPath = line.trim();
                }
                int exit = process.waitFor();
                if(exit != 0 || lastPath == null) throw new IllegalStateException("yt-dlp exited with " + exit);
                File media = new File(lastPath);
                if(!media.isFile()) throw new IllegalStateException("Downloaded file was not found");
                entry.setMediaFile(media);
                return media;
            } finally {
                entry.setDownloading(false);
            }
        }
    }

    private File findCachedMedia(YouTubeEntry entry) {
        String prefix = cachePrefix(entry) + ".";
        if(entry.getMediaFile() != null && entry.getMediaFile().isFile()
                && entry.getMediaFile().getName().startsWith(prefix)) return entry.getMediaFile();
        File[] files = cacheDirectory.listFiles(file -> file.isFile() && file.getName().startsWith(prefix)
                && !file.getName().endsWith(".part") && !file.getName().endsWith(".ytdl"));
        return files == null || files.length == 0 ? null : files[0];
    }

    private void startDecoders(YouTubeEntry entry, long offsetMillis, int expectedGeneration) {
        if(entry.getMediaFile() == null || !entry.getMediaFile().isFile()) return;
        PlaybackMode mode = playbackMode;
        loading = false;
        paused = false;
        playing = true;
        pausedPositionMillis = Math.max(0L, offsetMillis);
        playbackStartNanos = System.nanoTime();
        latestFrame = null;
        status = mode == PlaybackMode.MUSIC ? "Playing as music" : "Playing video in picture-in-picture";
        try {
            String seek = String.format(Locale.ROOT, "%.3f", offsetMillis / 1000.0);
            if(mode == PlaybackMode.VIDEO) {
                int videoWidth = getVideoWidth();
                int videoHeight = getVideoHeight();
                videoProcess = new ProcessBuilder(ffmpegCommand, "-nostdin", "-hide_banner", "-loglevel", "quiet",
                        "-re", "-ss", seek, "-i", entry.getMediaFile().getAbsolutePath(), "-an", "-vf",
                        "fps=60,scale=" + videoWidth + ":" + videoHeight + ":force_original_aspect_ratio=decrease,pad="
                                + videoWidth + ":" + videoHeight + ":(ow-iw)/2:(oh-ih)/2",
                        "-f", "rawvideo", "-pix_fmt", "rgba", "pipe:1")
                        .redirectError(ProcessBuilder.Redirect.INHERIT).start();
                startVideoReader(videoProcess.getInputStream(), expectedGeneration);
            }

            audioProcess = new ProcessBuilder(ffmpegCommand, "-nostdin", "-hide_banner", "-loglevel", "quiet",
                    "-ss", seek, "-i", entry.getMediaFile().getAbsolutePath(), "-vn", "-f", "s16le",
                    "-acodec", "pcm_s16le", "-ar", "44100", "-ac", "2", "pipe:1")
                    .redirectError(ProcessBuilder.Redirect.INHERIT).start();
            startAudioReader(audioProcess.getInputStream(), expectedGeneration, entry);
        } catch(Exception e) {
            playing = false;
            status = "Unable to start bundled ffmpeg playback";
            GlideLogger.error("Unable to start YouTube playback", e);
        }
    }

    private void startVideoReader(final InputStream input, final int expectedGeneration) {
        Thread thread = new Thread(() -> {
            int frameBytes = getVideoWidth() * getVideoHeight() * 4;
            byte[][] frames = { new byte[frameBytes], new byte[frameBytes], new byte[frameBytes] };
            int index = 0;
            try(BufferedInputStream stream = new BufferedInputStream(input, frameBytes)) {
                while(expectedGeneration == generation && readFully(stream, frames[index])) {
                    latestFrame = frames[index];
                    latestFrameSequence++;
                    index = (index + 1) % frames.length;
                }
            } catch(Exception ignored) {}
        }, "Flax-YouTube-Video");
        thread.setDaemon(true);
        thread.start();
    }

    private void startAudioReader(final InputStream input, final int expectedGeneration, final YouTubeEntry entry) {
        Thread thread = new Thread(() -> {
            SourceDataLine line = null;
            try(BufferedInputStream stream = new BufferedInputStream(input, 32768)) {
                AudioFormat format = new AudioFormat(44100.0F, 16, 2, true, false);
                line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format));
                line.open(format, 32768);
                line.start();
                audioLine = line;
                byte[] inputBuffer = new byte[8192];
                byte[] outputBuffer = new byte[8192];
                int read;
                while(expectedGeneration == generation && (read = stream.read(inputBuffer)) >= 0) {
                    amplifyPcm(inputBuffer, outputBuffer, read, volume);
                    line.write(outputBuffer, 0, read);
                }
                if(expectedGeneration == generation) line.drain();
            } catch(Exception e) {
                if(expectedGeneration == generation) GlideLogger.error("YouTube audio playback failed", e);
            } finally {
                if(line != null) {
                    try { line.stop(); } catch(Exception ignored) {}
                    try { line.close(); } catch(Exception ignored) {}
                }
                if(audioLine == line) audioLine = null;
            }
            if(expectedGeneration == generation && playing && current == entry) playNextAfter(entry);
        }, "Flax-YouTube-Audio");
        thread.setDaemon(true);
        thread.start();
    }

    private void amplifyPcm(byte[] input, byte[] output, int length, float gain) {
        int evenLength = length - (length & 1);
        for(int i = 0; i < evenLength; i += 2) {
            short sample = (short) ((input[i] & 0xFF) | (input[i + 1] << 8));
            int amplified = Math.round(sample * gain);
            if(amplified > Short.MAX_VALUE) amplified = Short.MAX_VALUE;
            else if(amplified < Short.MIN_VALUE) amplified = Short.MIN_VALUE;
            output[i] = (byte) amplified;
            output[i + 1] = (byte) (amplified >>> 8);
        }
        if(evenLength < length) output[evenLength] = input[evenLength];
    }

    private boolean readFully(InputStream stream, byte[] target) throws Exception {
        int offset = 0;
        while(offset < target.length) {
            int read = stream.read(target, offset, target.length - offset);
            if(read < 0) return false;
            offset += read;
        }
        return true;
    }

    public void togglePause() {
        if(current == null) {
            List<YouTubeEntry> entries = getPlaylist();
            if(!entries.isEmpty()) play(entries.get(0));
            return;
        }
        if(loading) return;
        if(playing) {
            pausedPositionMillis = getPositionMillis();
            generation++;
            playing = false;
            paused = true;
            stopDecoders();
            status = "Paused";
        } else if(paused) {
            int nextGeneration = ++generation;
            stopDecoders();
            startDecoders(current, pausedPositionMillis, nextGeneration);
        }
    }

    public void seekToFraction(float fraction) {
        YouTubeEntry entry = current;
        if(entry == null || entry.getDurationMillis() <= 0L || entry.getMediaFile() == null) return;
        boolean remainPaused = paused;
        long target = (long) (Math.max(0.0F, Math.min(1.0F, fraction)) * entry.getDurationMillis());
        int nextGeneration = ++generation;
        stopDecoders();
        pausedPositionMillis = target;
        if(remainPaused) {
            paused = true;
            playing = false;
            status = "Paused";
        } else {
            startDecoders(entry, target, nextGeneration);
        }
    }

    public void setPlaybackMode(PlaybackMode mode) {
        if(mode == null || mode == playbackMode) return;
        playbackMode = mode;
        savePlaybackSettings();
        latestFrame = null;

        YouTubeEntry entry = current;
        if(entry == null) {
            status = mode == PlaybackMode.MUSIC ? "Music mode selected" : "Video mode selected";
            return;
        }
        if(loading) {
            status = mode == PlaybackMode.MUSIC
                    ? "Downloading video; music mode will start when ready"
                    : "Downloading video; PiP will start when ready";
            return;
        }

        long position = getPositionMillis();
        boolean remainPaused = paused;
        int nextGeneration = ++generation;
        stopDecoders();
        pausedPositionMillis = position;
        if(remainPaused) {
            playing = false;
            paused = true;
            status = mode == PlaybackMode.MUSIC ? "Paused in music mode" : "Paused in video mode";
        } else if(entry.getMediaFile() != null && entry.getMediaFile().isFile()) {
            startDecoders(entry, position, nextGeneration);
        }
    }

    public void togglePlaybackMode() {
        setPlaybackMode(playbackMode == PlaybackMode.VIDEO ? PlaybackMode.MUSIC : PlaybackMode.VIDEO);
    }

    public void stop() {
        generation++;
        stopDecoders();
        playing = false;
        paused = false;
        loading = false;
        current = null;
        latestFrame = null;
        pausedPositionMillis = 0L;
        status = "Stopped";
    }

    private void stopDecoders() {
        Process video = videoProcess;
        Process audio = audioProcess;
        videoProcess = null;
        audioProcess = null;
        if(video != null) try { video.destroyForcibly(); } catch(Exception ignored) {}
        if(audio != null) try { audio.destroyForcibly(); } catch(Exception ignored) {}
        SourceDataLine line = audioLine;
        audioLine = null;
        if(line != null) {
            try { line.stop(); } catch(Exception ignored) {}
            try { line.flush(); } catch(Exception ignored) {}
            try { line.close(); } catch(Exception ignored) {}
        }
    }

    public void playNext() { playRelative(1); }
    public void playPrevious() { playRelative(-1); }

    private void playRelative(int direction) {
        List<YouTubeEntry> entries = getPlaylist();
        if(entries.isEmpty()) return;
        int index = entries.indexOf(current);
        if(index < 0) index = direction > 0 ? -1 : 0;
        play(entries.get((index + direction + entries.size()) % entries.size()));
    }

    private void playNextAfter(YouTubeEntry finished) {
        RepeatMode mode = repeatMode;
        if(mode == RepeatMode.VIDEO) {
            play(finished);
            return;
        }

        List<YouTubeEntry> entries = getPlaylist();
        int index = entries.indexOf(finished);
        if(index >= 0 && index + 1 < entries.size()) {
            play(entries.get(index + 1));
        } else if(mode == RepeatMode.PLAYLIST && !entries.isEmpty()) {
            play(entries.get(0));
        } else {
            stop();
        }
    }

    public void remove(YouTubeEntry entry) {
        if(entry == null) return;
        if(entry == current) stop();
        synchronized(playlist) {
            playlist.remove(entry);
            savePlaylist();
        }
        status = "Removed from playlist";
    }

    private YouTubeEntry findByUrl(String url) {
        synchronized(playlist) {
            for(YouTubeEntry entry : playlist) if(entry.getUrl().equals(url)) return entry;
        }
        return null;
    }

    private void loadPlaylist() {
        if(!playlistFile.isFile()) return;
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(playlistFile), StandardCharsets.UTF_8))) {
            String line;
            while((line = reader.readLine()) != null) {
                String[] parts = line.split("\\t", -1);
                if(parts.length < 3) continue;
                String url = decode(parts[0]);
                String title = decode(parts[1]);
                long duration = 0L;
                try { duration = Long.parseLong(parts[2]); } catch(Exception ignored) {}
                if(isSupportedUrl(url)) {
                    YouTubeEntry entry = new YouTubeEntry(url, title, duration);
                    File cached = findCachedMedia(entry);
                    if(cached != null) entry.setMediaFile(cached);
                    playlist.add(entry);
                }
            }
        } catch(Exception e) {
            GlideLogger.error("Unable to load YouTube playlist", e);
        }
    }

    private void savePlaylist() {
        synchronized(playlist) {
            try(PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(playlistFile), StandardCharsets.UTF_8))) {
                for(YouTubeEntry entry : playlist) {
                    writer.println(encode(entry.getUrl()) + "\t" + encode(entry.getTitle()) + "\t" + entry.getDurationMillis());
                }
            } catch(Exception e) {
                GlideLogger.error("Unable to save YouTube playlist", e);
            }
        }
    }

    private void loadPlaybackSettings() {
        if(!playbackSettingsFile.isFile()) return;
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(playbackSettingsFile), StandardCharsets.UTF_8))) {
            String repeat = reader.readLine();
            String mode = reader.readLine();
            if(repeat != null) repeatMode = RepeatMode.valueOf(repeat.trim().toUpperCase(Locale.ROOT));
            if(mode != null) playbackMode = PlaybackMode.valueOf(mode.trim().toUpperCase(Locale.ROOT));
        } catch(Exception e) {
            repeatMode = RepeatMode.OFF;
            playbackMode = PlaybackMode.VIDEO;
            GlideLogger.error("Unable to load YouTube playback settings", e);
        }
    }

    private void savePlaybackSettings() {
        try(PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(playbackSettingsFile), StandardCharsets.UTF_8))) {
            writer.println(repeatMode.name());
            writer.println(playbackMode.name());
        } catch(Exception e) {
            GlideLogger.error("Unable to save YouTube playback settings", e);
        }
    }

    public boolean isVideoLoopEnabled() { return repeatMode == RepeatMode.VIDEO; }
    public boolean isPlaylistLoopEnabled() { return repeatMode == RepeatMode.PLAYLIST; }

    public void toggleVideoLoop() {
        repeatMode = repeatMode == RepeatMode.VIDEO ? RepeatMode.OFF : RepeatMode.VIDEO;
        savePlaybackSettings();
        status = repeatMode == RepeatMode.VIDEO ? "Item loop enabled" : "Item loop disabled";
    }

    public void togglePlaylistLoop() {
        repeatMode = repeatMode == RepeatMode.PLAYLIST ? RepeatMode.OFF : RepeatMode.PLAYLIST;
        savePlaybackSettings();
        status = repeatMode == RepeatMode.PLAYLIST ? "Playlist loop enabled" : "Playlist loop disabled";
    }

    private String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private void drain(InputStream input) {
        try { while(input.read() >= 0) {} } catch(Exception ignored) {}
    }

    public List<YouTubeEntry> getPlaylist() {
        synchronized(playlist) {
            return Collections.unmodifiableList(new ArrayList<YouTubeEntry>(playlist));
        }
    }

    public YouTubeEntry getCurrent() { return current; }
    public byte[] getLatestFrame() { return latestFrame; }
    public long getLatestFrameSequence() { return latestFrameSequence; }
    public boolean isPlaying() { return playing; }
    public boolean isPaused() { return paused; }
    public boolean isLoading() { return loading; }
    public PlaybackMode getPlaybackMode() { return playbackMode; }
    public boolean isVideoMode() { return playbackMode == PlaybackMode.VIDEO; }
    public boolean isMusicMode() { return playbackMode == PlaybackMode.MUSIC; }
    public boolean isPipVisible() {
        return playbackMode == PlaybackMode.VIDEO && current != null && latestFrame != null && (playing || paused);
    }
    public boolean isMusicHudVisible() {
        return playbackMode == PlaybackMode.MUSIC && current != null && (playing || paused || loading);
    }
    public boolean isMusicPlaybackActive() {
        return playbackMode == PlaybackMode.MUSIC && current != null && (playing || paused || loading);
    }
    public float getVolume() { return volume; }
    public void setVolume(float volume) { this.volume = Math.max(0.0F, Math.min(2.0F, volume)); }
    public long getPositionMillis() {
        if(playing) {
            long position = pausedPositionMillis + Math.max(0L, (System.nanoTime() - playbackStartNanos) / 1_000_000L);
            return getDurationMillis() > 0L ? Math.min(position, getDurationMillis()) : position;
        }
        return pausedPositionMillis;
    }
    public long getDurationMillis() { return current == null ? 0L : current.getDurationMillis(); }
    public String getStatus() { return status; }
    public int getQualityHeight() { return qualityHeight; }
    public int getVideoHeight() { return qualityHeight; }
    public int getVideoWidth() { return qualityHeight == 480 ? 854 : qualityHeight * 16 / 9; }

    public void setQualityHeight(int qualityHeight) {
        int normalized = qualityHeight <= 360 ? 360 : qualityHeight >= 720 ? 720 : 480;
        if(this.qualityHeight == normalized) return;
        this.qualityHeight = normalized;
        status = "Quality set to " + normalized + "p";
        YouTubeEntry entry = current;
        if(playbackMode == PlaybackMode.VIDEO && entry != null && (playing || paused || loading)) play(entry);
    }

    private String cachePrefix(YouTubeEntry entry) {
        return Integer.toHexString(entry.getUrl().hashCode()) + "-" + qualityHeight;
    }

    public void shutdown() { stop(); }
}
