package me.eldodebug.soar.management.mods.impl;

import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.lwjgl.opengl.GL11;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.color.AccentColor;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventRender3D;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import me.eldodebug.soar.utils.ColorUtils;
import me.eldodebug.soar.utils.MathUtils;
import me.eldodebug.soar.utils.Render3DUtils;
import me.eldodebug.soar.utils.animation.simple.SimpleAnimation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;

public class HealthbarMod extends Mod {

	private final NumberSetting alphaSetting = new NumberSetting(TranslateText.ALPHA, this, 0.9D, 0.15D, 1.0D, false);
	private final NumberSetting barWidthSetting = new NumberSetting(TranslateText.WIDTH, this, 1.55D, 0.7D, 2.8D, false);
	private final NumberSetting sideOffsetSetting = new NumberSetting(TranslateText.SIDE_OFFSET, this, 0.08D, 0.0D, 0.45D, false);
	private final NumberSetting renderRangeSetting = new NumberSetting(TranslateText.RANGE, this, 42.0D, 10.0D, 120.0D, false);
	private final Map<UUID, SimpleAnimation> healthAnimationMap = new HashMap<UUID, SimpleAnimation>();
	private final Map<UUID, SimpleAnimation> delayedAnimationMap = new HashMap<UUID, SimpleAnimation>();

	public HealthbarMod() {
		super(TranslateText.HEALTH_BAR, TranslateText.HEALTH_BAR_DESCRIPTION, ModCategory.GHOST);
	}

	@EventTarget
	public void onRender3D(EventRender3D event) {
		if(mc.theWorld == null || mc.thePlayer == null) {
			return;
		}

		cleanupAnimations();
		float alpha = (float) alphaSetting.getValue();
		double maxRange = renderRangeSetting.getValue();
		double maxRangeSq = maxRange * maxRange;

		GlStateManager.pushMatrix();
		GlStateManager.enableBlend();
		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.disableDepth();
		GlStateManager.depthMask(false);
		GL11.glLineWidth(1.0F);

		for(EntityPlayer player : mc.theWorld.playerEntities) {
			if(player == null || player == mc.thePlayer || player.isDead) {
				continue;
			}

			if(player.isInvisible()) {
				continue;
			}

			if(mc.thePlayer.getDistanceSqToEntity(player) > maxRangeSq) {
				continue;
			}

			renderHealthbar(player, event.getPartialTicks(), alpha);
		}

		GlStateManager.depthMask(true);
		GlStateManager.enableDepth();
		GlStateManager.disableBlend();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
		ColorUtils.resetColor();
		GlStateManager.popMatrix();
	}

	private void renderHealthbar(EntityPlayer player, float partialTicks, float alpha) {
		AxisAlignedBB playerBox = getRenderBoundingBox(player, partialTicks);
		double barHeight = playerBox.maxY - playerBox.minY;
		if(barHeight <= 0.0D) {
			return;
		}

		float healthRatio = MathUtils.clamp(player.getHealth() / Math.max(1.0F, player.getMaxHealth()));
		float animatedHealth = getAnimatedHealth(player, healthRatio);
		float delayedHealth = getDelayedHealth(player, animatedHealth);

		double sideGap = sideOffsetSetting.getValue();
		double widthScale = barWidthSetting.getValue();
		double barHalfSize = Math.max(0.018D, 0.030D * widthScale);
		double centerX = (playerBox.minX + playerBox.maxX) * 0.5D;
		double centerZ = (playerBox.minZ + playerBox.maxZ) * 0.5D;

		// Always place the bar on the target's right side from local player's viewpoint.
		double toTargetLength = Math.sqrt((centerX * centerX) + (centerZ * centerZ));
		double rightX;
		double rightZ;

		if(toTargetLength < 1.0E-4D) {
			rightX = 1.0D;
			rightZ = 0.0D;
		} else {
			double invLength = 1.0D / toTargetLength;
			rightX = centerZ * invLength;
			rightZ = -centerX * invLength;
		}

		double playerRadius = Math.max(playerBox.maxX - centerX, playerBox.maxZ - centerZ);
		double radialDistance = playerRadius + sideGap + barHalfSize;
		double barCenterX = centerX + (rightX * radialDistance);
		double barCenterZ = centerZ + (rightZ * radialDistance);

		double barMinX = barCenterX - barHalfSize;
		double barMaxX = barCenterX + barHalfSize;
		double barMinZ = barCenterZ - barHalfSize;
		double barMaxZ = barCenterZ + barHalfSize;
		double barMinY = playerBox.minY;
		double barMaxY = playerBox.maxY;

		AxisAlignedBB barBackground = new AxisAlignedBB(barMinX, barMinY, barMinZ, barMaxX, barMaxY, barMaxZ);
		double innerPadding = 0.003D;
		double innerMinX = barMinX + innerPadding;
		double innerMaxX = barMaxX - innerPadding;
		double innerMinZ = barMinZ + innerPadding;
		double innerMaxZ = barMaxZ - innerPadding;

		double delayedTop = barMinY + (barHeight * MathUtils.clamp(delayedHealth));
		double currentTop = barMinY + (barHeight * MathUtils.clamp(animatedHealth));

		AxisAlignedBB delayedBar = new AxisAlignedBB(innerMinX, barMinY, innerMinZ, innerMaxX, delayedTop, innerMaxZ);
		AxisAlignedBB currentBar = new AxisAlignedBB(innerMinX, barMinY, innerMinZ, innerMaxX, currentTop, innerMaxZ);

		int red = (int) ((1.0F - animatedHealth) * 220.0F);
		int green = (int) (120.0F + (animatedHealth * 135.0F));
		AccentColor accentColor = Glide.getInstance().getColorManager().getCurrentColor();
		Color accent = accentColor.getInterpolateColor();
		ColorUtils.setColor(new Color(0, 0, 0).getRGB(), alpha * 0.32F);
		Render3DUtils.drawFillBox(barBackground);

		ColorUtils.setColor(new Color(18, 23, 30).getRGB(), alpha * 0.88F);
		Render3DUtils.drawFillBox(new AxisAlignedBB(innerMinX, barMinY, innerMinZ, innerMaxX, barMaxY, innerMaxZ));

		if(delayedTop > barMinY) {
			ColorUtils.setColor(new Color(255, 150, 110).getRGB(), alpha * 0.50F);
			Render3DUtils.drawFillBox(delayedBar);
		}

		if(currentTop > barMinY) {
			ColorUtils.setColor(new Color(red, green, 86).getRGB(), alpha * 0.92F);
			Render3DUtils.drawFillBox(currentBar);
		}

		int outlineAlpha = Math.max(35, (int) (alpha * 180.0F));
		RenderGlobal.drawOutlinedBoundingBox(barBackground, accent.getRed(), accent.getGreen(), accent.getBlue(), outlineAlpha);
		RenderGlobal.drawOutlinedBoundingBox(new AxisAlignedBB(innerMinX, barMinY, innerMinZ, innerMaxX, barMaxY, innerMaxZ), 255, 255, 255, Math.max(20, (int) (alpha * 80.0F)));
	}

	private AxisAlignedBB getRenderBoundingBox(EntityPlayer player, float partialTicks) {
		double interpX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
		double interpY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
		double interpZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

		double x = interpX - mc.getRenderManager().viewerPosX;
		double y = interpY - mc.getRenderManager().viewerPosY;
		double z = interpZ - mc.getRenderManager().viewerPosZ;

		AxisAlignedBB box = player.getEntityBoundingBox();
		return new AxisAlignedBB(
				box.minX - player.posX + x,
				box.minY - player.posY + y,
				box.minZ - player.posZ + z,
				box.maxX - player.posX + x,
				box.maxY - player.posY + y,
				box.maxZ - player.posZ + z
		);
	}

	private float getAnimatedHealth(EntityPlayer player, float healthRatio) {
		UUID uuid = player.getUniqueID();
		SimpleAnimation animation = healthAnimationMap.get(uuid);
		if(animation == null) {
			animation = new SimpleAnimation(healthRatio);
			healthAnimationMap.put(uuid, animation);
		}
		animation.setAnimation(healthRatio, 24);
		return MathUtils.clamp(animation.getValue());
	}

	private float getDelayedHealth(EntityPlayer player, float animatedHealth) {
		UUID uuid = player.getUniqueID();
		SimpleAnimation delayed = delayedAnimationMap.get(uuid);
		if(delayed == null) {
			delayed = new SimpleAnimation(animatedHealth);
			delayedAnimationMap.put(uuid, delayed);
		}

		float delayedValue = MathUtils.clamp(delayed.getValue());
		if(delayedValue < animatedHealth) {
			delayed.setValue(animatedHealth);
			delayedValue = animatedHealth;
		}

		delayed.setAnimation(animatedHealth, delayedValue > animatedHealth ? 8 : 24);
		return MathUtils.clamp(delayed.getValue());
	}

	private void cleanupAnimations() {
		Iterator<UUID> healthIterator = healthAnimationMap.keySet().iterator();
		while(healthIterator.hasNext()) {
			UUID uuid = healthIterator.next();
			EntityPlayer found = mc.theWorld.getPlayerEntityByUUID(uuid);
			if(found == null || found.isDead) {
				healthIterator.remove();
			}
		}

		Iterator<UUID> delayedIterator = delayedAnimationMap.keySet().iterator();
		while(delayedIterator.hasNext()) {
			UUID uuid = delayedIterator.next();
			EntityPlayer found = mc.theWorld.getPlayerEntityByUUID(uuid);
			if(found == null || found.isDead) {
				delayedIterator.remove();
			}
		}
	}
}

