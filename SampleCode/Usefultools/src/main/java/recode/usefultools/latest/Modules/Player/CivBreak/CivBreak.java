/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  imgui.ImDrawList
 *  imgui.ImFont
 *  imgui.ImGui
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Holder
 *  net.minecraft.core.Vec3i
 *  net.minecraft.core.component.DataComponents
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
 *  net.minecraft.network.protocol.game.ServerboundPlayerActionPacket$Action
 *  net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
 *  net.minecraft.network.protocol.game.ServerboundSwingPacket
 *  net.minecraft.util.Mth
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.enchantment.Enchantments
 *  net.minecraft.world.item.enchantment.ItemEnchantments
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package recode.usefultools.latest.Modules.Player.CivBreak;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Combat.KillAura.KillAura;
import recode.usefultools.latest.Modules.Combat.KillAura.KillAura_h;
import recode.usefultools.latest.Modules.Misc.RotationManager.RotationManager;
import recode.usefultools.latest.Modules.ModuleManager;
import recode.usefultools.latest.Modules.Player.CivBreak.CivBreak_h;
import recode.usefultools.latest.Modules.Player.Fucker.Fucker_h;
import recode.usefultools.latest.Modules.Visual.Interface.Interface;
import recode.usefultools.latest.Modules.Visual.Interface.Interface_h;
import recode.usefultools.latest.utils.ImGuiEngine;
import recode.usefultools.latest.utils.MathUtils;

public class CivBreak
extends BaseModule<CivBreak_h> {
    public static CivBreak instance;
    public final List<String> targetBlockNames = new ArrayList<String>();
    public BlockPos targetBlock = null;
    public float miningProgress = 0.0f;
    private BlockPos lastTargetBlock = null;
    private Vec3 lerpedBoxPos = null;
    private int delayTicksRemaining = 0;
    private int retryTicksRemaining = 0;
    private int stopRotateTicks = 0;
    private boolean awaitingNextTickPackets = false;
    private int nextTickPacketType = 0;
    private boolean waitingForBlockToReappear = false;
    private boolean hasMinedOnce = false;
    private boolean isFirstActivation = true;
    private int sessionBreaksCount = 0;
    private int lastLevelHash = 0;
    private BlockState lastNonAirState = null;
    public boolean isAboutToBreak = false;
    private int packetTickCounter = 0;
    private boolean hasPostponedPacket = false;

    public CivBreak() {
        super(new CivBreak_h());
        instance = this;
        this.targetBlockNames.add("end_stone");
    }

    @Override
    public void onEnable() {
        this.miningProgress = 0.0f;
        this.lastTargetBlock = null;
        this.lerpedBoxPos = null;
        this.delayTicksRemaining = 0;
        this.retryTicksRemaining = 0;
        this.stopRotateTicks = 0;
        this.awaitingNextTickPackets = false;
        this.nextTickPacketType = 0;
        this.waitingForBlockToReappear = false;
        this.hasMinedOnce = false;
        this.lastNonAirState = null;
        this.isAboutToBreak = false;
        this.packetTickCounter = 0;
        this.hasPostponedPacket = false;
        if (((CivBreak_h)this.h).blockSelectMode.value != CivBreak_h.BlockSelectMode.Exert) {
            this.targetBlock = null;
        }
    }

    @Override
    public void onDisable() {
        if (RotationManager.instance != null) {
            RotationManager.instance.reset("CivBreak");
        }
        this.miningProgress = 0.0f;
        this.lastTargetBlock = null;
        this.lerpedBoxPos = null;
        this.hasMinedOnce = false;
        this.isAboutToBreak = false;
        this.packetTickCounter = 0;
        this.hasPostponedPacket = false;
        if (((CivBreak_h)this.h).blockSelectMode.value != CivBreak_h.BlockSelectMode.Exert) {
            this.targetBlock = null;
        }
    }

    public CivBreak_h.Mode getEffectiveMode() {
        if (((CivBreak_h)this.h).mode.value != CivBreak_h.Mode.Auto) {
            return (CivBreak_h.Mode)((Object)((CivBreak_h)this.h).mode.value);
        }
        if (CivBreak.mc.player == null) {
            return CivBreak_h.Mode.FastBreak;
        }
        ItemStack stack = CivBreak.mc.player.getMainHandItem();
        String itemId = BuiltInRegistries.ITEM.getKey((Object)stack.getItem()).getPath().toLowerCase();
        boolean isGoldTool = itemId.contains("golden_");
        if (isGoldTool) {
            int efficiencyLevel = 0;
            ItemEnchantments enchantments = (ItemEnchantments)stack.get(DataComponents.ENCHANTMENTS);
            if (enchantments != null) {
                for (Holder holder : enchantments.keySet()) {
                    if (!holder.is(Enchantments.EFFICIENCY)) continue;
                    efficiencyLevel = enchantments.getLevel(holder);
                    break;
                }
            }
            if (efficiencyLevel >= 4) {
                return CivBreak_h.Mode.Normal;
            }
            return CivBreak_h.Mode.FastBreak;
        }
        return CivBreak_h.Mode.FastBreak;
    }

    @Override
    public void onUpdate() {
        KillAura aura;
        if (CivBreak.mc.player == null || CivBreak.mc.level == null) {
            return;
        }
        this.isAboutToBreak = false;
        if (this.targetBlock != null) {
            BlockState calcState;
            BlockState state = CivBreak.mc.level.getBlockState(this.targetBlock);
            boolean isAir = state.isAir();
            BlockState blockState = calcState = isAir && this.lastNonAirState != null ? this.lastNonAirState : state;
            if (!this.hasMinedOnce) {
                if (this.delayTicksRemaining <= 1) {
                    this.isAboutToBreak = true;
                } else if (this.delayTicksRemaining == 0) {
                    float baseBreakSpeed = CivBreak.mc.player.getDestroySpeed(calcState);
                    float divisor = (float)((CivBreak_h)this.h).breakSpeed.value;
                    float speed = divisor == 0.0f ? Float.POSITIVE_INFINITY : baseBreakSpeed / divisor;
                    float progressStep = speed / 100.0f;
                    if (this.miningProgress + progressStep >= 1.0f) {
                        this.isAboutToBreak = true;
                    }
                }
            } else if (this.delayTicksRemaining <= 1) {
                if (((CivBreak_h)this.h).delayMode.value == CivBreak_h.BreakDelayMode.AirDelay) {
                    if (this.retryTicksRemaining <= 1) {
                        this.isAboutToBreak = true;
                    }
                } else {
                    this.isAboutToBreak = true;
                }
            }
        }
        boolean isAboutToTriggerStopNow = false;
        if (this.targetBlock != null) {
            BlockState calcState;
            BlockState state = CivBreak.mc.level.getBlockState(this.targetBlock);
            boolean isAir = state.isAir();
            BlockState blockState = calcState = isAir && this.lastNonAirState != null ? this.lastNonAirState : state;
            if (!this.hasMinedOnce) {
                if (this.delayTicksRemaining <= 0) {
                    float baseBreakSpeed = CivBreak.mc.player.getDestroySpeed(calcState);
                    float divisor = (float)((CivBreak_h)this.h).breakSpeed.value;
                    float speed = divisor == 0.0f ? Float.POSITIVE_INFINITY : baseBreakSpeed / divisor;
                    float progressStep = speed / 100.0f;
                    if (this.miningProgress + progressStep >= 1.0f) {
                        isAboutToTriggerStopNow = true;
                    }
                }
            } else if (this.delayTicksRemaining <= 0 && this.retryTicksRemaining <= 0) {
                isAboutToTriggerStopNow = true;
            }
        }
        if (((CivBreak_h)this.h).mode.value == CivBreak_h.Mode.Shotbow) {
            boolean intervalHit;
            ++this.packetTickCounter;
            boolean bl = intervalHit = this.packetTickCounter % (int)((CivBreak_h)this.h).packetInterval.value == 0;
            if (intervalHit) {
                if (!isAboutToTriggerStopNow) {
                    this.sendBypassPacket();
                } else {
                    switch ((CivBreak_h.OverlapBehavior)((Object)((CivBreak_h)this.h).overlapBehavior.value)) {
                        case Normal: {
                            this.sendBypassPacket();
                            break;
                        }
                        case Skip: {
                            break;
                        }
                        case NextTick: {
                            this.hasPostponedPacket = true;
                        }
                    }
                }
            } else if (this.hasPostponedPacket && !isAboutToTriggerStopNow) {
                this.sendBypassPacket();
                this.hasPostponedPacket = false;
            }
        }
        if ((aura = (KillAura)ModuleManager.INSTANCE.getModuleByName("KillAura")) != null && ((KillAura_h)aura.h).enabled && aura.isSwapping) {
            return;
        }
        int currentLevelHash = CivBreak.mc.level.hashCode();
        if (currentLevelHash != this.lastLevelHash) {
            this.lastLevelHash = currentLevelHash;
            this.isFirstActivation = true;
            this.sessionBreaksCount = 0;
            if (((CivBreak_h)this.h).enabled && ((CivBreak_h)this.h).fastestMode.value) {
                CivBreak.mc.player.sendSystemMessage((Component)Component.literal((String)"§7[§bUT§7] §e[Fastest] Dimension swap detected. Resetting statistical baseline..."));
            }
        }
        if (this.awaitingNextTickPackets && this.targetBlock != null) {
            this.sendQueuedNextTickPacket();
            this.awaitingNextTickPackets = false;
        }
        if (this.targetBlock != null && CivBreak.mc.player.distanceToSqr(Vec3.atCenterOf((Vec3i)this.targetBlock)) > Mth.square((double)((CivBreak_h)this.h).range.value)) {
            this.resetMiningProgress();
        }
        if (this.targetBlock == null && ((CivBreak_h)this.h).blockSelectMode.value == CivBreak_h.BlockSelectMode.Fucker) {
            this.targetBlock = this.findClosestTarget();
        }
        if (this.targetBlock == null) {
            if (RotationManager.instance != null) {
                RotationManager.instance.reset("CivBreak");
            }
            this.lastTargetBlock = null;
            return;
        }
        this.handleSwingTick(false);
        BlockState state = CivBreak.mc.level.getBlockState(this.targetBlock);
        boolean isAir = state.isAir();
        if (!isAir) {
            this.lastNonAirState = state;
        }
        BlockState calcState = isAir && this.lastNonAirState != null ? this.lastNonAirState : state;
        float hardness = calcState.getDestroySpeed((BlockGetter)CivBreak.mc.level, this.targetBlock);
        int predictedTicks = 9999;
        if (hardness != -1.0f) {
            float destroySpeed = CivBreak.mc.player.getDestroySpeed(calcState);
            float speedMultiplier = CivBreak.mc.player.hasCorrectToolForDrops(calcState) ? 30.0f : 100.0f;
            float damagePerTick = destroySpeed / (hardness * speedMultiplier);
            predictedTicks = (int)Math.ceil(1.0f / damagePerTick);
        }
        if (!this.targetBlock.equals((Object)this.lastTargetBlock)) {
            CivBreak.mc.player.connection.send((Packet)new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, this.targetBlock, Direction.UP));
            this.logPacket("START", this.targetBlock);
            this.lastTargetBlock = this.targetBlock;
            this.miningProgress = 0.0f;
            this.hasMinedOnce = false;
            this.delayTicksRemaining = 0;
            this.retryTicksRemaining = 0;
            this.waitingForBlockToReappear = false;
        }
        boolean onGround = CivBreak.mc.player.onGround();
        boolean canAdvanceProgress = true;
        if (!onGround) {
            if (((CivBreak_h)this.h).onGroundMode.value == CivBreak_h.OnGroundMode.MiningStop) {
                canAdvanceProgress = false;
            } else if (((CivBreak_h)this.h).onGroundMode.value == CivBreak_h.OnGroundMode.Cancel) {
                this.resetMiningProgress();
                if (RotationManager.instance != null) {
                    RotationManager.instance.reset("CivBreak");
                }
                return;
            }
        }
        if (((CivBreak_h)this.h).delayMode.value == CivBreak_h.BreakDelayMode.AirDelay && isAir) {
            this.waitingForBlockToReappear = true;
            canAdvanceProgress = false;
            this.retryTicksRemaining = 0;
            this.handleRotationUpdate(false);
            return;
        }
        if (this.waitingForBlockToReappear && !isAir) {
            int min = (int)((CivBreak_h)this.h).minDelay.value;
            int max = (int)((CivBreak_h)this.h).maxDelay.value;
            if (min > max) {
                int tmp = min;
                min = max;
                max = tmp;
            }
            this.delayTicksRemaining = min + (int)(Math.random() * (double)(max - min + 1));
            this.waitingForBlockToReappear = false;
            canAdvanceProgress = false;
        }
        if (this.delayTicksRemaining > 0) {
            --this.delayTicksRemaining;
            this.handleRotationUpdate(false);
            return;
        }
        boolean isAuraSwapping = false;
        if (this.auraActiveAndSwapping(aura)) {
            isAuraSwapping = true;
        }
        if (isAuraSwapping) {
            this.delayTicksRemaining = 1;
            this.handleRotationUpdate(false);
            return;
        }
        if (!this.hasMinedOnce) {
            if (canAdvanceProgress) {
                float baseBreakSpeed = CivBreak.mc.player.getDestroySpeed(calcState);
                float divisor = (float)((CivBreak_h)this.h).breakSpeed.value;
                float speed = divisor == 0.0f ? Float.POSITIVE_INFINITY : baseBreakSpeed / divisor;
                float progressStep = speed / 100.0f;
                this.miningProgress += progressStep;
                if (this.miningProgress >= 1.0f) {
                    this.miningProgress = 1.0f;
                    boolean isSwappingRightNow = false;
                    if (this.auraActiveAndSwapping(aura)) {
                        isSwappingRightNow = true;
                    }
                    if (!isSwappingRightNow) {
                        this.hasMinedOnce = true;
                        this.triggerBreakSequence(predictedTicks);
                    } else {
                        this.delayTicksRemaining = 1;
                    }
                }
            }
        } else {
            this.miningProgress = 1.0f;
            if (!isAir || ((CivBreak_h)this.h).delayMode.value != CivBreak_h.BreakDelayMode.AirDelay) {
                if (((CivBreak_h)this.h).delayMode.value == CivBreak_h.BreakDelayMode.AirDelay) {
                    if (this.retryTicksRemaining > 0) {
                        --this.retryTicksRemaining;
                    } else {
                        this.triggerBreakSequence(predictedTicks);
                        this.retryTicksRemaining = (int)((CivBreak_h)this.h).retryDelay.value;
                    }
                } else {
                    this.triggerBreakSequence(predictedTicks);
                }
            }
        }
        this.handleRotationUpdate(false);
    }

    private void sendBypassPacket() {
        if (CivBreak.mc.player == null) {
            return;
        }
        switch ((CivBreak_h.BypassPacketType)((Object)((CivBreak_h)this.h).bypassPacketType.value)) {
            case Abort: {
                BlockPos pos = this.targetBlock != null ? this.targetBlock : BlockPos.ZERO;
                CivBreak.mc.player.connection.send((Packet)new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, pos, Direction.UP));
                this.logBypassPacket("ABORT");
                break;
            }
            case Release: {
                CivBreak.mc.player.connection.send((Packet)new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));
                this.logBypassPacket("RELEASE");
                break;
            }
            case Twoswap: {
                CivBreak.mc.player.connection.send((Packet)new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
                CivBreak.mc.player.connection.send((Packet)new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
                this.logBypassPacket("TWOSWAP (Offhand-Swap x2)");
            }
        }
    }

    private void logBypassPacket(String typeName) {
        if (CivBreak.mc.player != null && ((CivBreak_h)this.h).debugLogMode.value != CivBreak_h.DebugLogMode.NONE) {
            CivBreak.mc.player.sendSystemMessage((Component)Component.literal((String)("§7[§bUT§7] §e[CivBreak] Periodic " + typeName + " Sent")));
        }
    }

    private boolean auraActiveAndSwapping(KillAura aura) {
        if (aura == null || !((KillAura_h)aura.h).enabled) {
            return false;
        }
        return aura.isSwapping || ((CivBreak_h)this.h).delayOnAura.value && aura.isAboutToSwap;
    }

    private void handleRotationUpdate(boolean forceStopOnly) {
        boolean shouldRotate;
        if (!((CivBreak_h)this.h).rotate.value) {
            return;
        }
        boolean bl = shouldRotate = ((CivBreak_h)this.h).rotationMode.value == CivBreak_h.RotationMode.Always;
        if (((CivBreak_h)this.h).rotationMode.value == CivBreak_h.RotationMode.StopOnly || forceStopOnly) {
            if (this.stopRotateTicks > 0) {
                shouldRotate = true;
                if (!forceStopOnly) {
                    --this.stopRotateTicks;
                }
            }
            if (this.hasMinedOnce && this.delayTicksRemaining >= 0 && this.delayTicksRemaining <= (int)((CivBreak_h)this.h).startRotateBefore.value) {
                shouldRotate = true;
            }
            if (!this.hasMinedOnce && this.targetBlock != null) {
                float remainingProgress;
                int remainingTicks;
                BlockState state = CivBreak.mc.level.getBlockState(this.targetBlock);
                float baseBreakSpeed = CivBreak.mc.player.getDestroySpeed(state);
                float divisor = (float)((CivBreak_h)this.h).breakSpeed.value;
                float speed = divisor == 0.0f ? Float.POSITIVE_INFINITY : baseBreakSpeed / divisor;
                float progressStep = speed / 100.0f;
                if (progressStep > 0.0f && (remainingTicks = (int)Math.ceil((remainingProgress = 1.0f - this.miningProgress) / progressStep)) <= (int)((CivBreak_h)this.h).startRotateBefore.value) {
                    shouldRotate = true;
                }
            }
        }
        if (shouldRotate && this.targetBlock != null) {
            float[] rots = this.getRotationsToBlock(this.targetBlock);
            if (RotationManager.instance != null) {
                RotationManager.instance.setRotations(rots[0], rots[1], "CivBreak");
            }
        } else if (RotationManager.instance != null) {
            RotationManager.instance.reset("CivBreak");
        }
    }

    private int findBestTool(BlockPos pos) {
        if (CivBreak.mc.player == null || CivBreak.mc.level == null) {
            return -1;
        }
        BlockState state = CivBreak.mc.level.getBlockState(pos);
        int bestSlot = -1;
        float bestSpeed = 1.0f;
        for (int i = 0; i < 9; ++i) {
            float speed;
            ItemStack stack = CivBreak.mc.player.getInventory().getItem(i);
            if (stack.isEmpty() || !((speed = stack.getDestroySpeed(state)) > bestSpeed)) continue;
            bestSpeed = speed;
            bestSlot = i;
        }
        return bestSlot;
    }

    private int getSelectedSlot() {
        if (CivBreak.mc.player == null) {
            return 0;
        }
        try {
            Field field = Inventory.class.getDeclaredField("selected");
            field.setAccessible(true);
            return field.getInt(CivBreak.mc.player.getInventory());
        } catch (Exception e) {
            try {
                for (Field f : Inventory.class.getDeclaredFields()) {
                    if (f.getType() != Integer.TYPE || !f.getName().equals("selected") && !f.getName().equals("selectedSlot")) continue;
                    f.setAccessible(true);
                    return f.getInt(CivBreak.mc.player.getInventory());
                }
            } catch (Exception exception) {
                // empty catch block
            }
            return 0;
        }
    }

    private void setSelectedSlot(int slot) {
        if (CivBreak.mc.player == null) {
            return;
        }
        try {
            Field field = Inventory.class.getDeclaredField("selected");
            field.setAccessible(true);
            field.setInt(CivBreak.mc.player.getInventory(), slot);
        } catch (Exception e) {
            try {
                for (Field f : Inventory.class.getDeclaredFields()) {
                    if (f.getType() != Integer.TYPE || !f.getName().equals("selected") && !f.getName().equals("selectedSlot")) continue;
                    f.setAccessible(true);
                    f.setInt(CivBreak.mc.player.getInventory(), slot);
                    return;
                }
            } catch (Exception exception) {
                // empty catch block
            }
        }
    }

    private void triggerBreakSequence(int predictedTicks) {
        if (this.targetBlock == null || CivBreak.mc.player == null) {
            return;
        }
        this.handleSwingTick(true);
        if (((CivBreak_h)this.h).fastestMode.value && this.isFirstActivation) {
            ++this.sessionBreaksCount;
            int targetCount = (int)((CivBreak_h)this.h).bypassCount.value;
            CivBreak.mc.player.sendSystemMessage((Component)Component.literal((String)("§7[§bUT§7] §e[Fastest] Statistical Injection Progress: " + this.sessionBreaksCount + " / " + targetCount)));
            if (this.sessionBreaksCount >= targetCount) {
                this.isFirstActivation = false;
                this.sessionBreaksCount = 0;
                CivBreak.mc.player.sendSystemMessage((Component)Component.literal((String)"§7[§bUT§7] §a[Fastest] Statistical Baseline Spoofing Complete. Toggling OFF module..."));
                this.setEnabled(false);
                this.resetMiningProgress();
                return;
            }
        }
        int bestToolSlot = this.findBestToolSlotForBlock(this.targetBlock);
        int originalSlot = this.getSelectedSlot();
        boolean swapped = false;
        if (((CivBreak_h)this.h).switchMode.value != CivBreak_h.SwitchMode.None && bestToolSlot != -1 && originalSlot != bestToolSlot) {
            swapped = true;
            if (((CivBreak_h)this.h).switchMode.value == CivBreak_h.SwitchMode.Spoof) {
                this.setSelectedSlot(bestToolSlot);
                this.logSwitch("SPOOF", originalSlot, bestToolSlot);
            } else if (((CivBreak_h)this.h).switchMode.value == CivBreak_h.SwitchMode.Fake) {
                CivBreak.mc.player.connection.send((Packet)new ServerboundSetCarriedItemPacket(bestToolSlot));
                this.logSwitch("FAKE", originalSlot, bestToolSlot);
            }
        }
        CivBreak.mc.player.connection.send((Packet)new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, this.targetBlock, Direction.UP));
        this.logPacket("STOP", this.targetBlock);
        if (swapped) {
            if (((CivBreak_h)this.h).switchMode.value == CivBreak_h.SwitchMode.Spoof) {
                this.setSelectedSlot(originalSlot);
                this.logRevert("SPOOF", bestToolSlot, originalSlot);
            } else if (((CivBreak_h)this.h).switchMode.value == CivBreak_h.SwitchMode.Fake) {
                CivBreak.mc.player.connection.send((Packet)new ServerboundSetCarriedItemPacket(originalSlot));
                this.logRevert("FAKE", bestToolSlot, originalSlot);
            }
        }
        this.stopRotateTicks = (int)((CivBreak_h)this.h).stopRotateDuration.value;
        this.handleRotationUpdate(true);
        boolean oneTick = ((CivBreak_h)this.h).oneTickPackets.value;
        CivBreak_h.Mode activeMode = this.getEffectiveMode();
        if (activeMode == CivBreak_h.Mode.Normal || activeMode == CivBreak_h.Mode.Shotbow) {
            if (((CivBreak_h)this.h).bypassMode.value == CivBreak_h.BypassMode.AntiCiv2_2) {
                if (oneTick) {
                    CivBreak.mc.player.connection.send((Packet)new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, this.targetBlock, Direction.UP));
                    this.logPacket("ABORT", this.targetBlock);
                } else {
                    this.awaitingNextTickPackets = true;
                    this.nextTickPacketType = 1;
                }
            } else if (((CivBreak_h)this.h).bypassMode.value == CivBreak_h.BypassMode.AntiCiv2_8 || ((CivBreak_h)this.h).bypassMode.value == CivBreak_h.BypassMode.AntiCiv2_11) {
                if (oneTick) {
                    CivBreak.mc.player.connection.send((Packet)new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));
                    this.logPacket("RELEASE", BlockPos.ZERO);
                } else {
                    this.awaitingNextTickPackets = true;
                    this.nextTickPacketType = 2;
                }
            }
        } else if (activeMode == CivBreak_h.Mode.FastBreak) {
            if (oneTick) {
                CivBreak.mc.player.connection.send((Packet)new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, this.targetBlock, Direction.UP));
                this.logPacket("START", this.targetBlock);
            } else {
                this.awaitingNextTickPackets = true;
                this.nextTickPacketType = 3;
            }
        }
        if (((CivBreak_h)this.h).delayMode.value == CivBreak_h.BreakDelayMode.Normal || ((CivBreak_h)this.h).delayMode.value == CivBreak_h.BreakDelayMode.Simulation) {
            int min = (int)((CivBreak_h)this.h).minDelay.value;
            int max = (int)((CivBreak_h)this.h).maxDelay.value;
            if (min > max) {
                int tmp = min;
                min = max;
                max = tmp;
            }
            int randomPadding = min + (int)(Math.random() * (double)(max - min + 1));
            if (((CivBreak_h)this.h).delayMode.value == CivBreak_h.BreakDelayMode.Simulation) {
                float baseTicks = (float)predictedTicks - 0.78f;
                if (((CivBreak_h)this.h).fastestMode.value && !this.isFirstActivation) {
                    baseTicks -= 2.0f;
                }
                this.delayTicksRemaining = (int)Math.ceil(baseTicks) + randomPadding;
            } else {
                this.delayTicksRemaining = randomPadding;
            }
        }
    }

    private void logSwitch(String modeName, int from, int to) {
        if (CivBreak.mc.player != null && ((CivBreak_h)this.h).debugLogMode.value != CivBreak_h.DebugLogMode.NONE) {
            CivBreak.mc.player.sendSystemMessage((Component)Component.literal((String)("§7[§bUT§7] §e[CivBreak] " + modeName + " Switch: Slot " + from + " -> " + to)));
        }
    }

    private void logRevert(String modeName, int from, int to) {
        if (CivBreak.mc.player != null && ((CivBreak_h)this.h).debugLogMode.value != CivBreak_h.DebugLogMode.NONE) {
            CivBreak.mc.player.sendSystemMessage((Component)Component.literal((String)("§7[§bUT§7] §a[CivBreak] " + modeName + " Revert: Slot " + from + " -> " + to)));
        }
    }

    private int findBlockToolSlot(BlockPos pos) {
        if (CivBreak.mc.player == null) {
            return -1;
        }
        BlockState state = CivBreak.mc.level.getBlockState(pos);
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = CivBreak.mc.player.getInventory().getItem(i);
            if (!(CivBreak.mc.player.getDestroySpeed(state) > 1.0f) && !stack.isCorrectToolForDrops(state)) continue;
            return i;
        }
        return -1;
    }

    private int findBestToolSlotForBlock(BlockPos pos) {
        int slot = this.findBlockToolSlot(pos);
        if (slot == -1) {
            return this.findPickaxeSlot();
        }
        return slot;
    }

    private int findPickaxeSlot() {
        if (CivBreak.mc.player == null) {
            return -1;
        }
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = CivBreak.mc.player.getInventory().getItem(i);
            String itemId = BuiltInRegistries.ITEM.getKey((Object)stack.getItem()).getPath().toLowerCase();
            if (!itemId.contains("pickaxe")) continue;
            return i;
        }
        return -1;
    }

    private void sendQueuedNextTickPacket() {
        if (this.targetBlock == null || CivBreak.mc.player == null) {
            return;
        }
        switch (this.nextTickPacketType) {
            case 1: {
                CivBreak.mc.player.connection.send((Packet)new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, this.targetBlock, Direction.UP));
                this.logPacket("ABORT (Queued)", this.targetBlock);
                break;
            }
            case 2: {
                CivBreak.mc.player.connection.send((Packet)new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));
                this.logPacket("RELEASE (Queued)", BlockPos.ZERO);
                break;
            }
            case 3: {
                CivBreak.mc.player.connection.send((Packet)new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, this.targetBlock, Direction.UP));
                this.logPacket("START (Queued)", this.targetBlock);
            }
        }
        this.nextTickPacketType = 0;
    }

    private void resetMiningProgress() {
        if (this.targetBlock != null && CivBreak.mc.player != null) {
            CivBreak.mc.player.connection.send((Packet)new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, this.targetBlock, Direction.UP));
            this.logPacket("ABORT", this.targetBlock);
        }
        this.miningProgress = 0.0f;
        this.lastTargetBlock = null;
        this.hasMinedOnce = false;
        if (((CivBreak_h)this.h).blockSelectMode.value != CivBreak_h.BlockSelectMode.Exert) {
            this.targetBlock = null;
        }
    }

    private void handleSwingTick(boolean isDestroyed) {
        int delay;
        if (CivBreak.mc.player == null) {
            return;
        }
        CivBreak_h.SwingMode sMode = (CivBreak_h.SwingMode)((Object)((CivBreak_h)this.h).swingMode.value);
        if (sMode == CivBreak_h.SwingMode.NoSwing) {
            return;
        }
        if (isDestroyed) {
            if (sMode == CivBreak_h.SwingMode.Old) {
                CivBreak.mc.player.swing(InteractionHand.MAIN_HAND);
            } else if (sMode == CivBreak_h.SwingMode.OldPacket) {
                CivBreak.mc.player.connection.send((Packet)new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            }
        } else if ((sMode == CivBreak_h.SwingMode.Normal || sMode == CivBreak_h.SwingMode.Silent) && CivBreak.mc.player.tickCount % (delay = (int)((CivBreak_h)this.h).swingDelay.value) == 0) {
            if (sMode == CivBreak_h.SwingMode.Normal) {
                CivBreak.mc.player.swing(InteractionHand.MAIN_HAND);
            } else {
                CivBreak.mc.player.connection.send((Packet)new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            }
        }
    }

    private void logPacket(String actionName, BlockPos pos) {
        if (CivBreak.mc.player == null) {
            return;
        }
        CivBreak_h.DebugLogMode logMode = (CivBreak_h.DebugLogMode)((Object)((CivBreak_h)this.h).debugLogMode.value);
        if (logMode == CivBreak_h.DebugLogMode.NONE) {
            return;
        }
        boolean isStart = actionName.startsWith("START");
        boolean isStop = actionName.startsWith("STOP");
        boolean shouldLog = false;
        switch (logMode) {
            case ALL: {
                shouldLog = true;
                break;
            }
            case STOP: {
                shouldLog = isStop;
                break;
            }
            case ALL_EXCEPT_STOP: {
                shouldLog = !isStop;
                break;
            }
            case START_ONLY: {
                shouldLog = isStart;
                break;
            }
            case START_STOP: {
                boolean bl = shouldLog = isStart || isStop;
            }
        }
        if (shouldLog) {
            CivBreak.mc.player.sendSystemMessage((Component)Component.literal((String)("§7[§bUT§7] §e[CivBreak] Sent " + actionName + " -> X: " + pos.getX() + ", Y: " + pos.getY() + ", Z: " + pos.getZ())));
        }
    }

    private BlockPos findClosestTarget() {
        if (CivBreak.mc.player == null || CivBreak.mc.level == null) {
            return null;
        }
        BlockPos playerPos = CivBreak.mc.player.blockPosition();
        int r = (int)Math.ceil(((CivBreak_h)this.h).range.value);
        BlockPos closest = null;
        double closestDist = ((CivBreak_h)this.h).range.value;
        for (BlockPos pos : BlockPos.betweenClosed((BlockPos)playerPos.offset(-r, -r, -r), (BlockPos)playerPos.offset(r, r, r))) {
            double distSqr;
            double dist;
            String name;
            BlockState state = CivBreak.mc.level.getBlockState(pos);
            if (state.isAir() || !this.targetBlockNames.contains(name = BuiltInRegistries.BLOCK.getKey((Object)state.getBlock()).getPath().toLowerCase()) || !((dist = Math.sqrt(distSqr = CivBreak.mc.player.distanceToSqr(Vec3.atCenterOf((Vec3i)pos)))) <= closestDist)) continue;
            closest = pos.immutable();
            closestDist = dist;
        }
        return closest;
    }

    private float[] getRotationsToBlock(BlockPos pos) {
        if (CivBreak.mc.player == null) {
            return new float[]{0.0f, 0.0f};
        }
        Vec3 playerEyes = CivBreak.mc.player.getEyePosition(1.0f);
        Vec3 blockCenter = Vec3.atCenterOf((Vec3i)pos);
        double dx = blockCenter.x - playerEyes.x;
        double dy = blockCenter.y - playerEyes.y;
        double dz = blockCenter.z - playerEyes.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float)(-(Math.atan2(dy, dist) * 180.0 / Math.PI));
        return new float[]{Mth.wrapDegrees((float)yaw), Mth.wrapDegrees((float)pitch)};
    }

    private int getThemeColor(int index) {
        Interface ui = (Interface)ModuleManager.INSTANCE.getModuleByName("Interface");
        return ui != null ? ui.getCurrentColor(index) : -16711681;
    }

    @Override
    public void onRenderHUD() {
        int color;
        if (CivBreak.mc.player == null || CivBreak.mc.level == null || !((CivBreak_h)this.h).enabled) {
            return;
        }
        int n = color = ((CivBreak_h)this.h).colorMode.value == CivBreak_h.ColorMode.Theme ? this.getThemeColor(0) : -1;
        if (((CivBreak_h)this.h).esp.value && this.targetBlock != null) {
            this.drawBlockESP(this.targetBlock, color);
        }
        if (this.targetBlock != null) {
            this.drawProgressBar(color);
        }
    }

    private void drawBlockESP(BlockPos pos, int color) {
        Vec3 targetPos = new Vec3((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
        if (this.lerpedBoxPos == null) {
            this.lerpedBoxPos = targetPos;
        } else if (((CivBreak_h)this.h).easing.value) {
            float dt = ImGui.getIO().getDeltaTime();
            float factor = (float)((double)dt * ((CivBreak_h)this.h).easingSpeed.value);
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
        Vec3 camPos = CivBreak.mc.gameRenderer.getMainCamera().position();
        float pitch = CivBreak.mc.gameRenderer.getMainCamera().xRot();
        float yaw = CivBreak.mc.gameRenderer.getMainCamera().yRot();
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
            Vec3 ndc = CivBreak.mc.gameRenderer.projectPointToScreen(vertices[i]);
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
        if (((CivBreak_h)this.h).barMode.value == Fucker_h.ProgressBarMode.New && this.targetBlock != null) {
            boolean isMc;
            Interface ui = (Interface)ModuleManager.INSTANCE.getModuleByName("Interface");
            boolean bl = ((CivBreak_h)this.h).fontMode.value == Fucker_h.FontMode.InterfaceF ? ui != null && ((Interface_h)ui.h).font.value == Interface_h.FontType.Mojangles : (isMc = ((CivBreak_h)this.h).fontMode.value == Fucker_h.FontMode.Mojangles);
            String fontKey = isMc ? (((CivBreak_h)this.h).bold.value ? "minecraft_bold" : "minecraft") : (((CivBreak_h)this.h).bold.value ? "main_bold" : "main");
            ImFont font = ImGuiEngine.INSTANCE.fonts.getOrDefault(fontKey, ImGuiEngine.INSTANCE.fonts.get("main"));
            float fSize = 14.0f;
            float scale = fSize / font.getFontSize();
            BlockState state = CivBreak.mc.level.getBlockState(this.targetBlock);
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
            if (((CivBreak_h)this.h).shadow.value) {
                int shadowColor = ImGui.getColorU32((float)(r * 0.25f), (float)(g * 0.25f), (float)(b * 0.25f), 0.925f);
                float offset = 1.0f * scale;
                dl.addText(font, (int)fSize, isMc ? (float)Math.round(tx + offset) : tx + offset, isMc ? (float)Math.round(ty + offset) : ty + offset, shadowColor, text);
            }
            dl.addText(font, (int)fSize, isMc ? (float)Math.round(tx) : tx, isMc ? (float)Math.round(ty) : ty, color, text);
            ImGui.popFont();
        }
    }
}

