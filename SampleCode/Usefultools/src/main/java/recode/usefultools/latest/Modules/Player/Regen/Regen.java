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
 *  net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
 *  net.minecraft.network.protocol.game.ServerboundSwingPacket
 *  net.minecraft.util.Mth
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package recode.usefultools.latest.Modules.Player.Regen;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import java.lang.reflect.Field;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Misc.RotationManager.RotationManager;
import recode.usefultools.latest.Modules.ModuleManager;
import recode.usefultools.latest.Modules.Player.Regen.Regen_h;
import recode.usefultools.latest.Modules.Visual.Interface.Interface;
import recode.usefultools.latest.utils.ImGuiEngine;
import recode.usefultools.latest.utils.MathUtils;

public class Regen
extends BaseModule<Regen_h> {
    public static Regen instance;
    public float miningProgress = 0.0f;
    public BlockPos currentMiningBlock = null;
    public BlockPos lastMinedBlock = null;
    private BlockPos lastTargetBlock = null;
    private Vec3 lerpedBoxPos = null;
    private int originalSlot = -1;
    private int lastSentSlot = -1;
    private int lastClientSelectedSlot = -1;
    private boolean isSpoofingActive = false;
    private float lastProgress = 0.0f;
    private float easePercentage = 0.0f;
    private float visibleTimer = 0.0f;
    private boolean wasMiningBlock = false;

    public Regen() {
        super(new Regen_h());
        instance = this;
    }

    @Override
    public void onEnable() {
        this.resetMiningState();
    }

    @Override
    public void onDisable() {
        if (RotationManager.instance != null) {
            RotationManager.instance.reset("Regen");
        }
        this.resetMiningState();
    }

    private void resetMiningState() {
        this.restoreHotbarSlot();
        this.currentMiningBlock = null;
        this.miningProgress = 0.0f;
        this.lastMinedBlock = null;
        this.lastTargetBlock = null;
        this.lerpedBoxPos = null;
        this.lastProgress = 0.0f;
        this.easePercentage = 0.0f;
        this.visibleTimer = 0.0f;
        this.wasMiningBlock = false;
    }

    @Override
    public void onUpdate() {
        if (Regen.mc.player == null || Regen.mc.level == null) {
            return;
        }
        int clientSelectedSlot = this.getSelectedSlot();
        boolean clientSlotChanged = this.lastClientSelectedSlot != -1 && this.lastClientSelectedSlot != clientSelectedSlot;
        this.lastClientSelectedSlot = clientSelectedSlot;
        if (this.currentMiningBlock != null && !this.isValidTarget(this.currentMiningBlock)) {
            this.resetMiningProgress();
        }
        if (this.currentMiningBlock == null) {
            this.currentMiningBlock = this.findClosestTarget();
        }
        if (this.currentMiningBlock == null) {
            if (RotationManager.instance != null) {
                RotationManager.instance.reset("Regen");
            }
            this.lastTargetBlock = null;
            this.restoreHotbarSlot();
            return;
        }
        this.handleHotbarSwitch(clientSlotChanged);
        if (!this.currentMiningBlock.equals((Object)this.lastTargetBlock)) {
            Regen.mc.player.connection.send((Packet)new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, this.currentMiningBlock, Direction.UP));
            this.lastTargetBlock = this.currentMiningBlock;
        }
        boolean onGround = Regen.mc.player.onGround();
        boolean canAdvanceProgress = true;
        if (!onGround) {
            if (((Regen_h)this.h).onGroundMode.value == Regen_h.OnGroundMode.MiningStop) {
                canAdvanceProgress = false;
            } else if (((Regen_h)this.h).onGroundMode.value == Regen_h.OnGroundMode.Cancel) {
                this.resetMiningProgress();
                if (RotationManager.instance != null) {
                    RotationManager.instance.reset("Regen");
                }
                return;
            }
        }
        if (canAdvanceProgress) {
            float divisor;
            BlockState state = Regen.mc.level.getBlockState(this.currentMiningBlock);
            int actualSelected = this.getSelectedSlot();
            int pickaxeSlot = this.findPickaxeSlot();
            if (((Regen_h)this.h).switchMode.value == Regen_h.SwitchMode.Fake && pickaxeSlot != -1) {
                this.setSelectedSlot(pickaxeSlot);
            }
            float baseBreakSpeed = Regen.mc.player.getDestroySpeed(state);
            if (((Regen_h)this.h).switchMode.value == Regen_h.SwitchMode.Fake && pickaxeSlot != -1) {
                this.setSelectedSlot(actualSelected);
            }
            float speed = (divisor = (float)((Regen_h)this.h).breakSpeed.value) == 0.0f ? Float.POSITIVE_INFINITY : baseBreakSpeed / divisor;
            float progressStep = speed / 100.0f;
            this.miningProgress += progressStep;
        }
        if (((Regen_h)this.h).rotate.value && this.miningProgress >= (float)((Regen_h)this.h).rotationPercentage.value) {
            float[] rots = this.getRotationsToBlock(this.currentMiningBlock);
            this.applySpoofRotations(rots[0], rots[1]);
        } else if (RotationManager.instance != null) {
            RotationManager.instance.reset("Regen");
        }
        this.handleSwingTick(false);
        if (this.miningProgress >= 1.0f) {
            this.breakBlock(this.currentMiningBlock);
        }
    }

    private int getSelectedSlot() {
        if (Regen.mc.player == null) {
            return 0;
        }
        try {
            Field field = Inventory.class.getDeclaredField("selected");
            field.setAccessible(true);
            return field.getInt(Regen.mc.player.getInventory());
        } catch (Exception e) {
            try {
                for (Field f : Inventory.class.getDeclaredFields()) {
                    if (f.getType() != Integer.TYPE || !f.getName().equals("selected") && !f.getName().equals("selectedSlot")) continue;
                    f.setAccessible(true);
                    return f.getInt(Regen.mc.player.getInventory());
                }
            } catch (Exception exception) {
                // empty catch block
            }
            return 0;
        }
    }

    private void setSelectedSlot(int slot) {
        if (Regen.mc.player == null) {
            return;
        }
        try {
            Field field = Inventory.class.getDeclaredField("selected");
            field.setAccessible(true);
            field.setInt(Regen.mc.player.getInventory(), slot);
        } catch (Exception e) {
            try {
                for (Field f : Inventory.class.getDeclaredFields()) {
                    if (f.getType() != Integer.TYPE || !f.getName().equals("selected") && !f.getName().equals("selectedSlot")) continue;
                    f.setAccessible(true);
                    f.setInt(Regen.mc.player.getInventory(), slot);
                    return;
                }
            } catch (Exception exception) {
                // empty catch block
            }
        }
    }

    private int findPickaxeSlot() {
        if (Regen.mc.player == null) {
            return -1;
        }
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = Regen.mc.player.getInventory().getItem(i);
            String itemId = BuiltInRegistries.ITEM.getKey((Object)stack.getItem()).getPath().toLowerCase();
            if (!itemId.contains("pickaxe")) continue;
            return i;
        }
        return -1;
    }

    private void handleHotbarSwitch(boolean clientSlotChanged) {
        if (Regen.mc.player == null) {
            return;
        }
        Regen_h.SwitchMode mode = (Regen_h.SwitchMode)((Object)((Regen_h)this.h).switchMode.value);
        if (mode == Regen_h.SwitchMode.None) {
            return;
        }
        int pickaxeSlot = this.findPickaxeSlot();
        if (pickaxeSlot == -1) {
            return;
        }
        int currentSlot = this.getSelectedSlot();
        if (mode == Regen_h.SwitchMode.Full) {
            if (currentSlot != pickaxeSlot) {
                if (this.originalSlot == -1) {
                    this.originalSlot = currentSlot;
                }
                this.setSelectedSlot(pickaxeSlot);
            }
        } else if (mode == Regen_h.SwitchMode.Fake) {
            if (currentSlot == pickaxeSlot) {
                if (this.isSpoofingActive) {
                    this.isSpoofingActive = false;
                    this.lastSentSlot = pickaxeSlot;
                }
                return;
            }
            if (this.lastSentSlot != pickaxeSlot || clientSlotChanged || !this.isSpoofingActive) {
                Regen.mc.player.connection.send((Packet)new ServerboundSetCarriedItemPacket(pickaxeSlot));
                this.lastSentSlot = pickaxeSlot;
                this.isSpoofingActive = true;
            }
        }
    }

    private void restoreHotbarSlot() {
        if (Regen.mc.player == null) {
            return;
        }
        if (this.isSpoofingActive) {
            int currentActualSlot = this.getSelectedSlot();
            Regen.mc.player.connection.send((Packet)new ServerboundSetCarriedItemPacket(currentActualSlot));
            this.lastSentSlot = currentActualSlot;
            this.isSpoofingActive = false;
        }
        if (this.originalSlot != -1) {
            this.setSelectedSlot(this.originalSlot);
            this.originalSlot = -1;
        }
    }

    private boolean isValidTarget(BlockPos pos) {
        if (Regen.mc.player == null || Regen.mc.level == null) {
            return false;
        }
        if (Regen.mc.player.distanceToSqr(Vec3.atCenterOf((Vec3i)pos)) > Mth.square((double)((Regen_h)this.h).range.value)) {
            return false;
        }
        BlockState state = Regen.mc.level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        if (!((Regen_h)this.h).absorption.value) {
            return false;
        }
        float absorptionPoints = Regen.mc.player.getAbsorptionAmount();
        if (absorptionPoints >= 10.0f) {
            return false;
        }
        String name = BuiltInRegistries.BLOCK.getKey((Object)state.getBlock()).getPath().toLowerCase();
        return name.equals("redstone_ore");
    }

    private void resetMiningProgress() {
        if (this.currentMiningBlock != null && Regen.mc.player != null) {
            Regen.mc.player.connection.send((Packet)new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, this.currentMiningBlock, Direction.UP));
        }
        this.currentMiningBlock = null;
        this.miningProgress = 0.0f;
        this.lastMinedBlock = null;
        this.lastTargetBlock = null;
        this.restoreHotbarSlot();
    }

    private void breakBlock(BlockPos pos) {
        if (Regen.mc.player == null || Regen.mc.gameMode == null) {
            return;
        }
        this.handleSwingTick(true);
        if (((Regen_h)this.h).mode.value == Regen_h.Mode.Normal) {
            Regen.mc.gameMode.destroyBlock(pos);
        } else {
            Regen.mc.player.connection.send((Packet)new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
        }
        this.lastMinedBlock = pos;
        this.resetMiningProgress();
    }

    private void handleSwingTick(boolean isDestroyed) {
        int delay;
        if (Regen.mc.player == null) {
            return;
        }
        Regen_h.SwingMode sMode = (Regen_h.SwingMode)((Object)((Regen_h)this.h).swingMode.value);
        if (sMode == Regen_h.SwingMode.NoSwing) {
            return;
        }
        if (isDestroyed) {
            if (sMode == Regen_h.SwingMode.Old) {
                Regen.mc.player.swing(InteractionHand.MAIN_HAND);
            } else if (sMode == Regen_h.SwingMode.OldPacket) {
                Regen.mc.player.connection.send((Packet)new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            }
        } else if ((sMode == Regen_h.SwingMode.Normal || sMode == Regen_h.SwingMode.Silent) && Regen.mc.player.tickCount % (delay = (int)((Regen_h)this.h).swingDelay.value) == 0) {
            if (sMode == Regen_h.SwingMode.Normal) {
                Regen.mc.player.swing(InteractionHand.MAIN_HAND);
            } else {
                Regen.mc.player.connection.send((Packet)new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            }
        }
    }

    private void applySpoofRotations(float y, float p) {
        if (RotationManager.instance != null) {
            RotationManager.instance.setRotations(y, p, "Regen");
        }
    }

    private BlockPos findClosestTarget() {
        if (Regen.mc.player == null || Regen.mc.level == null) {
            return null;
        }
        BlockPos playerPos = Regen.mc.player.blockPosition();
        int r = (int)Math.ceil(((Regen_h)this.h).range.value);
        BlockPos closest = null;
        double closestDist = ((Regen_h)this.h).range.value;
        boolean foundNonLastMined = false;
        for (BlockPos pos : BlockPos.betweenClosed((BlockPos)playerPos.offset(-r, -r, -r), (BlockPos)playerPos.offset(r, r, r))) {
            double distSqr;
            double dist;
            if (!this.isValidTarget(pos) || !((dist = Math.sqrt(distSqr = Regen.mc.player.distanceToSqr(Vec3.atCenterOf((Vec3i)pos)))) <= closestDist)) continue;
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
        if (Regen.mc.player == null) {
            return new float[]{0.0f, 0.0f};
        }
        Vec3 playerEyes = Regen.mc.player.getEyePosition(1.0f);
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
        if (Regen.mc.player == null || Regen.mc.level == null || !((Regen_h)this.h).enabled) {
            return;
        }
        int n = color = ((Regen_h)this.h).colorMode.value == Regen_h.ColorMode.Theme ? this.getThemeColor(0) : -1;
        if (((Regen_h)this.h).esp.value && this.currentMiningBlock != null) {
            this.drawBlockESP(this.currentMiningBlock, color);
        }
        this.drawProgressBar(color);
    }

    private void drawBlockESP(BlockPos pos, int color) {
        Vec3 targetPos = new Vec3((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
        if (this.lerpedBoxPos == null) {
            this.lerpedBoxPos = targetPos;
        } else if (((Regen_h)this.h).easing.value) {
            float dt = ImGui.getIO().getDeltaTime();
            float factor = (float)((double)dt * ((Regen_h)this.h).easingSpeed.value);
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
        Vec3 camPos = Regen.mc.gameRenderer.getMainCamera().position();
        float pitch = Regen.mc.gameRenderer.getMainCamera().xRot();
        float yaw = Regen.mc.gameRenderer.getMainCamera().yRot();
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
            Vec3 ndc = Regen.mc.gameRenderer.projectPointToScreen(vertices[i]);
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
        boolean isMining;
        float sw = ImGui.getIO().getDisplaySizeX();
        float sh = ImGui.getIO().getDisplaySizeY();
        float delta = ImGui.getIO().getDeltaTime();
        boolean shouldShow = isMining = this.currentMiningBlock != null && this.miningProgress > 0.001f;
        if (((Regen_h)this.h).solsticeAnimation.value) {
            if (isMining && !this.wasMiningBlock) {
                this.easePercentage = 0.0f;
            }
            this.easePercentage = shouldShow ? (this.easePercentage += delta) : (this.easePercentage -= delta * 2.0f);
        } else if (shouldShow) {
            this.visibleTimer = 0.5f;
            this.easePercentage += delta;
        } else if (this.visibleTimer > 0.0f) {
            this.visibleTimer -= delta;
            this.easePercentage += delta;
        } else {
            this.easePercentage -= delta * 2.0f;
        }
        this.wasMiningBlock = isMining;
        this.easePercentage = Math.max(0.0f, Math.min(1.0f, this.easePercentage));
        float anim = MathUtils.easeOutExpo(this.easePercentage);
        if (anim <= 0.001f) {
            return;
        }
        float percentDone = Math.max(0.0f, Math.min(1.0f, this.miningProgress));
        if (((Regen_h)this.h).progressLerp.value) {
            if (percentDone < this.lastProgress) {
                this.lastProgress = percentDone;
            }
            this.lastProgress = percentDone = MathUtils.lerp(this.lastProgress, percentDone, delta * (float)((Regen_h)this.h).progressLerpSpeed.value);
        } else {
            this.lastProgress = percentDone;
        }
        ImDrawList dl = ImGui.getForegroundDrawList();
        if (((Regen_h)this.h).barMode.value == Regen_h.ProgressBarMode.Solstice) {
            this.renderSolsticeBar(dl, sw, sh, anim, percentDone, color);
        } else if (((Regen_h)this.h).barMode.value == Regen_h.ProgressBarMode.Astra) {
            this.renderAstraBar(dl, sw, sh, anim, percentDone, color);
        } else {
            this.renderOldBar(dl, sw, sh, percentDone, color);
        }
    }

    private void renderOldBar(ImDrawList dl, float sw, float sh, float percentDone, int color) {
        float barWidth = 100.0f;
        float barHeight = 4.0f;
        float x = (sw - barWidth) / 2.0f;
        float y = sh / 2.0f + 20.0f;
        int bgColor = ImGui.getColorU32(0.0f, 0.0f, 0.0f, 0.5f);
        dl.addRectFilled(x, y, x + barWidth, y + barHeight, bgColor, 2.0f);
        float fillWidth = barWidth * percentDone;
        if (fillWidth > 0.0f) {
            dl.addRectFilled(x, y, x + fillWidth, y + barHeight, color, 2.0f);
        }
    }

    private void renderSolsticeBar(ImDrawList dl, float sw, float sh, float anim, float percentDone, int color) {
        boolean isMc;
        float px = sw / 2.0f;
        float py = sh / 2.5f + sh / 2.85f;
        float boxWidth = 216.0f * anim;
        float boxHeight = 48.0f * anim;
        float x = px - boxWidth / 2.0f;
        float y = py - boxHeight / 2.0f;
        float daPadding = -25.0f * anim;
        float rounding = 99.0f;
        float max = x + boxWidth;
        float bgMinX = x + boxWidth * percentDone;
        float bgMaxX = x + boxWidth;
        float bgMaxY = y + boxHeight + daPadding;
        float progMaxX = x + (boxWidth * percentDone + 6.0f);
        progMaxX = Math.max(x, Math.min(progMaxX, max));
        dl.addRectFilled(bgMinX - 6.0f, y, bgMaxX, bgMaxY, ImGui.getColorU32(0.0f, 0.0f, 0.0f, 0.6f), rounding);
        if (percentDone > 0.001f) {
            dl.addRectFilled(x, y, progMaxX, bgMaxY, color, rounding);
        }
        int daPerc = (int)(percentDone * 100.0f);
        String text = "Mining " + daPerc + "%";
        if (Regen.mc.player != null && Regen.mc.player.getAbsorptionAmount() >= 10.0f) {
            text = "Queueing " + daPerc + "%";
        }
        boolean bl = isMc = ((Regen_h)this.h).fontMode.value == Regen_h.FontMode.Mojangles;
        String fontKey = isMc ? (((Regen_h)this.h).bold.value ? "minecraft_bold" : "minecraft") : (((Regen_h)this.h).bold.value ? "main_bold" : "main");
        ImFont font = ImGuiEngine.INSTANCE.fonts.getOrDefault(fontKey, ImGuiEngine.INSTANCE.fonts.get("main"));
        float fSize = 14.0f * anim;
        float scale = fSize / font.getFontSize();
        ImGui.pushFont((ImFont)font);
        float textWidth = ImGui.calcTextSize((String)text).x * scale;
        ImGui.popFont();
        float textX = x + (boxWidth - textWidth) / 2.0f;
        float textY = y + 1.6f * anim;
        if (((Regen_h)this.h).shadow.value) {
            float cr = (float)(color >> 16 & 0xFF) / 255.0f;
            float cg = (float)(color >> 8 & 0xFF) / 255.0f;
            float cb = (float)(color & 0xFF) / 255.0f;
            int shadowColor = ImGui.getColorU32((float)(cr * 0.25f), (float)(cg * 0.25f), (float)(cb * 0.25f), 0.925f);
            float offset = 1.0f * scale;
            dl.addText(font, (int)fSize, textX + offset, textY + offset, shadowColor, text);
        }
        dl.addText(font, (int)fSize, textX, textY, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), text);
    }

    private void renderAstraBar(ImDrawList dl, float sw, float sh, float anim, float percentDone, int color) {
        boolean isMc;
        float boxWidth = 260.0f * anim;
        float boxHeight = 70.0f * anim;
        float x = sw / 2.0f - boxWidth / 2.0f;
        float y = sh / 1.4f;
        dl.addRectFilled(x, y, x + boxWidth, y + boxHeight, ImGui.getColorU32(0.0f, 0.0f, 0.0f, 0.9f), 0.0f);
        float topBarHeight = 5.0f * anim;
        dl.addRectFilled(x, y, x + boxWidth, y + topBarHeight, color, 0.0f);
        int daPerc = (int)(percentDone * 100.0f);
        String text = "Mining " + daPerc + "%";
        if (Regen.mc.player != null && Regen.mc.player.getAbsorptionAmount() >= 10.0f) {
            text = "Queueing " + daPerc + "%";
        }
        boolean bl = isMc = ((Regen_h)this.h).fontMode.value == Regen_h.FontMode.Mojangles;
        String fontKey = isMc ? (((Regen_h)this.h).bold.value ? "minecraft_bold" : "minecraft") : (((Regen_h)this.h).bold.value ? "main_bold" : "main");
        ImFont font = ImGuiEngine.INSTANCE.fonts.getOrDefault(fontKey, ImGuiEngine.INSTANCE.fonts.get("main"));
        float fSize = 20.0f * anim;
        float scale = fSize / font.getFontSize();
        ImGui.pushFont((ImFont)font);
        float textWidth = ImGui.calcTextSize((String)text).x * scale;
        ImGui.popFont();
        float textX = x + (boxWidth - textWidth) / 2.0f;
        float textY = y + 20.0f * anim;
        if (((Regen_h)this.h).shadow.value) {
            float cr = (float)(color >> 16 & 0xFF) / 255.0f;
            float cg = (float)(color >> 8 & 0xFF) / 255.0f;
            float cb = (float)(color & 0xFF) / 255.0f;
            int shadowColor = ImGui.getColorU32((float)(cr * 0.25f), (float)(cg * 0.25f), (float)(cb * 0.25f), 0.925f);
            float offset = 1.0f * scale;
            dl.addText(font, (int)fSize, textX + offset, textY + offset, shadowColor, text);
        }
        dl.addText(font, (int)fSize, textX, textY, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), text);
        float barHeight = 5.0f * anim;
        dl.addRectFilled(x, y + boxHeight - barHeight, x + boxWidth * percentDone, y + boxHeight, color, 0.0f);
    }
}

