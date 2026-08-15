package me.eldodebug.soar.management.mods.impl;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.EXTFramebufferBlit;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.vector.Matrix4f;

import me.eldodebug.soar.logger.GlideLogger;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

/**
 * One-pass, depth-aware fog for both supported client runtimes.
 *
 * <p>The vanilla framebuffer owns its depth buffer as a renderbuffer, so it
 * cannot be sampled directly.  This class first blits the already rendered
 * colour and depth into an independent framebuffer with a depth texture, then
 * samples those two textures while writing the final image back to Minecraft's
 * framebuffer.  Keeping the input textures separate avoids a framebuffer
 * feedback loop and, unlike the previous approach, never places fog planes in
 * world space.</p>
 */
public final class DepthFogPostProcessor {

    private static final String VERTEX_SHADER = "soar/shaders/depth_fog.vert";
    private static final String FRAGMENT_SHADER = "soar/shaders/depth_fog.frag";

    private static final int BLIT_UNAVAILABLE = 0;
    private static final int BLIT_CORE = 1;
    private static final int BLIT_ARB = 2;
    private static final int BLIT_EXT = 3;

    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final FloatBuffer PROJECTION_BUFFER = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer MODEL_VIEW_BUFFER = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer INVERSE_PROJECTION_BUFFER = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer INVERSE_MODEL_VIEW_BUFFER = BufferUtils.createFloatBuffer(16);
    private static final IntBuffer VIEWPORT_BUFFER = BufferUtils.createIntBuffer(16);
    private static final Matrix4f PROJECTION_MATRIX = new Matrix4f();
    private static final Matrix4f MODEL_VIEW_MATRIX = new Matrix4f();
    private static final Matrix4f INVERSE_PROJECTION_MATRIX = new Matrix4f();
    private static final Matrix4f INVERSE_MODEL_VIEW_MATRIX = new Matrix4f();
    private static final long START_NANOS = System.nanoTime();

    private static int scratchFramebuffer = -1;
    private static int sceneTexture = -1;
    private static int depthTexture = -1;
    private static int resourceWidth = -1;
    private static int resourceHeight = -1;

    private static int shaderProgram = -1;
    private static int sceneTextureUniform = -1;
    private static int depthTextureUniform = -1;
    private static int inverseProjectionUniform = -1;
    private static int inverseModelViewUniform = -1;
    private static int cameraPositionUniform = -1;
    private static int fogColorUniform = -1;
    private static int fogStartUniform = -1;
    private static int fogEndUniform = -1;
    private static int fogDensityUniform = -1;
    private static int ambientFogUniform = -1;
    private static int timeUniform = -1;

    private static boolean disabledForSession;
    private static boolean failureReported;

    private DepthFogPostProcessor() {
    }

    /**
     * Executes the shared post-process after the world has drawn but before
     * the first-person hand and HUD are drawn.
     */
    public static void render(ShaderMod shaderMod, float partialTicks) {
        if (shaderMod == null || !shaderMod.isForgeFogEnabled() || !isPostProcessAvailable()) {
            return;
        }

        Entity viewEntity = MC.getRenderViewEntity();
        if (viewEntity == null || isInsideLiquid(viewEntity)) {
            return;
        }

        Framebuffer mainFramebuffer = MC.getFramebuffer();
        if (mainFramebuffer == null || mainFramebuffer.framebufferWidth <= 0
                || mainFramebuffer.framebufferHeight <= 0) {
            return;
        }

        try {
            if (!ensureResources(mainFramebuffer.framebufferWidth, mainFramebuffer.framebufferHeight)) {
                return;
            }
            if (!ensureShaderProgram()) {
                return;
            }
            if (!captureInverseMatrices()) {
                disable("Unable to invert the world render matrices for depth fog", null);
                return;
            }
            if (!copySceneBuffers(mainFramebuffer)) {
                return;
            }

            drawPostProcess(mainFramebuffer, shaderMod, viewEntity, partialTicks);
        } catch (Exception exception) {
            disable("Depth fog post-process failed and was disabled for this session", exception);
            safelyBindMainFramebuffer();
        }
    }

    /**
     * Lets the Forge listener retain vanilla land fog if this GPU cannot run
     * the post-process or a shader/FBO error has already occurred.
     */
    public static boolean isPostProcessAvailable() {
        if (disabledForSession) {
            return false;
        }

        try {
            return OpenGlHelper.isFramebufferEnabled()
                    && OpenGlHelper.areShadersSupported()
                    && getBlitBackend() != BLIT_UNAVAILABLE;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    /** Frees GL resources when the Shader module is disabled. */
    public static void release() {
        try {
            deleteFrameResources();
            if (shaderProgram >= 0) {
                GL20.glDeleteProgram(shaderProgram);
            }
        } catch (RuntimeException ignored) {
            // The client may already have destroyed its OpenGL context.
        } finally {
            shaderProgram = -1;
            resetUniformLocations();
            disabledForSession = false;
            failureReported = false;
        }
    }

    private static boolean isInsideLiquid(Entity entity) {
        return entity.isInsideOfMaterial(Material.water) || entity.isInsideOfMaterial(Material.lava);
    }

    private static int getBlitBackend() {
        try {
            ContextCapabilities capabilities = GLContext.getCapabilities();
            if (capabilities.OpenGL30) {
                return BLIT_CORE;
            }
            if (capabilities.GL_ARB_framebuffer_object) {
                return BLIT_ARB;
            }
            if (capabilities.GL_EXT_framebuffer_object && capabilities.GL_EXT_framebuffer_blit) {
                return BLIT_EXT;
            }
        } catch (RuntimeException ignored) {
            // Called before an OpenGL context is active.
        }
        return BLIT_UNAVAILABLE;
    }

    private static boolean ensureResources(int width, int height) {
        if (scratchFramebuffer >= 0 && sceneTexture >= 0 && depthTexture >= 0
                && resourceWidth == width && resourceHeight == height) {
            return true;
        }

        deleteFrameResources();

        try {
            sceneTexture = createTexture(width, height, GL11.GL_RGBA, GL11.GL_RGBA,
                    GL11.GL_UNSIGNED_BYTE, GL11.GL_NEAREST);
            depthTexture = createTexture(width, height, GL14.GL_DEPTH_COMPONENT24,
                    GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, GL11.GL_NEAREST);

            scratchFramebuffer = OpenGlHelper.glGenFramebuffers();
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, scratchFramebuffer);
            OpenGlHelper.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER,
                    OpenGlHelper.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, sceneTexture, 0);
            OpenGlHelper.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER,
                    OpenGlHelper.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, depthTexture, 0);

            int status = OpenGlHelper.glCheckFramebufferStatus(OpenGlHelper.GL_FRAMEBUFFER);
            if (status != OpenGlHelper.GL_FRAMEBUFFER_COMPLETE) {
                throw new IllegalStateException("scratch framebuffer status " + status);
            }

            resourceWidth = width;
            resourceHeight = height;
            return true;
        } catch (RuntimeException exception) {
            deleteFrameResources();
            disable("Unable to allocate depth fog framebuffer resources", exception);
            return false;
        } finally {
            safelyBindMainFramebuffer();
        }
    }

    private static int createTexture(int width, int height, int internalFormat,
            int format, int type, int filter) {
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        if (format == GL11.GL_DEPTH_COMPONENT) {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, GL11.GL_NONE);
        }
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, width, height, 0,
                format, type, (ByteBuffer) null);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
        return texture;
    }

    /** Blits colour plus depth from the live Minecraft FBO into independent textures. */
    private static boolean copySceneBuffers(Framebuffer mainFramebuffer) {
        int mask = GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT;
        try {
            switch (getBlitBackend()) {
                case BLIT_CORE:
                    GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mainFramebuffer.framebufferObject);
                    GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, scratchFramebuffer);
                    GL30.glBlitFramebuffer(0, 0, resourceWidth, resourceHeight,
                            0, 0, resourceWidth, resourceHeight, mask, GL11.GL_NEAREST);
                    break;
                case BLIT_ARB:
                    ARBFramebufferObject.glBindFramebuffer(ARBFramebufferObject.GL_READ_FRAMEBUFFER,
                            mainFramebuffer.framebufferObject);
                    ARBFramebufferObject.glBindFramebuffer(ARBFramebufferObject.GL_DRAW_FRAMEBUFFER,
                            scratchFramebuffer);
                    ARBFramebufferObject.glBlitFramebuffer(0, 0, resourceWidth, resourceHeight,
                            0, 0, resourceWidth, resourceHeight, mask, GL11.GL_NEAREST);
                    break;
                case BLIT_EXT:
                    EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferBlit.GL_READ_FRAMEBUFFER_EXT,
                            mainFramebuffer.framebufferObject);
                    EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferBlit.GL_DRAW_FRAMEBUFFER_EXT,
                            scratchFramebuffer);
                    EXTFramebufferBlit.glBlitFramebufferEXT(0, 0, resourceWidth, resourceHeight,
                            0, 0, resourceWidth, resourceHeight, mask, GL11.GL_NEAREST);
                    break;
                default:
                    disable("Depth fog requires framebuffer blit support", null);
                    return false;
            }
            return true;
        } catch (RuntimeException exception) {
            disable("Unable to copy the rendered scene depth for fog", exception);
            return false;
        } finally {
            mainFramebuffer.bindFramebuffer(true);
        }
    }

    private static boolean ensureShaderProgram() {
        if (shaderProgram >= 0) {
            return true;
        }

        int vertexShader = -1;
        int fragmentShader = -1;
        int program = -1;
        try {
            vertexShader = compileShader(VERTEX_SHADER, GL20.GL_VERTEX_SHADER);
            fragmentShader = compileShader(FRAGMENT_SHADER, GL20.GL_FRAGMENT_SHADER);
            program = GL20.glCreateProgram();
            GL20.glAttachShader(program, vertexShader);
            GL20.glAttachShader(program, fragmentShader);
            GL20.glLinkProgram(program);

            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                throw new IOException("Shader link error: "
                        + GL20.glGetProgramInfoLog(program, GL20.GL_INFO_LOG_LENGTH));
            }

            shaderProgram = program;
            program = -1;
            cacheUniformLocations();
            return true;
        } catch (Exception exception) {
            disable("Unable to compile the depth fog shader", exception);
            return false;
        } finally {
            if (vertexShader >= 0) {
                GL20.glDeleteShader(vertexShader);
            }
            if (fragmentShader >= 0) {
                GL20.glDeleteShader(fragmentShader);
            }
            if (program >= 0) {
                GL20.glDeleteProgram(program);
            }
        }
    }

    private static int compileShader(String resourcePath, int type) throws IOException {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, readResource(resourcePath));
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String infoLog = GL20.glGetShaderInfoLog(shader, GL20.GL_INFO_LOG_LENGTH);
            GL20.glDeleteShader(shader);
            throw new IOException("Shader compile error in " + resourcePath + ": " + infoLog);
        }
        return shader;
    }

    private static String readResource(String resourcePath) throws IOException {
        InputStream input = MC.getResourceManager().getResource(new ResourceLocation(resourcePath)).getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        StringBuilder source = new StringBuilder();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                source.append(line).append('\n');
            }
        } finally {
            reader.close();
        }
        return source.toString();
    }

    private static void cacheUniformLocations() {
        sceneTextureUniform = GL20.glGetUniformLocation(shaderProgram, "SceneTexture");
        depthTextureUniform = GL20.glGetUniformLocation(shaderProgram, "DepthTexture");
        inverseProjectionUniform = GL20.glGetUniformLocation(shaderProgram, "InvProjection");
        inverseModelViewUniform = GL20.glGetUniformLocation(shaderProgram, "InvModelView");
        cameraPositionUniform = GL20.glGetUniformLocation(shaderProgram, "CameraPosition");
        fogColorUniform = GL20.glGetUniformLocation(shaderProgram, "FogColor");
        fogStartUniform = GL20.glGetUniformLocation(shaderProgram, "FogStart");
        fogEndUniform = GL20.glGetUniformLocation(shaderProgram, "FogEnd");
        fogDensityUniform = GL20.glGetUniformLocation(shaderProgram, "FogDensity");
        ambientFogUniform = GL20.glGetUniformLocation(shaderProgram, "AmbientFog");
        timeUniform = GL20.glGetUniformLocation(shaderProgram, "Time");
    }

    private static void resetUniformLocations() {
        sceneTextureUniform = -1;
        depthTextureUniform = -1;
        inverseProjectionUniform = -1;
        inverseModelViewUniform = -1;
        cameraPositionUniform = -1;
        fogColorUniform = -1;
        fogStartUniform = -1;
        fogEndUniform = -1;
        fogDensityUniform = -1;
        ambientFogUniform = -1;
        timeUniform = -1;
    }

    private static boolean captureInverseMatrices() {
        PROJECTION_BUFFER.clear();
        MODEL_VIEW_BUFFER.clear();
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, PROJECTION_BUFFER);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MODEL_VIEW_BUFFER);

        PROJECTION_BUFFER.rewind();
        MODEL_VIEW_BUFFER.rewind();
        PROJECTION_MATRIX.load(PROJECTION_BUFFER);
        MODEL_VIEW_MATRIX.load(MODEL_VIEW_BUFFER);

        if (Matrix4f.invert(PROJECTION_MATRIX, INVERSE_PROJECTION_MATRIX) == null
                || Matrix4f.invert(MODEL_VIEW_MATRIX, INVERSE_MODEL_VIEW_MATRIX) == null) {
            return false;
        }

        INVERSE_PROJECTION_BUFFER.clear();
        INVERSE_PROJECTION_MATRIX.store(INVERSE_PROJECTION_BUFFER);
        INVERSE_PROJECTION_BUFFER.flip();
        INVERSE_MODEL_VIEW_BUFFER.clear();
        INVERSE_MODEL_VIEW_MATRIX.store(INVERSE_MODEL_VIEW_BUFFER);
        INVERSE_MODEL_VIEW_BUFFER.flip();
        return true;
    }

    private static void drawPostProcess(Framebuffer mainFramebuffer, ShaderMod shaderMod,
            Entity viewEntity, float partialTicks) {
        VIEWPORT_BUFFER.clear();
        GL11.glGetInteger(GL11.GL_VIEWPORT, VIEWPORT_BUFFER);
        int viewportX = VIEWPORT_BUFFER.get(0);
        int viewportY = VIEWPORT_BUFFER.get(1);
        int viewportWidth = VIEWPORT_BUFFER.get(2);
        int viewportHeight = VIEWPORT_BUFFER.get(3);
        int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int previousActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int previousTexture0;
        int previousTexture1;
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        previousTexture0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        previousTexture1 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL13.glActiveTexture(previousActiveTexture);

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            mainFramebuffer.bindFramebuffer(true);
            GL11.glViewport(0, 0, resourceWidth, resourceHeight);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_FOG);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

            GL20.glUseProgram(shaderProgram);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, sceneTexture);
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTexture);

            uploadUniforms(shaderMod, viewEntity, partialTicks);
            drawFullscreenQuad();
        } finally {
            GL11.glPopAttrib();
            GL20.glUseProgram(previousProgram);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture0);
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture1);
            GL13.glActiveTexture(previousActiveTexture);
            GL11.glViewport(viewportX, viewportY, viewportWidth, viewportHeight);
            mainFramebuffer.bindFramebuffer(true);
        }
    }

    private static void uploadUniforms(ShaderMod shaderMod, Entity viewEntity, float partialTicks) {
        GL20.glUniform1i(sceneTextureUniform, 0);
        GL20.glUniform1i(depthTextureUniform, 1);
        INVERSE_PROJECTION_BUFFER.rewind();
        GL20.glUniformMatrix4(inverseProjectionUniform, false, INVERSE_PROJECTION_BUFFER);
        INVERSE_MODEL_VIEW_BUFFER.rewind();
        GL20.glUniformMatrix4(inverseModelViewUniform, false, INVERSE_MODEL_VIEW_BUFFER);

        double cameraX = interpolate(viewEntity.prevPosX, viewEntity.posX, partialTicks);
        double cameraY = interpolate(viewEntity.prevPosY, viewEntity.posY, partialTicks) + viewEntity.getEyeHeight();
        double cameraZ = interpolate(viewEntity.prevPosZ, viewEntity.posZ, partialTicks);
        if (MC.getRenderManager() != null) {
            cameraX = MC.getRenderManager().viewerPosX;
            cameraY = MC.getRenderManager().viewerPosY;
            cameraZ = MC.getRenderManager().viewerPosZ;
        }
        GL20.glUniform3f(cameraPositionUniform, (float) cameraX, (float) cameraY, (float) cameraZ);

        Color color = shaderMod.getFogColor();
        if (color == null) {
            color = new Color(209, 214, 214);
        }
        GL20.glUniform3f(fogColorUniform, color.getRed() / 255.0F,
                color.getGreen() / 255.0F, color.getBlue() / 255.0F);
        GL20.glUniform1f(fogStartUniform, 30.0F);
        GL20.glUniform1f(fogEndUniform, shaderMod.getFogEndDistance());
        GL20.glUniform1f(fogDensityUniform, shaderMod.getFogDensity());
        GL20.glUniform1f(ambientFogUniform, shaderMod.isAmbientFogEnabled() ? 1.0F : 0.0F);
        GL20.glUniform1f(timeUniform, (System.nanoTime() - START_NANOS) / 1_000_000_000.0F);
    }

    private static double interpolate(double previous, double current, float partialTicks) {
        return previous + (current - previous) * partialTicks;
    }

    private static void drawFullscreenQuad() {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0F, 0.0F);
        GL11.glVertex2f(-1.0F, -1.0F);
        GL11.glTexCoord2f(1.0F, 0.0F);
        GL11.glVertex2f(1.0F, -1.0F);
        GL11.glTexCoord2f(1.0F, 1.0F);
        GL11.glVertex2f(1.0F, 1.0F);
        GL11.glTexCoord2f(0.0F, 1.0F);
        GL11.glVertex2f(-1.0F, 1.0F);
        GL11.glEnd();
    }

    private static void deleteFrameResources() {
        if (scratchFramebuffer >= 0) {
            OpenGlHelper.glDeleteFramebuffers(scratchFramebuffer);
        }
        if (sceneTexture >= 0) {
            GL11.glDeleteTextures(sceneTexture);
        }
        if (depthTexture >= 0) {
            GL11.glDeleteTextures(depthTexture);
        }
        scratchFramebuffer = -1;
        sceneTexture = -1;
        depthTexture = -1;
        resourceWidth = -1;
        resourceHeight = -1;
    }

    private static void safelyBindMainFramebuffer() {
        try {
            Framebuffer mainFramebuffer = MC.getFramebuffer();
            if (mainFramebuffer != null) {
                mainFramebuffer.bindFramebuffer(true);
            }
        } catch (RuntimeException ignored) {
            // There is no active display context during shutdown.
        }
    }

    private static void disable(String message, Exception exception) {
        disabledForSession = true;
        if (failureReported) {
            return;
        }
        failureReported = true;
        if (exception == null) {
            GlideLogger.warn(message);
        } else {
            GlideLogger.error(message, exception);
        }
    }
}
