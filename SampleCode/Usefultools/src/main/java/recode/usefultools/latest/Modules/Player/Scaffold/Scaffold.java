/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
 *  net.minecraft.network.protocol.game.ServerboundSwingPacket
 *  net.minecraft.util.Mth
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.item.BlockItem
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.Vec3
 */
package recode.usefultools.latest.Modules.Player.Scaffold;

import java.lang.reflect.Field;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Misc.RotationManager.RotationManager;
import recode.usefultools.latest.Modules.Player.Scaffold.Scaffold_h;

public class Scaffold
extends BaseModule<Scaffold_h> {
    public static Scaffold instance;
    public float serverYaw;
    public float serverPitch;
    public double startY;
    private double placeAccumulator = 0.0;
    private long lastPlaceTime = 0L;
    private int lastProcessedTick = -1;
    public BlockPos renderPos = null;
    private float lastLookYaw = 0.0f;
    private float lastLookPitch = 0.0f;
    private long lookTimer = 0L;
    public boolean rotating = false;
    private int originalSlot = -1;
    private int lastSentSlot = -1;
    private int lastClientSelectedSlot = -1;
    private boolean isSpoofingActive = false;
    private long targetLockStartTime = 0L;
    private boolean isCurrentlyInBeforeTime = false;
    private int sneakReleaseTicks = 0;

    public Scaffold() {
        super(new Scaffold_h());
        instance = this;
    }

    @Override
    public void onEnable() {
        if (Scaffold.mc.player == null) {
            return;
        }
        this.startY = Mth.floor((double)Scaffold.mc.player.getY());
        this.placeAccumulator = 0.0;
        this.lastPlaceTime = 0L;
        this.lastProcessedTick = -1;
        this.rotating = false;
        this.originalSlot = -1;
        this.lastSentSlot = -1;
        this.lastClientSelectedSlot = -1;
        this.isSpoofingActive = false;
        this.targetLockStartTime = 0L;
        this.isCurrentlyInBeforeTime = false;
        this.sneakReleaseTicks = 0;
    }

    @Override
    public void onDisable() {
        if (RotationManager.instance != null) {
            RotationManager.instance.reset("Scaffold");
        }
        this.restoreHotbarSlot(true);
        this.renderPos = null;
        this.rotating = false;
        if (Scaffold.mc.options != null && ((Scaffold_h)this.h).safeWalkMode.value == Scaffold_h.SafeWalkMode.Sneak) {
            Scaffold.mc.options.keyShift.setDown(false);
        }
    }

    /*
     * Enabled aggressive block sorting
     */
    @Override
    public void onUpdate() {
        boolean allowPlacement;
        boolean isMovingInput;
        boolean clientSlotChanged;
        block23: {
            BlockData currentData;
            block22: {
                boolean isNewPlacementCycle;
                if (Scaffold.mc.player == null || Scaffold.mc.level == null) {
                    return;
                }
                if (Scaffold.mc.player.tickCount == this.lastProcessedTick) {
                    return;
                }
                this.lastProcessedTick = Scaffold.mc.player.tickCount;
                int clientSelectedSlot = this.getSelectedSlot();
                clientSlotChanged = this.lastClientSelectedSlot != -1 && this.lastClientSelectedSlot != clientSelectedSlot;
                this.lastClientSelectedSlot = clientSelectedSlot;
                boolean isMovingForward = Scaffold.mc.options.keyUp.isDown() || Scaffold.mc.options.keyDown.isDown();
                boolean isMovingSideways = Scaffold.mc.options.keyLeft.isDown() || Scaffold.mc.options.keyRight.isDown();
                boolean bl = isMovingInput = isMovingForward || isMovingSideways;
                if (!((Scaffold_h)this.h).lockY.value || !isMovingInput || Scaffold.mc.options.keyJump.isDown() && Scaffold.mc.player.onGround()) {
                    this.startY = Mth.floor((double)Scaffold.mc.player.getY());
                }
                currentData = this.findIntelligentFoundation(isMovingInput);
                this.applyRotation(currentData);
                if (this.rotating) {
                    if (RotationManager.instance != null) {
                        RotationManager.instance.setRotations(this.serverYaw, this.serverPitch, "Scaffold");
                    }
                } else if (RotationManager.instance != null) {
                    RotationManager.instance.reset("Scaffold");
                }
                allowPlacement = true;
                this.isCurrentlyInBeforeTime = false;
                if (!((Scaffold_h)this.h).lockTimeBefore.value || currentData == null) break block22;
                boolean bl2 = isNewPlacementCycle = System.currentTimeMillis() - this.lastPlaceTime > 500L;
                if (isNewPlacementCycle) {
                    long elapsedBefore;
                    if (this.targetLockStartTime == 0L) {
                        this.targetLockStartTime = System.currentTimeMillis();
                    }
                    if ((elapsedBefore = System.currentTimeMillis() - this.targetLockStartTime) < (long)((Scaffold_h)this.h).beforeTime.value) {
                        allowPlacement = false;
                        this.isCurrentlyInBeforeTime = true;
                    }
                    break block23;
                } else {
                    this.targetLockStartTime = 0L;
                }
                break block23;
            }
            if (currentData == null) {
                this.targetLockStartTime = 0L;
            }
        }
        this.handleSafeWalkPhysics();
        if (allowPlacement && System.currentTimeMillis() - this.lastPlaceTime >= (long)((int)((Scaffold_h)this.h).placeDelay.value)) {
            this.placeAccumulator += ((Scaffold_h)this.h).places.value;
            while (this.placeAccumulator >= 1.0) {
                BlockData placeData = this.findIntelligentFoundation(isMovingInput);
                if (placeData != null) {
                    int targetSlot = this.findBlockInHotbar();
                    if (targetSlot != -1) {
                        this.handleFakeSwitch(targetSlot, clientSlotChanged);
                        if (this.placeBlock(placeData, targetSlot)) {
                            this.lastPlaceTime = System.currentTimeMillis();
                            this.lookTimer = System.currentTimeMillis() + (long)((Scaffold_h)this.h).lookTime.value;
                            this.targetLockStartTime = 0L;
                            this.isCurrentlyInBeforeTime = false;
                            this.placeAccumulator -= 1.0;
                            continue;
                        }
                        this.placeAccumulator = 0.0;
                        break;
                    }
                    this.placeAccumulator = 0.0;
                    break;
                }
                this.placeAccumulator = 0.0;
                break;
            }
        }
        if (((Scaffold_h)this.h).switchTime.value && System.currentTimeMillis() - this.lastPlaceTime >= (long)((Scaffold_h)this.h).switchTimeValue.value) {
            this.restoreHotbarSlot(false);
        } else if (!((Scaffold_h)this.h).switchTime.value && this.placeAccumulator < 1.0) {
            this.restoreHotbarSlot(false);
        }
        BlockData finalData = this.findIntelligentFoundation(isMovingInput);
        BlockPos blockPos = this.renderPos = finalData != null ? finalData.targetPos() : this.getTargetPosOnly(((Scaffold_h)this.h).extend.value, isMovingInput);
        if (Scaffold.mc.options.keyJump.isDown()) {
            this.handleTower();
        }
        if (((Scaffold_h)this.h).sprintMode.value == Scaffold_h.SprintMode.NONE) {
            Scaffold.mc.player.setSprinting(false);
        }
    }

    private void handleSafeWalkPhysics() {
        if (Scaffold.mc.player == null || Scaffold.mc.level == null) {
            return;
        }
        Scaffold_h.SafeWalkMode mode = (Scaffold_h.SafeWalkMode)((Object)((Scaffold_h)this.h).safeWalkMode.value);
        if (mode == Scaffold_h.SafeWalkMode.None) {
            return;
        }
        if (((Scaffold_h)this.h).lockTimeBefore.value && ((Scaffold_h)this.h).beforeTimeOnly.value && !this.isCurrentlyInBeforeTime) {
            if (mode == Scaffold_h.SafeWalkMode.Sneak) {
                Scaffold.mc.options.keyShift.setDown(false);
                this.sneakReleaseTicks = 0;
            }
            return;
        }
        double px = Scaffold.mc.player.getX();
        double pz = Scaffold.mc.player.getZ();
        if (mode == Scaffold_h.SafeWalkMode.Sneak) {
            BlockPos playerValPos = Scaffold.mc.player.blockPosition();
            double fracX = px - Math.floor(px);
            double fracZ = pz - Math.floor(pz);
            float clientYaw = Scaffold.mc.player.getYRot() % 360.0f;
            if (clientYaw < 0.0f) {
                clientYaw += 360.0f;
            }
            double thresholdX = ((Scaffold_h)this.h).edgeDistanceSideways.value;
            double thresholdZ = ((Scaffold_h)this.h).edgeDistanceForward.value;
            if (clientYaw >= 315.0f || clientYaw < 45.0f || clientYaw >= 135.0f && clientYaw < 225.0f) {
                thresholdZ = ((Scaffold_h)this.h).edgeDistanceForward.value;
                thresholdX = ((Scaffold_h)this.h).edgeDistanceSideways.value;
            } else {
                thresholdX = ((Scaffold_h)this.h).edgeDistanceForward.value;
                thresholdZ = ((Scaffold_h)this.h).edgeDistanceSideways.value;
            }
            boolean shouldSneak = false;
            if (fracX <= thresholdX && Scaffold.mc.level.getBlockState(playerValPos.west().below()).isAir()) {
                shouldSneak = true;
            }
            if (1.0 - fracX <= thresholdX && Scaffold.mc.level.getBlockState(playerValPos.east().below()).isAir()) {
                shouldSneak = true;
            }
            if (fracZ <= thresholdZ && Scaffold.mc.level.getBlockState(playerValPos.north().below()).isAir()) {
                shouldSneak = true;
            }
            if (1.0 - fracZ <= thresholdZ && Scaffold.mc.level.getBlockState(playerValPos.south().below()).isAir()) {
                shouldSneak = true;
            }
            if (shouldSneak) {
                Scaffold.mc.options.keyShift.setDown(true);
                this.sneakReleaseTicks = 0;
            } else {
                ++this.sneakReleaseTicks;
                if (this.sneakReleaseTicks >= (int)((Scaffold_h)this.h).sneakReleaseDelay.value) {
                    Scaffold.mc.options.keyShift.setDown(false);
                    this.sneakReleaseTicks = 0;
                }
            }
        }
    }

    public void onTravel(LocalPlayer player) {
        if (player == null || ((Scaffold_h)this.h).safeWalkMode.value != Scaffold_h.SafeWalkMode.Normal) {
            return;
        }
        if (((Scaffold_h)this.h).lockTimeBefore.value && ((Scaffold_h)this.h).beforeTimeOnly.value && !this.isCurrentlyInBeforeTime) {
            return;
        }
        Vec3 velocity = player.getDeltaMovement();
        double nextX = player.getX() + velocity.x;
        double nextZ = player.getZ() + velocity.z;
        BlockPos nextBelowPos = new BlockPos(Mth.floor((double)nextX), Mth.floor((double)(player.getY() - 1.0)), Mth.floor((double)nextZ));
        if (Scaffold.mc.level != null && Scaffold.mc.level.getBlockState(nextBelowPos).isAir()) {
            player.setDeltaMovement(0.0, velocity.y, 0.0);
        }
    }

    private Vec3 getDirectionVector(Direction face) {
        return new Vec3((double)face.getStepX(), (double)face.getStepY(), (double)face.getStepZ());
    }

    private boolean placeBlock(BlockData data, int slot) {
        Vec3 hitVec;
        BlockHitResult result;
        boolean success;
        if (Scaffold.mc.player == null) {
            return false;
        }
        int oldSlot = this.getSelectedSlot();
        if (((Scaffold_h)this.h).switchMode.value == Scaffold_h.SwitchMode.Full || ((Scaffold_h)this.h).switchMode.value == Scaffold_h.SwitchMode.FullReverse || ((Scaffold_h)this.h).switchMode.value == Scaffold_h.SwitchMode.Spoof) {
            if (this.originalSlot == -1) {
                this.originalSlot = oldSlot;
            }
            this.setSelectedSlot(slot);
        }
        if (success = Scaffold.mc.gameMode.useItemOn(Scaffold.mc.player, InteractionHand.MAIN_HAND, result = new BlockHitResult(hitVec = Vec3.atCenterOf((Vec3i)data.pos()).add(this.getDirectionVector(data.face()).scale(0.5)), data.face(), data.pos(), false)).consumesAction()) {
            if (((Scaffold_h)this.h).swingMode.value == Scaffold_h.SwingMode.NORMAL) {
                Scaffold.mc.player.swing(InteractionHand.MAIN_HAND);
            } else if (((Scaffold_h)this.h).swingMode.value == Scaffold_h.SwingMode.SILENT) {
                Scaffold.mc.player.connection.send((Packet)new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            }
        }
        if (((Scaffold_h)this.h).switchMode.value == Scaffold_h.SwitchMode.Spoof) {
            this.setSelectedSlot(oldSlot);
        }
        return success;
    }

    private void handleFakeSwitch(int targetSlot, boolean clientSlotChanged) {
        if (((Scaffold_h)this.h).switchMode.value != Scaffold_h.SwitchMode.Fake) {
            return;
        }
        int currentSlot = this.getSelectedSlot();
        if (currentSlot == targetSlot) {
            if (this.isSpoofingActive) {
                this.isSpoofingActive = false;
                this.lastSentSlot = targetSlot;
            }
            return;
        }
        if (this.lastSentSlot != targetSlot || clientSlotChanged || !this.isSpoofingActive) {
            Scaffold.mc.player.connection.send((Packet)new ServerboundSetCarriedItemPacket(targetSlot));
            this.lastSentSlot = targetSlot;
            this.isSpoofingActive = true;
        }
    }

    private void restoreHotbarSlot() {
        this.restoreHotbarSlot(false);
    }

    private void restoreHotbarSlot(boolean forceRevert) {
        if (Scaffold.mc.player == null) {
            return;
        }
        if (this.isSpoofingActive) {
            int currentActualSlot = this.getSelectedSlot();
            Scaffold.mc.player.connection.send((Packet)new ServerboundSetCarriedItemPacket(currentActualSlot));
            this.lastSentSlot = currentActualSlot;
            this.isSpoofingActive = false;
        }
        if (((Scaffold_h)this.h).switchMode.value == Scaffold_h.SwitchMode.FullReverse || forceRevert) {
            if (this.originalSlot != -1) {
                this.setSelectedSlot(this.originalSlot);
                this.originalSlot = -1;
            }
        } else if (((Scaffold_h)this.h).switchMode.value != Scaffold_h.SwitchMode.Full) {
            this.originalSlot = -1;
        }
    }

    private int getSelectedSlot() {
        if (Scaffold.mc.player == null) {
            return 0;
        }
        try {
            Field field = Inventory.class.getDeclaredField("selected");
            field.setAccessible(true);
            return field.getInt(Scaffold.mc.player.getInventory());
        } catch (Exception e) {
            try {
                for (Field f : Inventory.class.getDeclaredFields()) {
                    if (f.getType() != Integer.TYPE || !f.getName().equals("selected") && !f.getName().equals("selectedSlot")) continue;
                    f.setAccessible(true);
                    return f.getInt(Scaffold.mc.player.getInventory());
                }
            } catch (Exception exception) {
                // empty catch block
            }
            return 0;
        }
    }

    private void setSelectedSlot(int slot) {
        if (Scaffold.mc.player == null) {
            return;
        }
        try {
            Field field = Inventory.class.getDeclaredField("selected");
            field.setAccessible(true);
            field.setInt(Scaffold.mc.player.getInventory(), slot);
        } catch (Exception e) {
            try {
                for (Field f : Inventory.class.getDeclaredFields()) {
                    if (f.getType() != Integer.TYPE || !f.getName().equals("selected") && !f.getName().equals("selectedSlot")) continue;
                    f.setAccessible(true);
                    f.setInt(Scaffold.mc.player.getInventory(), slot);
                    return;
                }
            } catch (Exception exception) {
                // empty catch block
            }
        }
    }

    private int findBlockInHotbar() {
        if (Scaffold.mc.player == null) {
            return -1;
        }
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = Scaffold.mc.player.getInventory().getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) continue;
            return i;
        }
        return -1;
    }

    private void applyRotation(BlockData data) {
        if (((Scaffold_h)this.h).rotMode.value == Scaffold_h.RotMode.NONE) {
            this.rotating = false;
            if (RotationManager.instance != null) {
                RotationManager.instance.reset("Scaffold");
            }
            return;
        }
        float moveYaw = this.getMoveYaw();
        float tYaw = Scaffold.mc.player.getYRot();
        float tPitch = Scaffold.mc.player.getXRot();
        if (data != null) {
            Vec3 hitVec = Vec3.atCenterOf((Vec3i)data.pos()).add(this.getDirectionVector(data.face()).scale(0.5));
            double dx = hitVec.x - Scaffold.mc.player.getX();
            double dy = hitVec.y - (Scaffold.mc.player.getY() + (double)Scaffold.mc.player.getEyeHeight());
            double dz = hitVec.z - Scaffold.mc.player.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            float yaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
            float pitch = (float)(-(Math.atan2(dy, dist) * 180.0 / Math.PI));
            this.lastLookYaw = yaw;
            this.lastLookPitch = pitch;
        }
        this.rotating = true;
        switch ((Scaffold_h.RotMode)((Object)((Scaffold_h)this.h).rotMode.value)) {
            case NORMAL: {
                tYaw = moveYaw;
                tPitch = 82.0f;
                break;
            }
            case BACK: {
                tYaw = moveYaw + 180.0f;
                tPitch = 82.0f;
                break;
            }
            case DOWN: {
                if (System.currentTimeMillis() < this.lookTimer) {
                    tYaw = this.lastLookYaw;
                    tPitch = this.lastLookPitch;
                    break;
                }
                this.rotating = false;
                break;
            }
            case BACKWARDS: {
                if (System.currentTimeMillis() < this.lookTimer) {
                    tYaw = this.lastLookYaw + 180.0f;
                    tPitch = this.lastLookPitch;
                    break;
                }
                this.rotating = false;
                break;
            }
            case HIVE: {
                if (System.currentTimeMillis() - this.lastPlaceTime < 100L) {
                    tYaw = moveYaw + 180.0f;
                    tPitch = 82.0f;
                    break;
                }
                this.rotating = false;
                break;
            }
            case HYPIXEL_BACK: {
                tYaw = moveYaw + 180.0f;
                tPitch = this.lastLookPitch != 0.0f ? this.lastLookPitch : 82.0f;
                break;
            }
            case HYPIXEL_SIDEWAYS: {
                boolean isDiagonal;
                float playerYaw = Scaffold.mc.player.getYRot();
                float absMod = Math.abs(Mth.wrapDegrees((float)playerYaw) % 90.0f);
                boolean bl = isDiagonal = absMod > 22.5f && absMod < 67.5f;
                if (isDiagonal) {
                    tYaw = moveYaw + 180.0f;
                } else {
                    float backYaw = moveYaw + 180.0f;
                    float yaw1 = backYaw + 45.0f;
                    float yaw2 = backYaw - 45.0f;
                    float targetBlockYaw = this.lastLookYaw != 0.0f ? this.lastLookYaw : playerYaw;
                    float diff1 = Math.abs(Mth.wrapDegrees((float)(yaw1 - targetBlockYaw)));
                    float diff2 = Math.abs(Mth.wrapDegrees((float)(yaw2 - targetBlockYaw)));
                    tYaw = diff1 < diff2 ? yaw1 : yaw2;
                }
                float f = tPitch = this.lastLookPitch != 0.0f ? this.lastLookPitch : 82.0f;
            }
        }
        if (((Scaffold_h)this.h).fakeBack.value) {
            tYaw = moveYaw + 180.0f;
        }
        if (this.rotating) {
            this.serverYaw = Mth.wrapDegrees((float)tYaw);
            this.serverPitch = Mth.clamp((float)tPitch, -90.0f, 90.0f);
        } else if (RotationManager.instance != null) {
            RotationManager.instance.reset("Scaffold");
        }
    }

    private BlockData findIntelligentFoundation(boolean isMoving) {
        double startD;
        float moveYaw = this.getMoveYaw();
        double rad = Math.toRadians(moveYaw);
        double absExtend = Math.abs(((Scaffold_h)this.h).extend.value);
        double sign = Math.signum(((Scaffold_h)this.h).extend.value);
        if (sign == 0.0) {
            sign = 1.0;
        }
        for (double d = startD = ((Scaffold_h)this.h).extendOnly.value ? absExtend : 0.0; d <= absExtend; d += 0.1) {
            double currentD = d * sign;
            double x = Scaffold.mc.player.getX() + -Math.sin(rad) * currentD;
            double z = Scaffold.mc.player.getZ() + Math.cos(rad) * currentD;
            double y = ((Scaffold_h)this.h).lockY.value && isMoving ? this.startY - 1.0 : Scaffold.mc.player.getY() - 1.0;
            BlockPos airPos = new BlockPos(Mth.floor((double)x), Mth.floor((double)y), Mth.floor((double)z));
            if (!this.isReplaceable(airPos)) continue;
            for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN}) {
                BlockPos neighbor = airPos.relative(dir);
                if (!this.isSolid(neighbor)) continue;
                return new BlockData(neighbor, dir.getOpposite(), airPos);
            }
            BlockPos nearestSolid = null;
            double minFoundDist = Double.MAX_VALUE;
            int searchRange = (int)((Scaffold_h)this.h).diagonalRange.value;
            for (int ox = -searchRange; ox <= searchRange; ++ox) {
                for (int oy = -searchRange; oy <= searchRange; ++oy) {
                    for (int oz = -searchRange; oz <= searchRange; ++oz) {
                        double dist;
                        BlockPos checkPos = airPos.offset(ox, oy, oz);
                        if (!this.isSolid(checkPos) || !((dist = airPos.distSqr((Vec3i)checkPos)) < minFoundDist)) continue;
                        minFoundDist = dist;
                        nearestSolid = checkPos;
                    }
                }
            }
            if (nearestSolid == null) continue;
            return new BlockData(nearestSolid, this.getClosestFace(airPos, nearestSolid), airPos);
        }
        return null;
    }

    private boolean isReplaceable(BlockPos pos) {
        return Scaffold.mc.level.getBlockState(pos).canBeReplaced();
    }

    private boolean isSolid(BlockPos pos) {
        BlockState state = Scaffold.mc.level.getBlockState(pos);
        return !state.isAir() && state.canOcclude();
    }

    private Direction getClosestFace(BlockPos air, BlockPos solid) {
        if (air.getY() > solid.getY()) {
            return Direction.UP;
        }
        if (air.getY() < solid.getY()) {
            return Direction.DOWN;
        }
        if (air.getX() > solid.getX()) {
            return Direction.EAST;
        }
        if (air.getX() < solid.getX()) {
            return Direction.WEST;
        }
        if (air.getZ() > solid.getZ()) {
            return Direction.SOUTH;
        }
        if (air.getZ() < solid.getZ()) {
            return Direction.NORTH;
        }
        return Direction.UP;
    }

    private BlockPos getTargetPosOnly(double dist, boolean isMoving) {
        float yaw = this.getMoveYaw();
        double rad = Math.toRadians(yaw);
        double x = Scaffold.mc.player.getX() + -Math.sin(rad) * dist;
        double z = Scaffold.mc.player.getZ() + Math.cos(rad) * dist;
        double y = ((Scaffold_h)this.h).lockY.value && isMoving ? this.startY - 1.0 : Scaffold.mc.player.getY() - 1.0;
        return new BlockPos(Mth.floor((double)x), Mth.floor((double)y), Mth.floor((double)z));
    }

    private void handleTower() {
        Vec3 v = Scaffold.mc.player.getDeltaMovement();
        if (((Scaffold_h)this.h).towerMode.value == Scaffold_h.TowerMode.VANILLA && Scaffold.mc.player.onGround()) {
            Scaffold.mc.player.setDeltaMovement(v.x, 0.42, v.z);
        } else if (((Scaffold_h)this.h).towerMode.value == Scaffold_h.TowerMode.VELOCITY && (Scaffold.mc.player.onGround() || v.y < 0.15)) {
            Scaffold.mc.player.setDeltaMovement(v.x, 0.42, v.z);
        }
    }

    public float getMoveYaw() {
        float yaw = Scaffold.mc.player != null ? Scaffold.mc.player.getYRot() : 0.0f;
        float f = 0.0f;
        float s = 0.0f;
        if (Scaffold.mc.options.keyUp.isDown()) {
            f += 1.0f;
        }
        if (Scaffold.mc.options.keyDown.isDown()) {
            f -= 1.0f;
        }
        if (Scaffold.mc.options.keyLeft.isDown()) {
            s += 1.0f;
        }
        if (Scaffold.mc.options.keyRight.isDown()) {
            s -= 1.0f;
        }
        if (f == 0.0f && s == 0.0f) {
            return yaw;
        }
        boolean back = f < 0.0f;
        float moveYaw = yaw;
        if (f != 0.0f) {
            if (s > 0.0f) {
                moveYaw += back ? 45.0f : -45.0f;
            } else if (s < 0.0f) {
                moveYaw += back ? -45.0f : 45.0f;
            }
            if (back) {
                moveYaw += 180.0f;
            }
        } else if (s > 0.0f) {
            moveYaw -= 90.0f;
        } else if (s < 0.0f) {
            moveYaw += 90.0f;
        }
        return Mth.wrapDegrees((float)moveYaw);
    }

    public record BlockData(BlockPos pos, Direction face, BlockPos targetPos) {
    }
}

