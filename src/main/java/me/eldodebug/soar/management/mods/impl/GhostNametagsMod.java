package me.eldodebug.soar.management.mods.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.lwjgl.opengl.GL11;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.color.AccentColor;
import me.eldodebug.soar.injection.interfaces.IMixinRenderManager;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventRender3D;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.ComboSetting;
import me.eldodebug.soar.management.mods.settings.impl.BooleanSetting;
import me.eldodebug.soar.management.mods.settings.impl.combo.Option;
import me.eldodebug.soar.utils.ColorUtils;
import me.eldodebug.soar.utils.MathUtils;
import me.eldodebug.soar.utils.animation.simple.SimpleAnimation;
import me.eldodebug.soar.utils.render.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class GhostNametagsMod extends Mod {

	private static GhostNametagsMod instance;
	private final ComboSetting themeSetting = new ComboSetting(TranslateText.THEME, this, TranslateText.NORMAL, new ArrayList<Option>(Arrays.asList(
		    new Option(TranslateText.NORMAL), new Option(TranslateText.FANCY))));
	private final BooleanSetting showHealth = new BooleanSetting(TranslateText.HEALTH, this, true);
	private final BooleanSetting showDistance = new BooleanSetting(TranslateText.DISTANCE, this, true);
	private final BooleanSetting showHeldItem = new BooleanSetting(TranslateText.ITEM_INFO, this, true);
	private final BooleanSetting showArmor = new BooleanSetting(TranslateText.ARMOR_STATUS, this, true);
	private final Map<UUID, SimpleAnimation> healthAnimationMap = new HashMap<UUID, SimpleAnimation>();

	public GhostNametagsMod() {
		super(TranslateText.GHOST_NAMETAGS, TranslateText.GHOST_NAMETAGS_DESCRIPTION, ModCategory.GHOST);
		instance = this;
	}

	public static GhostNametagsMod getInstance() {
		return instance;
	}

	@EventTarget
	public void onRender3D(EventRender3D event) {
		if(mc.theWorld == null || mc.thePlayer == null) {
			return;
		}

		cleanupAnimations();

		for(EntityPlayer player : mc.theWorld.playerEntities) {
			if(player == null || player == mc.thePlayer || player.isDead) {
				continue;
			}

			renderNameTag(player, event.getPartialTicks());
		}
	}

	private void renderNameTag(EntityPlayer player, float partialTicks) {
		if(themeSetting.getOption().getTranslate().equals(TranslateText.FANCY)) {
			renderFancyNameTag(player, partialTicks);
		} else {
			renderClassicNameTag(player, partialTicks);
		}
	}

	private void renderClassicNameTag(EntityPlayer player, float partialTicks) {
		double x = interpolate(player.lastTickPosX, player.posX, partialTicks) - ((IMixinRenderManager) mc.getRenderManager()).getRenderPosX();
		double y = interpolate(player.lastTickPosY, player.posY, partialTicks) - ((IMixinRenderManager) mc.getRenderManager()).getRenderPosY() + player.height + 0.62D;
		double z = interpolate(player.lastTickPosZ, player.posZ, partialTicks) - ((IMixinRenderManager) mc.getRenderManager()).getRenderPosZ();

		String name = ColorUtils.removeColorCode(player.getDisplayName().getFormattedText());
		if(name == null || name.trim().isEmpty()) {
			name = player.getName();
		}

		StringBuilder secondary = new StringBuilder();
		if(showHealth.isToggled()) {
			secondary.append(Math.max(0, Math.round(player.getHealth())))
					.append(" HP");
		}
		if(showDistance.isToggled()) {
			if(secondary.length() > 0) secondary.append("  |  ");
			secondary.append(Math.max(1, Math.round(mc.thePlayer.getDistanceToEntity(player)))).append("m");
		}
		float distance = mc.thePlayer.getDistanceToEntity(player);
		float scale = 0.018F * Math.max(1.0F, distance * 0.12F);

		int nameWidth = fr.getStringWidth(name);
		int subWidth = fr.getStringWidth(secondary.toString());
		int width = Math.max(nameWidth, subWidth);
		int boxHeight = fr.FONT_HEIGHT * 2 + 6;

		Color accent = Glide.getInstance().getColorManager().getCurrentColor().getInterpolateColor();

		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
		GlStateManager.scale(-scale, -scale, scale);

		GlStateManager.disableLighting();
		GlStateManager.disableDepth();
		GlStateManager.depthMask(false);
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

		RenderUtils.drawRect(-width / 2 - 4, -3, width + 8, boxHeight, new Color(18, 18, 18, 175));
		RenderUtils.drawRect(-width / 2 - 4, -3, 2, boxHeight, new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 220));
		fr.drawStringWithShadow(name, -nameWidth / 2.0F, -0.5F, Color.WHITE.getRGB());
		fr.drawStringWithShadow(secondary.toString(), -subWidth / 2.0F, fr.FONT_HEIGHT + 1F, new Color(225, 225, 225, 230).getRGB());

		GlStateManager.disableBlend();
		GlStateManager.depthMask(true);
		GlStateManager.enableDepth();
		GlStateManager.enableLighting();
		GL11.glColor4f(1F, 1F, 1F, 1F);
		GlStateManager.popMatrix();
	}

	private void renderFancyNameTag(EntityPlayer player, float partialTicks) {
		double x = interpolate(player.lastTickPosX, player.posX, partialTicks) - ((IMixinRenderManager) mc.getRenderManager()).getRenderPosX();
		double y = interpolate(player.lastTickPosY, player.posY, partialTicks) - ((IMixinRenderManager) mc.getRenderManager()).getRenderPosY() + player.height + 0.62D;
		double z = interpolate(player.lastTickPosZ, player.posZ, partialTicks) - ((IMixinRenderManager) mc.getRenderManager()).getRenderPosZ();

		String name = ColorUtils.removeColorCode(player.getDisplayName().getFormattedText());
		if(name == null || name.trim().isEmpty()) {
			name = player.getName();
		}
		name = truncate(name, 20);

		StringBuilder secondary = new StringBuilder();
		if(showHealth.isToggled()) {
			secondary.append(formatHealthValue(player)).append(" HP");
		}
		if(showDistance.isToggled()) {
			if(secondary.length() > 0) secondary.append("  |  ");
			secondary.append(Math.max(1, Math.round(mc.thePlayer.getDistanceToEntity(player)))).append("m");
		}
		String secondaryText = secondary.length() > 0 ? secondary.toString() : "No extra info";
		String heldItem = showHeldItem.isToggled() ? "ITEM  " + truncate(getHeldItemName(player), 26) : "";
		String armorItems = showArmor.isToggled() ? "ARMOR  " + truncate(getArmorDisplayName(player), 26) : "";
		float animatedHealth = getAnimatedHealthRatio(player);
		String healthPercent = (int) (MathUtils.clamp(animatedHealth) * 100.0F) + "%";

		float distance = mc.thePlayer.getDistanceToEntity(player);
		float scale = 0.018F * Math.max(1.0F, distance * 0.12F);

		int nameWidth = fr.getStringWidth(name);
		int subWidth = fr.getStringWidth(secondaryText);
		int heldWidth = fr.getStringWidth(heldItem);
		int armorWidth = fr.getStringWidth(armorItems);
		int percentWidth = fr.getStringWidth(healthPercent);
		int contentWidth = Math.max(Math.max(nameWidth, subWidth), Math.max(heldWidth, armorWidth));
		int width = Math.max(120, Math.max(contentWidth + 18, percentWidth + 78));
		int infoLineCount = (showHeldItem.isToggled() ? 1 : 0) + (showArmor.isToggled() ? 1 : 0);
		int boxHeight = 24 + (fr.FONT_HEIGHT * 2) + 7 + (infoLineCount * fr.FONT_HEIGHT);

		AccentColor accentColor = Glide.getInstance().getColorManager().getCurrentColor();
		Color accentA = accentColor.getColor1();
		Color accentB = accentColor.getColor2();
		Color accent = accentColor.getInterpolateColor();
		int boxX = -width / 2 - 8;
		int boxY = -5;
		int boxW = width + 16;

		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
		GlStateManager.scale(-scale, -scale, scale);

		GlStateManager.disableLighting();
		GlStateManager.disableDepth();
		GlStateManager.depthMask(false);
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

		RenderUtils.drawRect(boxX - 1.5F, boxY - 1.5F, boxW + 3.0F, boxHeight + 3.0F, new Color(0, 0, 0, 82));
		RenderUtils.drawRect(boxX, boxY, boxW, boxHeight, new Color(12, 16, 24, 214));
		RenderUtils.drawRect(boxX + 1.2F, boxY + 1.2F, boxW - 2.4F, boxHeight - 2.4F, new Color(19, 25, 35, 92));
		RenderUtils.drawOutline(boxX, boxY, boxW, boxHeight, 0.7F, new Color(255, 255, 255, 48));
		RenderUtils.drawRect(boxX + 0.6F, boxY + 0.6F, (boxW - 1.2F) / 2.0F, 1.9F, new Color(accentA.getRed(), accentA.getGreen(), accentA.getBlue(), 240));
		RenderUtils.drawRect(boxX + 0.6F + ((boxW - 1.2F) / 2.0F), boxY + 0.6F, (boxW - 1.2F) / 2.0F, 1.9F, new Color(accentB.getRed(), accentB.getGreen(), accentB.getBlue(), 240));
		RenderUtils.drawRect(boxX + 0.6F, boxY + 0.6F, 2.0F, boxHeight - 1.2F, new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 212));

		float textLeft = boxX + 8.0F;
		float headerY = boxY + 4.0F;
		fr.drawStringWithShadow("FLAX TARGET", textLeft, headerY, new Color(176, 188, 210, 226).getRGB());
		fr.drawStringWithShadow(healthPercent, boxX + boxW - percentWidth - 8.0F, headerY, new Color(229, 236, 252, 236).getRGB());

		float nameY = headerY + fr.FONT_HEIGHT + 1.0F;
		fr.drawStringWithShadow(name, textLeft, nameY, new Color(247, 250, 255, 250).getRGB());
		fr.drawStringWithShadow(secondaryText, textLeft, nameY + fr.FONT_HEIGHT + 1.0F, new Color(193, 206, 228, 234).getRGB());

		float healthBarX = boxX + 8.0F;
		float healthBarY = nameY + (fr.FONT_HEIGHT * 2) + 3.0F;
		float healthBarW = boxW - 16.0F;
		float healthFill = Math.max(0.0F, Math.min(1.0F, animatedHealth)) * healthBarW;
		RenderUtils.drawRect(healthBarX, healthBarY, healthBarW, 3.8F, new Color(255, 255, 255, 24));
		RenderUtils.drawRect(healthBarX, healthBarY, healthFill / 2.0F, 3.8F, new Color(accentA.getRed(), accentA.getGreen(), accentA.getBlue(), 228));
		RenderUtils.drawRect(healthBarX + (healthFill / 2.0F), healthBarY, healthFill / 2.0F, 3.8F, new Color(accentB.getRed(), accentB.getGreen(), accentB.getBlue(), 228));

		int lineY = (int) (healthBarY + 6.5F);
		if(showHeldItem.isToggled()) {
			fr.drawStringWithShadow(heldItem, textLeft, lineY, new Color(185, 197, 219, 224).getRGB());
			lineY += fr.FONT_HEIGHT;
		}
		if(showArmor.isToggled()) {
			fr.drawStringWithShadow(armorItems, textLeft, lineY, new Color(185, 197, 219, 224).getRGB());
		}

		GlStateManager.disableBlend();
		GlStateManager.depthMask(true);
		GlStateManager.enableDepth();
		GlStateManager.enableLighting();
		GL11.glColor4f(1F, 1F, 1F, 1F);
		GlStateManager.popMatrix();
	}

	private String getHeldItemName(EntityPlayer player) {
		ItemStack held = player.getCurrentEquippedItem();
		if(held == null) {
			return "None";
		}
		return ColorUtils.removeColorCode(held.getDisplayName());
	}

	private String getArmorDisplayName(EntityPlayer player) {
		StringBuilder armorNames = new StringBuilder();
		for(int i = 3; i >= 0; i--) {
			ItemStack armor = player.inventory.armorInventory[i];
			if(armor != null) {
				if(armorNames.length() > 0) {
					armorNames.append(", ");
				}
				armorNames.append(ColorUtils.removeColorCode(armor.getDisplayName()));
			}
		}

		return armorNames.length() == 0 ? "None" : armorNames.toString();
	}

	private float getAnimatedHealthRatio(EntityPlayer player) {
		float healthRatio = MathUtils.clamp(player.getHealth() / Math.max(1.0F, player.getMaxHealth()));
		UUID uuid = player.getUniqueID();
		SimpleAnimation animation = healthAnimationMap.get(uuid);
		if(animation == null) {
			animation = new SimpleAnimation(healthRatio);
			healthAnimationMap.put(uuid, animation);
		}
		animation.setAnimation(healthRatio, 18);
		return MathUtils.clamp(animation.getValue());
	}

	private String formatHealthValue(EntityPlayer player) {
		float value = Math.max(0.0F, player.getHealth());
		return String.format("%.1f", value);
	}

	private String truncate(String text, int max) {
		if(text == null) {
			return "";
		}
		if(text.length() <= max) {
			return text;
		}
		return text.substring(0, Math.max(0, max - 3)) + "...";
	}

	private void cleanupAnimations() {
		Iterator<UUID> iterator = healthAnimationMap.keySet().iterator();
		while(iterator.hasNext()) {
			UUID uuid = iterator.next();
			EntityPlayer found = mc.theWorld.getPlayerEntityByUUID(uuid);
			if(found == null || found.isDead) {
				iterator.remove();
			}
		}
	}

	private double interpolate(double start, double end, float partialTicks) {
		return start + (end - start) * partialTicks;
	}

	public ComboSetting getThemeSetting() {
		return themeSetting;
	}

}

