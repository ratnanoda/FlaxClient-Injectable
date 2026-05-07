package me.eldodebug.soar.management.mods.impl;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import me.eldodebug.soar.injection.interfaces.IMixinRenderManager;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventRender3D;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import me.eldodebug.soar.utils.MathUtils;
import me.eldodebug.soar.utils.render.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;

public class HealthbarMod extends Mod {

	private final NumberSetting alphaSetting = new NumberSetting(TranslateText.ALPHA, this, 0.9D, 0.15D, 1.0D, false);

	public HealthbarMod() {
		super(TranslateText.HEALTH_BAR, TranslateText.HEALTH_BAR_DESCRIPTION, ModCategory.OTHER);
	}

	@EventTarget
	public void onRender3D(EventRender3D event) {
		if(mc.theWorld == null || mc.thePlayer == null) {
			return;
		}

		for(EntityPlayer player : mc.theWorld.playerEntities) {
			if(player == null || player == mc.thePlayer || player.isDead) {
				continue;
			}

			renderHealthbar(player, event.getPartialTicks());
		}
	}

	private void renderHealthbar(EntityPlayer player, float partialTicks) {
		double x = interpolate(player.lastTickPosX, player.posX, partialTicks) - ((IMixinRenderManager) mc.getRenderManager()).getRenderPosX();
		double y = interpolate(player.lastTickPosY, player.posY, partialTicks) - ((IMixinRenderManager) mc.getRenderManager()).getRenderPosY() + player.height + 0.62D;
		double z = interpolate(player.lastTickPosZ, player.posZ, partialTicks) - ((IMixinRenderManager) mc.getRenderManager()).getRenderPosZ();

		float distance = mc.thePlayer.getDistanceToEntity(player);
		float scale = 0.018F * Math.max(1.0F, distance * 0.12F);
		float healthRatio = MathUtils.clamp(player.getHealth() / Math.max(1.0F, player.getMaxHealth()));
		float alpha = (float) alphaSetting.getValue();

		float barHeight = 29.0F;
		float barWidth = 3.6F;
		float barX = 16.5F;
		float barY = -1.5F;

		int red = (int) ((1.0F - healthRatio) * 255.0F);
		int green = (int) (healthRatio * 255.0F);
		float fillHeight = (barHeight - 2.0F) * healthRatio;
		float fillY = barY + (barHeight - 1.0F) - fillHeight;

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

		RenderUtils.drawRect(barX, barY, barWidth, barHeight, new Color(15, 15, 15, (int) (190 * alpha)));
		RenderUtils.drawOutline(barX - 0.2F, barY - 0.2F, barWidth + 0.4F, barHeight + 0.4F, 0.75F, new Color(255, 255, 255, (int) (205 * alpha)));
		RenderUtils.drawRect(barX + 1.0F, fillY, barWidth - 2.0F, fillHeight, new Color(red, green, 75, (int) (230 * alpha)));

		GlStateManager.disableBlend();
		GlStateManager.depthMask(true);
		GlStateManager.enableDepth();
		GlStateManager.enableLighting();
		GL11.glColor4f(1F, 1F, 1F, 1F);
		GlStateManager.popMatrix();
	}

	private double interpolate(double from, double to, float partialTicks) {
		return from + ((to - from) * partialTicks);
	}
}

