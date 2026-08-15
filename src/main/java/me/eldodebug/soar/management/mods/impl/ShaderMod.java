package me.eldodebug.soar.management.mods.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.lwjgl.opengl.GL11;

import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventRender3D;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.BooleanSetting;
import me.eldodebug.soar.management.mods.settings.impl.ColorSetting;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.world.World;

/**
 * Atmospheric world-space shader/effect module.
 *
 * - Renders a persistent field of animated geometric objects, network lines,
 *   tetrahedrons and cubes around the player (without standalone points).
 * - A shared depth-aware post-process adds the module's atmospheric fog.
 *
 * Object rendering intentionally remains on Minecraft's legacy OpenGL path;
 * fog itself is a separate GLSL post-process so it can affect the sky and all
 * rendered world pixels consistently.
 */
public class ShaderMod extends Mod {

    private static final int CHUNK_RADIUS = 2;
    private static final int MAX_OBJECTS_PER_CHUNK = 4;
    private static ShaderMod instance;

    private final ColorSetting fogColorSetting = new ColorSetting(
            TranslateText.FOG_COLOR,
            this,
            new Color(209, 214, 214),
            true);

    private final ColorSetting objectColorSetting = new ColorSetting(
            TranslateText.OBJECT_COLOR,
            this,
            new Color(104, 210, 255, 255),
            true);

    private final BooleanSetting fogEnabledSetting = new BooleanSetting(
            TranslateText.TOGGLE, this, true);

    /** Adds a visible, world-space mist even at the player's position. */
    private final BooleanSetting ambientFogSetting = new BooleanSetting(
            TranslateText.AMBIENT_FOG, this, false);

    /* Average object density per 16x16 chunk rather than a global count. */
    private final NumberSetting objectDensitySetting = new NumberSetting(
            TranslateText.DENSITY, this, 1.0D, 0.25D, 2.0D, false);

    /** 0% hides objects; 100% makes their primary outlines fully opaque. */
    private final NumberSetting objectOpacitySetting = new NumberSetting(
            TranslateText.OBJECT_OPACITY, this, 100.0D, 0.0D, 100.0D, true);

    /** Renders softly luminous faces inside cubes and tetrahedrons. */
    private final BooleanSetting objectFillSetting = new BooleanSetting(
            TranslateText.OBJECT_FILL, this, false);

    private final NumberSetting fogDistanceSetting = new NumberSetting(
            TranslateText.FOG_DISTANCE, this, 65.0D, 40.0D, 120.0D, true);

    /** Percentage strength; the shader still blends smoothly at its far end. */
    private final NumberSetting fogDensitySetting = new NumberSetting(
            TranslateText.FOG_DENSITY, this, 75.0D, 0.0D, 100.0D, true);

    private final NumberSetting objectScaleSetting = new NumberSetting(
            TranslateText.SCALE, this, 1.0D, 0.25D, 2.5D, false);

    private final NumberSetting lineWidthSetting = new NumberSetting(
            TranslateText.LINE_WIDTH, this, 1.15D, 0.5D, 4.0D, false);

    private final List<ShaderObject> objects = new ArrayList<ShaderObject>();
    private final Map<Long, List<ShaderObject>> objectsByChunk = new HashMap<Long, List<ShaderObject>>();

    private World sceneWorld;
    private long sceneStartNanos;
    private int visibleCenterChunkX = Integer.MIN_VALUE;
    private int visibleCenterChunkZ = Integer.MIN_VALUE;
    private double generatedDensity = Double.NaN;
    private AnimatedPosition[] animatedPositions = new AnimatedPosition[0];

    public ShaderMod() {
        super(TranslateText.SHADER, TranslateText.SHADER_DESCRIPTION, ModCategory.RENDER);
        instance = this;
    }

    public static ShaderMod getInstance() {
        return instance;
    }

    /** Used by the Forge-only listener without loading Forge classes here. */
    public boolean isForgeFogEnabled() {
        return isToggled() && fogEnabledSetting.isToggled();
    }

    public Color getFogColor() {
        return fogColorSetting.getColor();
    }

    /** Whether the optional near-player atmospheric mist is enabled. */
    public boolean isAmbientFogEnabled() {
        return ambientFogSetting.isToggled();
    }

    /** Used by Forge fog and the Mod Menu's inline slider. */
    public float getFogEndDistance() {
        return Math.max(40.0F, Math.min(120.0F, fogDistanceSetting.getValueFloat()));
    }

    public void setFogEndDistance(float distance) {
        fogDistanceSetting.setValue(Math.max(40.0F, Math.min(120.0F, distance)));
    }

    /** Returns the fog-strength slider in the shader-friendly 0..1 range. */
    public float getFogDensity() {
        return Math.max(0.0F, Math.min(1.0F, fogDensitySetting.getValueFloat() / 100.0F));
    }

    /** Used by the Forge GUI's live density slider. */
    public void setFogDensity(float density) {
        fogDensitySetting.setValue(Math.max(0.0F, Math.min(1.0F, density)) * 100.0F);
    }

    @Override
    public void onDisable() {
        DepthFogPostProcessor.release();
        super.onDisable();
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        // Forge owns the final world rendering point through RenderWorldLast.
        // Keeping its complete pass there guarantees objects are drawn after
        // the post-process, regardless of event ordering between integrations.
        if (Boolean.getBoolean("flax.runtime.forge")) {
            return;
        }

        renderFogAndObjects(event.getPartialTicks());
    }

    /**
     * Shared final-world pass.  The fog first transforms the already rendered
     * terrain and sky; geometric objects are then drawn over it with depth
     * testing, so fog cannot erase their lines while terrain can still occlude
     * them correctly.
     */
    public void renderFogAndObjects(float partialTicks) {
        if (!isToggled() || !prepareObjectScene()) {
            return;
        }

        DepthFogPostProcessor.render(this, partialTicks);
        renderObjectField((System.nanoTime() - sceneStartNanos) / 1_000_000_000.0F);
    }

    private boolean prepareObjectScene() {
        if (mc.theWorld == null || mc.thePlayer == null || mc.getRenderManager() == null) {
            return false;
        }

        if (sceneWorld != mc.theWorld) {
            rebuildScene();
        }

        refreshVisibleObjects();
        return true;
    }

    /** Clears cached chunk geometry when the world changes. */
    private void rebuildScene() {
        sceneWorld = mc.theWorld;
        sceneStartNanos = System.nanoTime();
        visibleCenterChunkX = Integer.MIN_VALUE;
        visibleCenterChunkZ = Integer.MIN_VALUE;
        generatedDensity = Double.NaN;
        objectsByChunk.clear();
        objects.clear();
        animatedPositions = new AnimatedPosition[0];
    }

    /**
     * Keeps a small cache of deterministic, world-space object fields. Moving
     * into a newly loaded chunk expands the field with that chunk's objects;
     * objects in existing chunks never inherit the player's movement.
     */
    private void refreshVisibleObjects() {
        int centerChunkX = ((int) Math.floor(mc.thePlayer.posX)) >> 4;
        int centerChunkZ = ((int) Math.floor(mc.thePlayer.posZ)) >> 4;
        double density = objectDensitySetting.getValue();

        if (centerChunkX == visibleCenterChunkX
                && centerChunkZ == visibleCenterChunkZ
                && Double.compare(density, generatedDensity) == 0) {
            return;
        }

        if (Double.compare(density, generatedDensity) != 0) {
            objectsByChunk.clear();
            generatedDensity = density;
        }

        visibleCenterChunkX = centerChunkX;
        visibleCenterChunkZ = centerChunkZ;
        pruneChunkCache(centerChunkX, centerChunkZ);
        objects.clear();
        for (int chunkX = centerChunkX - CHUNK_RADIUS; chunkX <= centerChunkX + CHUNK_RADIUS; chunkX++) {
            for (int chunkZ = centerChunkZ - CHUNK_RADIUS; chunkZ <= centerChunkZ + CHUNK_RADIUS; chunkZ++) {
                long key = getChunkKey(chunkX, chunkZ);
                List<ShaderObject> chunkObjects = objectsByChunk.get(key);
                if (chunkObjects == null) {
                    chunkObjects = createChunkObjects(chunkX, chunkZ, density);
                    objectsByChunk.put(key, chunkObjects);
                }
                objects.addAll(chunkObjects);
            }
        }

        buildConnections();
        animatedPositions = new AnimatedPosition[objects.size()];
    }

    /** Bounds the cache for long play sessions. Re-entered chunks regenerate
     * identically because their seed is based on world and chunk coordinates. */
    private void pruneChunkCache(int centerChunkX, int centerChunkZ) {
        Iterator<Long> iterator = objectsByChunk.keySet().iterator();
        while (iterator.hasNext()) {
            long key = iterator.next();
            int chunkX = (int) (key >> 32);
            int chunkZ = (int) key;
            if (Math.abs(chunkX - centerChunkX) > CHUNK_RADIUS
                    || Math.abs(chunkZ - centerChunkZ) > CHUNK_RADIUS) {
                iterator.remove();
            }
        }
    }

    private List<ShaderObject> createChunkObjects(int chunkX, int chunkZ, double density) {
        Random chunkRandom = new Random(getChunkSeed(chunkX, chunkZ));
        double exactObjectCount = density * 2.0D;
        int objectCount = (int) Math.floor(exactObjectCount);
        if (chunkRandom.nextDouble() < exactObjectCount - objectCount) {
            objectCount++;
        }
        objectCount = Math.max(0, Math.min(MAX_OBJECTS_PER_CHUNK, objectCount));

        List<ShaderObject> chunkObjects = new ArrayList<ShaderObject>(objectCount);
        for (int i = 0; i < objectCount; i++) {
            ShaderObject object = new ShaderObject();
            object.baseX = chunkX * 16.0D + 1.0D + chunkRandom.nextDouble() * 14.0D;
            object.baseZ = chunkZ * 16.0D + 1.0D + chunkRandom.nextDouble() * 14.0D;
            object.baseY = getObjectHeight(object.baseX, object.baseZ, chunkRandom);

            // Only geometric objects are rendered: no standalone points.
            object.type = chunkRandom.nextBoolean() ? 1 : 2;
            object.size = 0.28F + chunkRandom.nextFloat() * 0.72F;
            object.phase = chunkRandom.nextFloat() * ((float) Math.PI * 2.0F);
            object.bobSpeed = 0.32F + chunkRandom.nextFloat() * 1.05F;
            object.bobAmount = 0.08F + chunkRandom.nextFloat() * 0.62F;
            object.orbitSpeed = (chunkRandom.nextBoolean() ? 1.0F : -1.0F)
                    * (0.08F + chunkRandom.nextFloat() * 0.34F);
            object.orbitAmount = chunkRandom.nextFloat() * 0.72F;
            object.pulseSpeed = 0.65F + chunkRandom.nextFloat() * 1.9F;
            object.rotationX = chunkRandom.nextFloat() * 360.0F;
            object.rotationY = chunkRandom.nextFloat() * 360.0F;
            object.rotationZ = chunkRandom.nextFloat() * 360.0F;
            object.rotationSpeedX = -24.0F + chunkRandom.nextFloat() * 48.0F;
            object.rotationSpeedY = -42.0F + chunkRandom.nextFloat() * 84.0F;
            object.rotationSpeedZ = -20.0F + chunkRandom.nextFloat() * 40.0F;
            object.brightness = 0.72F + chunkRandom.nextFloat() * 0.42F;
            object.hasOrbitRing = chunkRandom.nextFloat() < 0.22F;
            object.hasSecondLink = chunkRandom.nextFloat() < 0.42F;
            chunkObjects.add(object);
        }
        return chunkObjects;
    }

    private double getObjectHeight(double x, double z, Random chunkRandom) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        return sceneWorld.getHeight(new net.minecraft.util.BlockPos(blockX, 0, blockZ)).getY()
                + 1.0D + chunkRandom.nextDouble() * 18.0D;
    }

    private long getChunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
    }

    private long getChunkSeed(int chunkX, int chunkZ) {
        return sceneWorld.getSeed() ^ (chunkX * 341873128712L) ^ (chunkZ * 132897987541L);
    }

    /**
     * Connect each object to nearby neighbours. The topology stays fixed while
     * positions animate, which produces a smooth living network instead of noise.
     */
    private void buildConnections() {
        for (int i = 0; i < objects.size(); i++) {
            ShaderObject source = objects.get(i);

            int nearest = -1;
            int secondNearest = -1;
            double nearestDist = Double.MAX_VALUE;
            double secondDist = Double.MAX_VALUE;

            for (int j = 0; j < objects.size(); j++) {
                if (i == j) {
                    continue;
                }

                ShaderObject target = objects.get(j);
                double dx = source.baseX - target.baseX;
                double dy = source.baseY - target.baseY;
                double dz = source.baseZ - target.baseZ;
                double distSq = dx * dx + dy * dy + dz * dz;

                if (distSq < nearestDist) {
                    secondDist = nearestDist;
                    secondNearest = nearest;
                    nearestDist = distSq;
                    nearest = j;
                } else if (distSq < secondDist) {
                    secondDist = distSq;
                    secondNearest = j;
                }
            }

            // Not every object needs two links; the gaps make the field less uniform.
            source.linkA = nearest;
            source.linkB = source.hasSecondLink ? secondNearest : -1;
        }
    }

    private void renderObjectField(float time) {
        RenderManager renderManager = mc.getRenderManager();
        Color baseColor = objectColorSetting.getColor();
        float objectOpacity = getObjectOpacity();
        boolean fillObjects = objectFillSetting.isToggled();

        if (objectOpacity <= 0.0F) {
            return;
        }

        boolean fogWasEnabled = GL11.glIsEnabled(GL11.GL_FOG);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(-renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ);
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            // These objects deliberately render after the depth fog. Keep
            // vanilla fixed-function fog from fading them a second time.
            GlStateManager.disableFog();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.depthMask(false);
            GlStateManager.enableDepth();
            GlStateManager.disableCull();

            // Smooth lines become a large fuzzy halo when their width is
            // increased. Keep object edges crisp and let the width setting
            // control only the actual rasterized line.
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_FASTEST);

            int visibleCount = objects.size();
            updateAnimatedPositions(time, visibleCount);

            // Draw the network first so objects remain visually on top of their links.
            drawConnections(time, baseColor, visibleCount, objectOpacity);

            float objectScale = objectScaleSetting.getValueFloat();
            for (int i = 0; i < visibleCount; i++) {
                ShaderObject object = objects.get(i);
                AnimatedPosition pos = animatedPositions[i];

                float pulse = (1.0F + (float) Math.sin(time * object.pulseSpeed + object.phase) * 0.16F)
                        * objectScale;
                float shimmer = 0.72F + 0.28F
                        * (float) Math.sin(time * (0.75F + object.pulseSpeed * 0.16F) + object.phase);
                /* Keep the animation in luminance, not alpha: at 100% the
                 * primary wireframe is genuinely opaque. */
                float alpha = objectOpacity;
                Color color = multiplyColor(baseColor,
                        object.brightness * (0.90F + 0.10F * shimmer));

                if (object.hasOrbitRing) {
                    drawOrbitRing(pos.x, pos.y, pos.z, object.size * 2.1F, time, object, color, alpha);
                }

                if (object.type == 1) {
                    drawTetrahedron(pos.x, pos.y, pos.z, object.size * pulse, time, object, color, alpha,
                            fillObjects);
                } else {
                    drawCube(pos.x, pos.y, pos.z, object.size * pulse, time, object, color, alpha,
                            fillObjects);
                }
            }
        } finally {
            GL11.glLineWidth(1.0F);
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            GlStateManager.enableCull();
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.enableLighting();
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            if (fogWasEnabled) {
                GlStateManager.enableFog();
            } else {
                GlStateManager.disableFog();
            }
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
    }

    private void updateAnimatedPositions(float time, int visibleCount) {
        if (animatedPositions.length < visibleCount) {
            animatedPositions = new AnimatedPosition[objects.size()];
        }
        for (int i = 0; i < visibleCount; i++) {
            if (animatedPositions[i] == null) animatedPositions[i] = new AnimatedPosition();
            updateAnimatedPosition(animatedPositions[i], objects.get(i), time);
        }
    }

    private void drawConnections(float time, Color baseColor, int visibleCount, float objectOpacity) {
        GL11.glLineWidth(getLineWidth());
        GL11.glBegin(GL11.GL_LINES);

        for (int i = 0; i < visibleCount; i++) {
            ShaderObject source = objects.get(i);
            drawConnection(i, source.linkA, source, time, baseColor, visibleCount, objectOpacity);
            drawConnection(i, source.linkB, source, time, baseColor, visibleCount, objectOpacity);
        }

        GL11.glEnd();
    }

    private void drawConnection(int sourceIndex, int targetIndex, ShaderObject source,
            float time, Color baseColor, int visibleCount, float objectOpacity) {
        if (targetIndex < 0 || targetIndex >= visibleCount || targetIndex <= sourceIndex) {
            return;
        }

        ShaderObject target = objects.get(targetIndex);
        AnimatedPosition a = animatedPositions[sourceIndex];
        AnimatedPosition b = animatedPositions[targetIndex];

        double dx = a.x - b.x;
        double dy = a.y - b.y;
        double dz = a.z - b.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance > 13.5D) {
            return;
        }

        float fade = (float) (1.0D - distance / 13.5D);
        float flicker = 0.72F + 0.28F * (float) Math.sin(time * 1.15F + source.phase);
        float alpha = objectOpacity;
        Color c = multiplyColor(baseColor, (source.brightness + target.brightness) * 0.5F
                * fade * (0.82F + 0.18F * flicker));

        glColor(c, alpha);
        GL11.glVertex3d(a.x, a.y, a.z);
        GL11.glVertex3d(b.x, b.y, b.z);
    }

    private void updateAnimatedPosition(AnimatedPosition result, ShaderObject object, float time) {

        double orbitAngle = time * object.orbitSpeed + object.phase;
        double orbitX = Math.cos(orbitAngle) * object.orbitAmount;
        double orbitZ = Math.sin(orbitAngle) * object.orbitAmount;
        double bob = Math.sin(time * object.bobSpeed + object.phase) * object.bobAmount;

        // A second, slower sine keeps motion from looking mechanically circular.
        double driftX = Math.sin(time * 0.21F + object.phase * 1.7F) * 0.24D;
        double driftZ = Math.cos(time * 0.17F + object.phase * 1.3F) * 0.24D;

        result.x = object.baseX + orbitX + driftX;
        result.y = object.baseY + bob;
        result.z = object.baseZ + orbitZ + driftZ;
    }

    private void drawCube(double x, double y, double z, float size, float time,
            ShaderObject object, Color color, float alpha, boolean fillObjects) {

        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x, y, z);
            GlStateManager.rotate(object.rotationX + time * object.rotationSpeedX, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(object.rotationY + time * object.rotationSpeedY, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(object.rotationZ + time * object.rotationSpeedZ, 0.0F, 0.0F, 1.0F);

            float s = size;

            if (fillObjects) {
                drawCubeFill(s, color, alpha);
            }

            // Main wireframe.
            GL11.glLineWidth(getLineWidth());
            glColor(color, alpha);
            drawCubeEdges(s);

            // Tiny brighter inner cube adds depth and a subtle pulse/glow.
            GL11.glLineWidth(getLineWidth() * 0.65F);
            glColor(mixWithWhite(color, 0.34F), alpha * 0.28F);
            drawCubeEdges(s * 0.62F);
        } finally {
            GlStateManager.popMatrix();
        }
    }

    private void drawCubeEdges(float s) {
        GL11.glBegin(GL11.GL_LINES);

        // Bottom
        edge(-s, -s, -s, s, -s, -s);
        edge(s, -s, -s, s, -s, s);
        edge(s, -s, s, -s, -s, s);
        edge(-s, -s, s, -s, -s, -s);

        // Top
        edge(-s, s, -s, s, s, -s);
        edge(s, s, -s, s, s, s);
        edge(s, s, s, -s, s, s);
        edge(-s, s, s, -s, s, -s);

        // Vertical
        edge(-s, -s, -s, -s, s, -s);
        edge(s, -s, -s, s, s, -s);
        edge(s, -s, s, s, s, s);
        edge(-s, -s, s, -s, s, s);

        GL11.glEnd();
    }

    private void drawTetrahedron(double x, double y, double z, float size, float time,
            ShaderObject object, Color color, float alpha, boolean fillObjects) {

        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x, y, z);
            GlStateManager.rotate(object.rotationX + time * object.rotationSpeedX, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(object.rotationY + time * object.rotationSpeedY, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(object.rotationZ + time * object.rotationSpeedZ, 0.0F, 0.0F, 1.0F);

            float topY = size * 1.25F;
            float bottomY = -size * 0.78F;
            float rearZ = -size * 0.72F;
            float frontZ = size * 0.98F;

            if (fillObjects) {
                drawTetrahedronFill(size, topY, bottomY, rearZ, frontZ, color, alpha);
            }

            // Wireframe.
            GL11.glLineWidth(getLineWidth());
            glColor(color, alpha);
            GL11.glBegin(GL11.GL_LINES);
            edge(0, topY, 0, -size, bottomY, rearZ);
            edge(0, topY, 0, size, bottomY, rearZ);
            edge(0, topY, 0, 0, bottomY, frontZ);
            edge(-size, bottomY, rearZ, size, bottomY, rearZ);
            edge(size, bottomY, rearZ, 0, bottomY, frontZ);
            edge(0, bottomY, frontZ, -size, bottomY, rearZ);
            GL11.glEnd();

        } finally {
            GlStateManager.popMatrix();
        }
    }

    private void drawOrbitRing(double x, double y, double z, float radius, float time,
            ShaderObject object, Color color, float alpha) {

        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x, y, z);
            GlStateManager.rotate(time * 26.0F + object.phase * 57.29578F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(54.0F + (float) Math.sin(time * 0.4F + object.phase) * 25.0F, 1.0F, 0.0F, 0.0F);

            GL11.glLineWidth(getLineWidth() * 0.75F);
            glColor(color, alpha);
            GL11.glBegin(GL11.GL_LINE_LOOP);
            for (int i = 0; i < 24; i++) {
                double angle = i / 24.0D * Math.PI * 2.0D;
                GL11.glVertex3d(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
            }
            GL11.glEnd();
        } finally {
            GlStateManager.popMatrix();
        }
    }

    /**
     * Draws a genuinely solid cube when opacity is 100%.  Unlike the former
     * decorative fill, an opaque fill writes depth, so back faces and other
     * objects are hidden by the nearest face just like a normal 3D model.
     * Lower values still honour the common Object Opacity slider.
     */
    private void drawCubeFill(float s, Color color, float alpha) {
        boolean opaqueFill = beginObjectFill(alpha);
        try {
            GL11.glBegin(GL11.GL_QUADS);
            // Front / back
            glColor(multiplyColor(color, 0.98F), alpha);
            vertex(-s, -s, s); vertex(s, -s, s); vertex(s, s, s); vertex(-s, s, s);
            glColor(multiplyColor(color, 0.72F), alpha);
            vertex(s, -s, -s); vertex(-s, -s, -s); vertex(-s, s, -s); vertex(s, s, -s);
            // Left / right
            glColor(multiplyColor(color, 0.82F), alpha);
            vertex(-s, -s, -s); vertex(-s, -s, s); vertex(-s, s, s); vertex(-s, s, -s);
            glColor(multiplyColor(color, 0.92F), alpha);
            vertex(s, -s, s); vertex(s, -s, -s); vertex(s, s, -s); vertex(s, s, s);
            // Top / bottom
            glColor(multiplyColor(color, 1.16F), alpha);
            vertex(-s, s, s); vertex(s, s, s); vertex(s, s, -s); vertex(-s, s, -s);
            glColor(multiplyColor(color, 0.62F), alpha);
            vertex(-s, -s, -s); vertex(s, -s, -s); vertex(s, -s, s); vertex(-s, -s, s);
            GL11.glEnd();
        } finally {
            endObjectFill(opaqueFill);
        }
    }

    /** Applies the same faceted, opaque treatment to tetrahedrons. */
    private void drawTetrahedronFill(float size, float topY, float bottomY,
            float rearZ, float frontZ, Color color, float alpha) {
        boolean opaqueFill = beginObjectFill(alpha);
        try {
            GL11.glBegin(GL11.GL_TRIANGLES);
            glColor(multiplyColor(color, 1.08F), alpha);
            triangle(0, topY, 0, -size, bottomY, rearZ, size, bottomY, rearZ);
            glColor(multiplyColor(color, 0.94F), alpha);
            triangle(0, topY, 0, size, bottomY, rearZ, 0, bottomY, frontZ);
            glColor(multiplyColor(color, 0.78F), alpha);
            triangle(0, topY, 0, 0, bottomY, frontZ, -size, bottomY, rearZ);
            glColor(multiplyColor(color, 0.66F), alpha);
            triangle(-size, bottomY, rearZ, 0, bottomY, frontZ, size, bottomY, rearZ);
            GL11.glEnd();
        } finally {
            endObjectFill(opaqueFill);
        }
    }

    /**
     * The field renderer normally uses translucent lines with depth writes
     * disabled.  At 100% opacity, temporarily turn blending off and enable
     * depth writes for the filled mesh so it behaves as a solid 3D object.
     */
    private boolean beginObjectFill(float alpha) {
        if (alpha < 0.999F) {
            return false;
        }

        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        return true;
    }

    private void endObjectFill(boolean opaqueFill) {
        if (!opaqueFill) {
            return;
        }

        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        // Filled faces and their wireframe share an edge.  LEQUAL keeps the
        // crisp outline visible after the opaque face has populated depth.
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
    }

    private void glColor(Color color, float alpha) {
        GL11.glColor4f(
                color.getRed() / 255.0F,
                color.getGreen() / 255.0F,
                color.getBlue() / 255.0F,
                clamp01(alpha));
    }

    private Color multiplyColor(Color color, float multiplier) {
        int r = clamp255(Math.round(color.getRed() * multiplier));
        int g = clamp255(Math.round(color.getGreen() * multiplier));
        int b = clamp255(Math.round(color.getBlue() * multiplier));
        return new Color(r, g, b);
    }

    private Color mixWithWhite(Color color, float amount) {
        amount = clamp01(amount);
        int r = clamp255(Math.round(color.getRed() + (255 - color.getRed()) * amount));
        int g = clamp255(Math.round(color.getGreen() + (255 - color.getGreen()) * amount));
        int b = clamp255(Math.round(color.getBlue() + (255 - color.getBlue()) * amount));
        return new Color(r, g, b);
    }

    private int clamp255(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private float getLineWidth() {
        // Very wide GL lines are driver-dependent and often become fuzzy even
        // with smoothing disabled. Keep the control responsive but bounded.
        return Math.max(0.5F, Math.min(3.0F, lineWidthSetting.getValueFloat()));
    }

    private float getObjectOpacity() {
        return clamp01(objectOpacitySetting.getValueFloat() / 100.0F);
    }

    private void vertex(float x, float y, float z) {
        GL11.glVertex3f(x, y, z);
    }

    private void edge(float x1, float y1, float z1, float x2, float y2, float z2) {
        GL11.glVertex3f(x1, y1, z1);
        GL11.glVertex3f(x2, y2, z2);
    }

    private void triangle(float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3) {
        GL11.glVertex3f(x1, y1, z1);
        GL11.glVertex3f(x2, y2, z2);
        GL11.glVertex3f(x3, y3, z3);
    }

    private static class AnimatedPosition {
        double x;
        double y;
        double z;

        AnimatedPosition() {}
    }

    private static class ShaderObject {
        int type;

        double baseX;
        double baseY;
        double baseZ;

        float size;
        float phase;
        float bobSpeed;
        float bobAmount;
        float orbitSpeed;
        float orbitAmount;
        float pulseSpeed;

        float rotationX;
        float rotationY;
        float rotationZ;
        float rotationSpeedX;
        float rotationSpeedY;
        float rotationSpeedZ;

        float brightness;
        boolean hasOrbitRing;
        boolean hasSecondLink;

        int linkA = -1;
        int linkB = -1;
    }

}
