/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  imgui.ImDrawList
 *  imgui.ImFont
 *  imgui.ImGui
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
 *  net.minecraft.network.protocol.game.ServerboundPlayerActionPacket$Action
 *  net.minecraft.network.protocol.game.ServerboundSwingPacket
 *  net.minecraft.util.Mth
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package recode.usefultools.latest.Modules.Player.Fucker;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Misc.RotationManager.RotationManager;
import recode.usefultools.latest.Modules.ModuleManager;
import recode.usefultools.latest.Modules.Player.Fucker.Fucker_h;
import recode.usefultools.latest.Modules.Visual.Interface.Interface;
import recode.usefultools.latest.Modules.Visual.Interface.Interface_h;
import recode.usefultools.latest.utils.ImGuiEngine;
import recode.usefultools.latest.utils.MathUtils;

public class Fucker
extends BaseModule<Fucker_h> {
    public static Fucker instance;
    public float miningProgress = 0.0f;
    public BlockPos currentMiningBlock = null;
    public BlockPos lastMinedBlock = null;
    public BlockPos activeMiningPos = null;
    private BlockPos lastTargetBlock = null;
    private Vec3 lerpedBoxPos = null;

    public Fucker() {
        super(new Fucker_h());
        instance = this;
    }

    @Override
    public void onEnable() {
        this.resetMiningState();
    }

    @Override
    public void onDisable() {
        if (RotationManager.instance != null) {
            RotationManager.instance.reset("Fucker");
        }
        this.resetMiningState();
    }

    private void resetMiningState() {
        this.currentMiningBlock = null;
        this.activeMiningPos = null;
        this.miningProgress = 0.0f;
        this.lastMinedBlock = null;
        this.lastTargetBlock = null;
        this.lerpedBoxPos = null;
    }

    @Override
    public void onUpdate() {
        if (Fucker.mc.player == null || Fucker.mc.level == null) {
            return;
        }
        if (this.currentMiningBlock != null && !this.isValidTarget(this.currentMiningBlock)) {
            this.resetMiningProgress();
        }
        if (this.currentMiningBlock == null) {
            this.currentMiningBlock = this.findClosestTarget();
        }
        if (this.currentMiningBlock == null) {
            if (RotationManager.instance != null) {
                RotationManager.instance.reset("Fucker");
            }
            this.lastTargetBlock = null;
            this.activeMiningPos = null;
            return;
        }
        this.activeMiningPos = this.getExposedMiningTarget(this.currentMiningBlock);
        if (this.activeMiningPos == null) {
            this.resetMiningProgress();
            return;
        }
        if (!this.activeMiningPos.equals((Object)this.lastTargetBlock)) {
            Fucker.mc.player.connection.send((Packet)new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, this.activeMiningPos, Direction.UP));
            this.lastTargetBlock = this.activeMiningPos;
        }
        boolean onGround = Fucker.mc.player.onGround();
        boolean canAdvanceProgress = true;
        if (!onGround) {
            if (((Fucker_h)this.h).onGroundMode.value == Fucker_h.OnGroundMode.MiningStop) {
                canAdvanceProgress = false;
            } else if (((Fucker_h)this.h).onGroundMode.value == Fucker_h.OnGroundMode.Cancel) {
                this.resetMiningProgress();
                if (RotationManager.instance != null) {
                    RotationManager.instance.reset("Fucker");
                }
                return;
            }
        }
        if (canAdvanceProgress) {
            BlockState state = Fucker.mc.level.getBlockState(this.activeMiningPos);
            float hardness = state.getDestroySpeed((BlockGetter)Fucker.mc.level, this.activeMiningPos);
            float progressStep = 0.05f;
            if (hardness != -1.0f && hardness != 0.0f) {
                float destroySpeed = Fucker.mc.player.getDestroySpeed(state);
                float speedMultiplier = Fucker.mc.player.hasCorrectToolForDrops(state) ? 30.0f : 100.0f;
                progressStep = destroySpeed / (hardness * speedMultiplier) * (float)((Fucker_h)this.h).breakSpeed.value;
            } else if (hardness == 0.0f) {
                progressStep = 1.0f;
            }
            this.miningProgress += progressStep;
        }
        if (((Fucker_h)this.h).rotate.value && this.miningProgress >= (float)((Fucker_h)this.h).rotationPercentage.value) {
            float[] rots = this.getRotationsToBlock(this.activeMiningPos);
            this.applySpoofRotations(rots[0], rots[1]);
        } else if (RotationManager.instance != null) {
            RotationManager.instance.reset("Fucker");
        }
        this.handleSwingTick(false);
        if (this.miningProgress >= 1.0f) {
            this.breakBlock(this.activeMiningPos);
        }
    }

    private BlockPos getExposedMiningTarget(BlockPos target) {
        BlockPos closestNeighbor;
        if (Fucker.mc.level == null) {
            return null;
        }
        if (((Fucker_h)this.h).exposedMode.value == Fucker_h.ExposedMode.None) {
            return target;
        }
        if (this.hasAirAdjacent(target)) {
            return target;
        }
        if (((Fucker_h)this.h).exposedMode.value == Fucker_h.ExposedMode.Legit) {
            return this.getLegitExposedTarget(target, 0);
        }
        if (((Fucker_h)this.h).exposedMode.value == Fucker_h.ExposedMode.Surround && (closestNeighbor = this.getClosestAdjacentBlock(target)) != null) {
            return closestNeighbor;
        }
        return target;
    }

    private boolean hasAirAdjacent(BlockPos pos) {
        if (Fucker.mc.level == null) {
            return false;
        }
        for (Direction dir : Direction.values()) {
            if (!Fucker.mc.level.getBlockState(pos.relative(dir)).isAir()) continue;
            return true;
        }
        return false;
    }

    private BlockPos getClosestAdjacentBlock(BlockPos pos) {
        if (Fucker.mc.level == null || Fucker.mc.player == null) {
            return null;
        }
        BlockPos closest = null;
        double minDist = Double.MAX_VALUE;
        for (Direction dir : Direction.values()) {
            double dist;
            BlockPos neighbor = pos.relative(dir);
            BlockState state = Fucker.mc.level.getBlockState(neighbor);
            if (state.isAir() || !((dist = Fucker.mc.player.distanceToSqr(Vec3.atCenterOf((Vec3i)neighbor))) < minDist)) continue;
            minDist = dist;
            closest = neighbor;
        }
        return closest;
    }

    private BlockPos getLegitExposedTarget(BlockPos pos, int depth) {
        if (depth > 5 || Fucker.mc.level == null) {
            return pos;
        }
        if (this.hasAirAdjacent(pos)) {
            return pos;
        }
        BlockPos closestNeighbor = this.getClosestAdjacentBlock(pos);
        if (closestNeighbor == null) {
            return pos;
        }
        return this.getLegitExposedTarget(closestNeighbor, depth + 1);
    }

    private boolean isValidTarget(BlockPos pos) {
        if (Fucker.mc.player == null || Fucker.mc.level == null) {
            return false;
        }
        if (Fucker.mc.player.distanceToSqr(Vec3.atCenterOf((Vec3i)pos)) > Mth.square((double)((Fucker_h)this.h).range.value)) {
            return false;
        }
        BlockState state = Fucker.mc.level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        String name = BuiltInRegistries.BLOCK.getKey((Object)state.getBlock()).getPath().toLowerCase();
        boolean isBed = name.contains("bed") && !name.contains("bedrock");
        return ((Fucker_h)this.h).targetBlocks.value.contains(name) || ((Fucker_h)this.h).bed.value && isBed;
    }

    private void resetMiningProgress() {
        if (this.currentMiningBlock != null && Fucker.mc.player != null) {
            Fucker.mc.player.connection.send((Packet)new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, this.currentMiningBlock, Direction.UP));
        }
        this.currentMiningBlock = null;
        this.activeMiningPos = null;
        this.miningProgress = 0.0f;
        this.lastTargetBlock = null;
    }

    private void breakBlock(BlockPos pos) {
        if (Fucker.mc.player == null) {
            return;
        }
        this.handleSwingTick(true);
        if (((Fucker_h)this.h).mode.value == Fucker_h.Mode.Normal) {
            if (Fucker.mc.gameMode != null) {
                Fucker.mc.gameMode.destroyBlock(pos);
            }
        } else {
            Fucker.mc.player.connection.send((Packet)new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
        }
        this.lastMinedBlock = pos;
        this.resetMiningProgress();
    }

    private void handleSwingTick(boolean isDestroyed) {
        if (Fucker.mc.player == null) {
            return;
        }
        Fucker_h.SwingMode sMode = (Fucker_h.SwingMode)((Object)((Fucker_h)this.h).swingMode.value);
        if (sMode == Fucker_h.SwingMode.NoSwing) {
            return;
        }
        if (isDestroyed) {
            if (sMode == Fucker_h.SwingMode.Old) {
                Fucker.mc.player.swing(InteractionHand.MAIN_HAND);
            } else if (sMode == Fucker_h.SwingMode.OldPacket) {
                Fucker.mc.player.connection.send((Packet)new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            }
        } else if (sMode == Fucker_h.SwingMode.Normal) {
            Fucker.mc.player.swing(InteractionHand.MAIN_HAND);
        } else if (sMode == Fucker_h.SwingMode.Silent) {
            Fucker.mc.player.connection.send((Packet)new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
        }
    }

    private void applySpoofRotations(float y, float p) {
        if (RotationManager.instance != null) {
            RotationManager.instance.setRotations(y, p, "Fucker");
        }
    }

    private BlockPos findClosestTarget() {
        if (Fucker.mc.player == null || Fucker.mc.level == null) {
            return null;
        }
        BlockPos playerPos = Fucker.mc.player.blockPosition();
        int r = (int)Math.ceil(((Fucker_h)this.h).range.value);
        BlockPos closest = null;
        double closestDist = ((Fucker_h)this.h).range.value;
        boolean foundNonLastMined = false;
        for (BlockPos pos : BlockPos.betweenClosed((BlockPos)playerPos.offset(-r, -r, -r), (BlockPos)playerPos.offset(r, r, r))) {
            double distSqr;
            double dist;
            if (!this.isValidTarget(pos) || !((dist = Math.sqrt(distSqr = Fucker.mc.player.distanceToSqr(Vec3.atCenterOf((Vec3i)pos)))) <= closestDist)) continue;
            if (pos.equals((Object)this.lastMinedBlock)) {
                if (foundNonLastMined) continue;
                closest = pos.immutable();
                closestDist = dist;
                continue;
            }
            closest = pos.immutable();
            closestDist = dist;
            foundNonLastMined = true;
        }
        return closest;
    }

    private float[] getRotationsToBlock(BlockPos pos) {
        if (Fucker.mc.player == null) {
            return new float[]{0.0f, 0.0f};
        }
        Vec3 playerEyes = Fucker.mc.player.getEyePosition(1.0f);
        Vec3 blockCenter = Vec3.atCenterOf((Vec3i)pos);
        double diffX = blockCenter.x - playerEyes.x;
        double diffY = blockCenter.y - playerEyes.y;
        double diffZ = blockCenter.z - playerEyes.z;
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float)(Math.atan2(diffZ, diffX) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float)(-(Math.atan2(diffY, dist) * 180.0 / Math.PI));
        return new float[]{Mth.wrapDegrees((float)yaw), Mth.wrapDegrees((float)pitch)};
    }

    private int getThemeColor(int index) {
        Interface ui = (Interface)ModuleManager.INSTANCE.getModuleByName("Interface");
        return ui != null ? ui.getCurrentColor(index) : -16711681;
    }

    @Override
    public void onRenderHUD() {
        int color;
        if (Fucker.mc.player == null || Fucker.mc.level == null || !((Fucker_h)this.h).enabled) {
            return;
        }
        int n = color = ((Fucker_h)this.h).colorMode.value == Fucker_h.ColorMode.Theme ? this.getThemeColor(0) : -1;
        if (((Fucker_h)this.h).esp.value && this.activeMiningPos != null) {
            this.drawBlockESP(this.activeMiningPos, color);
        }
        if (this.activeMiningPos != null) {
            this.drawProgressBar(color);
        }
    }

    private void drawBlockESP(BlockPos pos, int color) {
        Vec3 targetPos = new Vec3((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
        if (this.lerpedBoxPos == null) {
            this.lerpedBoxPos = targetPos;
        } else if (((Fucker_h)this.h).easing.value) {
            float dt = ImGui.getIO().getDeltaTime();
            float factor = (float)((double)dt * ((Fucker_h)this.h).easingSpeed.value);
            factor = Mth.clamp((float)factor, 0.0f, 1.0f);
            double lx = MathUtils.lerp((float)this.lerpedBoxPos.x, (float)targetPos.x, factor);
            double ly = MathUtils.lerp((float)this.lerpedBoxPos.y, (float)targetPos.y, factor);
            double lz = MathUtils.lerp((float)this.lerpedBoxPos.z, (float)targetPos.z, factor);
            this.lerpedBoxPos = new Vec3(lx, ly, lz);
        } else {
            this.lerpedBoxPos = targetPos;
        }
        AABB box = new AABB(this.lerpedBoxPos.x, this.lerpedBoxPos.y, this.lerpedBoxPos.z, this.lerpedBoxPos.x + 1.0, this.lerpedBoxPos.y + 1.0, this.lerpedBoxPos.z + 1.0);
        Vec3[] vertices = new Vec3[]{new Vec3(box.minX, box.minY, box.minZ), new Vec3(box.maxX, box.minY, box.minZ), new Vec3(box.maxX, box.minY, box.maxZ), new Vec3(box.minX, box.minY, box.maxZ), new Vec3(box.minX, box.maxY, box.minZ), new Vec3(box.maxX, box.maxY, box.minZ), new Vec3(box.maxX, box.maxY, box.maxZ), new Vec3(box.minX, box.maxY, box.maxZ)};
        float[] xs = new float[8];
        float[] ys = new float[8];
        boolean[] visible = new boolean[8];
        Vec3 camPos = Fucker.mc.gameRenderer.getMainCamera().position();
        float pitch = Fucker.mc.gameRenderer.getMainCamera().xRot();
        float yaw = Fucker.mc.gameRenderer.getMainCamera().yRot();
        float fRad = pitch * ((float)Math.PI / 180);
        float f1Rad = -yaw * ((float)Math.PI / 180);
        float cosYaw = Mth.cos((double)f1Rad);
        float sinYaw = Mth.sin((double)f1Rad);
        float cosPitch = Mth.cos((double)fRad);
        float sinPitch = Mth.sin((double)fRad);
        Vec3 lookVec = new Vec3((double)(sinYaw * cosPitch), (double)(-sinPitch), (double)(cosYaw * cosPitch));
        for (int i = 0; i < 8; ++i) {
            Vec3 toTarget = vertices[i].subtract(camPos);
            if (toTarget.dot(lookVec) <= 0.0) {
                visible[i] = false;
                continue;
            }
            Vec3 ndc = Fucker.mc.gameRenderer.projectPointToScreen(vertices[i]);
            float sw = ImGui.getIO().getDisplaySizeX();
            float sh = ImGui.getIO().getDisplaySizeY();
            xs[i] = (float)((ndc.x + 1.0) * 0.5 * (double)sw);
            ys[i] = (float)((1.0 - ndc.y) * 0.5 * (double)sh);
            visible[i] = true;
        }
        ImDrawList dl = ImGui.getForegroundDrawList();
        this.drawESPLine(dl, xs, ys, visible, 0, 1, color);
        this.drawESPLine(dl, xs, ys, visible, 1, 2, color);
        this.drawESPLine(dl, xs, ys, visible, 2, 3, color);
        this.drawESPLine(dl, xs, ys, visible, 3, 0, color);
        this.drawESPLine(dl, xs, ys, visible, 4, 5, color);
        this.drawESPLine(dl, xs, ys, visible, 5, 6, color);
        this.drawESPLine(dl, xs, ys, visible, 6, 7, color);
        this.drawESPLine(dl, xs, ys, visible, 7, 4, color);
        this.drawESPLine(dl, xs, ys, visible, 0, 4, color);
        this.drawESPLine(dl, xs, ys, visible, 1, 5, color);
        this.drawESPLine(dl, xs, ys, visible, 2, 6, color);
        this.drawESPLine(dl, xs, ys, visible, 3, 7, color);
    }

    private void drawESPLine(ImDrawList dl, float[] xs, float[] ys, boolean[] visible, int i, int j, int color) {
        if (visible[i] && visible[j]) {
            dl.addLine(xs[i], ys[i], xs[j], ys[j], color, 1.5f);
        }
    }

    private void drawProgressBar(int color) {
        float sw = ImGui.getIO().getDisplaySizeX();
        float sh = ImGui.getIO().getDisplaySizeY();
        float barWidth = 100.0f;
        float barHeight = 4.0f;
        float x = (sw - barWidth) / 2.0f;
        float y = sh / 2.0f + 20.0f;
        ImDrawList dl = ImGui.getForegroundDrawList();
        int bgColor = ImGui.getColorU32(0.0f, 0.0f, 0.0f, 0.5f);
        dl.addRectFilled(x, y, x + barWidth, y + barHeight, bgColor, 2.0f);
        float fillWidth = barWidth * Math.min(1.0f, Math.max(0.0f, this.miningProgress));
        if (fillWidth > 0.0f) {
            dl.addRectFilled(x, y, x + fillWidth, y + barHeight, color, 2.0f);
        }
        if (((Fucker_h)this.h).barMode.value == Fucker_h.ProgressBarMode.New && this.activeMiningPos != null) {
            boolean isMc;
            Interface ui = (Interface)ModuleManager.INSTANCE.getModuleByName("Interface");
            boolean bl = ((Fucker_h)this.h).fontMode.value == Fucker_h.FontMode.InterfaceF ? ui != null && ((Interface_h)ui.h).font.value == Interface_h.FontType.Mojangles : (isMc = ((Fucker_h)this.h).fontMode.value == Fucker_h.FontMode.Mojangles);
            String fontKey = isMc ? (((Fucker_h)this.h).bold.value ? "minecraft_bold" : "minecraft") : (((Fucker_h)this.h).bold.value ? "main_bold" : "main");
            ImFont font = ImGuiEngine.INSTANCE.fonts.getOrDefault(fontKey, ImGuiEngine.INSTANCE.fonts.get("main"));
            float fSize = 14.0f;
            float scale = fSize / font.getFontSize();
            BlockState state = Fucker.mc.level.getBlockState(this.activeMiningPos);
            String rawBlockName = BuiltInRegistries.BLOCK.getKey((Object)state.getBlock()).getPath();
            String[] words = rawBlockName.split("_");
            StringBuilder sb = new StringBuilder();
            for (String word : words) {
                if (word.isEmpty()) continue;
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
            String formattedName = sb.toString().trim();
            String text = String.format("%s - %d%%", formattedName, (int)(Math.min(1.0f, Math.max(0.0f, this.miningProgress)) * 100.0f));
            ImGui.pushFont((ImFont)font);
            float textWidth = ImGui.calcTextSize((String)text).x * scale;
            float textHeight = ImGui.calcTextSize((String)text).y * scale;
            ImGui.popFont();
            float tx = (sw - textWidth) / 2.0f;
            float ty = y - textHeight - 4.0f;
            ImGui.pushFont((ImFont)font);
            float r = (float)(color >> 16 & 0xFF) / 255.0f;
            float g = (float)(color >> 8 & 0xFF) / 255.0f;
            float b = (float)(color & 0xFF) / 255.0f;
            if (((Fucker_h)this.h).shadow.value) {
                int shadowColor = ImGui.getColorU32((float)(r * 0.25f), (float)(g * 0.25f), (float)(b * 0.25f), 0.925f);
                float offset = 1.0f * scale;
                dl.addText(font, (int)fSize, isMc ? (float)Math.round(tx + offset) : tx + offset, isMc ? (float)Math.round(ty + offset) : ty + offset, shadowColor, text);
            }
            dl.addText(font, (int)fSize, isMc ? (float)Math.round(tx) : tx, isMc ? (float)Math.round(ty) : ty, color, text);
            ImGui.popFont();
        }
    }
}

