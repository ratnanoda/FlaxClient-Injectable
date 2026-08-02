package me.eldodebug.soar.management.music;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import me.eldodebug.soar.logger.GlideLogger;
import me.eldodebug.soar.management.media.MediaToolResolver;
import net.minecraft.client.Minecraft;

/** Lightweight, local MP3 player used by the ClickGUI Music page. */
public final class MusicManager {

    private final Object stateLock = new Object();
    private volatile List<MusicTrack> tracks = Collections.emptyList();
    private volatile MusicTrack currentTrack;
    private volatile SourceDataLine activeLine;
    private volatile Process playbackProcess;
    private volatile boolean enabled = true;
    private volatile boolean playing;
    private volatile boolean paused;
    private volatile boolean trackLoop;
    private volatile boolean playlistLoop;
    private volatile float volume = 1.0F;
    private volatile long positionMillis;
    private volatile int playbackGeneration;
    private File musicDirectory;
    private final String ffmpegCommand;
    private final String ffprobeCommand;

    public MusicManager() {
        ffmpegCommand = MediaToolResolver.resolve("ffmpeg", "FLAX_FFMPEG");
        ffprobeCommand = MediaToolResolver.resolve("ffprobe", "FLAX_FFPROBE");
        installBundledTracks();
        refreshTracks();
    }

    private void installBundledTracks() {
        File directory = resolveMusicDirectory();
        if(!directory.exists()) directory.mkdirs();
        try(InputStream resource = MusicManager.class.getClassLoader().getResourceAsStream(
                "assets/minecraft/soar/music/default-musics.zip")) {
            if(resource == null) return;
            try(ZipInputStream zip = new ZipInputStream(new BufferedInputStream(resource))) {
                ZipEntry entry;
                byte[] buffer = new byte[32 * 1024];
                while((entry = zip.getNextEntry()) != null) {
                    if(entry.isDirectory()) continue;
                    String fileName = new File(entry.getName()).getName();
                    if(!fileName.toLowerCase(Locale.ROOT).endsWith(".mp3")) continue;
                    File destination = new File(directory, fileName);
                    if(destination.isFile() && destination.length() > 0L) continue;
                    try(FileOutputStream output = new FileOutputStream(destination)) {
                        int read;
                        while((read = zip.read(buffer)) != -1) {
                            if(read > 0) output.write(buffer, 0, read);
                        }
                    }
                }
            }
        } catch(Exception e) {
            GlideLogger.error("Unable to install bundled music", e);
        }
    }

    public void refreshTracks() {
        File directory = resolveMusicDirectory();
        if(!directory.exists()) directory.mkdirs();
        musicDirectory = directory;

        File[] files = directory.listFiles(file -> file.isFile()
                && file.getName().toLowerCase(Locale.ROOT).endsWith(".mp3"));
        if(files == null) files = new File[0];
        Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));

        MusicTrack active = currentTrack;
        List<MusicTrack> updated = new ArrayList<MusicTrack>();
        for(File file : files) {
            updated.add(active != null && active.getFile().equals(file) ? active : new MusicTrack(file));
        }
        tracks = Collections.unmodifiableList(updated);

        Thread metadataThread = new Thread(() -> {
            for(MusicTrack track : updated) {
                track.setDurationMillis(readDuration(track.getFile()));
            }
        }, "Flax-Music-Metadata");
        metadataThread.setDaemon(true);
        metadataThread.start();
    }

    private File resolveMusicDirectory() {
        String configured = System.getenv("FLAX_MUSIC_DIR");
        if(configured != null && !configured.trim().isEmpty()) return new File(configured.trim());

        return new File(Minecraft.getMinecraft().mcDataDir, "Musics");
    }

    public void play(MusicTrack track) {
        if(track == null || !track.getFile().isFile()) return;
        if(!MediaToolResolver.canRun(ffmpegCommand, "-version")) {
            playing = false;
            GlideLogger.warn("Music playback requires the packaged ffmpeg.exe");
            return;
        }
        enabled = true;
        currentTrack = track;
        paused = false;
        startPlayback(track, 0L);
    }

    public void togglePause() {
        if(currentTrack == null) {
            if(!tracks.isEmpty()) play(tracks.get(0));
            return;
        }
        paused = !paused;
        SourceDataLine line = activeLine;
        if(line != null) {
            if(paused) line.stop();
            else line.start();
        }
        synchronized(stateLock) {
            stateLock.notifyAll();
        }
    }

    public void stop() {
        stopInternal(true);
    }

    public void disable() {
        enabled = false;
        stopInternal(true);
    }

    private void stopInternal(boolean clearTrack) {
        playbackGeneration++;
        playing = false;
        paused = false;
        positionMillis = 0L;
        if(clearTrack) currentTrack = null;
        SourceDataLine line = activeLine;
        activeLine = null;
        Process process = playbackProcess;
        playbackProcess = null;
        if(process != null) {
            try { process.destroyForcibly(); } catch(Exception ignored) {}
        }
        if(line != null) {
            try { line.stop(); } catch(Exception ignored) {}
            try { line.flush(); } catch(Exception ignored) {}
            try { line.close(); } catch(Exception ignored) {}
        }
        synchronized(stateLock) {
            stateLock.notifyAll();
        }
    }

    public void seekToFraction(float fraction) {
        MusicTrack track = currentTrack;
        if(track == null) return;
        float clamped = Math.max(0.0F, Math.min(1.0F, fraction));
        long duration = track.getDurationMillis();
        if(duration <= 0L) return;
        startPlayback(track, (long) (duration * clamped));
    }

    public void playNext() {
        playRelative(1);
    }

    public void playPrevious() {
        playRelative(-1);
    }

    private void playRelative(int direction) {
        List<MusicTrack> snapshot = tracks;
        if(snapshot.isEmpty()) return;
        int index = snapshot.indexOf(currentTrack);
        if(index < 0) index = direction > 0 ? -1 : 0;
        int next = (index + direction + snapshot.size()) % snapshot.size();
        play(snapshot.get(next));
    }

    private void startPlayback(MusicTrack track, long startMillis) {
        int generation = ++playbackGeneration;
        SourceDataLine oldLine = activeLine;
        activeLine = null;
        Process oldProcess = playbackProcess;
        playbackProcess = null;
        if(oldProcess != null) {
            try { oldProcess.destroyForcibly(); } catch(Exception ignored) {}
        }
        if(oldLine != null) {
            try { oldLine.stop(); } catch(Exception ignored) {}
            try { oldLine.close(); } catch(Exception ignored) {}
        }
        positionMillis = Math.max(0L, startMillis);
        playing = true;

        Thread playbackThread = new Thread(() -> decodeAndPlay(track, startMillis, generation),
                "Flax-Music-Playback");
        playbackThread.setDaemon(true);
        playbackThread.start();
    }

    private void decodeAndPlay(MusicTrack track, long startMillis, int generation) {
        SourceDataLine line = null;
        Process process = null;
        boolean completedNaturally = false;
        String seek = String.format(Locale.ROOT, "%.3f", Math.max(0L, startMillis) / 1000.0D);
        try {
            process = new ProcessBuilder(ffmpegCommand, "-nostdin", "-hide_banner",
                    "-loglevel", "error", "-ss", seek,
                    "-i", track.getFile().getAbsolutePath(), "-vn", "-f", "s16le",
                    "-acodec", "pcm_s16le", "-ar", "44100", "-ac", "2", "pipe:1")
                    .redirectError(ProcessBuilder.Redirect.INHERIT).start();
            playbackProcess = process;

            AudioFormat format = new AudioFormat(44100.0F, 16, 2, true, false);
            line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format));
            line.open(format, 32768);
            line.start();
            activeLine = line;

            byte[] inputBuffer = new byte[8192];
            byte[] outputBuffer = new byte[8192];
            try(BufferedInputStream input = new BufferedInputStream(process.getInputStream(), 64 * 1024)) {
                int read;
                while(generation == playbackGeneration && (read = input.read(inputBuffer)) >= 0) {
                    synchronized(stateLock) {
                        while(paused && generation == playbackGeneration) stateLock.wait(250L);
                    }
                    if(generation != playbackGeneration) break;
                    amplifyPcm(inputBuffer, outputBuffer, read, volume);
                    line.write(outputBuffer, 0, read);
                    positionMillis = startMillis + line.getMicrosecondPosition() / 1000L;
                }
            }
            completedNaturally = generation == playbackGeneration && process.waitFor() == 0;
            if(completedNaturally && line != null) line.drain();
        } catch(InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch(Exception e) {
            if(generation == playbackGeneration) {
                playing = false;
                GlideLogger.error("Unable to play music: " + track.getFile(), e);
            }
        } finally {
            if(process != null && process.isAlive()) try { process.destroyForcibly(); } catch(Exception ignored) {}
            if(playbackProcess == process) playbackProcess = null;
            if(line != null) {
                try { line.stop(); } catch(Exception ignored) {}
                try { line.close(); } catch(Exception ignored) {}
            }
            if(activeLine == line) activeLine = null;
        }

        if(completedNaturally && generation == playbackGeneration) onTrackFinished(track);
    }

    private void onTrackFinished(MusicTrack finished) {
        if(trackLoop) {
            play(finished);
            return;
        }
        List<MusicTrack> snapshot = tracks;
        int index = snapshot.indexOf(finished);
        if(index >= 0 && index + 1 < snapshot.size()) {
            play(snapshot.get(index + 1));
        } else if(playlistLoop && !snapshot.isEmpty()) {
            play(snapshot.get(0));
        } else {
            stopInternal(true);
        }
    }

    private long readDuration(File file) {
        try {
            Process process = new ProcessBuilder(ffprobeCommand, "-nostdin", "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    file.getAbsolutePath()).redirectErrorStream(true).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String seconds = reader.readLine();
            if(process.waitFor() != 0 || seconds == null) return 0L;
            return Math.max(0L, Math.round(Double.parseDouble(seconds.trim()) * 1000.0D));
        } catch(Exception ignored) {
            return 0L;
        }
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

    public List<MusicTrack> getTracks() { return tracks; }
    public MusicTrack getCurrentTrack() { return currentTrack; }
    public File getMusicDirectory() { return musicDirectory; }
    public boolean isEnabled() { return enabled; }
    public boolean isPlaying() { return playing; }
    public boolean isPaused() { return paused; }
    public boolean isTrackLoop() { return trackLoop; }
    public void setTrackLoop(boolean trackLoop) { this.trackLoop = trackLoop; }
    public boolean isPlaylistLoop() { return playlistLoop; }
    public void setPlaylistLoop(boolean playlistLoop) { this.playlistLoop = playlistLoop; }
    public float getVolume() { return volume; }
    public void setVolume(float volume) { this.volume = Math.max(0.0F, Math.min(2.0F, volume)); }
    public long getPositionMillis() { return positionMillis; }
    public long getDurationMillis() { return currentTrack == null ? 0L : currentTrack.getDurationMillis(); }
}
