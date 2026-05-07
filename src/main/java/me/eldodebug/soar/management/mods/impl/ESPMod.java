package me.eldodebug.soar.management.mods.impl;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventRender3D;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.BooleanSetting;
import me.eldodebug.soar.management.mods.settings.impl.ColorSetting;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import me.eldodebug.soar.utils.ColorUtils;
import me.eldodebug.soar.utils.Render3DUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;

public class ESPMod extends Mod {

	private final ColorSetting colorSetting = new ColorSetting(TranslateText.COLOR, this, new Color(0, 255, 170), false);
	private final NumberSetting alphaSetting = new NumberSetting(TranslateText.ALPHA, this, 0.85, 0.05, 1.0, false);
	private final NumberSetting lineWidthSetting = new NumberSetting(TranslateText.LINE_WIDTH, this, 2, 1, 5, true);
	private final BooleanSetting fillSetting = new BooleanSetting(TranslateText.FILL, this, true);
	private final BooleanSetting outlineSetting = new BooleanSetting(TranslateText.OUTLINE, this, true);

	public ESPMod() {
		super(TranslateText.ESP, TranslateText.ESP_DESCRIPTION, ModCategory.GHOST);
	}

	@EventTarget
	public void onRender3D(EventRender3D event) {
		if(mc.theWorld == null || mc.thePlayer == null) {
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
