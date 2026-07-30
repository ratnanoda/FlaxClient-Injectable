package me.eldodebug.soar.management.mods.impl.skin3d.layers;

import java.util.Set;

import com.google.common.collect.Sets;

import me.eldodebug.soar.attach.MinecraftAccess;
import me.eldodebug.soar.attach.PlayerState;
import me.eldodebug.soar.management.mods.impl.Skin3DMod;
import me.eldodebug.soar.utils.SkinUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class HeadLayerFeatureRenderer implements LayerRenderer<AbstractClientPlayer> {

	private Set<Item> hideHeadLayers = Sets.newHashSet(Items.skull);
    private final boolean thinArms;
	private static final Minecraft mc = Minecraft.getMinecraft();
	private RenderPlayer playerRenderer;
	
    public HeadLayerFeatureRenderer(RenderPlayer playerRenderer) {
        thinArms = MinecraftAccess.hasThinArms(playerRenderer);
        this.playerRenderer = playerRenderer;
    }

    @Override
    public void doRenderLayer(AbstractClientPlayer player, float paramFloat1, float paramFloat2, float paramFloat3, float deltaTick, float paramFloat5, float paramFloat6, float paramFloat7) {
    	
		if (!player.hasSkin() || player.isInvisible()) {
			return;
		}
		
		if(mc.thePlayer.getPositionVector().squareDistanceTo(player.getPositionVector()) > Skin3DMod.getInstance().getRenderDistanceLOD() * Skin3DMod.getInstance().getRenderDistanceLOD()) {
			return;
		}
		
		ItemStack itemStack = player.getEquipmentInSlot(1);
		
		if (itemStack != null && hideHeadLayers.contains(itemStack.getItem())) {
			return;
		}
		
		if(PlayerState.getHeadLayer(player) == null && !setupModel(player)) {
			return;
		}

		renderCustomHelmet(player, deltaTick);
	}

	private boolean setupModel(AbstractClientPlayer abstractClientPlayerEntity) {
		
		if(!SkinUtils.hasCustomSkin(abstractClientPlayerEntity)) {
			return false;
		}
		
		SkinUtils.setup3dLayers(abstractClientPlayerEntity, thinArms, null);
		
		return true;
	}

	public void renderCustomHelmet(AbstractClientPlayer abstractClientPlayer, float deltaTick) {
		
		if(PlayerState.getHeadLayer(abstractClientPlayer) == null) {
			return;
		}
		
		if(playerRenderer.getMainModel().bipedHead.isHidden) {
			return;
		}
		
		float voxelSize = Skin3DMod.getInstance().getHeadVoxelSize();

		GlStateManager.pushMatrix();
		
		if(abstractClientPlayer.isSneaking()) {
            GlStateManager.translate(0.0F, 0.2F, 0.0F);
        }
		
		playerRenderer.getMainModel().bipedHead.postRender(0.0625F);
	    GlStateManager.scale(0.0625, 0.0625, 0.0625);
		GlStateManager.scale(voxelSize, voxelSize, voxelSize);
		
		boolean tintRed = abstractClientPlayer.hurtTime > 0 || abstractClientPlayer.deathTime > 0;
		PlayerState.getHeadLayer(abstractClientPlayer).render(tintRed);
		GlStateManager.popMatrix();
	}

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
