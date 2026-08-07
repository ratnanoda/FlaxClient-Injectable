/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  imgui.ImDrawList
 *  imgui.ImGui
 *  imgui.ImVec2
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package recode.usefultools.latest.Modules.Visual.BlockESP;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.ModuleManager;
import recode.usefultools.latest.Modules.Visual.BlockESP.BlockESP_h;
import recode.usefultools.latest.Modules.Visual.Interface.Interface;

public class BlockESP
extends BaseModule<BlockESP_h> {
    public static BlockESP instance;
    private final Map<BlockPos, Integer> cachedBlocks = new ConcurrentHashMap<BlockPos, Integer>();
    private final Map<BlockPos, EspRenderCache> renderCaches = new ConcurrentHashMap<BlockPos, EspRenderCache>();
    private int searchCenterX = 0;
    private int searchCenterZ = 0;
    private int currentChunkX = 0;
    private int currentChunkZ = 0;
    private int subChunkIndex = 0;
    private int directionIndex = 0;
    private int steps = 1;
    private int stepsCount = 0;
    private final static int[][] DIRECTIONS;
    private double lastX = 0.0;
    private double lastZ = 0.0;

    public BlockESP() {
        super(new BlockESP_h());
        instance = this;
    }

    @Override
    public void onEnable() {
        this.reset();
    }

    @Override
    public void onDisable() {
        this.cachedBlocks.clear();
        this.renderCaches.clear();
    }

    private void reset() {
        this.cachedBlocks.clear();
        this.renderCaches.clear();
        this.subChunkIndex = 0;
        this.directionIndex = 0;
        this.steps = 1;
        this.stepsCount = 0;
        if (BlockESP.mc.player != null) {
            BlockPos playerPos = BlockESP.mc.player.blockPosition();
            this.searchCenterX = playerPos.getX() >> 4;
            this.searchCenterZ = playerPos.getZ() >> 4;
            this.currentChunkX = this.searchCenterX;
            this.currentChunkZ = this.searchCenterZ;
            this.lastX = BlockESP.mc.player.getX();
            this.lastZ = BlockESP.mc.player.getZ();
        }
    }

    private void moveToNext() {
        if (BlockESP.mc.level == null || BlockESP.mc.player == null) {
            this.reset();
            return;
        }
        ClientLevel heightAccessor = BlockESP.mc.level;
        int buildDepth = heightAccessor.getMinY();
        int numSubchunks = heightAccessor.getHeight() / 16;
        int playerSubChunkY = BlockESP.mc.player.blockPosition().getY() - buildDepth >> 4;
        int minYRange = Math.max(0, playerSubChunkY - (int)((BlockESP_h)this.h).renderChunksY.value);
        int maxYRange = Math.min(numSubchunks - 1, playerSubChunkY + (int)((BlockESP_h)this.h).renderChunksY.value);
        if (this.subChunkIndex < minYRange) {
            this.subChunkIndex = minYRange;
        }
        if (this.subChunkIndex < maxYRange) {
            ++this.subChunkIndex;
            return;
        }
        this.currentChunkX += DIRECTIONS[this.directionIndex][0];
        this.currentChunkZ += DIRECTIONS[this.directionIndex][1];
        ++this.stepsCount;
        if (this.stepsCount >= this.steps) {
            this.stepsCount = 0;
            this.directionIndex = (this.directionIndex + 1) % DIRECTIONS.length;
            if (this.directionIndex % 2 == 0) {
                ++this.steps;
            }
        }
        this.subChunkIndex = minYRange;
    }

    private void processSubChunk(int chunkX, int chunkZ, int subYIndex) {
        if (BlockESP.mc.level == null || BlockESP.mc.player == null) {
            return;
        }
        ClientLevel heightAccessor = BlockESP.mc.level;
        int buildDepth = heightAccessor.getMinY();
        int numSubchunks = heightAccessor.getHeight() / 16;
        if (subYIndex < 0 || subYIndex >= numSubchunks) {
            return;
        }
        int startX = chunkX * 16;
        int startZ = chunkZ * 16;
        int startY = buildDepth + subYIndex * 16;
        List<String> enabledBlocks = ((BlockESP_h)this.h).targetBlocks.value;
        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                for (int y = 0; y < 16; ++y) {
                    boolean isBed;
                    BlockPos targetPos = new BlockPos(startX + x, startY + y, startZ + z);
                    BlockState state = BlockESP.mc.level.getBlockState(targetPos);
                    if (state.isAir()) {
                        this.cachedBlocks.remove(targetPos);
                        continue;
                    }
                    String blockName = BuiltInRegistries.BLOCK.getKey((Object)state.getBlock()).getPath().toLowerCase();
                    boolean bl = isBed = blockName.contains("bed") && !blockName.contains("bedrock");
                    if (enabledBlocks.contains(blockName) || ((BlockESP_h)this.h).bed.value && isBed) {
                        if (((BlockESP_h)this.h).exposedOnly.value && !this.isBlockExposed(targetPos)) {
                            this.cachedBlocks.remove(targetPos);
                            continue;
                        }
                        int color = this.getColorForBlock(blockName, targetPos.hashCode());
                        this.cachedBlocks.put(targetPos, color);
                        continue;
                    }
                    this.cachedBlocks.remove(targetPos);
                }
            }
        }
    }

    private double getBPS() {
        if (BlockESP.mc.player == null) {
            return 0.0;
        }
        double dx = BlockESP.mc.player.getX() - this.lastX;
        double dz = BlockESP.mc.player.getZ() - this.lastZ;
        return Math.sqrt(dx * dx + dz * dz) * 20.0;
    }

    @Override
    public void onUpdate() {
        if (BlockESP.mc.player == null || BlockESP.mc.level == null) {
            this.cachedBlocks.clear();
            this.renderCaches.clear();
            return;
        }
        this.getBPS();
        this.lastX = BlockESP.mc.player.getX();
        this.lastZ = BlockESP.mc.player.getZ();
        double maxRadiusBlocksX = Math.max(((BlockESP_h)this.h).renderChunksXZ.value * 16.0, ((BlockESP_h)this.h).renderRangeBlocksXZ.value) + 16.0;
        double maxRadiusBlocksY = Math.max(((BlockESP_h)this.h).renderChunksY.value * 16.0, ((BlockESP_h)this.h).renderRangeBlocksY.value) + 16.0;
        this.cachedBlocks.keySet().removeIf(pos -> {
            double dx = Math.abs((double)pos.getX() - BlockESP.mc.player.getX());
            double dy = Math.abs((double)pos.getY() - BlockESP.mc.player.getY());
            double dz = Math.abs((double)pos.getZ() - BlockESP.mc.player.getZ());
            return dx > maxRadiusBlocksX || dy > maxRadiusBlocksY || dz > maxRadiusBlocksX;
        });
        this.renderCaches.keySet().retainAll(this.cachedBlocks.keySet());
        double dx = this.currentChunkX - this.searchCenterX;
        double dz = this.currentChunkZ - this.searchCenterZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > ((BlockESP_h)this.h).renderChunksXZ.value) {
            BlockPos playerPos = BlockESP.mc.player.blockPosition();
            this.searchCenterX = playerPos.getX() >> 4;
            this.searchCenterZ = playerPos.getZ() >> 4;
            this.currentChunkX = this.searchCenterX;
            this.currentChunkZ = this.searchCenterZ;
            this.stepsCount = 0;
            this.steps = 1;
            this.directionIndex = 0;
            this.subChunkIndex = 0;
        }
        int updatesPerTick = (int)((BlockESP_h)this.h).layersPerTick.value;
        for (int i = 0; i < updatesPerTick; ++i) {
            this.processSubChunk(this.currentChunkX, this.currentChunkZ, this.subChunkIndex);
            this.moveToNext();
        }
        BlockPos playerPos = BlockESP.mc.player.blockPosition();
        ClientLevel heightAccessor = BlockESP.mc.level;
        int buildDepth = heightAccessor.getMinY();
        int numSubchunks = heightAccessor.getHeight() / 16;
        int playerSubChunkY = playerPos.getY() - buildDepth >> 4;
        int playerChunkX = playerPos.getX() >> 4;
        int playerChunkZ = playerPos.getZ() >> 4;
        if (playerSubChunkY >= 0 && playerSubChunkY < numSubchunks) {
            this.processSubChunk(playerChunkX, playerChunkZ, playerSubChunkY);
        }
    }

    public void onBlockUpdate(BlockPos pos, BlockState newState) {
        if (!((BlockESP_h)this.h).enabled) {
            return;
        }
        if (newState.isAir()) {
            this.cachedBlocks.remove(pos);
            this.renderCaches.remove(pos);
        } else {
            boolean isBed;
            String blockName = BuiltInRegistries.BLOCK.getKey((Object)newState.getBlock()).getPath().toLowerCase();
            boolean bl = isBed = blockName.contains("bed") && !blockName.contains("bedrock");
            if (((BlockESP_h)this.h).targetBlocks.value.contains(blockName) || ((BlockESP_h)this.h).bed.value && isBed) {
                if (((BlockESP_h)this.h).exposedOnly.value && !this.isBlockExposed(pos)) {
                    this.cachedBlocks.remove(pos);
                    this.renderCaches.remove(pos);
                    return;
                }
                int color = this.getColorForBlock(blockName, pos.hashCode());
                this.cachedBlocks.put(pos, color);
            } else {
                this.cachedBlocks.remove(pos);
                this.renderCaches.remove(pos);
            }
        }
    }

    private boolean isBlockExposed(BlockPos pos) {
        if (BlockESP.mc.level == null) {
            return false;
        }
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            BlockState neighborState = BlockESP.mc.level.getBlockState(neighbor);
            if (!neighborState.isAir() && neighborState.isCollisionShapeFullBlock((BlockGetter)BlockESP.mc.level, neighbor)) continue;
            return true;
        }
        return false;
    }

    private List<List<BlockPos>> clusterAdjacentBlocks(List<BlockPos> blocks) {
        ArrayList<List<BlockPos>> clusters = new ArrayList<List<BlockPos>>();
        HashSet<BlockPos> visited = new HashSet<BlockPos>();
        HashSet<BlockPos> blockSet = new HashSet<BlockPos>(blocks);
        for (BlockPos pos : blocks) {
            if (visited.contains(pos)) continue;
            ArrayList<BlockPos> cluster = new ArrayList<BlockPos>();
            LinkedList<BlockPos> queue = new LinkedList<BlockPos>();
            queue.add(pos);
            visited.add(pos);
            while (!queue.isEmpty()) {
                BlockPos current = (BlockPos)queue.poll();
                cluster.add(current);
                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = current.relative(dir);
                    if (!blockSet.contains(neighbor) || visited.contains(neighbor)) continue;
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
            clusters.add(cluster);
        }
        return clusters;
    }

    private boolean isPointInsidePolygon(EspPoint p, List<EspPoint> poly) {
        int n = poly.size();
        if (n < 3) {
            return false;
        }
        boolean positive = false;
        boolean negative = false;
        for (int i = 0; i < n; ++i) {
            EspPoint p1 = poly.get(i);
            EspPoint p2 = poly.get((i + 1) % n);
            float cross = (p2.x - p1.x) * (p.y - p1.y) - (p2.y - p1.y) * (p.x - p1.x);
            if (cross > 0.0f) {
                positive = true;
            }
            if (cross < 0.0f) {
                negative = true;
            }
            if (!positive || !negative) continue;
            return false;
        }
        return true;
    }

    @Override
    public void onRenderHUD() {
        double distSqr;
        double range1Sqr;
        if (BlockESP.mc.player == null || BlockESP.mc.level == null || !((BlockESP_h)this.h).enabled) {
            return;
        }
        BlockPos playerPos = BlockESP.mc.player.blockPosition();
        if (((BlockESP_h)this.h).renderCurrentChunk.value) {
            double playerY = BlockESP.mc.player.getY();
            double y = playerY - 16.0;
            double minX = this.currentChunkX * 16;
            double minZ = this.currentChunkZ * 16;
            double maxX = minX + 16.0;
            double maxZ = minZ + 16.0;
            AABB chunkBox = new AABB(minX, y, minZ, maxX, y + 1.0, maxZ);
            this.drawBoxESP(chunkBox, -1);
        }
        ArrayList<BlockPos> visibleBlocks = new ArrayList<BlockPos>();
        for (BlockPos pos : this.cachedBlocks.keySet()) {
            double limitXZ;
            double dx = Math.abs(pos.getX() - playerPos.getX());
            double dy = Math.abs(pos.getY() - playerPos.getY());
            double dz = Math.abs(pos.getZ() - playerPos.getZ());
            boolean insideRenderRange = false;
            if (((BlockESP_h)this.h).renderRangeMode.value == BlockESP_h.RenderRangeMode.Old) {
                double maxR = ((BlockESP_h)this.h).renderChunksXZ.value * 16.0;
                insideRenderRange = dx <= maxR && dy <= maxR && dz <= maxR;
            } else if (((BlockESP_h)this.h).renderRangeMode.value == BlockESP_h.RenderRangeMode.Custom) {
                limitXZ = ((BlockESP_h)this.h).renderRangeBlocksXZ.value;
                double limitY = ((BlockESP_h)this.h).renderRangeBlocksY.value;
                insideRenderRange = dx <= limitXZ && dy <= limitY && dz <= limitXZ;
            } else if (((BlockESP_h)this.h).renderRangeMode.value == BlockESP_h.RenderRangeMode.Sync) {
                limitXZ = ((BlockESP_h)this.h).renderChunksXZ.value * 16.0;
                double limitY = ((BlockESP_h)this.h).renderChunksY.value * 16.0;
                boolean bl = insideRenderRange = dx <= limitXZ && dy <= limitY && dz <= limitXZ;
            }
            if (!insideRenderRange) continue;
            visibleBlocks.add(pos);
        }
        HashSet<BlockPos> blockSet = new HashSet<BlockPos>(visibleBlocks);
        if (((BlockESP_h)this.h).csRenderMode.value == BlockESP_h.CSRenderMode.Simple) {
            int color;
            ArrayList<BlockPos> blocksToMerge = new ArrayList<BlockPos>();
            ArrayList<BlockPos> blocksToRenderNormal = new ArrayList<BlockPos>();
            range1Sqr = ((BlockESP_h)this.h).csRenderRange1.value * ((BlockESP_h)this.h).csRenderRange1.value;
            for (BlockPos pos : visibleBlocks) {
                distSqr = BlockESP.mc.player.distanceToSqr(Vec3.atCenterOf((Vec3i)pos));
                if (distSqr > range1Sqr) {
                    blocksToMerge.add(pos);
                    continue;
                }
                blocksToRenderNormal.add(pos);
            }
            List<List<BlockPos>> clusters = this.clusterAdjacentBlocks(blocksToMerge);
            for (List<BlockPos> cluster : clusters) {
                if (cluster.isEmpty()) continue;
                color = this.cachedBlocks.getOrDefault(cluster.get(0), -8355712);
                if (((BlockESP_h)this.h).outlineMode.value == BlockESP_h.OutlineMode.Line6) {
                    ArrayList<EspPoint> allProjectedPoints = new ArrayList<EspPoint>();
                    Vec3 camPos = BlockESP.mc.gameRenderer.getMainCamera().position();
                    float pitch = BlockESP.mc.gameRenderer.getMainCamera().xRot();
                    float yaw = BlockESP.mc.gameRenderer.getMainCamera().yRot();
                    float fRad = pitch * ((float)Math.PI / 180);
                    float f1Rad = -yaw * ((float)Math.PI / 180);
                    float cosYaw = Mth.cos((double)f1Rad);
                    float sinYaw = Mth.sin((double)f1Rad);
                    float cosPitch = Mth.cos((double)fRad);
                    float sinPitch = Mth.sin((double)fRad);
                    Vec3 lookVec = new Vec3((double)(sinYaw * cosPitch), (double)(-sinPitch), (double)(cosYaw * cosPitch));
                    for (BlockPos pos : cluster) {
                        Vec3[] vertices;
                        double px = pos.getX();
                        double py = pos.getY();
                        double pz = pos.getZ();
                        AABB box = new AABB(px, py, pz, px + 1.0, py + 1.0, pz + 1.0);
                        for (Vec3 v : vertices = new Vec3[]{new Vec3(box.minX, box.minY, box.minZ), new Vec3(box.maxX, box.minY, box.minZ), new Vec3(box.maxX, box.minY, box.maxZ), new Vec3(box.minX, box.minY, box.maxZ), new Vec3(box.minX, box.maxY, box.minZ), new Vec3(box.maxX, box.maxY, box.minZ), new Vec3(box.maxX, box.maxY, box.maxZ), new Vec3(box.minX, box.maxY, box.maxZ)}) {
                            Vec3 proj = this.projectPoint(v, camPos, lookVec);
                            if (proj == null) continue;
                            allProjectedPoints.add(new EspPoint((float)proj.x, (float)proj.y));
                        }
                    }
                    if (allProjectedPoints.size() < 3) continue;
                    List<EspPoint> hull = this.getConvexHull(allProjectedPoints);
                    ImDrawList dl = ImGui.getForegroundDrawList();
                    if (((BlockESP_h)this.h).filled.value) {
                        float fAlpha = (float)((BlockESP_h)this.h).filledOpacity.value;
                        int fColor = ImGui.getColorU32((float)((float)(color >> 16 & 0xFF) / 255.0f), (float)((float)(color >> 8 & 0xFF) / 255.0f), (float)((float)(color & 0xFF) / 255.0f), (float)fAlpha);
                        ImVec2[] imPoints = new ImVec2[hull.size()];
                        for (int idx = 0; idx < hull.size(); ++idx) {
                            imPoints[idx] = new ImVec2(hull.get((int)idx).x, hull.get((int)idx).y);
                        }
                        dl.addConvexPolyFilled(imPoints, hull.size(), fColor);
                    }
                    float oAlpha = (float)((BlockESP_h)this.h).outlineOpacity.value;
                    int oColor = ImGui.getColorU32((float)((float)(color >> 16 & 0xFF) / 255.0f), (float)((float)(color >> 8 & 0xFF) / 255.0f), (float)((float)(color & 0xFF) / 255.0f), (float)oAlpha);
                    float thick = (float)((BlockESP_h)this.h).lineWidth.value;
                    for (int i = 0; i < hull.size(); ++i) {
                        EspPoint p1 = hull.get(i);
                        EspPoint p2 = hull.get((i + 1) % hull.size());
                        dl.addLine(p1.x, p1.y, p2.x, p2.y, oColor, thick);
                    }
                    continue;
                }
                int minX = Integer.MAX_VALUE;
                int minY = Integer.MAX_VALUE;
                int minZ = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE;
                int maxY = Integer.MIN_VALUE;
                int maxZ = Integer.MIN_VALUE;
                for (BlockPos p : cluster) {
                    if (p.getX() < minX) {
                        minX = p.getX();
                    }
                    if (p.getY() < minY) {
                        minY = p.getY();
                    }
                    if (p.getZ() < minZ) {
                        minZ = p.getZ();
                    }
                    if (p.getX() > maxX) {
                        maxX = p.getX();
                    }
                    if (p.getY() > maxY) {
                        maxY = p.getY();
                    }
                    if (p.getZ() <= maxZ) continue;
                    maxZ = p.getZ();
                }
                AABB mergedBox = new AABB((double)minX, (double)minY, (double)minZ, (double)maxX + 1.0, (double)maxY + 1.0, (double)maxZ + 1.0);
                this.drawBoxESP(mergedBox, color);
            }
            for (BlockPos pos : blocksToRenderNormal) {
                color = this.cachedBlocks.getOrDefault(pos, -8355712);
                this.drawBlockESP(pos, color);
            }
        } else if (((BlockESP_h)this.h).csRenderMode.value == BlockESP_h.CSRenderMode.Simple12line) {
            ArrayList<BlockPos> blocksToMerge = new ArrayList<BlockPos>();
            ArrayList<BlockPos> blocksToRenderNormal = new ArrayList<BlockPos>();
            range1Sqr = ((BlockESP_h)this.h).csRenderRange1.value * ((BlockESP_h)this.h).csRenderRange1.value;
            for (BlockPos pos : visibleBlocks) {
                distSqr = BlockESP.mc.player.distanceToSqr(Vec3.atCenterOf((Vec3i)pos));
                if (distSqr > range1Sqr) {
                    blocksToMerge.add(pos);
                    continue;
                }
                blocksToRenderNormal.add(pos);
            }
            for (BlockPos pos : blocksToMerge) {
                int color = this.cachedBlocks.getOrDefault(pos, -8355712);
                this.drawBlockESPSimplified(pos, color, blockSet, true);
            }
            for (BlockPos pos : blocksToRenderNormal) {
                int color = this.cachedBlocks.getOrDefault(pos, -8355712);
                this.drawBlockESP(pos, color);
            }
        } else {
            for (BlockPos pos : visibleBlocks) {
                int color = this.cachedBlocks.getOrDefault(pos, -8355712);
                this.drawBlockESP(pos, color);
            }
        }
    }

    public int getColorForBlock(String blockName, int index) {
        String matchPrefix = blockName.toLowerCase() + ":";
        for (String entry : ((BlockESP_h)this.h).blockColors.value) {
            if (!entry.toLowerCase().startsWith(matchPrefix)) continue;
            String colorPart = entry.substring(matchPrefix.length()).trim();
            if (colorPart.equalsIgnoreCase("themecolor")) {
                return this.getThemeColor(index);
            }
            try {
                if (colorPart.startsWith("#")) {
                    colorPart = colorPart.substring(1);
                }
                if (colorPart.length() == 6) {
                    return (int)Long.parseLong("FF" + colorPart, 16);
                }
                if (colorPart.length() != 8) continue;
                return (int)Long.parseLong(colorPart, 16);
            } catch (NumberFormatException e) {
                return -8355712;
            }
        }
        return -8355712;
    }

    private int getThemeColor(int index) {
        Interface ui = (Interface)ModuleManager.INSTANCE.getModuleByName("Interface");
        return ui != null ? ui.getCurrentColor(index) : -8355712;
    }

    private Vec3 projectPoint(Vec3 worldPos, Vec3 camPos, Vec3 lookVec) {
        Vec3 toTarget = worldPos.subtract(camPos);
        if (toTarget.dot(lookVec) <= 0.0) {
            return null;
        }
        Vec3 ndc = BlockESP.mc.gameRenderer.projectPointToScreen(worldPos);
        float sw = ImGui.getIO().getDisplaySizeX();
        float sh = ImGui.getIO().getDisplaySizeY();
        float x = (float)((ndc.x + 1.0) * 0.5 * (double)sw);
        float y = (float)((1.0 - ndc.y) * 0.5 * (double)sh);
        float marginX = sw * 3.0f;
        float marginY = sh * 3.0f;
        if (x < -marginX || x > sw + marginX || y < -marginY || y > sh + marginY) {
            return null;
        }
        return new Vec3((double)x, (double)y, 0.0);
    }

    private void drawBlockESPSimplified(BlockPos pos, int color, Set<BlockPos> blockSet, boolean force12line) {
        if (BlockESP.mc.player == null) {
            return;
        }
        EspRenderCache cache = this.renderCaches.computeIfAbsent(pos, k -> new EspRenderCache());
        double px = pos.getX();
        double py = pos.getY();
        double pz = pos.getZ();
        AABB box = new AABB(px, py, pz, px + 1.0, py + 1.0, pz + 1.0);
        this.calculateBoxProjection(box, cache.xs, cache.ys, cache.visible);
        boolean hasEast = blockSet.contains(pos.east());
        boolean hasWest = blockSet.contains(pos.west());
        boolean hasUp = blockSet.contains(pos.above());
        boolean hasDown = blockSet.contains(pos.below());
        boolean hasSouth = blockSet.contains(pos.south());
        boolean hasNorth = blockSet.contains(pos.north());
        this.drawBoxExposedEdges(cache.xs, cache.ys, cache.visible, color, hasEast, hasWest, hasUp, hasDown, hasSouth, hasNorth, force12line);
    }

    private void drawBlockESP(BlockPos pos, int color) {
        if (BlockESP.mc.player == null) {
            return;
        }
        long currentTick = BlockESP.mc.player.tickCount;
        double dist = Math.sqrt(BlockESP.mc.player.distanceToSqr(Vec3.atCenterOf((Vec3i)pos)));
        long requiredDelay = 0L;
        if (((BlockESP_h)this.h).csRenderMode.value == BlockESP_h.CSRenderMode.Custom) {
            requiredDelay = dist <= ((BlockESP_h)this.h).csRenderRange1.value ? 0L : (dist <= ((BlockESP_h)this.h).csRenderRange2.value ? (long)((BlockESP_h)this.h).csRenderDelay2.value : (dist <= ((BlockESP_h)this.h).csRenderRange3.value ? (long)((BlockESP_h)this.h).csRenderDelay3.value : (long)((BlockESP_h)this.h).csRenderDelay3.value));
        }
        EspRenderCache cache = this.renderCaches.computeIfAbsent(pos, k -> new EspRenderCache());
        if (cache.lastUpdateTick == -1L || currentTick - cache.lastUpdateTick >= requiredDelay) {
            double px = pos.getX();
            double py = pos.getY();
            double pz = pos.getZ();
            AABB box = new AABB(px, py, pz, px + 1.0, py + 1.0, pz + 1.0);
            this.calculateBoxProjection(box, cache.xs, cache.ys, cache.visible);
            cache.lastUpdateTick = currentTick;
        }
        this.drawBoxFromCachedProjection(cache.xs, cache.ys, cache.visible, color);
    }

    private void calculateBoxProjection(AABB box, float[] xs, float[] ys, boolean[] visible) {
        Vec3 camPos = BlockESP.mc.gameRenderer.getMainCamera().position();
        float pitch = BlockESP.mc.gameRenderer.getMainCamera().xRot();
        float yaw = BlockESP.mc.gameRenderer.getMainCamera().yRot();
        float fRad = pitch * ((float)Math.PI / 180);
        float f1Rad = -yaw * ((float)Math.PI / 180);
        float cosYaw = Mth.cos((double)f1Rad);
        float sinYaw = Mth.sin((double)f1Rad);
        float cosPitch = Mth.cos((double)fRad);
        float sinPitch = Mth.sin((double)fRad);
        Vec3 lookVec = new Vec3((double)(sinYaw * cosPitch), (double)(-sinPitch), (double)(cosYaw * cosPitch));
        Vec3[] vertices = new Vec3[]{new Vec3(box.minX, box.minY, box.minZ), new Vec3(box.maxX, box.minY, box.minZ), new Vec3(box.maxX, box.minY, box.maxZ), new Vec3(box.minX, box.minY, box.maxZ), new Vec3(box.minX, box.maxY, box.minZ), new Vec3(box.maxX, box.maxY, box.minZ), new Vec3(box.maxX, box.maxY, box.maxZ), new Vec3(box.minX, box.maxY, box.maxZ)};
        for (int i = 0; i < 8; ++i) {
            Vec3 proj = this.projectPoint(vertices[i], camPos, lookVec);
            if (proj != null) {
                xs[i] = (float)proj.x;
                ys[i] = (float)proj.y;
                visible[i] = true;
                continue;
            }
            visible[i] = false;
        }
    }

    private void drawBoxFromCachedProjection(float[] xs, float[] ys, boolean[] visible, int color) {
        ImDrawList dl = ImGui.getForegroundDrawList();
        float oAlpha = (float)((BlockESP_h)this.h).outlineOpacity.value;
        int oColor = ImGui.getColorU32((float)((float)(color >> 16 & 0xFF) / 255.0f), (float)((float)(color >> 8 & 0xFF) / 255.0f), (float)((float)(color & 0xFF) / 255.0f), (float)oAlpha);
        float thick = (float)((BlockESP_h)this.h).lineWidth.value;
        if (((BlockESP_h)this.h).filled.value) {
            float fAlpha = (float)((BlockESP_h)this.h).filledOpacity.value;
            int fColor = ImGui.getColorU32((float)((float)(color >> 16 & 0xFF) / 255.0f), (float)((float)(color >> 8 & 0xFF) / 255.0f), (float)((float)(color & 0xFF) / 255.0f), (float)fAlpha);
            if (visible[0] && visible[1] && visible[2] && visible[3]) {
                dl.addQuadFilled(xs[0], ys[0], xs[1], ys[1], xs[2], ys[2], xs[3], ys[3], fColor);
            }
            if (visible[4] && visible[5] && visible[6] && visible[7]) {
                dl.addQuadFilled(xs[4], ys[4], xs[5], ys[5], xs[6], ys[6], xs[7], ys[7], fColor);
            }
            if (visible[0] && visible[1] && visible[5] && visible[4]) {
                dl.addQuadFilled(xs[0], ys[0], xs[1], ys[1], xs[5], ys[5], xs[4], ys[4], fColor);
            }
            if (visible[1] && visible[2] && visible[6] && visible[5]) {
                dl.addQuadFilled(xs[1], ys[1], xs[2], ys[2], xs[6], ys[6], xs[5], ys[5], fColor);
            }
            if (visible[2] && visible[3] && visible[7] && visible[6]) {
                dl.addQuadFilled(xs[2], ys[2], xs[3], ys[3], xs[7], ys[7], xs[6], ys[6], fColor);
            }
            if (visible[3] && visible[0] && visible[4] && visible[7]) {
                dl.addQuadFilled(xs[3], ys[3], xs[0], ys[0], xs[4], ys[4], xs[7], ys[7], fColor);
            }
        }
        if (((BlockESP_h)this.h).outlineMode.value == BlockESP_h.OutlineMode.Line12) {
            this.drawESPLine(dl, xs, ys, visible, 0, 1, oColor, thick);
            this.drawESPLine(dl, xs, ys, visible, 1, 2, oColor, thick);
            this.drawESPLine(dl, xs, ys, visible, 2, 3, oColor, thick);
            this.drawESPLine(dl, xs, ys, visible, 3, 0, oColor, thick);
            this.drawESPLine(dl, xs, ys, visible, 4, 5, oColor, thick);
            this.drawESPLine(dl, xs, ys, visible, 5, 6, oColor, thick);
            this.drawESPLine(dl, xs, ys, visible, 6, 7, oColor, thick);
            this.drawESPLine(dl, xs, ys, visible, 7, 4, oColor, thick);
            this.drawESPLine(dl, xs, ys, visible, 0, 4, oColor, thick);
            this.drawESPLine(dl, xs, ys, visible, 1, 5, oColor, thick);
            this.drawESPLine(dl, xs, ys, visible, 2, 6, oColor, thick);
            this.drawESPLine(dl, xs, ys, visible, 3, 7, oColor, thick);
        } else {
            ArrayList<EspPoint> pts = new ArrayList<EspPoint>();
            for (int i = 0; i < 8; ++i) {
                if (!visible[i]) continue;
                pts.add(new EspPoint(xs[i], ys[i]));
            }
            if (pts.size() >= 3) {
                List<EspPoint> hull = this.getConvexHull(pts);
                for (int i = 0; i < hull.size(); ++i) {
                    EspPoint p1 = hull.get(i);
                    EspPoint p2 = hull.get((i + 1) % hull.size());
                    dl.addLine(p1.x, p1.y, p2.x, p2.y, oColor, thick);
                }
            }
        }
    }

    private void drawBoxExposedEdges(float[] xs, float[] ys, boolean[] visible, int color, boolean hasEast, boolean hasWest, boolean hasUp, boolean hasDown, boolean hasSouth, boolean hasNorth, boolean force12line) {
        ImDrawList dl = ImGui.getForegroundDrawList();
        float oAlpha = (float)((BlockESP_h)this.h).outlineOpacity.value;
        int oColor = ImGui.getColorU32((float)((float)(color >> 16 & 0xFF) / 255.0f), (float)((float)(color >> 8 & 0xFF) / 255.0f), (float)((float)(color & 0xFF) / 255.0f), (float)oAlpha);
        float thick = (float)((BlockESP_h)this.h).lineWidth.value;
        if (((BlockESP_h)this.h).filled.value) {
            float fAlpha = (float)((BlockESP_h)this.h).filledOpacity.value;
            int fColor = ImGui.getColorU32((float)((float)(color >> 16 & 0xFF) / 255.0f), (float)((float)(color >> 8 & 0xFF) / 255.0f), (float)((float)(color & 0xFF) / 255.0f), (float)fAlpha);
            if (!hasDown && visible[0] && visible[1] && visible[2] && visible[3]) {
                dl.addQuadFilled(xs[0], ys[0], xs[1], ys[1], xs[2], ys[2], xs[3], ys[3], fColor);
            }
            if (!hasUp && visible[4] && visible[5] && visible[6] && visible[7]) {
                dl.addQuadFilled(xs[4], ys[4], xs[5], ys[5], xs[6], ys[6], xs[7], ys[7], fColor);
            }
            if (!hasNorth && visible[0] && visible[1] && visible[5] && visible[4]) {
                dl.addQuadFilled(xs[0], ys[0], xs[1], ys[1], xs[5], ys[5], xs[4], ys[4], fColor);
            }
            if (!hasEast && visible[1] && visible[2] && visible[6] && visible[5]) {
                dl.addQuadFilled(xs[1], ys[1], xs[2], ys[2], xs[6], ys[6], xs[5], ys[5], fColor);
            }
            if (!hasSouth && visible[2] && visible[3] && visible[7] && visible[6]) {
                dl.addQuadFilled(xs[2], ys[2], xs[3], ys[3], xs[7], ys[7], xs[6], ys[6], fColor);
            }
            if (!hasWest && visible[3] && visible[0] && visible[4] && visible[7]) {
                dl.addQuadFilled(xs[3], ys[3], xs[0], ys[0], xs[4], ys[4], xs[7], ys[7], fColor);
            }
        }
        if (force12line || ((BlockESP_h)this.h).outlineMode.value == BlockESP_h.OutlineMode.Line12) {
            if (!hasDown && !hasNorth) {
                this.drawESPLine(dl, xs, ys, visible, 0, 1, oColor, thick);
            }
            if (!hasDown && !hasSouth) {
                this.drawESPLine(dl, xs, ys, visible, 2, 3, oColor, thick);
            }
            if (!hasUp && !hasNorth) {
                this.drawESPLine(dl, xs, ys, visible, 4, 5, oColor, thick);
            }
            if (!hasUp && !hasSouth) {
                this.drawESPLine(dl, xs, ys, visible, 6, 7, oColor, thick);
            }
            if (!hasWest && !hasNorth) {
                this.drawESPLine(dl, xs, ys, visible, 0, 4, oColor, thick);
            }
            if (!hasEast && !hasNorth) {
                this.drawESPLine(dl, xs, ys, visible, 1, 5, oColor, thick);
            }
            if (!hasEast && !hasSouth) {
                this.drawESPLine(dl, xs, ys, visible, 2, 6, oColor, thick);
            }
            if (!hasWest && !hasSouth) {
                this.drawESPLine(dl, xs, ys, visible, 3, 7, oColor, thick);
            }
            if (!hasWest && !hasDown) {
                this.drawESPLine(dl, xs, ys, visible, 3, 0, oColor, thick);
            }
            if (!hasEast && !hasDown) {
                this.drawESPLine(dl, xs, ys, visible, 1, 2, oColor, thick);
            }
            if (!hasWest && !hasUp) {
                this.drawESPLine(dl, xs, ys, visible, 7, 4, oColor, thick);
            }
            if (!hasEast && !hasUp) {
                this.drawESPLine(dl, xs, ys, visible, 5, 6, oColor, thick);
            }
        } else {
            ArrayList<EspPoint> pts = new ArrayList<EspPoint>();
            for (int i = 0; i < 8; ++i) {
                if (!visible[i]) continue;
                pts.add(new EspPoint(xs[i], ys[i]));
            }
            if (pts.size() >= 3) {
                List<EspPoint> hull = this.getConvexHull(pts);
                for (int i = 0; i < hull.size(); ++i) {
                    EspPoint p1 = hull.get(i);
                    EspPoint p2 = hull.get((i + 1) % hull.size());
                    dl.addLine(p1.x, p1.y, p2.x, p2.y, oColor, thick);
                }
            }
        }
    }

    private void drawBoxESP(AABB box, int color) {
        float[] xs = new float[8];
        float[] ys = new float[8];
        boolean[] visible = new boolean[8];
        this.calculateBoxProjection(box, xs, ys, visible);
        this.drawBoxFromCachedProjection(xs, ys, visible, color);
    }

    private List<EspPoint> getConvexHull(List<EspPoint> pts) {
        pts.sort((a, b) -> a.x != b.x ? Float.compare(a.x, b.x) : Float.compare(a.y, b.y));
        ArrayList<EspPoint> lower = new ArrayList<EspPoint>();
        for (EspPoint p : pts) {
            while (lower.size() >= 2 && this.crossProduct((EspPoint)lower.get(lower.size() - 2), (EspPoint)lower.get(lower.size() - 1), p) <= 0.0f) {
                lower.remove(lower.size() - 1);
            }
            lower.add(p);
        }
        ArrayList<EspPoint> upper = new ArrayList<EspPoint>();
        for (int i = pts.size() - 1; i >= 0; --i) {
            EspPoint p = pts.get(i);
            while (upper.size() >= 2 && this.crossProduct((EspPoint)upper.get(upper.size() - 2), (EspPoint)upper.get(upper.size() - 1), p) <= 0.0f) {
                upper.remove(upper.size() - 1);
            }
            upper.add(p);
        }
        lower.remove(lower.size() - 1);
        upper.remove(upper.size() - 1);
        lower.addAll(upper);
        return lower;
    }

    private float crossProduct(EspPoint o, EspPoint a, EspPoint b) {
        return (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x);
    }

    private void drawESPLine(ImDrawList dl, float[] xs, float[] ys, boolean[] visible, int i, int j, int color, float thickness) {
        if (visible[i] && visible[j]) {
            dl.addLine(xs[i], ys[i], xs[j], ys[j], color, thickness);
        }
    }

    static {
        DIRECTIONS = new int[][]{{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
    }

    private static class EspPoint {
        float x;
        float y;

        EspPoint(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private static class EspRenderCache {
        float[] xs = new float[8];
        float[] ys = new float[8];
        boolean[] visible = new boolean[8];
        long lastUpdateTick = -1L;

        private EspRenderCache() {
        }
    }
}

