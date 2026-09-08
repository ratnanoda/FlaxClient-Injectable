package me.eldodebug.soar.utils.render;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;

/** Isolates third-party HUD rendering from the host client's OpenGL pipeline. */
public final class RenderStateGuard {

    private static final ThreadLocal<FloatBuffer> COLOR_BUFFER = new ThreadLocal<FloatBuffer>() {
        @Override
        protected FloatBuffer initialValue() {
            // LWJGL 2 validates glGetFloat buffers against the largest possible
            // result (a 4x4 matrix), even for the four-component CURRENT_COLOR.
            return BufferUtils.createFloatBuffer(16);
        }
    };

    private RenderStateGuard() {}

    public static void runIsolated(Runnable render) {
        StateSnapshot state = StateSnapshot.capture();
        int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int previousActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int previousMatrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        int previousFramebuffer = OpenGlHelper.framebufferSupported
                ? GL11.glGetInteger(ARBFramebufferObject.GL_FRAMEBUFFER_BINDING)
                : 0;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushClientAttrib(-1);
        pushMatrix(GL11.GL_TEXTURE);
        pushMatrix(GL11.GL_PROJECTION);
        pushMatrix(GL11.GL_MODELVIEW);
        GL11.glMatrixMode(previousMatrixMode);
        // Lunar may leave one of its compositor programs active at hook points.
        // Flax renderers either use fixed-function GL or bind their own program.
        GL20.glUseProgram(0);
        try {
            render.run();
        } finally {
            popMatrix(GL11.GL_MODELVIEW);
            popMatrix(GL11.GL_PROJECTION);
            popMatrix(GL11.GL_TEXTURE);
            GL11.glPopClientAttrib();
            GL11.glPopAttrib();

            if(OpenGlHelper.framebufferSupported) {
                OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, previousFramebuffer);
            }
            GL20.glUseProgram(previousProgram);
            GlStateManager.setActiveTexture(previousActiveTexture);
            state.restoreGlStateManagerCache();
            GlStateManager.matrixMode(previousMatrixMode);
        }
    }

    /**
     * glPopAttrib restores the driver, but it cannot restore Minecraft's private
     * GlStateManager cache.  A renderer such as FontRenderer can therefore leave
     * the cache disagreeing with OpenGL and the next hotbar/hand draw is skipped
     * or blended with stale state.  Re-applying the captured driver state through
     * GlStateManager brings both sides back into agreement.
     */
    private static final class StateSnapshot {
        private final boolean alpha;
        private final boolean blend;
        private final boolean cull;
        private final boolean depth;
        private final boolean lighting;
        private final boolean texture2D;
        private final boolean depthMask;
        private final int alphaFunc;
        private final float alphaRef;
        private final int depthFunc;
        private final int blendSrcRgb;
        private final int blendDstRgb;
        private final int blendSrcAlpha;
        private final int blendDstAlpha;
        private final int textureBinding;
        private final float red;
        private final float green;
        private final float blue;
        private final float alphaColor;

        private StateSnapshot() {
            alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
            blend = GL11.glIsEnabled(GL11.GL_BLEND);
            cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
            texture2D = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
            depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            alphaFunc = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
            alphaRef = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
            depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
            blendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
            blendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
            blendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
            blendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
            textureBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            FloatBuffer color = COLOR_BUFFER.get();
            color.clear();
            GL11.glGetFloat(GL11.GL_CURRENT_COLOR, color);
            red = color.get(0);
            green = color.get(1);
            blue = color.get(2);
            alphaColor = color.get(3);
        }

        static StateSnapshot capture() {
            return new StateSnapshot();
        }

        void restoreGlStateManagerCache() {
            setAlpha(alpha);
            setBlend(blend);
            setCull(cull);
            setDepth(depth);
            setLighting(lighting);
            setTexture(texture2D);
            GlStateManager.depthMask(depthMask);
            GlStateManager.alphaFunc(alphaFunc, alphaRef);
            GlStateManager.depthFunc(depthFunc);
            GlStateManager.tryBlendFuncSeparate(blendSrcRgb, blendDstRgb,
                    blendSrcAlpha, blendDstAlpha);
            GlStateManager.bindTexture(textureBinding);
            GlStateManager.color(red, green, blue, alphaColor);
        }

        private static void setAlpha(boolean enabled) {
            if(enabled) GlStateManager.enableAlpha(); else GlStateManager.disableAlpha();
        }

        private static void setBlend(boolean enabled) {
            if(enabled) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
        }

        private static void setCull(boolean enabled) {
            if(enabled) GlStateManager.enableCull(); else GlStateManager.disableCull();
        }

        private static void setDepth(boolean enabled) {
            if(enabled) GlStateManager.enableDepth(); else GlStateManager.disableDepth();
        }

        private static void setLighting(boolean enabled) {
            if(enabled) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
        }

        private static void setTexture(boolean enabled) {
            if(enabled) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
        }
    }

    private static void pushMatrix(int mode) {
        GL11.glMatrixMode(mode);
        GL11.glPushMatrix();
    }

    private static void popMatrix(int mode) {
        GL11.glMatrixMode(mode);
        GL11.glPopMatrix();
    }
}
