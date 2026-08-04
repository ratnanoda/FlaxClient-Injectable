package me.eldodebug.soar.management.mods.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lwjgl.opengl.GL11;

import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventRender2D;
import me.eldodebug.soar.management.event.impl.EventRender3D;
import me.eldodebug.soar.management.event.impl.EventUpdate;
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
import me.eldodebug.soar.utils.render.RenderUtils;
import me.eldodebug.soar.utils.render.WorldToScreen;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
    private static final int MAX_DEFENSE_ICONS = 8;

    private static final int ICON_SIZE = 18;
    private static final int ICON_GAP = 3;
    private static final int PANEL_PADDING = 6;
    private static final int MAX_ICONS_PER_ROW = 4;

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
    private final BooleanSetting checkDefBlockSetting =
            new CheckDefBlockSetting(this);

    private final List<Bed> beds = new ArrayList<Bed>();
    private final Map<BlockPos, PanelMotion> panelMotions =
            new HashMap<BlockPos, PanelMotion>();
    private int scanTimer;
    private long lastPanelFrameNanos;

    public BedESPMod() {
        super(TranslateText.BED_ESP, TranslateText.BED_ESP_DESCRIPTION, ModCategory.GHOST);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        beds.clear();
        panelMotions.clear();
        lastPanelFrameNanos = 0L;
        scanTimer = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        beds.clear();
        panelMotions.clear();
        lastPanelFrameNanos = 0L;
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if(mc.theWorld == null || mc.thePlayer == null) {
            beds.clear();
            panelMotions.clear();
            lastPanelFrameNanos = 0L;
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

        int playerX = MathHelper.floor_double(player.posX);
        int playerY = MathHelper.floor_double(player.posY);
        int playerZ = MathHelper.floor_double(player.posZ);

        int playerChunkX = playerX >> 4;
        int playerChunkZ = playerZ >> 4;
        int minSection = Math.max(0, (playerY - VERTICAL_RANGE) >> 4);
        int maxSection = Math.min(15, (playerY + VERTICAL_RANGE) >> 4);

        Map<BlockPos, Bed> found = new LinkedHashMap<BlockPos, Bed>();

        for(int chunkX = playerChunkX - viewChunks;
                chunkX <= playerChunkX + viewChunks;
                chunkX++) {
            for(int chunkZ = playerChunkZ - viewChunks;
                    chunkZ <= playerChunkZ + viewChunks;
                    chunkZ++) {
                Chunk chunk = mc.theWorld.getChunkFromChunkCoords(chunkX, chunkZ);
                if(chunk.isEmpty()) continue;

                ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();
                int baseX = chunkX << 4;
                int baseZ = chunkZ << 4;

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

                                found.put(
                                        footPos,
                                        new Bed(
                                                footPos,
                                                headPos,
                                                computeAdjacentBlocks(footPos, headPos)));
                            }
                        }
                    }
                }
            }
        }

        beds.clear();
        beds.addAll(found.values());
        panelMotions.keySet().retainAll(found.keySet());
    }

    private List<ItemStack> computeAdjacentBlocks(BlockPos footPos, BlockPos headPos) {
        Map<String, ItemStack> uniqueStacks = new LinkedHashMap<String, ItemStack>();

        int minX = Math.min(footPos.getX(), headPos.getX()) - 1;
        int maxX = Math.max(footPos.getX(), headPos.getX()) + 1;
        int minZ = Math.min(footPos.getZ(), headPos.getZ()) - 1;
        int maxZ = Math.max(footPos.getZ(), headPos.getZ()) + 1;
        int minY = Math.min(footPos.getY(), headPos.getY());
        int maxY = Math.max(footPos.getY(), headPos.getY()) + 1;

        for(int y = minY; y <= maxY; y++) {
            for(int x = minX; x <= maxX; x++) {
                for(int z = minZ; z <= maxZ; z++) {
                    BlockPos position = new BlockPos(x, y, z);
                    if(position.equals(footPos) || position.equals(headPos)) continue;

                    int distance = Math.min(
                            shellDistance(position, footPos),
                            shellDistance(position, headPos));
                    if(distance != 1) continue;

                    IBlockState state = mc.theWorld.getBlockState(position);
                    Block block = state.getBlock();
                    if(block == Blocks.air || block == Blocks.bed) continue;

                    Item item = Item.getItemFromBlock(block);
                    if(item == null) continue;

                    int metadata;
                    try {
                        metadata = block.damageDropped(state);
                    } catch(Exception ignored) {
                        metadata = block.getMetaFromState(state);
                    }

                    String key = Item.getIdFromItem(item) + ":" + metadata;
                    if(!uniqueStacks.containsKey(key)) {
                        uniqueStacks.put(key, new ItemStack(item, 1, metadata));
                    }
                }
            }
        }

        List<ItemStack> result = new ArrayList<ItemStack>(uniqueStacks.values());
        Collections.sort(result, (first, second) -> {
            int itemCompare = Integer.compare(
                    Item.getIdFromItem(first.getItem()),
                    Item.getIdFromItem(second.getItem()));
            if(itemCompare != 0) return itemCompare;
            return Integer.compare(first.getMetadata(), second.getMetadata());
        });

        if(result.size() > MAX_DEFENSE_ICONS) {
            return new ArrayList<ItemStack>(result.subList(0, MAX_DEFENSE_ICONS));
        }
        return result;
    }

    private int shellDistance(BlockPos first, BlockPos second) {
        return Math.max(
                Math.max(
                        Math.abs(first.getX() - second.getX()),
                        Math.abs(first.getY() - second.getY())),
                Math.abs(first.getZ() - second.getZ()));
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if(mc.theWorld == null || mc.thePlayer == null || beds.isEmpty()) return;

        if(checkDefBlockSetting.isToggled()) {
            WorldToScreen.capture();
        }

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

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if(mc.theWorld == null || mc.thePlayer == null || beds.isEmpty()) return;
        if(!checkDefBlockSetting.isToggled()) return;

        ScaledResolution resolution = new ScaledResolution(mc);
        float deltaTime = panelFrameDelta();
        List<ProjectedPanel> panels = new ArrayList<ProjectedPanel>();
        Set<BlockPos> activeKeys = new HashSet<BlockPos>();

        for(Bed bed : beds) {
            activeKeys.add(bed.footPos);
            if(bed.adjacentStacks.isEmpty()) continue;

            double centerX = (bed.box.minX + bed.box.maxX) / 2.0D;
            double centerY = bed.box.maxY + 0.96D;
            double centerZ = (bed.box.minZ + bed.box.maxZ) / 2.0D;
            float[] screen = WorldToScreen.project(centerX, centerY, centerZ);
            if(screen == null) continue;

            double distance = mc.thePlayer.getDistance(centerX, centerY, centerZ);
            if(distance > MAX_SCAN_CHUNKS * 16.0D + 16.0D) continue;

            int columns = Math.min(MAX_ICONS_PER_ROW, bed.adjacentStacks.size());
            int rows =
                    (bed.adjacentStacks.size() + MAX_ICONS_PER_ROW - 1)
                            / MAX_ICONS_PER_ROW;
            int panelWidth =
                    columns * ICON_SIZE
                            + Math.max(0, columns - 1) * ICON_GAP
                            + PANEL_PADDING * 2;
            int panelHeight =
                    rows * ICON_SIZE
                            + Math.max(0, rows - 1) * ICON_GAP
                            + PANEL_PADDING * 2;
            float scale =
                    (float) Math.max(0.82D, Math.min(1.0D, 1.05D - distance / 560.0D));
            float scaledWidth = panelWidth * scale;

            float margin = Math.max(28.0F, scaledWidth * 0.65F);
            if(screen[0] < -margin
                    || screen[0] > resolution.getScaledWidth() + margin
                    || screen[1] < -margin
                    || screen[1] > resolution.getScaledHeight() + margin) {
                continue;
            }

            float targetX = screen[0];
            float targetBottomY = screen[1];
            PanelMotion motion = panelMotions.get(bed.footPos);
            if(motion == null) {
                motion = new PanelMotion(targetX, targetBottomY, scale);
                panelMotions.put(bed.footPos, motion);
            } else {
                motion.update(targetX, targetBottomY, scale, deltaTime);
            }

            panels.add(
                    new ProjectedPanel(
                            motion.x,
                            motion.y,
                            motion.scale,
                            distance,
                            bed.adjacentStacks));
        }

        panelMotions.keySet().retainAll(activeKeys);
        Collections.sort(
                panels,
                (first, second) -> Double.compare(second.distance, first.distance));
        for(ProjectedPanel panel : panels) {
            renderProjectedIcons(panel);
        }
    }

    private float panelFrameDelta() {
        long now = System.nanoTime();
        if(lastPanelFrameNanos == 0L) {
            lastPanelFrameNanos = now;
            return 1.0F / 60.0F;
        }

        float deltaTime = (now - lastPanelFrameNanos) / 1000000000.0F;
        lastPanelFrameNanos = now;
        return Math.max(0.0F, Math.min(0.05F, deltaTime));
    }

    private void renderProjectedIcons(ProjectedPanel panel) {
        List<ItemStack> icons = panel.icons;
        int columns = Math.min(MAX_ICONS_PER_ROW, icons.size());
        int rows = (icons.size() + MAX_ICONS_PER_ROW - 1) / MAX_ICONS_PER_ROW;
        int contentWidth =
                columns * ICON_SIZE + Math.max(0, columns - 1) * ICON_GAP;
        int panelWidth = contentWidth + PANEL_PADDING * 2;
        int panelHeight =
                rows * ICON_SIZE
                        + Math.max(0, rows - 1) * ICON_GAP
                        + PANEL_PADDING * 2;

        float scale = panel.scale;
        float scaledHeight = panelHeight * scale;
        float centerX = panel.screenX;
        float bottomY = panel.screenY;

        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(centerX, bottomY - scaledHeight, 0.0F);
            GlStateManager.scale(scale, scale, 1.0F);
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.enableAlpha();
            GlStateManager.enableTexture2D();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            RenderUtils.drawRoundedRect(
                    -panelWidth / 2.0F - 3.0F,
                    -1.0F,
                    panelWidth + 6.0F,
                    panelHeight + 6.0F,
                    8.0F,
                    new Color(0, 0, 0, 105));
            RenderUtils.drawRoundedRect(
                    -panelWidth / 2.0F,
                    0.0F,
                    panelWidth,
                    panelHeight,
                    6.0F,
                    new Color(18, 19, 24, 242));
            RenderUtils.drawRoundedOutline(
                    -panelWidth / 2.0F,
                    0.0F,
                    panelWidth,
                    panelHeight,
                    6.0F,
                    1.0F,
                    new Color(76, 79, 91, 155));

            Color accent = colorSetting.getColor();
            RenderUtils.drawRoundedRect(
                    -panelWidth / 2.0F + 5.0F,
                    2.0F,
                    panelWidth - 10.0F,
                    1.5F,
                    0.75F,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 210));

            GlStateManager.enableRescaleNormal();
            GlStateManager.enableColorMaterial();
            RenderHelper.enableGUIStandardItemLighting();
            float oldZLevel = mc.getRenderItem().zLevel;
            mc.getRenderItem().zLevel = 220.0F;
            try {
                for(int index = 0; index < icons.size(); index++) {
                    int row = index / MAX_ICONS_PER_ROW;
                    int column = index % MAX_ICONS_PER_ROW;
                    int itemsInRow = Math.min(
                            MAX_ICONS_PER_ROW,
                            icons.size() - row * MAX_ICONS_PER_ROW);
                    int rowWidth =
                            itemsInRow * ICON_SIZE
                                    + Math.max(0, itemsInRow - 1) * ICON_GAP;
                    int slotX = -rowWidth / 2 + column * (ICON_SIZE + ICON_GAP);
                    int slotY = PANEL_PADDING + row * (ICON_SIZE + ICON_GAP);

                    RenderUtils.drawRoundedRect(
                            slotX - 1.0F,
                            slotY - 1.0F,
                            ICON_SIZE + 2.0F,
                            ICON_SIZE + 2.0F,
                            4.0F,
                            new Color(29, 30, 37, 232));
                    RenderUtils.drawRoundedOutline(
                            slotX - 1.0F,
                            slotY - 1.0F,
                            ICON_SIZE + 2.0F,
                            ICON_SIZE + 2.0F,
                            4.0F,
                            0.7F,
                            new Color(89, 92, 105, 135));

                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                    mc.getRenderItem().renderItemAndEffectIntoGUI(
                            icons.get(index),
                            slotX + 1,
                            slotY + 1);
                }
            } finally {
                mc.getRenderItem().zLevel = oldZLevel;
                RenderHelper.disableStandardItemLighting();
                GlStateManager.disableRescaleNormal();
                GlStateManager.disableColorMaterial();
            }
        } finally {
            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
            GlStateManager.disableBlend();
            GlStateManager.enableLighting();
            GlStateManager.enableTexture2D();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
    }

    private static class CheckDefBlockSetting extends BooleanSetting {

        private CheckDefBlockSetting(Mod parent) {
            super(TranslateText.BLOCK, parent, false);
        }

        @Override
        public String getName() {
            return "CheckDefBlock";
        }

        @Override
        public String getNameKey() {
            return "text.checkdefblock";
        }
    }

    private static class ProjectedPanel {
        private final float screenX;
        private final float screenY;
        private final float scale;
        private final double distance;
        private final List<ItemStack> icons;

        private ProjectedPanel(
                float screenX,
                float screenY,
                float scale,
                double distance,
                List<ItemStack> icons) {
            this.screenX = screenX;
            this.screenY = screenY;
            this.scale = scale;
            this.distance = distance;
            this.icons = icons;
        }
    }

    private static class PanelMotion {
        private float x;
        private float y;
        private float scale;

        private PanelMotion(float x, float y, float scale) {
            this.x = x;
            this.y = y;
            this.scale = scale;
        }

        private void update(float targetX, float targetY, float targetScale, float deltaTime) {
            float positionFactor = 1.0F - (float) Math.exp(-24.0F * deltaTime);
            float scaleFactor = 1.0F - (float) Math.exp(-16.0F * deltaTime);

            x += (targetX - x) * positionFactor;
            y += (targetY - y) * positionFactor;
            scale += (targetScale - scale) * scaleFactor;

            if(Math.abs(targetX - x) < 0.01F) x = targetX;
            if(Math.abs(targetY - y) < 0.01F) y = targetY;
            if(Math.abs(targetScale - scale) < 0.0005F) scale = targetScale;
        }
    }

    private static class Bed {
        private final BlockPos footPos;
        private final AxisAlignedBB box;
        private final List<ItemStack> adjacentStacks;

        private Bed(
                BlockPos footPos,
                BlockPos headPos,
                List<ItemStack> adjacentStacks) {
            int minX = Math.min(footPos.getX(), headPos.getX());
            int minZ = Math.min(footPos.getZ(), headPos.getZ());
            int maxX = Math.max(footPos.getX(), headPos.getX());
            int maxZ = Math.max(footPos.getZ(), headPos.getZ());

            this.footPos = footPos;
            this.box = new AxisAlignedBB(
                    minX,
                    footPos.getY(),
                    minZ,
                    maxX + 1,
                    footPos.getY() + 0.5625D,
                    maxZ + 1);
            this.adjacentStacks = adjacentStacks;
        }
    }
}
