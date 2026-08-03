package me.eldodebug.soar.gui.modmenu.category.impl;

import java.awt.Color;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.gui.modmenu.GuiModMenu;
import me.eldodebug.soar.gui.modmenu.category.Category;
import me.eldodebug.soar.management.color.AccentColor;
import me.eldodebug.soar.management.color.palette.ColorPalette;
import me.eldodebug.soar.management.color.palette.ColorType;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.impl.YouTubePipMod;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.management.nanovg.font.Icons;
import me.eldodebug.soar.management.nanovg.font.LegacyIcon;
import me.eldodebug.soar.management.youtube.YouTubeEntry;
import me.eldodebug.soar.management.youtube.YouTubeManager;
import me.eldodebug.soar.management.youtube.YouTubeManager.PlaybackMode;
import me.eldodebug.soar.ui.comp.impl.field.CompTextBox;
import me.eldodebug.soar.utils.ColorUtils;
import me.eldodebug.soar.utils.animation.simple.SimpleAnimation;
import me.eldodebug.soar.utils.mouse.MouseUtils;

public final class YouTubeCategory extends Category {

    private static final float PADDING = 12.0F;
    private static final float ROW_HEIGHT = 31.0F;
    private static final float MODE_BUTTON_WIDTH = 48.0F;
    private final CompTextBox urlBox = new CompTextBox();
    private final SimpleAnimation listScrollAnimation = new SimpleAnimation();
    private float listScrollTarget;
    private boolean draggingProgress;
    private boolean draggingVolume;
    private float seekPreview;

    public YouTubeCategory(GuiModMenu parent) {
        super(parent, TranslateText.YOUTUBE, Icons.YOUTUBE, Fonts.GLICONIC, false, true);
        urlBox.setDefaultText("Paste a YouTube URL to download...");
        urlBox.setMaxStringLength(512);
    }

    @Override
    public void initGui() {
        listScrollTarget = 0.0F;
        listScrollAnimation.setValue(0.0F);
        draggingProgress = false;
        draggingVolume = false;
        urlBox.setFocused(false);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Glide instance = Glide.getInstance();
        YouTubeManager youtube = instance.getYouTubeManager();
        NanoVGManager nvg = instance.getNanoVGManager();
        ColorPalette palette = instance.getColorManager().getPalette();
        AccentColor accent = instance.getColorManager().getCurrentColor();

        float pageX = getX() + PADDING;
        float pageY = getY() + 9.0F;
        float pageWidth = getWidth() - PADDING * 2.0F;
        float pageHeight = getHeight() - 18.0F;
        float inputHeight = 25.0F;
        float addWidth = 68.0F;
        urlBox.setPosition(pageX, pageY, pageWidth - addWidth - 7.0F, inputHeight);
        urlBox.draw(mouseX, mouseY, partialTicks);
        drawAddButton(nvg, palette, accent, pageX + pageWidth - addWidth, pageY, addWidth, inputHeight);

        float panelsY = pageY + inputHeight + 9.0F;
        float panelsHeight = pageHeight - inputHeight - 9.0F;
        float listWidth = Math.max(220.0F, pageWidth * 0.54F);
        if(pageWidth - listWidth - 10.0F < 158.0F) listWidth = Math.max(190.0F, pageWidth - 168.0F);
        float playerX = pageX + listWidth + 10.0F;
        float playerWidth = pageWidth - listWidth - 10.0F;
        drawPanel(nvg, palette, pageX, panelsY, listWidth, panelsHeight);
        drawPanel(nvg, palette, playerX, panelsY, playerWidth, panelsHeight);
        drawPlaylist(nvg, palette, accent, youtube, pageX, panelsY, listWidth, panelsHeight, mouseX, mouseY);
        drawPlayer(nvg, palette, accent, youtube, playerX, panelsY, playerWidth, panelsHeight, mouseX);
    }

    private void drawAddButton(NanoVGManager nvg, ColorPalette palette, AccentColor accent,
            float x, float y, float width, float height) {
        nvg.drawGradientRoundedRect(x, y, width, height, 7.0F,
                ColorUtils.applyAlpha(accent.getColor1(), 190), ColorUtils.applyAlpha(accent.getColor2(), 190));
        nvg.drawCenteredText("Download", x + width / 2.0F, y + 8.0F,
                palette.getFontColor(ColorType.DARK), 8.2F, Fonts.SEMIBOLD);
    }

    private void drawPlaylist(NanoVGManager nvg, ColorPalette palette, AccentColor accent,
            YouTubeManager youtube, float x, float y, float width, float height, int mouseX, int mouseY) {
        List<YouTubeEntry> entries = youtube.getPlaylist();
        nvg.drawText("Downloaded media", x + 11.0F, y + 9.0F,
                palette.getFontColor(ColorType.DARK), 10.0F, Fonts.SEMIBOLD);
        String count = entries.size() + (entries.size() == 1 ? " item" : " items");
        float countWidth = nvg.getTextWidth(count, 7.5F, Fonts.REGULAR);
        nvg.drawText(count, x + width - countWidth - 11.0F, y + 10.5F,
                palette.getFontColor(ColorType.NORMAL, 150), 7.5F, Fonts.REGULAR);

        float listY = y + 29.0F;
        float listHeight = height - 35.0F;
        float maxScroll = Math.max(0.0F, entries.size() * ROW_HEIGHT - listHeight);
        if(MouseUtils.isInside(mouseX, mouseY, x, listY, width, listHeight)) {
            int wheel = Mouse.getDWheel();
            if(wheel != 0) listScrollTarget += wheel / 2.4F;
        }
        listScrollTarget = Math.max(-maxScroll, Math.min(0.0F, listScrollTarget));
        listScrollAnimation.setAnimation(listScrollTarget, 18);

        nvg.save();
        nvg.scissor(x + 3.0F, listY, width - 6.0F, listHeight);
        nvg.translate(0.0F, listScrollAnimation.getValue());
        if(entries.isEmpty()) {
            nvg.drawCenteredText("Paste a link above to download a video", x + width / 2.0F, listY + 24.0F,
                    palette.getFontColor(ColorType.NORMAL, 165), 8.0F, Fonts.REGULAR);
        }
        for(int i = 0; i < entries.size(); i++) {
            YouTubeEntry entry = entries.get(i);
            float rowY = listY + i * ROW_HEIGHT;
            float screenY = rowY + listScrollAnimation.getValue();
            if(screenY + ROW_HEIGHT < listY || screenY > listY + listHeight) continue;
            boolean selected = entry == youtube.getCurrent();
            boolean hovered = MouseUtils.isInside(mouseX, mouseY, x + 5.0F, screenY + 2.0F,
                    width - 10.0F, ROW_HEIGHT - 4.0F);
            if(selected || hovered) {
                nvg.drawRoundedRect(x + 5.0F, rowY + 2.0F, width - 10.0F, ROW_HEIGHT - 4.0F, 6.0F,
                        translucent(palette.getBackgroundColor(ColorType.NORMAL), selected ? 105 : 58));
            }
            if(selected) {
                nvg.drawGradientRoundedRect(x + 6.0F, rowY + 7.0F, 3.0F, 17.0F, 1.5F,
                        ColorUtils.applyAlpha(accent.getColor1(), 235), ColorUtils.applyAlpha(accent.getColor2(), 235));
            }
            String mediaIcon = selected && youtube.isMusicMode() ? Icons.YOUTUBE_MUSIC : Icons.YOUTUBE;
            nvg.drawText(mediaIcon, x + 14.0F, rowY + 9.5F,
                    selected ? new Color(255, 85, 85) : palette.getFontColor(ColorType.NORMAL),
                    10.0F, Fonts.GLICONIC);
            String title = nvg.getLimitText(entry.getTitle(), 8.5F, Fonts.MEDIUM, width - 112.0F);
            nvg.drawText(title, x + 32.0F, rowY + 9.5F,
                    palette.getFontColor(selected ? ColorType.DARK : ColorType.NORMAL), 8.5F, Fonts.MEDIUM);
            String detail;
            if(entry.isDownloading()) detail = "Downloading";
            else if(entry.getDownloadError() != null) detail = "Failed";
            else if(entry.getMediaFile() != null && entry.getMediaFile().isFile()) detail = formatTime(entry.getDurationMillis());
            else detail = "Queued";
            float detailWidth = nvg.getTextWidth(detail, 7.0F, Fonts.REGULAR);
            nvg.drawText(detail, x + width - detailWidth - 12.0F, rowY + 10.5F,
                    palette.getFontColor(ColorType.NORMAL, 140), 7.0F, Fonts.REGULAR);
        }
        nvg.restore();
    }

    private void drawPlayer(NanoVGManager nvg, ColorPalette palette, AccentColor accent,
            YouTubeManager youtube, float x, float y, float width, float height, int mouseX) {
        YouTubeEntry current = youtube.getCurrent();
        drawModeButton(nvg, palette, accent, x + 12.0F, y + 6.0F, MODE_BUTTON_WIDTH,
                "Video", youtube.isVideoMode());
        drawModeButton(nvg, palette, accent, x + 64.0F, y + 6.0F, MODE_BUTTON_WIDTH,
                "Music", youtube.isMusicMode());

        boolean showPipButton = width >= 188.0F;
        YouTubePipMod pip = YouTubePipMod.getInstance();
        boolean pipEnabled = pip != null && pip.isToggled() && youtube.isVideoMode();
        float pipButtonX = x + width - 64.0F;
        if(showPipButton) {
            if(pipEnabled) {
                nvg.drawGradientRoundedRect(pipButtonX, y + 6.0F, 52.0F, 18.0F, 6.0F,
                        ColorUtils.applyAlpha(accent.getColor1(), 205),
                        ColorUtils.applyAlpha(accent.getColor2(), 205));
            } else {
                nvg.drawRoundedRect(pipButtonX, y + 6.0F, 52.0F, 18.0F, 6.0F,
                        translucent(palette.getBackgroundColor(ColorType.NORMAL), youtube.isVideoMode() ? 105 : 55));
            }
            nvg.drawCenteredText(youtube.isMusicMode() ? "PiP --" : pipEnabled ? "PiP ON" : "PiP OFF",
                    pipButtonX + 26.0F, y + 11.5F,
                    palette.getFontColor(youtube.isVideoMode() ? ColorType.DARK : ColorType.NORMAL, 170),
                    7.2F, Fonts.SEMIBOLD);
        }

        String title = current == null ? "Nothing is playing"
                : nvg.getLimitText(current.getTitle(), 9.2F, Fonts.SEMIBOLD, width - 24.0F);
        nvg.drawText(title, x + 12.0F, y + 31.0F,
                palette.getFontColor(ColorType.DARK), 9.2F, Fonts.SEMIBOLD);
        nvg.drawText(nvg.getLimitText(youtube.getStatus(), 7.2F, Fonts.REGULAR, width - 24.0F),
                x + 12.0F, y + 48.0F, palette.getFontColor(ColorType.NORMAL, 145), 7.2F, Fonts.REGULAR);

        float progressX = x + 12.0F;
        float progressY = y + 69.0F;
        float sliderWidth = width - 24.0F;
        if(draggingProgress) updateProgressPreview(mouseX, progressX, sliderWidth);
        float progress = draggingProgress ? seekPreview : youtube.getDurationMillis() <= 0L ? 0.0F
                : Math.min(1.0F, youtube.getPositionMillis() / (float) youtube.getDurationMillis());
        drawSlider(nvg, accent, progressX, progressY, sliderWidth, progress, draggingProgress);
        nvg.drawText(formatTime(youtube.getPositionMillis()), progressX, progressY + 8.0F,
                palette.getFontColor(ColorType.NORMAL, 150), 7.0F, Fonts.REGULAR);
        String total = formatTime(youtube.getDurationMillis());
        nvg.drawText(total, progressX + sliderWidth - nvg.getTextWidth(total, 7.0F, Fonts.REGULAR),
                progressY + 8.0F, palette.getFontColor(ColorType.NORMAL, 150), 7.0F, Fonts.REGULAR);

        float controlsY = y + 101.0F;
        float center = x + width / 2.0F;
        drawControl(nvg, palette, center - 57.0F, controlsY, LegacyIcon.BACK, false);
        drawControl(nvg, palette, center - 21.0F, controlsY,
                youtube.isPaused() || !youtube.isPlaying() ? LegacyIcon.PLAY : LegacyIcon.PAUSE, true);
        drawControl(nvg, palette, center + 15.0F, controlsY, LegacyIcon.FORWARD, false);
        drawControl(nvg, palette, center + 51.0F, controlsY, LegacyIcon.X, false);

        float loopY = y + 134.0F;
        float loopGap = 6.0F;
        float loopWidth = (width - 24.0F - loopGap) / 2.0F;
        drawLoopButton(nvg, palette, accent, x + 12.0F, loopY, loopWidth,
                "Loop item", youtube.isVideoLoopEnabled());
        drawLoopButton(nvg, palette, accent, x + 12.0F + loopWidth + loopGap, loopY, loopWidth,
                "Loop list", youtube.isPlaylistLoopEnabled());

        float volumeY = y + Math.min(height - 39.0F, 176.0F);
        nvg.drawText(LegacyIcon.VOLUME_2, x + 12.0F, volumeY - 4.0F,
                palette.getFontColor(ColorType.NORMAL), 10.0F, Fonts.LEGACYICON);
        String percent = Math.round(youtube.getVolume() * 100.0F) + "%";
        nvg.drawText(percent, x + width - nvg.getTextWidth(percent, 8.0F, Fonts.MEDIUM) - 12.0F,
                volumeY - 3.0F, palette.getFontColor(ColorType.DARK), 8.0F, Fonts.MEDIUM);
        float volumeX = x + 30.0F;
        float volumeWidth = width - 75.0F;
        if(draggingVolume) updateVolume(youtube, mouseX, volumeX, volumeWidth);
        drawSlider(nvg, accent, volumeX, volumeY, volumeWidth, youtube.getVolume() / 2.0F, draggingVolume);
        nvg.drawCenteredText("Left-click play  •  right-click remove", x + width / 2.0F, y + height - 15.0F,
                palette.getFontColor(ColorType.NORMAL, 110), 6.8F, Fonts.REGULAR);
    }

    private void drawModeButton(NanoVGManager nvg, ColorPalette palette, AccentColor accent,
            float x, float y, float width, String label, boolean active) {
        if(active) {
            nvg.drawGradientRoundedRect(x, y, width, 18.0F, 6.0F,
                    ColorUtils.applyAlpha(accent.getColor1(), 205),
                    ColorUtils.applyAlpha(accent.getColor2(), 205));
        } else {
            nvg.drawRoundedRect(x, y, width, 18.0F, 6.0F,
                    translucent(palette.getBackgroundColor(ColorType.NORMAL), 92));
        }
        nvg.drawCenteredText(label, x + width / 2.0F, y + 5.6F,
                palette.getFontColor(active ? ColorType.DARK : ColorType.NORMAL), 7.2F, Fonts.SEMIBOLD);
    }

    private void drawPanel(NanoVGManager nvg, ColorPalette palette, float x, float y, float width, float height) {
        nvg.drawRoundedRect(x, y, width, height, 10.0F,
                translucent(palette.getBackgroundColor(ColorType.DARK), 78));
        nvg.drawOutlineRoundedRect(x + 0.5F, y + 0.5F, width - 1.0F, height - 1.0F,
                10.0F, 0.65F, new Color(255, 255, 255, 26));
    }

    private void drawSlider(NanoVGManager nvg, AccentColor accent, float x, float y,
            float width, float value, boolean active) {
        float clamped = Math.max(0.0F, Math.min(1.0F, value));
        nvg.drawRoundedRect(x, y, width, 4.0F, 2.0F, new Color(255, 255, 255, 42));
        nvg.drawGradientRoundedRect(x, y, width * clamped, 4.0F, 2.0F,
                ColorUtils.applyAlpha(accent.getColor1(), 220), ColorUtils.applyAlpha(accent.getColor2(), 220));
        nvg.drawCircle(x + width * clamped, y + 2.0F, active ? 4.0F : 3.2F,
                new Color(255, 255, 255, 225));
    }

    private void drawControl(NanoVGManager nvg, ColorPalette palette,
            float x, float y, String icon, boolean primary) {
        nvg.drawRoundedRect(x, y, 28.0F, 25.0F, 7.0F,
                translucent(palette.getBackgroundColor(primary ? ColorType.NORMAL : ColorType.DARK),
                        primary ? 150 : 92));
        nvg.drawCenteredText(icon, x + 14.0F, y + 6.5F,
                palette.getFontColor(ColorType.DARK), 10.0F, Fonts.LEGACYICON);
    }

    private void drawLoopButton(NanoVGManager nvg, ColorPalette palette, AccentColor accent,
            float x, float y, float width, String label, boolean active) {
        if(active) {
            nvg.drawGradientRoundedRect(x, y, width, 20.0F, 6.0F,
                    ColorUtils.applyAlpha(accent.getColor1(), 205),
                    ColorUtils.applyAlpha(accent.getColor2(), 205));
        } else {
            nvg.drawRoundedRect(x, y, width, 20.0F, 6.0F,
                    translucent(palette.getBackgroundColor(ColorType.DARK), 92));
        }
        nvg.drawCenteredText(label, x + width / 2.0F, y + 6.0F,
                palette.getFontColor(ColorType.DARK), 7.2F, Fonts.MEDIUM);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        YouTubeManager youtube = Glide.getInstance().getYouTubeManager();
        float pageX = getX() + PADDING;
        float pageY = getY() + 9.0F;
        float pageWidth = getWidth() - PADDING * 2.0F;
        float pageHeight = getHeight() - 18.0F;
        float addWidth = 68.0F;
        urlBox.mouseClicked(mouseX, mouseY, mouseButton);
        if(mouseButton == 0 && MouseUtils.isInside(mouseX, mouseY,
                pageX + pageWidth - addWidth, pageY, addWidth, 25.0F)) {
            submitUrl();
            return;
        }

        float panelsY = pageY + 34.0F;
        float panelsHeight = pageHeight - 34.0F;
        float listWidth = Math.max(220.0F, pageWidth * 0.54F);
        if(pageWidth - listWidth - 10.0F < 158.0F) listWidth = Math.max(190.0F, pageWidth - 168.0F);
        float playerX = pageX + listWidth + 10.0F;
        float playerWidth = pageWidth - listWidth - 10.0F;
        float listY = panelsY + 29.0F;
        float listHeight = panelsHeight - 35.0F;
        if(MouseUtils.isInside(mouseX, mouseY, pageX, listY, listWidth, listHeight)) {
            List<YouTubeEntry> entries = youtube.getPlaylist();
            for(int i = 0; i < entries.size(); i++) {
                float rowY = listY + i * ROW_HEIGHT + listScrollAnimation.getValue();
                if(MouseUtils.isInside(mouseX, mouseY, pageX + 5.0F, rowY + 2.0F,
                        listWidth - 10.0F, ROW_HEIGHT - 4.0F)) {
                    if(mouseButton == 0) youtube.play(entries.get(i));
                    else if(mouseButton == 1) youtube.remove(entries.get(i));
                    return;
                }
            }
        }
        if(mouseButton != 0) return;

        if(MouseUtils.isInside(mouseX, mouseY, playerX + 12.0F, panelsY + 6.0F,
                MODE_BUTTON_WIDTH, 18.0F)) {
            youtube.setPlaybackMode(PlaybackMode.VIDEO);
            return;
        }
        if(MouseUtils.isInside(mouseX, mouseY, playerX + 64.0F, panelsY + 6.0F,
                MODE_BUTTON_WIDTH, 18.0F)) {
            youtube.setPlaybackMode(PlaybackMode.MUSIC);
            return;
        }

        YouTubePipMod pip = YouTubePipMod.getInstance();
        if(playerWidth >= 188.0F && youtube.isVideoMode() && pip != null
                && MouseUtils.isInside(mouseX, mouseY,
                        playerX + playerWidth - 64.0F, panelsY + 6.0F, 52.0F, 18.0F)) {
            pip.toggle();
            return;
        }

        float progressX = playerX + 12.0F;
        float progressY = panelsY + 69.0F;
        float sliderWidth = playerWidth - 24.0F;
        if(MouseUtils.isInside(mouseX, mouseY, progressX - 3.0F, progressY - 5.0F,
                sliderWidth + 6.0F, 14.0F)) {
            draggingProgress = true;
            updateProgressPreview(mouseX, progressX, sliderWidth);
            return;
        }

        float controlsY = panelsY + 101.0F;
        float center = playerX + playerWidth / 2.0F;
        if(insideControl(mouseX, mouseY, center - 57.0F, controlsY)) youtube.playPrevious();
        else if(insideControl(mouseX, mouseY, center - 21.0F, controlsY)) youtube.togglePause();
        else if(insideControl(mouseX, mouseY, center + 15.0F, controlsY)) youtube.playNext();
        else if(insideControl(mouseX, mouseY, center + 51.0F, controlsY)) youtube.stop();

        float loopY = panelsY + 134.0F;
        float loopGap = 6.0F;
        float loopWidth = (playerWidth - 24.0F - loopGap) / 2.0F;
        if(MouseUtils.isInside(mouseX, mouseY, playerX + 12.0F, loopY, loopWidth, 20.0F)) {
            youtube.toggleVideoLoop();
            return;
        }
        if(MouseUtils.isInside(mouseX, mouseY, playerX + 12.0F + loopWidth + loopGap,
                loopY, loopWidth, 20.0F)) {
            youtube.togglePlaylistLoop();
            return;
        }

        float volumeY = panelsY + Math.min(panelsHeight - 39.0F, 176.0F);
        float volumeX = playerX + 30.0F;
        float volumeWidth = playerWidth - 75.0F;
        if(MouseUtils.isInside(mouseX, mouseY, volumeX - 3.0F, volumeY - 5.0F,
                volumeWidth + 6.0F, 14.0F)) {
            draggingVolume = true;
            updateVolume(youtube, mouseX, volumeX, volumeWidth);
        }
    }

    private void submitUrl() {
        String url = urlBox.getText();
        if(url == null || url.trim().isEmpty()) return;
        Glide.getInstance().getYouTubeManager().addUrl(url);
        urlBox.setText("");
        urlBox.setFocused(false);
    }

    private boolean insideControl(int mouseX, int mouseY, float x, float y) {
        return MouseUtils.isInside(mouseX, mouseY, x, y, 28.0F, 25.0F);
    }

    private void updateProgressPreview(int mouseX, float x, float width) {
        seekPreview = Math.max(0.0F, Math.min(1.0F, (mouseX - x) / width));
    }

    private void updateVolume(YouTubeManager manager, int mouseX, float x, float width) {
        manager.setVolume(Math.max(0.0F, Math.min(1.0F, (mouseX - x) / width)) * 2.0F);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        if(mouseButton == 0) {
            if(draggingProgress) Glide.getInstance().getYouTubeManager().seekToFraction(seekPreview);
            draggingProgress = false;
            draggingVolume = false;
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if(urlBox.isFocused() && keyCode == Keyboard.KEY_RETURN) {
            submitUrl();
            return;
        }
        urlBox.keyTyped(typedChar, keyCode);
    }

    private Color translucent(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private String formatTime(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        return String.format("%d:%02d", seconds / 60L, seconds % 60L);
    }
}
