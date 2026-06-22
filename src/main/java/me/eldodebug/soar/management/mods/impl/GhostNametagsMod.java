package me.eldodebug.soar.management.mods.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.opengl.GL11;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventRender2D;
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
import me.eldodebug.soar.utils.render.WorldToScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

public class GhostNametagsMod extends Mod {

	private static GhostNametagsMod instance;

	private final ComboSetting styleSetting = new ComboSetting(TranslateText.STYLE, this, TranslateText.MODERN, new ArrayList<Option>(Arrays.asList(
			new Option(TranslateText.CLASSIC), new Option(TranslateText.MODERN), new Option(TranslateText.MINIMAL), new Option(TranslateText.OUTLINED))));
	private final ColorSetting backgroundColorSetting = new ColorSetting(TranslateText.BACKGROUND, this, new Color(16, 17, 22), false);
	private final NumberSetting backgroundOpacitySetting = new NumberSetting(TranslateText.BACKGROUND_OPACITY, this, 75, 0, 100, true);
	private final BooleanSetting textShadowSetting = new BooleanSetting(TranslateText.TEXT_SHADOW, this, true);
	private final BooleanSetting showHealth = new BooleanSetting(TranslateText.HEALTH, this, true);
	private final BooleanSetting showDistance = new BooleanSetting(TranslateText.DISTANCE, this, true);
	private final NumberSetting scaleSetting = new NumberSetting(TranslateText.SCALE, this, 1.0, 0.5, 2.0, false);
	private final BooleanSetting customNameColorSetting = new BooleanSetting(TranslateText.CUSTOM_NAME_COLOR, this, false);
	private final ColorSetting nameColorSetting = new ColorSetting(TranslateText.NAME_COLOR, this, new Color(255, 255, 255), false);

	public GhostNametagsMod() {
		super(TranslateText.GHOST_NAMETAGS, TranslateText.GHOST_NAMETAGS_DESCRIPTION, ModCategory.GHOST);
		instance = this;
	}

	public static GhostNametagsMod getInstance() {
		return instance;
	}

	@EventTarget
	public void onRender3D(EventRender3D event) {
		// Capture the camera matrices during the world pass; the nametags are
		// drawn as a flat 2D overlay so they always face the camera.
		if(mc.theWorld == null || mc.thePlayer == null) {
			return;
		}
		WorldToScreen.capture();
	}

	@EventTarget
	public void onRender2D(EventRender2D event) {
		if(mc.theWorld == null || mc.thePlayer == null) {
			return;
		}

		List<EntityPlayer> targets = new ArrayList<EntityPlayer>();
		for(EntityPlayer player : mc.theWorld.playerEntities) {
			if(player == null || player == mc.thePlayer || player.isDead) {
				continue;
			}
			targets.add(player);
		}

		// Draw the farthest players first so nearer nametags overlap on top.
		targets.sort((a, b) -> Double.compare(
				mc.thePlayer.getDistanceSqToEntity(b),
				mc.thePlayer.getDistanceSqToEntity(a)));

		for(EntityPlayer player : targets) {
			renderNameTag(player, event.getPartialTicks());
		}
	}

	private void renderNameTag(EntityPlayer player, float partialTicks) {

		double worldX = interpolate(player.lastTickPosX, player.posX, partialTicks);
		double worldY = interpolate(player.lastTickPosY, player.posY, partialTicks) + player.height + 0.62D;
		double worldZ = interpolate(player.lastTickPosZ, player.posZ, partialTicks);

		float[] screen = WorldToScreen.project(worldX, worldY, worldZ);
		if(screen == null) {
			return;
		}

		float distance = mc.thePlayer.getDistanceToEntity(player);
		float distanceScale = MathHelper.clamp_float(0.55F - distance * 0.0015F, 0.32F, 0.55F);
		float scale = distanceScale * scaleSetting.getValueFloat();

		drawNameTag(player, screen[0], screen[1], scale);
	}

	private void drawNameTag(EntityPlayer player, float screenX, float screenY, float scale) {

		String name = ColorUtils.removeColorCode(player.getDisplayName().getFormattedText());
		if(name == null || name.trim().isEmpty()) {
			name = player.getName();
		}

		String healthText = showHealth.isToggled() ? (Math.max(0, Math.round(player.getHealth())) + " HP") : "";
		String distText = showDistance.isToggled() ? (Math.max(1, Math.round(mc.thePlayer.getDistanceToEntity(player))) + "m") : "";
		String separator = (!healthText.isEmpty() && !distText.isEmpty()) ? "  " : "";
		String secondary = healthText + separator + distText;
		boolean hasSecondary = !secondary.isEmpty();

		int nameWidth = fr.getStringWidth(name);
		int subWidth = hasSecondary ? fr.getStringWidth(secondary) : 0;
		int contentWidth = Math.max(nameWidth, subWidth);

		float padX = 4.0F;
		float padY = 2.5F;
		float lineHeight = fr.FONT_HEIGHT;
		int lines = hasSecondary ? 2 : 1;
		float textHeight = lineHeight * lines + (lines - 1);

		float panelW = contentWidth + padX * 2.0F;
		float panelH = textHeight + padY * 2.0F;
		float panelX = -panelW / 2.0F;

		String preset = styleSetting.getOption().getTranslate().getKey();

		GlStateManager.pushMatrix();
		GlStateManager.translate(screenX, screenY, 0.0F);
		GlStateManager.scale(scale, scale, 1.0F);
		GlStateManager.translate(0.0F, -panelH - 2.0F, 0.0F);

		GlStateManager.disableLighting();
		GlStateManager.disableDepth();
		GlStateManager.depthMask(false);
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

		drawBackground(preset, panelX, 0.0F, panelW, panelH);

		GlStateManager.enableBlend();

		Color nameColor = customNameColorSetting.isToggled() ? nameColorSetting.getColor() : Color.WHITE;
		drawText(name, -nameWidth / 2.0F, padY, nameColor);

		if(hasSecondary) {
			float segY = padY + lineHeight + 1.0F;
			float segX = -subWidth / 2.0F;
			Color subColor = new Color(205, 210, 220);
			if(!healthText.isEmpty()) {
				drawText(healthText, segX, segY, getHealthColor(player));
				segX += fr.getStringWidth(healthText);
			}
			if(!separator.isEmpty()) {
				drawText(separator, segX, segY, subColor);
				segX += fr.getStringWidth(separator);
			}
			if(!distText.isEmpty()) {
				drawText(distText, segX, segY, subColor);
			}
		}

		GlStateManager.disableBlend();
		GlStateManager.depthMask(true);
		GlStateManager.enableDepth();
		GlStateManager.enableLighting();
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.popMatrix();
	}

	private void drawBackground(String preset, float x, float y, float width, float height) {

		if(preset.equals(TranslateText.MINIMAL.getKey())) {
			// Text only - no background.
			return;
		}

		int opacityAlpha = (int) (MathUtils.clamp(backgroundOpacitySetting.getValueFloat() / 100.0F) * 255.0F);
		Color base = backgroundColorSetting.getColor();
		Color background = new Color(base.getRed(), base.getGreen(), base.getBlue(), opacityAlpha);
		Color accent = Glide.getInstance().getColorManager().getCurrentColor().getInterpolateColor();

		if(preset.equals(TranslateText.CLASSIC.getKey())) {
			RenderUtils.drawRect(x, y, width, height, background);
			return;
		}

		float radius = Math.min(height / 2.0F, 4.0F);

		if(preset.equals(TranslateText.OUTLINED.getKey())) {
			RenderUtils.drawRoundedRect(x, y, width, height, radius, background);
			RenderUtils.drawRoundedOutline(x, y, width, height, radius, 1.2F,
					new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 235));
			return;
		}

		// Modern: a tooltip-like panel - soft outer shadow, dark rounded body, a
		// lighter inner-edge highlight for depth, a faint top gloss and a thin
		// accent line along the top.
		RenderUtils.drawRoundedRect(x - 1.0F, y - 1.0F, width + 2.0F, height + 2.0F, radius + 1.0F,
				new Color(0, 0, 0, (int) (opacityAlpha * 0.55F)));
		RenderUtils.drawRoundedRect(x, y, width, height, radius, background);
		RenderUtils.drawRoundedRect(x, y, width, height / 2.0F, radius,
				new Color(255, 255, 255, (int) (opacityAlpha * 0.06F)));
		RenderUtils.drawRoundedOutline(x, y, width, height, radius, 0.8F, new Color(255, 255, 255, 28));
		RenderUtils.drawRoundedRect(x + radius * 0.6F, y, width - radius * 1.2F, 1.0F, 0.5F,
				new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 230));
	}

	private void drawText(String text, float x, float y, Color color) {
		fr.drawString(text, x, y, color.getRGB(), textShadowSetting.isToggled());
	}

	// Green at full health, fading through yellow to red as it drops.
	private Color getHealthColor(EntityPlayer player) {
		float ratio = MathUtils.clamp(player.getHealth() / Math.max(1.0F, player.getMaxHealth()));
		return Color.getHSBColor(ratio / 3.0F, 0.85F, 1.0F);
	}

	private double interpolate(double start, double end, float partialTicks) {
		return start + (end - start) * partialTicks;
	}

	public ComboSetting getThemeSetting() {
		return styleSetting;
	}
}
