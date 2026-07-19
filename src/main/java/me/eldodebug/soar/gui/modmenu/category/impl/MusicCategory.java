package me.eldodebug.soar.gui.modmenu.category.impl;

import java.awt.Color;
import java.util.List;

import org.lwjgl.input.Mouse;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.gui.modmenu.GuiModMenu;
import me.eldodebug.soar.gui.modmenu.category.Category;
import me.eldodebug.soar.management.color.AccentColor;
import me.eldodebug.soar.management.color.ColorManager;
import me.eldodebug.soar.management.color.palette.ColorPalette;
import me.eldodebug.soar.management.color.palette.ColorType;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.music.MusicManager;
import me.eldodebug.soar.management.music.MusicTrack;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.management.nanovg.font.LegacyIcon;
import me.eldodebug.soar.utils.ColorUtils;
import me.eldodebug.soar.utils.animation.simple.SimpleAnimation;
import me.eldodebug.soar.utils.file.FileUtils;
import me.eldodebug.soar.utils.mouse.MouseUtils;

public final class MusicCategory extends Category {

    private static final float PADDING = 12.0F;
    private static final float ROW_HEIGHT = 30.0F;
    private final SimpleAnimation listScrollAnimation = new SimpleAnimation();
    private float listScrollTarget;
    private boolean draggingProgress;
    private boolean draggingVolume;
    private float seekPreview;

    public MusicCategory(GuiModMenu parent) {
        super(parent, TranslateText.MUSIC, LegacyIcon.MUSIC, false, true);
    }

    @Override
    public void initGui() {
        listScrollTarget = 0.0F;
        listScrollAnimation.setValue(0.0F);
        draggingProgress = false;
        draggingVolume = false;
    }

    @Override
    public void initCategory() {
        Glide.getInstance().getMusicManager().refreshTracks();
        listScrollTarget = 0.0F;
        listScrollAnimation.setValue(0.0F);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Glide instance = Glide.getInstance();
        MusicManager music = instance.getMusicManager();
        NanoVGManager nvg = instance.getNanoVGManager();
        ColorManager colorManager = instance.getColorManager();
        ColorPalette palette = colorManager.getPalette();
        AccentColor accent = colorManager.getCurrentColor();

        float pageX = getX() + PADDING;
        float pageY = getY() + 9.0F;
        float pageWidth = getWidth() - PADDING * 2.0F;
        float pageHeight = getHeight() - 18.0F;
        float listWidth = Math.max(230.0F, pageWidth * 0.56F);
        float gap = 10.0F;
        float playerX = pageX + listWidth + gap;
        float playerWidth = pageWidth - listWidth - gap;

        drawPanel(nvg, palette, pageX, pageY, listWidth, pageHeight);
        drawPanel(nvg, palette, playerX, pageY, playerWidth, pageHeight);
        drawTrackList(nvg, palette, accent, music, pageX, pageY, listWidth, pageHeight, mouseX, mouseY);
        drawPlayer(nvg, palette, accent, music, playerX, pageY, playerWidth, pageHeight, mouseX, mouseY);
    }

    private void drawTrackList(NanoVGManager nvg, ColorPalette palette, AccentColor accent,
            MusicManager music, float x, float y, float width, float height, int mouseX, int mouseY) {
        List<MusicTrack> tracks = music.getTracks();
        nvg.drawText("Musics", x + 11.0F, y + 10.0F, palette.getFontColor(ColorType.DARK), 10.0F, Fonts.SEMIBOLD);
        String count = tracks.size() + (tracks.size() == 1 ? " track" : " tracks");
        float countWidth = nvg.getTextWidth(count, 7.5F, Fonts.REGULAR);
        nvg.drawText(count, x + width - countWidth - 11.0F, y + 11.5F,
                palette.getFontColor(ColorType.NORMAL, 155), 7.5F, Fonts.REGULAR);

        float listY = y + 31.0F;
        float listHeight = height - 39.0F;
        float maxScroll = Math.max(0.0F, tracks.size() * ROW_HEIGHT - listHeight);
        if(MouseUtils.isInside(mouseX, mouseY, x, listY, width, listHeight)) {
            int wheel = Mouse.getDWheel();
            if(wheel != 0) listScrollTarget += wheel / 2.4F;
        }
        listScrollTarget = Math.max(-maxScroll, Math.min(0.0F, listScrollTarget));
        listScrollAnimation.setAnimation(listScrollTarget, 18);

        nvg.save();
        nvg.scissor(x + 3.0F, listY, width - 6.0F, listHeight);
        nvg.translate(0.0F, listScrollAnimation.getValue());
        if(tracks.isEmpty()) {
            nvg.drawCenteredText("Click here to add MP3 files", x + width / 2.0F, listY + 24.0F,
                    palette.getFontColor(ColorType.NORMAL, 170), 8.5F, Fonts.REGULAR);
        }
        for(int i = 0; i < tracks.size(); i++) {
            MusicTrack track = tracks.get(i);
            float rowY = listY + i * ROW_HEIGHT;
            float screenY = rowY + listScrollAnimation.getValue();
            if(screenY + ROW_HEIGHT < listY || screenY > listY + listHeight) continue;
            boolean selected = track == music.getCurrentTrack();
            boolean hovered = MouseUtils.isInside(mouseX, mouseY, x + 5.0F, screenY + 2.0F, width - 10.0F, ROW_HEIGHT - 4.0F);
            if(selected || hovered) {
                nvg.drawRoundedRect(x + 5.0F, rowY + 2.0F, width - 10.0F, ROW_HEIGHT - 4.0F, 6.0F,
                        translucent(palette.getBackgroundColor(ColorType.NORMAL), selected ? 104 : 58));
            }
            if(selected) {
                nvg.drawGradientRoundedRect(x + 6.0F, rowY + 7.0F, 3.0F, 16.0F, 1.5F,
                        ColorUtils.applyAlpha(accent.getColor1(), 235), ColorUtils.applyAlpha(accent.getColor2(), 235));
            }
            String title = nvg.getLimitText(track.getTitle(), 8.7F, Fonts.MEDIUM, width - 73.0F);
            nvg.drawText(title, x + 14.0F, rowY + 10.0F,
                    palette.getFontColor(selected ? ColorType.DARK : ColorType.NORMAL), 8.7F, Fonts.MEDIUM);
            String duration = formatTime(track.getDurationMillis());
            float durationWidth = nvg.getTextWidth(duration, 7.0F, Fonts.REGULAR);
            nvg.drawText(duration, x + width - durationWidth - 12.0F, rowY + 11.0F,
                    palette.getFontColor(ColorType.NORMAL, 140), 7.0F, Fonts.REGULAR);
        }
        nvg.restore();
    }

    private void drawPlayer(NanoVGManager nvg, ColorPalette palette, AccentColor accent,
            MusicManager music, float x, float y, float width, float height, int mouseX, int mouseY) {
        MusicTrack current = music.getCurrentTrack();
        nvg.drawText("Now Playing", x + 12.0F, y + 10.0F, palette.getFontColor(ColorType.NORMAL, 165), 8.0F, Fonts.MEDIUM);
        nvg.drawText(LegacyIcon.MUSIC, x + 12.0F, y + 36.0F, palette.getFontColor(ColorType.DARK), 16.0F, Fonts.LEGACYICON);
        String title = current == null ? "Nothing is playing" : nvg.getLimitText(current.getTitle(), 9.5F, Fonts.SEMIBOLD, width - 55.0F);
        nvg.drawText(title, x + 38.0F, y + 38.0F, palette.getFontColor(ColorType.DARK), 9.5F, Fonts.SEMIBOLD);

        float progressX = x + 12.0F;
        float progressY = y + 70.0F;
        float sliderWidth = width - 24.0F;
        if(draggingProgress) updateProgressPreview(mouseX, progressX, sliderWidth);
        float progress = draggingProgress ? seekPreview : music.getDurationMillis() <= 0L ? 0.0F
                : Math.min(1.0F, music.getPositionMillis() / (float) music.getDurationMillis());
        drawSlider(nvg, accent, progressX, progressY, sliderWidth, progress, draggingProgress);
        nvg.drawText(formatTime(music.getPositionMillis()), progressX, progressY + 8.0F,
                palette.getFontColor(ColorType.NORMAL, 150), 7.0F, Fonts.REGULAR);
        String total = formatTime(music.getDurationMillis());
        nvg.drawText(total, progressX + sliderWidth - nvg.getTextWidth(total, 7.0F, Fonts.REGULAR), progressY + 8.0F,
                palette.getFontColor(ColorType.NORMAL, 150), 7.0F, Fonts.REGULAR);

        float controlsY = y + 101.0F;
        drawControl(nvg, palette, x + width / 2.0F - 57.0F, controlsY, LegacyIcon.BACK, false);
        drawControl(nvg, palette, x + width / 2.0F - 21.0F, controlsY,
                music.isPaused() || current == null ? LegacyIcon.PLAY : LegacyIcon.PAUSE, true);
        drawControl(nvg, palette, x + width / 2.0F + 15.0F, controlsY, LegacyIcon.FORWARD, false);
        drawControl(nvg, palette, x + width / 2.0F + 51.0F, controlsY, LegacyIcon.X, false);

        float toggleY = y + 143.0F;
        drawToggle(nvg, palette, accent, x + 12.0F, toggleY, (width - 30.0F) / 2.0F,
                "Track loop", music.isTrackLoop());
        drawToggle(nvg, palette, accent, x + 18.0F + (width - 30.0F) / 2.0F, toggleY,
                (width - 30.0F) / 2.0F, "All loop", music.isPlaylistLoop());

        float volumeY = y + Math.min(height - 46.0F, 188.0F);
        nvg.drawText(LegacyIcon.VOLUME_2, x + 12.0F, volumeY - 4.0F,
                palette.getFontColor(ColorType.NORMAL), 10.0F, Fonts.LEGACYICON);
        String percent = Math.round(music.getVolume() * 100.0F) + "%";
        nvg.drawText(percent, x + width - nvg.getTextWidth(percent, 8.0F, Fonts.MEDIUM) - 12.0F, volumeY - 3.0F,
                palette.getFontColor(ColorType.DARK), 8.0F, Fonts.MEDIUM);
        float volumeX = x + 30.0F;
        float volumeWidth = width - 75.0F;
        if(draggingVolume) updateVolume(music, mouseX, volumeX, volumeWidth);
        drawSlider(nvg, accent, volumeX, volumeY, volumeWidth, music.getVolume() / 2.0F, draggingVolume);

        nvg.drawCenteredText("Shift + Delete  •  disable music", x + width / 2.0F, y + height - 18.0F,
                palette.getFontColor(ColorType.NORMAL, 115), 7.0F, Fonts.REGULAR);
    }

    private void drawPanel(NanoVGManager nvg, ColorPalette palette, float x, float y, float width, float height) {
        nvg.drawRoundedRect(x, y, width, height, 10.0F,
                translucent(palette.getBackgroundColor(ColorType.DARK), 78));
        nvg.drawOutlineRoundedRect(x + 0.5F, y + 0.5F, width - 1.0F, height - 1.0F,
                10.0F, 0.65F, new Color(255, 255, 255, 26));
    }

    private void drawSlider(NanoVGManager nvg, AccentColor accent, float x, float y, float width,
            float value, boolean active) {
        float clamped = Math.max(0.0F, Math.min(1.0F, value));
        nvg.drawRoundedRect(x, y, width, 4.0F, 2.0F, new Color(255, 255, 255, 42));
        nvg.drawGradientRoundedRect(x, y, width * clamped, 4.0F, 2.0F,
                ColorUtils.applyAlpha(accent.getColor1(), 220), ColorUtils.applyAlpha(accent.getColor2(), 220));
        nvg.drawCircle(x + width * clamped, y + 2.0F, active ? 4.0F : 3.2F, new Color(255, 255, 255, 225));
    }

    private void drawControl(NanoVGManager nvg, ColorPalette palette, float x, float y, String icon, boolean primary) {
        nvg.drawRoundedRect(x, y, 28.0F, 25.0F, 7.0F,
                translucent(palette.getBackgroundColor(primary ? ColorType.NORMAL : ColorType.DARK), primary ? 150 : 92));
        nvg.drawCenteredText(icon, x + 14.0F, y + 7.0F, palette.getFontColor(ColorType.DARK), 10.0F, Fonts.LEGACYICON);
    }

    private void drawToggle(NanoVGManager nvg, ColorPalette palette, AccentColor accent, float x, float y,
            float width, String label, boolean enabled) {
        if(enabled) {
            nvg.drawGradientRoundedRect(x, y, width, 23.0F, 6.0F,
                    ColorUtils.applyAlpha(accent.getColor1(), 155), ColorUtils.applyAlpha(accent.getColor2(), 155));
        } else {
            nvg.drawRoundedRect(x, y, width, 23.0F, 6.0F,
                    translucent(palette.getBackgroundColor(ColorType.NORMAL), 76));
        }
        nvg.drawCenteredText(label, x + width / 2.0F, y + 7.5F,
                palette.getFontColor(enabled ? ColorType.DARK : ColorType.NORMAL), 7.8F, Fonts.MEDIUM);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if(mouseButton != 0) return;
        MusicManager music = Glide.getInstance().getMusicManager();
        float pageX = getX() + PADDING;
        float pageY = getY() + 9.0F;
        float pageWidth = getWidth() - PADDING * 2.0F;
        float pageHeight = getHeight() - 18.0F;
        float listWidth = Math.max(230.0F, pageWidth * 0.56F);
        float playerX = pageX + listWidth + 10.0F;
        float playerWidth = pageWidth - listWidth - 10.0F;
        float listY = pageY + 31.0F;
        float listHeight = pageHeight - 39.0F;

        if(MouseUtils.isInside(mouseX, mouseY, pageX, listY, listWidth, listHeight)) {
            List<MusicTrack> tracks = music.getTracks();
            if(tracks.isEmpty()) {
                FileUtils.openFolderAtPath(music.getMusicDirectory());
                return;
            }
            for(int i = 0; i < tracks.size(); i++) {
                float rowY = listY + i * ROW_HEIGHT + listScrollAnimation.getValue();
                if(MouseUtils.isInside(mouseX, mouseY, pageX + 5.0F, rowY + 2.0F, listWidth - 10.0F, ROW_HEIGHT - 4.0F)) {
                    music.play(tracks.get(i));
                    return;
                }
            }
        }

        float progressX = playerX + 12.0F;
        float progressY = pageY + 70.0F;
        float sliderWidth = playerWidth - 24.0F;
        if(MouseUtils.isInside(mouseX, mouseY, progressX - 3.0F, progressY - 5.0F, sliderWidth + 6.0F, 14.0F)) {
            draggingProgress = true;
            updateProgressPreview(mouseX, progressX, sliderWidth);
            return;
        }

        float controlsY = pageY + 101.0F;
        float center = playerX + playerWidth / 2.0F;
        if(insideControl(mouseX, mouseY, center - 57.0F, controlsY)) music.playPrevious();
        else if(insideControl(mouseX, mouseY, center - 21.0F, controlsY)) music.togglePause();
        else if(insideControl(mouseX, mouseY, center + 15.0F, controlsY)) music.playNext();
        else if(insideControl(mouseX, mouseY, center + 51.0F, controlsY)) music.stop();

        float toggleY = pageY + 143.0F;
        float toggleWidth = (playerWidth - 30.0F) / 2.0F;
        if(MouseUtils.isInside(mouseX, mouseY, playerX + 12.0F, toggleY, toggleWidth, 23.0F)) {
            music.setTrackLoop(!music.isTrackLoop());
        } else if(MouseUtils.isInside(mouseX, mouseY, playerX + 18.0F + toggleWidth, toggleY, toggleWidth, 23.0F)) {
            music.setPlaylistLoop(!music.isPlaylistLoop());
        }

        float volumeY = pageY + Math.min(pageHeight - 46.0F, 188.0F);
        float volumeX = playerX + 30.0F;
        float volumeWidth = playerWidth - 75.0F;
        if(MouseUtils.isInside(mouseX, mouseY, volumeX - 3.0F, volumeY - 5.0F, volumeWidth + 6.0F, 14.0F)) {
            draggingVolume = true;
            updateVolume(music, mouseX, volumeX, volumeWidth);
        }
    }

    private boolean insideControl(int mouseX, int mouseY, float x, float y) {
        return MouseUtils.isInside(mouseX, mouseY, x, y, 28.0F, 25.0F);
    }

    private void updateProgressPreview(int mouseX, float x, float width) {
        seekPreview = Math.max(0.0F, Math.min(1.0F, (mouseX - x) / width));
    }

    private void updateVolume(MusicManager music, int mouseX, float x, float width) {
        music.setVolume(Math.max(0.0F, Math.min(1.0F, (mouseX - x) / width)) * 2.0F);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        if(mouseButton == 0) {
            if(draggingProgress) Glide.getInstance().getMusicManager().seekToFraction(seekPreview);
            draggingProgress = false;
            draggingVolume = false;
        }
    }

    private Color translucent(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private String formatTime(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        return String.format("%d:%02d", seconds / 60L, seconds % 60L);
    }
}
