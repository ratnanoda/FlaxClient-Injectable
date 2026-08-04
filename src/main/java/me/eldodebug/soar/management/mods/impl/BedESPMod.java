package me.eldodebug.soar.management.mods.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventRender3D;
import me.eldodebug.soar.management.event.impl.EventUpdate;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.ColorSetting;
import me.eldodebug.soar.management.mods.settings.impl.ComboSetting;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import me.eldodebug.soar.management.mods.settings.impl.combo.Option;
import me.eldodebug.soar.utils.ColorUtils;
import me.eldodebug.soar.utils.Render3DUtils;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class BedESPMod extends Mod {

    private static final int MAX_SCAN_CHUNKS = 8;
    private static final int VERTICAL_RANGE = 64;
    private static final int SCAN_INTERVAL_TICKS = 20;

    private final ColorSetting colorSetting = new ColorSetting(
            TranslateText.COLOR, this, new Color(255, 64, 64), false);
    private final NumberSetting alphaSetting = new NumberSetting(
            TranslateText.ALPHA, this, 0.85, 0.05, 1.0, false);
    private final NumberSetting lineWidthSetting = new NumberSetting(
            TranslateText.LINE_WIDTH, this, 2, 1, 5, true);
    private final ComboSetting modeSetting = new ComboSetting(
            TranslateText.MODE,
            this,
            TranslateText.BOX,
            new ArrayList<Option>(Arrays.asList(
                    new Option(TranslateText.OUTLINE),
                    new Option(TranslateText.BOX),
                    new Option(TranslateText.GLOW))));

    private final List<Bed> beds = new ArrayList<Bed>();
    private int scanTimer;

    public BedESPMod() {
        super(TranslateText.BED_ESP, TranslateText.BED_ESP_DESCRIPTION, ModCategory.GHOST);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        beds.clear();
        scanTimer = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        beds.clear();
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if(mc.theWorld == null || mc.thePlayer == null) {
            beds.clear();
            return;
        }

        if(scanTimer > 0) {
            scanTimer--;
            return;
        }

        scanTimer = SCAN_INTERVAL_TICKS;
        scanBeds();
    }

    private void scanBeds() {
        EntityPlayer player = mc.thePlayer;
        int viewChunks = Math.min(mc.gameSettings.renderDistanceChunks, MAX_SCAN_CHUNKS);

        int px = MathHelper.floor_double(player.posX);
        int py = MathHelper.floor_double(player.posY);
        int pz = MathHelper.floor_double(player.posZ);

        int pcx = px >> 4;
        int pcz = pz >> 4;
        int minSection = Math.max(0, (py - VERTICAL_RANGE) >> 4);
        int maxSection = Math.min(15, (py + VERTICAL_RANGE) >> 4);

        Map<BlockPos, Bed> found = new LinkedHashMap<BlockPos, Bed>();

        for(int cx = pcx - viewChunks; cx <= pcx + viewChunks; cx++) {
            for(int cz = pcz - viewChunks; cz <= pcz + viewChunks; cz++) {
                Chunk chunk = mc.theWorld.getChunkFromChunkCoords(cx, cz);
                if(chunk.isEmpty()) continue;

                ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();
                int baseX = cx << 4;
                int baseZ = cz << 4;

                for(int sectionIndex = minSection;
                        sectionIndex <= maxSection && sectionIndex < sections.length;
                        sectionIndex++) {
                    ExtendedBlockStorage section = sections[sectionIndex];
                    if(section == null || section.isEmpty()) continue;

                    int baseY = sectionIndex << 4;
                    for(int localY = 0; localY < 16; localY++) {
                        for(int localX = 0; localX < 16; localX++) {
                            for(int localZ = 0; localZ < 16; localZ++) {
                                IBlockState state = section.get(localX, localY, localZ);
                                if(state.getBlock() != Blocks.bed) continue;

                                int worldX = baseX + localX;
                                int worldY = baseY + localY;
                                int worldZ = baseZ + localZ;
                                EnumFacing facing =
                                        (EnumFacing) state.getValue(BlockDirectional.FACING);
                                boolean head = state.getValue(BlockBed.PART)
                                        == BlockBed.EnumPartType.HEAD;

                                BlockPos here = new BlockPos(worldX, worldY, worldZ);
                                BlockPos footPos = head
                                        ? here.offset(facing.getOpposite())
                                        : here;
                                BlockPos headPos = head ? here : here.offset(facing);
                                if(found.containsKey(footPos)) continue;

                                found.put(footPos, new Bed(footPos, headPos));
                            }
                        }
                    }
                }
            }
        }

        beds.clear();
        beds.addAll(found.values());
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if(mc.theWorld == null || mc.thePlayer == null || beds.isEmpty()) return;

        RenderManager renderManager = mc.getRenderManager();
        double viewX = renderManager.viewerPosX;
        double viewY = renderManager.viewerPosY;
        double viewZ = renderManager.viewerPosZ;
        Color color = colorSetting.getColor();
        float alpha = alphaSetting.getValueFloat();
        int alphaInt = (int) (alpha * 255);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GL11.glLineWidth(lineWidthSetting.getValueFloat());

        boolean outlineMode = modeSetting.getOption().getTranslate()
                .equals(TranslateText.OUTLINE);
        boolean glowMode = modeSetting.getOption().getTranslate()
                .equals(TranslateText.GLOW);

        for(Bed bed : beds) {
            AxisAlignedBB box = new AxisAlignedBB(
                    bed.box.minX - viewX,
                    bed.box.minY - viewY,
                    bed.box.minZ - viewZ,
                    bed.box.maxX - viewX,
                    bed.box.maxY - viewY,
                    bed.box.maxZ - viewZ);

            if(glowMode) {
                for(int layer = 1; layer <= 3; layer++) {
                    double grow = 0.04D * layer;
                    ColorUtils.setColor(color.getRGB(), alpha * 0.12F);
                    Render3DUtils.drawFillBox(box.expand(grow, grow, grow));
                }
            } else if(!outlineMode) {
                ColorUtils.setColor(color.getRGB(), alpha * 0.22F);
                Render3DUtils.drawFillBox(box);
            }

            RenderGlobal.drawOutlinedBoundingBox(
                    box,
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue(),
                    alphaInt);
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        ColorUtils.resetColor();
        GlStateManager.popMatrix();
    }

    private static class Bed {
        private final AxisAlignedBB box;

        private Bed(BlockPos footPos, BlockPos headPos) {
            int minX = Math.min(footPos.getX(), headPos.getX());
            int minZ = Math.min(footPos.getZ(), headPos.getZ());
            int maxX = Math.max(footPos.getX(), headPos.getX());
            int maxZ = Math.max(footPos.getZ(), headPos.getZ());

            this.box = new AxisAlignedBB(
                    minX,
                    footPos.getY(),
                    minZ,
                    maxX + 1,
                    footPos.getY() + 0.5625D,
                    maxZ + 1);
        }
    }
}
