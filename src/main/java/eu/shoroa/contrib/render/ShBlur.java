/*
 * Nanovg Blur
 * © Shoroa 2025, All Rights Reserved
 */

package eu.shoroa.contrib.render;

import eu.shoroa.contrib.shader.UIShader;
import eu.shoroa.contrib.shader.uniform.Uniform;
import me.eldodebug.soar.Glide;
import me.eldodebug.soar.logger.GlideLogger;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.impl.InternalSettingsMod;
import me.eldodebug.soar.management.mods.settings.impl.ComboSetting;
import me.eldodebug.soar.management.mods.settings.impl.combo.Option;
import me.eldodebug.soar.utils.render.RenderStateGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.Util;
import org.lwjgl.BufferUtils;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL2;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.nio.FloatBuffer;

public class ShBlur {
    // nanovg_gl.h flag; older LWJGL bindings do not expose it publicly.
    private static final int NVG_IMAGE_NODELETE = 1 << 16;
    private static final ShBlur instance = new ShBlur();

    public static ShBlur getInstance() {
        return instance;
    }

    private int nvgImage = -1;
    private final Minecraft mc = Minecraft.getMinecraft();
    private Framebuffer framebuffer;
    private Framebuffer framebuffer1;
    private Framebuffer framebuffer2;
    private Framebuffer framebuffer3;
    private FloatBuffer weightBuffer = BufferUtils.createFloatBuffer(128);
    private UIShader shader;
    private long lastUpdate = System.currentTimeMillis();
    private float radius = 4f;
    private int displayWidth = -1;
    private int displayHeight = -1;
    private boolean initialized;
    private boolean failureReported;

    public synchronized void init() {
        if (initialized) {
            ensureSize();
            return;
        }
        try {
            shader = new UIShader("soar/shaders/vertex.vert", "soar/shaders/blur.frag");
            shader.init();
            createFramebuffers();
            cacheRadius(radius);
            initialized = true;
            failureReported = false;
        } catch (Exception e) {
            initialized = false;
            if (!failureReported) {
                GlideLogger.error("UI blur initialization failed; blur will be skipped", e);
                failureReported = true;
            }
        }
    }

    public void cacheRadius(float radius) {
        weightBuffer = BufferUtils.createFloatBuffer(128);
        int kernelRadius = Math.max(1, Math.min(127, Math.round(radius)));
        for (int i = 0; i <= kernelRadius; i++) {
            weightBuffer.put(gauss((float) i, radius / 2f));
        }
        weightBuffer.flip();
    }

    public synchronized void resize() {
        if (!initialized) return;
        deleteFramebuffers();
        createFramebuffers();
    }

    private void ensureSize() {
        if (displayWidth != mc.displayWidth || displayHeight != mc.displayHeight) resize();
    }

    private void createFramebuffers() {
        int halfWidth = Math.max(1, mc.displayWidth / 2);
        int halfHeight = Math.max(1, mc.displayHeight / 2);
        int sixthWidth = Math.max(1, mc.displayWidth / 6);
        int sixthHeight = Math.max(1, mc.displayHeight / 6);
        framebuffer = new Framebuffer(halfWidth, halfHeight, false);
        framebuffer1 = new Framebuffer(halfWidth, halfHeight, false);
        framebuffer2 = new Framebuffer(sixthWidth, sixthHeight, false);
        framebuffer3 = new Framebuffer(sixthWidth, sixthHeight, false);
        framebuffer3.setFramebufferFilter(GL11.GL_LINEAR);
        displayWidth = mc.displayWidth;
        displayHeight = mc.displayHeight;
        nvgImage = -1;
    }

    private void deleteFramebuffers() {
        if (nvgImage != -1 && Glide.getInstance().getNanoVGManager() != null) {
            NanoVG.nvgDeleteImage(Glide.getInstance().getNanoVGManager().getContext(), nvgImage);
            nvgImage = -1;
        }
        if (framebuffer != null) framebuffer.deleteFramebuffer();
        if (framebuffer1 != null) framebuffer1.deleteFramebuffer();
        if (framebuffer2 != null) framebuffer2.deleteFramebuffer();
        if (framebuffer3 != null) framebuffer3.deleteFramebuffer();
        framebuffer = framebuffer1 = framebuffer2 = framebuffer3 = null;
    }

    private int nvgImageFromHandle(int texture, int width, int height) {
        return NanoVGGL2.nvglCreateImageFromHandle(Glide.getInstance().getNanoVGManager().getContext(),
                texture, width, height, NanoVG.NVG_IMAGE_FLIPY | NVG_IMAGE_NODELETE);
    }

    public void render() {
        if (!InternalSettingsMod.getInstance().getBlurSetting().isToggled()) return;
        if(Util.getOSType() == Util.EnumOS.OSX) return;
        try {
            RenderStateGuard.runIsolated(this::renderInternal);
        } catch (RuntimeException e) {
            if (!failureReported) {
                GlideLogger.error("UI blur render failed; blur will be skipped for this frame", e);
                failureReported = true;
            }
        }
    }

    private void renderInternal() {
        if (!initialized) init();
        if (!initialized) return;
        ensureSize();
        ScaledResolution sr = new ScaledResolution(mc);
        if (System.currentTimeMillis() - lastUpdate > 15) {
            lastUpdate = System.currentTimeMillis();
            try {
                shader.attach();
                shader.uniform(Uniform.makeInt("radius", Math.max(1, Math.min(127, Math.round(radius)))));
                weightBuffer.rewind();
                shader.uniform(Uniform.makeFloatBuffer("kernels", weightBuffer));
                shader.uniform(Uniform.makeInt("ignoreAlpha", 1));

                framebuffer.bindFramebuffer(true);
                renderPass(mc.getFramebuffer().framebufferTexture, 1f, 0f,
                        mc.getFramebuffer().framebufferWidth, mc.getFramebuffer().framebufferHeight, sr);
                framebuffer1.bindFramebuffer(true);
                renderPass(framebuffer.framebufferTexture, 0f, 1f,
                        framebuffer.framebufferWidth, framebuffer.framebufferHeight, sr);
                framebuffer2.bindFramebuffer(true);
                renderPass(framebuffer1.framebufferTexture, 1f, 0f,
                        framebuffer1.framebufferWidth, framebuffer1.framebufferHeight, sr);
                framebuffer3.bindFramebuffer(true);
                renderPass(framebuffer2.framebufferTexture, 0f, 1f,
                        framebuffer2.framebufferWidth, framebuffer2.framebufferHeight, sr);
            } finally {
                shader.detach();
            }
        }
        if (nvgImage == -1) {
            nvgImage = nvgImageFromHandle(framebuffer3.framebufferTexture,
                    framebuffer3.framebufferWidth, framebuffer3.framebufferHeight);
        }
    }

    private void renderPass(int texture, float directionX, float directionY,
                            int sourceWidth, int sourceHeight, ScaledResolution sr) {
        bindTexture(texture, 10);
        shader.uniform(Uniform.makeInt("texture", 10));
        shader.uniform(Uniform.makeVec2("direction", directionX, directionY));
        shader.uniform(Uniform.makeVec2("texelSize",
                1f / Math.max(1, sourceWidth), 1f / Math.max(1, sourceHeight)));
        shader.rect(0f, 0f, sr.getScaledWidth(), sr.getScaledHeight());
    }

    private void bindTexture(int texture, int id) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + id);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
    }

    private float gauss(float x, float sigma) {
        double PI = 3.141592653;
        double output = 1.0 / Math.sqrt(2.0 * PI * (sigma * sigma));
        return (float) (output * Math.exp(-(x * x) / (2.0 * (sigma * sigma))));
    }

    public void drawBlur(float x, float y, float w, float h, float radius) {
        if (!InternalSettingsMod.getInstance().getBlurSetting().isToggled()) return;
        if(Util.getOSType() == Util.EnumOS.OSX) return;
        if (nvgImage == -1) return;
        long ctx = Glide.getInstance().getNanoVGManager().getContext();
        ScaledResolution sr = new ScaledResolution(mc);

        ComboSetting setting = InternalSettingsMod.getInstance().getModThemeSetting();
        Option theme = setting.getOption();
        boolean rectShape = theme.getTranslate().equals(TranslateText.RECT) || theme.getTranslate().equals(TranslateText.GRADIENT_SIMPLE);

        NVGPaint paint = NVGPaint.calloc();

        NanoVG.nvgBeginPath(ctx);
        if (rectShape) {
            NanoVG.nvgRect(ctx, x, y, w, h);
        } else {
            NanoVG.nvgRoundedRect(ctx, x, y, w, h, radius);
        }
        NanoVG.nvgImagePattern(ctx, 0f, 0f, sr.getScaledWidth(), sr.getScaledHeight(), 0f, nvgImage, 1f, paint);
        NanoVG.nvgFillPaint(ctx, paint);
        NanoVG.nvgFill(ctx);
        NanoVG.nvgClosePath(ctx);

        paint.free();
    }

    public void drawBlur(Runnable r) {
        if (!InternalSettingsMod.getInstance().getBlurSetting().isToggled()) return;
        if(Util.getOSType() == Util.EnumOS.OSX) return;
        if (nvgImage == -1) return;
        long ctx = Glide.getInstance().getNanoVGManager().getContext();
        ScaledResolution sr = new ScaledResolution(mc);
        NVGPaint paint = NVGPaint.calloc();
        NanoVG.nvgBeginPath(ctx);
        r.run();
        NanoVG.nvgImagePattern(ctx, 0f, 0f, sr.getScaledWidth(), sr.getScaledHeight(), 0f, nvgImage, 1f, paint);
        NanoVG.nvgFillPaint(ctx, paint);
        NanoVG.nvgFill(ctx);
        NanoVG.nvgClosePath(ctx);

        paint.free();
    }
}
