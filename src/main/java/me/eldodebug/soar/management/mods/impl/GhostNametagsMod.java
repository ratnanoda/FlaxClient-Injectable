package me.eldodebug.soar.management.mods.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.opengl.GL11;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventRender3D;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.BooleanSetting;
import me.eldodebug.soar.management.mods.settings.impl.ColorSetting;
import me.eldodebug.soar.management.mods.settings.impl.ComboSetting;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import me.eldodebug.soar.management.mods.settings.impl.combo.Option;
import me.eldodebug.soar.utils.ColorUtils;
import me.eldodebug.soar.utils.MathUtils;
import me.eldodebug.soar.utils.render.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

/**
 * Smooth world-space nametags.
 *
 * This version intentionally does NOT use WorldToScreen or screen-space
 * smoothing. The tag is rendered in EventRender3D as a camera-facing billboard
 * using the same interpolated camera origin as the world renderer.
 */
public class GhostNametagsMod extends Mod {

    private static GhostNametagsMod instance;

    private final ComboSetting styleSetting = new ComboSetting(
            TranslateText.STYLE,
            this,
            TranslateText.MODERN,
            new ArrayList<Option>(Arrays.asList(
                    new Option(TranslateText.CLASSIC),
                    new Option(TranslateText.MODERN),
                    new Option(TranslateText.MINIMAL),
                    new Option(TranslateText.OUTLINED))));

    private final ColorSetting backgroundColorSetting = new ColorSetting(
            TranslateText.BACKGROUND, this, new Color(16, 17, 22), false);
    private final NumberSetting backgroundOpacitySetting = new NumberSetting(
            TranslateText.BACKGROUND_OPACITY, this, 78, 0, 100, true);
    private final BooleanSetting textShadowSetting = new BooleanSetting(
            TranslateText.TEXT_SHADOW, this, true);
    private final BooleanSetting showHealth = new BooleanSetting(
            TranslateText.HEALTH, this, true);
    private final BooleanSetting showDistance = new BooleanSetting(
            TranslateText.DISTANCE, this, true);
    private final NumberSetting scaleSetting = new NumberSetting(
            TranslateText.SCALE, this, 1.0, 0.5, 2.0, false);
    private final BooleanSetting customNameColorSetting = new BooleanSetting(
            TranslateText.CUSTOM_NAME_COLOR, this, false);
    private final ColorSetting nameColorSetting = new ColorSetting(
            TranslateText.NAME_COLOR, this, new Color(255, 255, 255), false);

    public GhostNametagsMod() {
        super(TranslateText.GHOST_NAMETAGS, TranslateText.GHOST_NAMETAGS_DESCRIPTION, ModCategory.GHOST);
        instance = this;
    }

    public static GhostNametagsMod getInstance() {
        return instance;
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.theWorld == null || mc.thePlayer == null || mc.getRenderManager() == null) {
            return;
        }

        final float partialTicks = event.getPartialTicks();
        final List<EntityPlayer> targets = new ArrayList<EntityPlayer>();

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == null || player == mc.thePlayer || player.isDead) {
                continue;
            }
            targets.add(player);
        }

        // Tags ignore depth, so render farthest -> nearest for stable overlap.
        targets.sort((a, b) -> Double.compare(
                getInterpolatedDistanceSq(b, partialTicks),
                getInterpolatedDistanceSq(a, partialTicks)));

        for (EntityPlayer player : targets) {
            renderNametag(player, partialTicks);
        }
    }

    private void renderNametag(EntityPlayer player, float partialTicks) {
        final RenderManager renderManager = mc.getRenderManager();

        // Smooth entity position for the exact current render frame.
        final double entityX = interpolate(player.lastTickPosX, player.posX, partialTicks);
        final double entityY = interpolate(player.lastTickPosY, player.posY, partialTicks);
        final double entityZ = interpolate(player.lastTickPosZ, player.posZ, partialTicks);

        // Render relative to the current interpolated camera origin.
        final double renderX = entityX - renderManager.viewerPosX;
        final double renderY = entityY - renderManager.viewerPosY + player.height + 0.48D;
        final double renderZ = entityZ - renderManager.viewerPosZ;

        final float distance = getInterpolatedDistance(player, partialTicks);
        final float worldScale = getWorldScale(distance);

        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(renderX, renderY, renderZ);
            GL11.glNormal3f(0.0F, 1.0F, 0.0F);

            // Billboard: rotate with the rendered camera every frame.
            GlStateManager.rotate(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
            final float pitchMultiplier = mc.gameSettings.thirdPersonView == 2 ? -1.0F : 1.0F;
            GlStateManager.rotate(renderManager.playerViewX * pitchMultiplier, 1.0F, 0.0F, 0.0F);
            GlStateManager.scale(-worldScale, -worldScale, worldScale);

            setupTagRenderState();
            drawNametagContents(player, distance);
        } finally {
            restoreTagRenderState();
            GlStateManager.popMatrix();
        }
    }

    private void drawNametagContents(EntityPlayer player, float distance) {
        String name = ColorUtils.removeColorCode(player.getDisplayName().getFormattedText());
        if (name == null || name.trim().isEmpty()) {
            name = player.getName();
        }

        final String healthText = showHealth.isToggled()
                ? Math.max(0, Math.round(player.getHealth())) + " HP"
                : "";
        final String distanceText = showDistance.isToggled()
                ? Math.max(1, Math.round(distance)) + "m"
                : "";

        final boolean hasHealth = !healthText.isEmpty();
        final boolean hasDistance = !distanceText.isEmpty();
        final boolean hasSecondary = hasHealth || hasDistance;
        final String separator = hasHealth && hasDistance ? "  " : "";
        final String secondary = healthText + separator + distanceText;

        final int nameWidth = fr.getStringWidth(name);
        final int secondaryWidth = hasSecondary ? fr.getStringWidth(secondary) : 0;
        final float contentWidth = Math.max(nameWidth, secondaryWidth);

        final float paddingX = 5.0F;
        final float paddingTop = 3.0F;
        final float paddingBottom = 3.0F;
        final float lineGap = hasSecondary ? 1.0F : 0.0F;
        final float textHeight = fr.FONT_HEIGHT + (hasSecondary ? fr.FONT_HEIGHT + lineGap : 0.0F);

        final float panelWidth = contentWidth + paddingX * 2.0F;
        final float panelHeight = textHeight + paddingTop + paddingBottom;
        final float panelX = -panelWidth / 2.0F;
        final float panelY = -panelHeight - 4.0F;

        drawBackground(panelX, panelY, panelWidth, panelHeight);

        // Rounded-rect helpers may change GL texture/blend state internally.
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        final Color nameColor = customNameColorSetting.isToggled()
                ? nameColorSetting.getColor()
                : Color.WHITE;

        final float nameX = -nameWidth / 2.0F;
        final float nameY = panelY + paddingTop;
        drawText(name, nameX, nameY, nameColor);

        if (hasSecondary) {
            float segmentX = -secondaryWidth / 2.0F;
            final float secondaryY = nameY + fr.FONT_HEIGHT + lineGap;
            final Color muted = new Color(200, 206, 218);

            if (hasHealth) {
                drawText(healthText, segmentX, secondaryY, getHealthColor(player));
                segmentX += fr.getStringWidth(healthText);
            }

            if (!separator.isEmpty()) {
                drawText(separator, segmentX, secondaryY, muted);
                segmentX += fr.getStringWidth(separator);
            }

            if (hasDistance) {
                drawText(distanceText, segmentX, secondaryY, muted);
            }
        }
    }

    private void drawBackground(float x, float y, float width, float height) {
        final String preset = styleSetting.getOption().getTranslate().getKey();

        if (preset.equals(TranslateText.MINIMAL.getKey())) {
            return;
        }

        final int alpha = (int) (
                MathUtils.clamp(backgroundOpacitySetting.getValueFloat() / 100.0F) * 255.0F);
        final Color base = backgroundColorSetting.getColor();
        final Color background = new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
        final Color accent = Glide.getInstance().getColorManager().getCurrentColor().getInterpolateColor();

        // Every background style is rounded now.
        final float radius = Math.min(5.0F, height * 0.28F);

        if (preset.equals(TranslateText.CLASSIC.getKey())) {
            RenderUtils.drawRoundedRect(x, y, width, height, radius, background);
            return;
        }

        if (preset.equals(TranslateText.OUTLINED.getKey())) {
            RenderUtils.drawRoundedRect(x, y, width, height, radius, background);
            RenderUtils.drawRoundedOutline(
                    x, y, width, height, radius, 1.0F,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 225));
            return;
        }

        // Modern: soft rounded shadow, body, subtle inner border and accent pill.
        RenderUtils.drawRoundedRect(
                x - 1.25F,
                y - 1.25F,
                width + 2.5F,
                height + 2.5F,
                radius + 1.0F,
                new Color(0, 0, 0, Math.min(180, (int) (alpha * 0.62F))));

        RenderUtils.drawRoundedRect(x, y, width, height, radius, background);

        RenderUtils.drawRoundedOutline(
                x, y, width, height, radius, 0.75F,
                new Color(255, 255, 255, Math.min(42, Math.max(20, alpha / 7))));

        final float accentWidth = Math.max(8.0F, Math.min(width * 0.38F, 28.0F));
        RenderUtils.drawRoundedRect(
                -accentWidth / 2.0F,
                y + 0.75F,
                accentWidth,
                1.25F,
                0.65F,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 220));
    }

    private void setupTagRenderState() {
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void restoreTagRenderState() {
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawText(String text, float x, float y, Color color) {
        fr.drawString(text, x, y, color.getRGB(), textShadowSetting.isToggled());
    }

    private Color getHealthColor(EntityPlayer player) {
        final float ratio = MathUtils.clamp(
                player.getHealth() / Math.max(1.0F, player.getMaxHealth()));
        return Color.getHSBColor(ratio / 3.0F, 0.82F, 1.0F);
    }

    private float getWorldScale(float distance) {
        // Continuous scaling keeps the apparent tag size stable without snapping.
        float distanceScale = 0.00245F * (distance + 1.0F);
        distanceScale = MathHelper.clamp_float(distanceScale, 0.02666667F, 0.135F);
        return distanceScale * scaleSetting.getValueFloat();
    }

    private float getInterpolatedDistance(EntityPlayer player, float partialTicks) {
        return (float) Math.sqrt(getInterpolatedDistanceSq(player, partialTicks));
    }

    private double getInterpolatedDistanceSq(EntityPlayer player, float partialTicks) {
        final double playerX = interpolate(player.lastTickPosX, player.posX, partialTicks);
        final double playerY = interpolate(player.lastTickPosY, player.posY, partialTicks);
        final double playerZ = interpolate(player.lastTickPosZ, player.posZ, partialTicks);

        final double selfX = interpolate(mc.thePlayer.lastTickPosX, mc.thePlayer.posX, partialTicks);
        final double selfY = interpolate(mc.thePlayer.lastTickPosY, mc.thePlayer.posY, partialTicks);
        final double selfZ = interpolate(mc.thePlayer.lastTickPosZ, mc.thePlayer.posZ, partialTicks);

        final double dx = playerX - selfX;
        final double dy = playerY - selfY;
        final double dz = playerZ - selfZ;
        return dx * dx + dy * dy + dz * dz;
    }

    private double interpolate(double previous, double current, float partialTicks) {
        return previous + (current - previous) * partialTicks;
    }

    public ComboSetting getThemeSetting() {
        return styleSetting;
    }
}