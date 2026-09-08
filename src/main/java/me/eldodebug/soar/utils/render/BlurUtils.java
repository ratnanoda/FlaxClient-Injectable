package me.eldodebug.soar.utils.render;

import java.util.List;

import me.eldodebug.soar.attach.MinecraftAccess;
import me.eldodebug.soar.logger.GlideLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.util.ResourceLocation;

/** Full-screen menu blur with resize-safe shader ownership and GL isolation. */
public final class BlurUtils {

    private static final Minecraft MC = Minecraft.getMinecraft();
    private static ShaderGroup blurShader;
    private static Framebuffer sourceFramebuffer;
    private static int displayWidth = -1;
    private static int displayHeight = -1;
    private static int scaleFactor = -1;
    private static boolean failureReported;

    private BlurUtils() {}

    private static boolean ensureShader(ScaledResolution resolution) {
        Framebuffer currentSource = MC.getFramebuffer();
        boolean stale = blurShader == null
                || sourceFramebuffer != currentSource
                || displayWidth != MC.displayWidth
                || displayHeight != MC.displayHeight
                || scaleFactor != resolution.getScaleFactor();
        if (!stale) return true;

        disposeShader();
        try {
            blurShader = new ShaderGroup(MC.getTextureManager(), MC.getResourceManager(),
                    currentSource, new ResourceLocation("shaders/post/blurArea.json"));
            blurShader.createBindFramebuffers(MC.displayWidth, MC.displayHeight);
            sourceFramebuffer = currentSource;
            displayWidth = MC.displayWidth;
            displayHeight = MC.displayHeight;
            scaleFactor = resolution.getScaleFactor();
            failureReported = false;
            return true;
        } catch (Exception e) {
            disposeShader();
            if (!failureReported) {
                GlideLogger.error("Menu blur initialization failed; blur will be skipped", e);
                failureReported = true;
            }
            return false;
        }
    }

    private static void disposeShader() {
        if (blurShader != null) {
            blurShader.deleteShaderGroup();
            blurShader = null;
        }
        sourceFramebuffer = null;
        displayWidth = displayHeight = scaleFactor = -1;
    }

    public static void drawBlurScreen(float radius) {
        ScaledResolution resolution = new ScaledResolution(MC);
        final float safeRadius = Math.max(1.0F, Math.min(radius, 40.0F));
        RenderStateGuard.runIsolated(() -> {
            if (!ensureShader(resolution)) return;
            try {
                List<Shader> passes = MinecraftAccess.getShaders(blurShader);
                if (passes == null || passes.size() < 2) return;

                float coordinateScale = resolution.getScaleFactor() / 2.0F;
                float blurWidth = resolution.getScaledWidth() * coordinateScale;
                float blurHeight = resolution.getScaledHeight() * coordinateScale;
                for (int i = 0; i < 2; i++) {
                    setUniform(passes.get(i), "BlurXY", 0.0F, 0.0F);
                    setUniform(passes.get(i), "BlurCoord", blurWidth, blurHeight);
                    setUniform(passes.get(i), "Radius", safeRadius);
                }
                blurShader.loadShaderGroup(MinecraftAccess.getTimer(MC).renderPartialTicks);
            } catch (RuntimeException e) {
                disposeShader();
                if (!failureReported) {
                    GlideLogger.error("Menu blur render failed; the shader will be recreated", e);
                    failureReported = true;
                }
            }
        });
    }

    private static void setUniform(Shader pass, String name, float value) {
        ShaderUniform uniform = pass.getShaderManager().getShaderUniform(name);
        if (uniform != null) uniform.set(value);
    }

    private static void setUniform(Shader pass, String name, float x, float y) {
        ShaderUniform uniform = pass.getShaderManager().getShaderUniform(name);
        if (uniform != null) uniform.set(x, y);
    }
}
