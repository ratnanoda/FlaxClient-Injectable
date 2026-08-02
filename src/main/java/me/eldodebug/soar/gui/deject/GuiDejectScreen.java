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

    private static final long FADE_NANOS = 1_500_000_000L;
    private static final long HOLD_END_NANOS = 2_500_000_000L;
    private static final long TOTAL_NANOS = 4_000_000_000L;

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
        final float enter = smootherStep(saturate(elapsed / (float) FADE_NANOS));
        final float exit = smootherStep(saturate(
                (elapsed - HOLD_END_NANOS) / (float) (TOTAL_NANOS - HOLD_END_NANOS)));
        final float visibility = Math.max(0.0F, enter * (1.0F - exit));

        if(visibility > 0.001F) {
            try {
                BlurUtils.drawBlurScreen(0.8F + visibility * 18.2F);
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
                            new Color(4, 7, 14, (int) (visibility * 76.0F)));

                    float centerX = resolution.getScaledWidth() / 2.0F;
                    float centerY = resolution.getScaledHeight() / 2.0F - 7.0F;
                    float textX = centerX - (1.0F - enter) * 150.0F + exit * 175.0F;

                    nvg.save();
                    nvg.setAlpha(visibility);
                    nvg.drawCenteredText("Thank you for using FlaxClient!",
                            textX, centerY, Color.WHITE, 18.0F, Fonts.SEMIBOLD);
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

    private static float smootherStep(float value) {
        return value * value * value * (value * (value * 6.0F - 15.0F) + 10.0F);
    }
}
