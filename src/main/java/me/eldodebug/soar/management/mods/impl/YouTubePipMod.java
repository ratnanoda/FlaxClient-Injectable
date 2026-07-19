package me.eldodebug.soar.management.mods.impl;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.color.AccentColor;
import me.eldodebug.soar.management.color.palette.ColorPalette;
import me.eldodebug.soar.management.color.palette.ColorType;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventRender2D;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.HUDMod;
import me.eldodebug.soar.management.mods.settings.impl.ComboSetting;
import me.eldodebug.soar.management.mods.settings.impl.combo.Option;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.management.youtube.YouTubeManager;
import me.eldodebug.soar.utils.ColorUtils;
import net.minecraft.client.gui.ScaledResolution;

public final class YouTubePipMod extends HUDMod {

    private static final int BASE_WIDTH = 320;
    private static final int BASE_HEIGHT = 180;
    private static YouTubePipMod instance;

    private final ComboSetting qualitySetting = new ComboSetting(TranslateText.QUALITY, this,
            TranslateText.QUALITY_480P, new ArrayList<Option>(Arrays.asList(
                    new Option(TranslateText.QUALITY_360P),
                    new Option(TranslateText.QUALITY_480P),
                    new Option(TranslateText.QUALITY_720P))));

    private ByteBuffer frameBuffer;
    private int textureId;
    private int textureWidth;
    private int textureHeight;
    private byte[] uploadedFrame;

    public YouTubePipMod() {
        super(TranslateText.YOUTUBE_PIP, TranslateText.YOUTUBE_PIP_DESCRIPTION);
        instance = this;
        ScaledResolution resolution = new ScaledResolution(mc);
        setX(Math.max(8, resolution.getScaledWidth() - BASE_WIDTH - 8));
        setY(Math.max(8, resolution.getScaledHeight() - BASE_HEIGHT - 8));
        setWidth(BASE_WIDTH);
        setHeight(BASE_HEIGHT);
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        YouTubeManager manager = Glide.getInstance().getYouTubeManager();
        if(manager == null) return;
        int quality = getQualityHeight();
        if(manager.getQualityHeight() != quality) manager.setQualityHeight(quality);
        if(!manager.isPipVisible() && !isEditing()) return;

        int sourceWidth = manager.getVideoWidth();
        int sourceHeight = manager.getVideoHeight();
        byte[] frame = manager.getLatestFrame();
        if(frame != null && frame.length == sourceWidth * sourceHeight * 4) uploadFrame(frame, sourceWidth, sourceHeight);

        setWidth(BASE_WIDTH);
        setHeight(BASE_HEIGHT);
        final float drawWidth = getWidth();
        final float drawHeight = getHeight();
        NanoVGManager nvg = Glide.getInstance().getNanoVGManager();
        ColorPalette palette = Glide.getInstance().getColorManager().getPalette();
        AccentColor accent = Glide.getInstance().getColorManager().getCurrentColor();

        nvg.setupAndDraw(() -> {
            nvg.drawShadow(getX(), getY(), drawWidth, drawHeight, 9.0F * getScale(), 7);
            Color base = palette.getBackgroundColor(ColorType.DARK);
            nvg.drawRoundedRect(getX() - 2.0F, getY() - 2.0F, drawWidth + 4.0F, drawHeight + 4.0F,
                    10.0F * getScale(), new Color(base.getRed(), base.getGreen(), base.getBlue(), 190));
            if(textureId != 0 && frame != null) {
                nvg.drawRoundedImage(textureId, getX(), getY(), drawWidth, drawHeight, 8.0F * getScale());
            } else {
                nvg.drawRoundedRect(getX(), getY(), drawWidth, drawHeight, 8.0F * getScale(), new Color(7, 10, 18, 220));
                nvg.drawCenteredText("YouTube PiP", getX() + drawWidth / 2.0F,
                        getY() + drawHeight / 2.0F - 7.0F * getScale(), Color.WHITE,
                        12.0F * getScale(), Fonts.SEMIBOLD);
            }
            nvg.drawGradientOutlineRoundedRect(getX() - 1.0F, getY() - 1.0F, drawWidth + 2.0F, drawHeight + 2.0F,
                    9.0F * getScale(), Math.max(0.7F, getScale()),
                    ColorUtils.applyAlpha(accent.getColor1(), 190), ColorUtils.applyAlpha(accent.getColor2(), 190));
            if(manager.isPaused() && !isEditing()) {
                nvg.drawRoundedRect(getX(), getY(), drawWidth, drawHeight, 8.0F * getScale(), new Color(0, 0, 0, 75));
                nvg.drawCenteredText("Paused", getX() + drawWidth / 2.0F,
                        getY() + drawHeight / 2.0F - 5.0F * getScale(), Color.WHITE,
                        10.0F * getScale(), Fonts.SEMIBOLD);
            }
            if(isEditing()) drawEditorHint(nvg, drawWidth, drawHeight, quality);
        });
    }

    private void drawEditorHint(NanoVGManager nvg, float width, float height, int quality) {
        float hintHeight = Math.max(16.0F, 22.0F * getScale());
        nvg.drawRoundedRect(getX() + 4.0F, getY() + height - hintHeight - 4.0F,
                width - 8.0F, hintHeight, 5.0F * getScale(), new Color(0, 0, 0, 150));
        nvg.drawCenteredText("Right click: " + quality + "p  •  Scroll: size  •  Drag: move",
                getX() + width / 2.0F, getY() + height - hintHeight + 1.0F,
                Color.WHITE, Math.max(6.5F, 8.0F * getScale()), Fonts.MEDIUM);
    }

    private void uploadFrame(byte[] frame, int width, int height) {
        if(textureId == 0 || width != textureWidth || height != textureHeight) createTexture(width, height);
        if(uploadedFrame == frame) return;
        frameBuffer.clear();
        frameBuffer.put(frame);
        frameBuffer.flip();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, width, height,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, frameBuffer);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        uploadedFrame = frame;
    }

    private void createTexture(int width, int height) {
        textureId = GL11.glGenTextures();
        textureWidth = width;
        textureHeight = height;
        frameBuffer = BufferUtils.createByteBuffer(width * height * 4);
        uploadedFrame = null;
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    public int getQualityHeight() {
        TranslateText selected = qualitySetting.getOption().getTranslate();
        if(selected == TranslateText.QUALITY_360P) return 360;
        if(selected == TranslateText.QUALITY_720P) return 720;
        return 480;
    }

    public void cycleQuality() {
        ArrayList<Option> options = qualitySetting.getOptions();
        int next = (options.indexOf(qualitySetting.getOption()) + 1) % options.size();
        qualitySetting.setOption(options.get(next));
        YouTubeManager manager = Glide.getInstance().getYouTubeManager();
        if(manager != null) manager.setQualityHeight(getQualityHeight());
    }

    public static YouTubePipMod getInstance() { return instance; }
}
