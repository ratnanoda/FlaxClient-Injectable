package me.eldodebug.soar.gui.deject;

import java.awt.Color;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.attach.DejectBridge;
import me.eldodebug.soar.attach.LateLoadStatus;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.utils.render.BlurUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;

public final class GuiDejectScreen extends GuiScreen {

    private static final long BLUR_IN_NANOS = 500_000_000L;
    private static final long HOLD_END_NANOS = 1_500_000_000L;
    private static final long TOTAL_NANOS = 2_000_000_000L;

    private long startedAt;
    private boolean queued;

    @Override
    public void initGui() {
        startedAt = System.nanoTime();
        queued = false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if(startedAt == 0L) {
            startedAt = System.nanoTime();
        }
        long elapsed = Math.max(0L, System.nanoTime() - startedAt);
        final float enter = easeOutCubic(saturate(elapsed / (float) BLUR_IN_NANOS));
        final float exit = easeInCubic(saturate(
                (elapsed - HOLD_END_NANOS) / (float) (TOTAL_NANOS - HOLD_END_NANOS)));
        final float visibility = Math.max(0.0F, enter * (1.0F - exit));

        if(visibility > 0.001F) {
            try {
                BlurUtils.drawBlurScreen(1.0F + visibility * 19.0F);
            } catch(Throwable ignored) {
            }
        }

        final ScaledResolution resolution = new ScaledResolution(mc);
        final NanoVGManager nvg = Glide.getInstance().getNanoVGManager();
        if(nvg != null) {
            nvg.setupAndDraw(new Runnable() {
                @Override
                public void run() {
                    nvg.drawRect(0, 0, resolution.getScaledWidth(), resolution.getScaledHeight(),
                            new Color(4, 7, 14, (int) (visibility * 82.0F)));
                    float centerX = resolution.getScaledWidth() / 2.0F;
                    float centerY = resolution.getScaledHeight() / 2.0F - 7.0F;
                    float textX = centerX - (1.0F - enter) * 180.0F + exit * 220.0F;
                    nvg.save();
                    nvg.setAlpha(visibility);
                    nvg.drawCenteredText("Thank you for using FlaxClient!",
                            textX, centerY, Color.WHITE, 18.0F, Fonts.SEMIBOLD);
                    nvg.drawHorizontalGradientRect(textX - 104.0F, centerY + 30.0F,
                            104.0F, 1.5F,
                            new Color(255, 255, 255, 0),
                            new Color(255, 255, 255, 175));
                    nvg.drawHorizontalGradientRect(textX, centerY + 30.0F,
                            104.0F, 1.5F,
                            new Color(255, 255, 255, 175),
                            new Color(255, 255, 255, 0));
                    nvg.restore();
                }
            });
        }

        if(elapsed >= TOTAL_NANOS && !queued) {
            queued = true;
            mc.addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    LateLoadStatus.beginDeject();
                    mc.displayGuiScreen(null);
                    DejectBridge.requestDeject();
                }
            });
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static float saturate(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static float easeOutCubic(float value) {
        float inverse = 1.0F - value;
        return 1.0F - inverse * inverse * inverse;
    }

    private static float easeInCubic(float value) {
        return value * value * value;
    }
}
