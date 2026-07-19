package me.eldodebug.soar.management.mods.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;

import org.lwjgl.opengl.GL11;

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
import me.eldodebug.soar.utils.Render3DUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;

public class ESPMod extends Mod {
	private static ESPMod instance;

	private final ComboSetting modeSetting = new ComboSetting(TranslateText.MODE, this, TranslateText.BOX,
			new ArrayList<Option>(Arrays.asList(new Option(TranslateText.BOX), new Option(TranslateText.REAL))));
	private final ColorSetting colorSetting = new ColorSetting(TranslateText.COLOR, this, new Color(0, 255, 170), false);
	private final NumberSetting alphaSetting = new NumberSetting(TranslateText.ALPHA, this, 0.85, 0.05, 1.0, false);
	private final NumberSetting lineWidthSetting = new NumberSetting(TranslateText.LINE_WIDTH, this, 2, 1, 5, true);
	private final BooleanSetting fillSetting = new BooleanSetting(TranslateText.FILL, this, true);
	private final BooleanSetting outlineSetting = new BooleanSetting(TranslateText.OUTLINE, this, true);
	private Framebuffer realFramebuffer;
	private boolean renderingRealPlayers;

	public ESPMod() {
		super(TranslateText.ESP, TranslateText.ESP_DESCRIPTION, ModCategory.GHOST);
		instance = this;
	}

	public static ESPMod getInstance() {
		return instance;
	}

	public boolean isRealMode() {
		return isToggled() && modeSetting.getOption().getTranslate().equals(TranslateText.REAL);
	}

	@Override
	public void onDisable() {
		super.onDisable();
		if(realFramebuffer != null) {
			realFramebuffer.deleteFramebuffer();
			realFramebuffer = null;
		}
	}

	@EventTarget
	public void onRender3D(EventRender3D event) {
		if(mc.theWorld == null || mc.thePlayer == null) {
			return;
		}
		if(isRealMode()) {
			renderRealPlayers(event.getPartialTicks());
			return;
		}

		GlStateManager.pushMatrix();
		GlStateManager.enableBlend();
		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.disableDepth();
		GlStateManager.depthMask(false);
		GL11.glLineWidth(lineWidthSetting.getValueFloat());

		Color color = colorSetting.getColor();
		int alpha = (int) (alphaSetting.getValue() * 255);

		for(EntityPlayer player : mc.theWorld.playerEntities) {
			if(player == null || player == mc.thePlayer || player.isDead) {
				continue;
			}

			AxisAlignedBB box = getRenderBoundingBox(player, event.getPartialTicks());

			if(fillSetting.isToggled()) {
				ColorUtils.setColor(color.getRGB(), alphaSetting.getValueFloat() * 0.22F);
				Render3DUtils.drawFillBox(box);
			}

			if(outlineSetting.isToggled()) {
				RenderGlobal.drawOutlinedBoundingBox(box, color.getRed(), color.getGreen(), color.getBlue(), alpha);
			}
		}

		GlStateManager.depthMask(true);
		GlStateManager.enableDepth();
		GlStateManager.disableBlend();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
		ColorUtils.resetColor();
		GlStateManager.popMatrix();
	}

	/**
	 * Render complete players into a transparent framebuffer with its own depth
	 * buffer, then composite it over the world. Walls are absent from that depth
	 * buffer, while body parts, skin layers, armor and held items still depth-test
	 * against each other exactly as they do in Minecraft's normal player pass.
	 */
	private void renderRealPlayers(float partialTicks) {
		if(renderingRealPlayers || !OpenGlHelper.isFramebufferEnabled()) {
			return;
		}
		ensureRealFramebuffer();
		if(realFramebuffer == null) {
			return;
		}

		renderingRealPlayers = true;
		RenderManager renderManager = mc.getRenderManager();
		boolean shadowsWereEnabled = renderManager.isRenderShadow();
		try {
			// glClear obeys these masks. EventRender3D can be reached after particle
			// rendering left depth writes disabled, which otherwise preserves stale
			// player depth/alpha from the previous frame.
			GlStateManager.colorMask(true, true, true, true);
			GlStateManager.depthMask(true);
			realFramebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
			realFramebuffer.framebufferClear();
			realFramebuffer.bindFramebuffer(true);
			GlStateManager.enableDepth();
			GlStateManager.depthMask(true);
			GlStateManager.enableAlpha();
			GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
			GlStateManager.enableTexture2D();
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			mc.entityRenderer.enableLightmap();
			RenderHelper.enableStandardItemLighting();
			renderManager.setRenderShadow(false);

			for(EntityPlayer player : mc.theWorld.playerEntities) {
				if(player == null || player == mc.thePlayer || player.isDead) {
					continue;
				}
				renderManager.renderEntityStatic(player, partialTicks, false);
			}
		} finally {
			renderManager.setRenderShadow(shadowsWereEnabled);
			RenderHelper.disableStandardItemLighting();
			mc.entityRenderer.disableLightmap();
			mc.getFramebuffer().bindFramebuffer(true);
			renderingRealPlayers = false;
		}

		// framebufferRenderExt installs an orthographic projection. Preserve the
		// world matrices because EventRender3D runs immediately before the hand.
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPushMatrix();
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPushMatrix();
		boolean fogWasEnabled = GL11.glIsEnabled(GL11.GL_FOG);
		GlStateManager.disableFog();
		GlStateManager.disableLighting();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
				GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
		realFramebuffer.framebufferRenderExt(mc.displayWidth, mc.displayHeight, false);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPopMatrix();
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPopMatrix();
		GL11.glMatrixMode(GL11.GL_MODELVIEW);

		GlStateManager.colorMask(true, true, true, true);
		GlStateManager.depthMask(true);
		GlStateManager.enableDepth();
		GlStateManager.enableAlpha();
		GlStateManager.disableBlend();
		GlStateManager.disableLighting();
		if(fogWasEnabled) {
			GlStateManager.enableFog();
		} else {
			GlStateManager.disableFog();
		}
		GlStateManager.enableTexture2D();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private void ensureRealFramebuffer() {
		if(realFramebuffer != null
				&& realFramebuffer.framebufferWidth == mc.displayWidth
				&& realFramebuffer.framebufferHeight == mc.displayHeight) {
			return;
		}
		if(realFramebuffer != null) {
			realFramebuffer.deleteFramebuffer();
		}
		realFramebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, true);
		realFramebuffer.setFramebufferFilter(GL11.GL_NEAREST);
	}

	private AxisAlignedBB getRenderBoundingBox(EntityPlayer player, float partialTicks) {
		double interpX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
		double interpY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
		double interpZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

		double x = interpX - mc.getRenderManager().viewerPosX;
		double y = interpY - mc.getRenderManager().viewerPosY;
		double z = interpZ - mc.getRenderManager().viewerPosZ;

		AxisAlignedBB box = player.getEntityBoundingBox();
		AxisAlignedBB interpBox = new AxisAlignedBB(
				box.minX - player.posX + x,
				box.minY - player.posY + y,
				box.minZ - player.posZ + z,
				box.maxX - player.posX + x,
				box.maxY - player.posY + y,
				box.maxZ - player.posZ + z
		);
		return interpBox.expand(0.05D, 0.1D, 0.05D);
	}
}
