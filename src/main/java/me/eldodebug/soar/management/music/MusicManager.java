package me.eldodebug.soar.management.music;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
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

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import me.eldodebug.soar.logger.GlideLogger;
import net.minecraft.client.Minecraft;

/** Lightweight, local MP3 player used by the ClickGUI Music page. */
public final class MusicManager {

    private final Object stateLock = new Object();
    private volatile List<MusicTrack> tracks = Collections.emptyList();
    private volatile MusicTrack currentTrack;
    private volatile SourceDataLine activeLine;
    private volatile boolean enabled = true;
    private volatile boolean playing;
    private volatile boolean paused;
    private volatile boolean trackLoop;
    private volatile boolean playlistLoop;
    private volatile float volume = 1.0F;
    private volatile long positionMillis;
    private volatile int playbackGeneration;
    private File musicDirectory;

    public MusicManager() {
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
        Bitstream bitstream = null;
        SourceDataLine line = null;
        long decodedMillis = 0L;
        boolean completedNaturally = false;
        try(BufferedInputStream input = new BufferedInputStream(new FileInputStream(track.getFile()), 64 * 1024)) {
            bitstream = new Bitstream(input);
            Decoder decoder = new Decoder();
            Header header;
            byte[] pcmBytes = null;

            while(generation == playbackGeneration && (header = bitstream.readFrame()) != null) {
                float frameMillis = header.ms_per_frame();
                SampleBuffer samples = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                decodedMillis += Math.max(1L, Math.round(frameMillis));

                if(decodedMillis >= startMillis) {
                    if(line == null) {
                        AudioFormat format = new AudioFormat(samples.getSampleFrequency(), 16,
                                samples.getChannelCount(), true, false);
                        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                        line = (SourceDataLine) AudioSystem.getLine(info);
                        line.open(format, 32768);
                        line.start();
                        activeLine = line;
                    }

                    synchronized(stateLock) {
                        while(paused && generation == playbackGeneration) stateLock.wait(250L);
                    }
                    if(generation != playbackGeneration) break;

                    int sampleCount = samples.getBufferLength();
                    if(pcmBytes == null || pcmBytes.length < sampleCount * 2) pcmBytes = new byte[sampleCount * 2];
                    short[] pcm = samples.getBuffer();
                    float gain = volume;
                    for(int i = 0; i < sampleCount; i++) {
                        int amplified = Math.round(pcm[i] * gain);
                        if(amplified > Short.MAX_VALUE) amplified = Short.MAX_VALUE;
                        else if(amplified < Short.MIN_VALUE) amplified = Short.MIN_VALUE;
                        pcmBytes[i * 2] = (byte) amplified;
                        pcmBytes[i * 2 + 1] = (byte) (amplified >>> 8);
                    }
                    line.write(pcmBytes, 0, sampleCount * 2);
                    positionMillis = decodedMillis;
                }
                bitstream.closeFrame();
            }
            completedNaturally = generation == playbackGeneration;
            if(completedNaturally && line != null) line.drain();
        } catch(InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch(Exception e) {
            if(generation == playbackGeneration) {
                playing = false;
                GlideLogger.error("Unable to play music: " + track.getFile(), e);
            }
        } finally {
            if(bitstream != null) {
                try { bitstream.close(); } catch(Exception ignored) {}
            }
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
        try(BufferedInputStream input = new BufferedInputStream(new FileInputStream(file), 16 * 1024)) {
            Bitstream stream = new Bitstream(input);
            Header first = stream.readFrame();
            if(first == null) return 0L;
            long duration = Math.max(0L, Math.round(first.total_ms((int) Math.min(Integer.MAX_VALUE, file.length()))));
            stream.closeFrame();
            stream.close();
            return duration;
        } catch(Exception ignored) {
            return 0L;
        }
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
