package me.eldodebug.soar.management.music;

import java.awt.Color;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventRender2D;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.HUDMod;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.management.nanovg.font.LegacyIcon;
import net.minecraft.client.gui.ScaledResolution;

/** Fixed bottom-right now-playing display using the same theme renderer as FPS Display. */
public final class MusicHudRenderer extends HUDMod {

    public MusicHudRenderer() {
        super(TranslateText.MUSIC, TranslateText.MUSIC_INFO_DESCRIPTION);
        setDraggable(false);
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        MusicManager manager = Glide.getInstance().getMusicManager();
        MusicTrack track = manager == null ? null : manager.getCurrentTrack();
        if(manager == null || !manager.isEnabled() || !manager.isPlaying() || track == null) return;

        ScaledResolution resolution = new ScaledResolution(mc);
        NanoVGManager nvg = Glide.getInstance().getNanoVGManager();
        String title = fitTitle(nvg, track.getTitle(), 172.0F);
        float width = 206.0F;
        float height = 34.0F;
        setX(resolution.getScaledWidth() - (int) width - 8);
        setY(resolution.getScaledHeight() - (int) height - 8);

        nvg.setupAndDraw(() -> {
            drawBackground(width, height);
            drawText(LegacyIcon.MUSIC, 7.0F, 7.0F, 12.0F, Fonts.LEGACYICON);
            drawText(title, 24.0F, 6.0F, 8.7F, getHudFont(2));

            long duration = manager.getDurationMillis();
            float progress = duration <= 0L ? 0.0F : Math.min(1.0F,
                    manager.getPositionMillis() / (float) duration);
            drawRoundedRect(24.0F, 21.5F, 172.0F, 3.0F, 1.5F, new Color(255, 255, 255, 45));
            drawRoundedRect(24.0F, 21.5F, 172.0F * progress, 3.0F, 1.5F, getFontColor(205));
            drawText(manager.isPaused() ? "Paused" : formatTime(manager.getPositionMillis()),
                    24.0F, 26.0F, 6.5F, getHudFont(1), getFontColor(145));
            setWidth((int) width);
            setHeight((int) height);
        });
    }

    private String fitTitle(NanoVGManager nvg, String title, float maxWidth) {
        if(nvg.getTextWidth(title, 8.7F, getHudFont(2)) <= maxWidth) return title;
        String result = title;
        while(result.length() > 2 && nvg.getTextWidth(result + "…", 8.7F, getHudFont(2)) > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "…";
    }

    private String formatTime(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        return String.format("%d:%02d", seconds / 60L, seconds % 60L);
    }
}
