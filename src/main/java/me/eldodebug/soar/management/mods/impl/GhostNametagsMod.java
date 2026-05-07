package me.eldodebug.soar.management.mods.impl;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.injection.interfaces.IMixinRenderManager;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventRender3D;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.utils.ColorUtils;
import me.eldodebug.soar.utils.render.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;

public class GhostNametagsMod extends Mod {

	private static GhostNametagsMod instance;

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

		for(EntityPlayer player : mc.theWorld.playerEntities) {
			if(player == null || player == mc.thePlayer || player.isDead) {
				continue;
			}

			renderNameTag(player, event.getPartialTicks());
		}
	}

	private void renderNameTag(EntityPlayer player, float partialTicks) {
		double x = interpolate(player.lastTickPosX, player.posX, partialTicks) - ((IMixinRenderManager) mc.getRenderManager()).getRenderPosX();
		double y = interpolate(player.lastTickPosY, player.posY, partialTicks) - ((IMixinRenderManager) mc.getRenderManager()).getRenderPosY() + player.height + 0.62D;
		double z = interpolate(player.lastTickPosZ, player.posZ, partialTicks) - ((IMixinRenderManager) mc.getRenderManager()).getRenderPosZ();

		String name = ColorUtils.removeColorCode(player.getDisplayName().getFormattedText());
		if(name == null || name.trim().isEmpty()) {
			name = player.getName();
		}

		String secondary = Math.max(0, Math.round(player.getHealth())) + " HP  |  " + Math.max(1, Math.round(mc.thePlayer.getDistanceToEntity(player))) + "m";
		float distance = mc.thePlayer.getDistanceToEntity(player);
		float scale = 0.018F * Math.max(1.0F, distance * 0.12F);

		int nameWidth = fr.getStringWidth(name);
		int subWidth = fr.getStringWidth(secondary);
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
		fr.drawStringWithShadow(secondary, -subWidth / 2.0F, fr.FONT_HEIGHT + 1F, new Color(225, 225, 225, 230).getRGB());

		GlStateManager.disableBlend();
		GlStateManager.depthMask(true);
		GlStateManager.enableDepth();
		GlStateManager.enableLighting();
		GL11.glColor4f(1F, 1F, 1F, 1F);
		GlStateManager.popMatrix();
	}

	private double interpolate(double start, double end, float partialTicks) {
		return start + (end - start) * partialTicks;
	}
}

