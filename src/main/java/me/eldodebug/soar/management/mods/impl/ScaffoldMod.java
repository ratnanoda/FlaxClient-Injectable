package me.eldodebug.soar.management.mods.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventCameraRotation;
import me.eldodebug.soar.management.event.impl.EventPlayerHeadRotation;
import me.eldodebug.soar.management.event.impl.EventUpdate;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.material.Material;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * LiquidBounce legacy Normal Scaffold placement/search behavior, adapted to
 * FlaxClient's Java event system. LiquidBounce is GPL-3.0 licensed:
 * https://github.com/CCBlueX/LiquidBounce
 *
 * Extend is intentionally fixed to zero. While Scaffold is enabled, the real
 * player/server rotation continuously keeps a downward placement stance. The
 * camera stays detached and movement input is always remapped from camera yaw.
 */
public class ScaffoldMod extends Mod {

    /** User-requested fixed Extend value. No forward expansion is performed. */
    private static final int EXTEND = 0;

    private static final int HORIZONTAL_CLUTCH_BLOCKS = 3;
    private static final int VERTICAL_CLUTCH_BLOCKS = 2;
    private static final float INPUT_EPSILON = 0.0001F;
    private static final double MAX_AREA_SAMPLE = 0.9D;
    private static final double AREA_SAMPLE_STEP = 0.1D;

    /** Continuous placement stance used between real placement targets. */
    private static final float HOLD_PITCH = 80.5F;
    private static final float HOLD_YAW_STEP = 16.0F;
    private static final float HOLD_PITCH_STEP = 4.0F;
    private static final float HOLD_YAW_WOBBLE = 0.24F;
    private static final float HOLD_PITCH_WOBBLE = 0.14F;

    /** Slow, bounded movement variation. It never accelerates above vanilla. */
    private static final float MIN_NATURAL_SPEED = 0.970F;
    private static final float MAX_NATURAL_SPEED = 0.998F;
    private static final float NATURAL_SPEED_LERP = 0.18F;

    /** Same leading-edge sampling geometry used by the existing SafeWalk mod. */
    private static final double EDGE_PROBE_EPSILON = 0.001D;
    private static final double EDGE_SIDE_SAMPLE_OFFSET = 0.22D;

    private static boolean silentRotationActive;
    private static float cameraYaw;
    private static float cameraPitch;
    private static float previousCameraYaw;
    private static float previousCameraPitch;
    private static float silentYaw;
    private static float silentPitch;
    private static float movementSpeedFactor = 1.0F;
    private static boolean edgeGuardActive;

    private final Random humanRandom = new Random();

    private int originalSlot = -1;
    private boolean hasPlacementRotation;
    private float lastPlacementYaw;
    private float lastPlacementPitch;
    private float targetSpeedFactor = 1.0F;
    private int speedVariationTicks;
    private double wobblePhase;
    private boolean forcedSneak;

    public ScaffoldMod() {
        super(TranslateText.SCAFFOLD, TranslateText.SCAFFOLD_DESCRIPTION, ModCategory.BLATANT);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        originalSlot = mc.thePlayer == null ? -1 : mc.thePlayer.inventory.currentItem;
        hasPlacementRotation = false;
        targetSpeedFactor = 1.0F;
        movementSpeedFactor = 1.0F;
        speedVariationTicks = 0;
        wobblePhase = humanRandom.nextDouble() * Math.PI * 2.0D;
        forcedSneak = false;
        edgeGuardActive = false;
        deactivateSilentRotation();

        if(hasPlayerContext()) {
            activateContinuousRotation();
            updateHoldingRotation();
        }
    }

    @Override
    public void onDisable() {
        releaseForcedSneak();
        edgeGuardActive = false;
        movementSpeedFactor = 1.0F;
        deactivateSilentRotation();
        restoreOriginalSlot();
        hasPlacementRotation = false;
        super.onDisable();
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if(!hasPlayerContext()) {
            releaseForcedSneak();
            edgeGuardActive = false;
            movementSpeedFactor = 1.0F;
            deactivateSilentRotation();
            return;
        }

        // The detached camera and server-facing placement stance remain active
        // for the complete enabled lifetime, even on ticks with no block to place.
        activateContinuousRotation();

        Placement placement = null;
        int blockSlot = -1;
        if(canPlace()) {
            blockSlot = findBlockSlot();
            if(blockSlot >= 0) {
                placement = findBlock();
            }
        }

        if(placement != null) {
            lockSilentRotation(placement.yaw, placement.pitch);
            hasPlacementRotation = true;
            lastPlacementYaw = placement.yaw;
            lastPlacementPitch = placement.pitch;
        } else {
            updateHoldingRotation();
        }

        if(placement != null && blockSlot >= 0) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(blockSlot);
            if(isUsableBlock(stack)) {
                placeWithSpoofedSlot(stack, blockSlot, placement);
            }
        }

        // Re-evaluate after placement. A successful local placement fills the
        // sampled gap immediately; otherwise forced sneak prevents crossing it.
        updateEdgeGuard();
        updateNaturalMovement();
        applySilentRotationToPlayer();
    }

    private void placeWithSpoofedSlot(ItemStack stack, int blockSlot,
            Placement placement) {
        int visibleSlot = mc.thePlayer.inventory.currentItem;
        boolean spoofed = blockSlot != visibleSlot;
        if(spoofed) {
            mc.thePlayer.sendQueue.addToSendQueue(
                    new C09PacketHeldItemChange(blockSlot));
        }

        try {
            if(placeBlock(stack, placement)) {
                mc.thePlayer.swingItem();
            }
        } finally {
            if(spoofed) {
                mc.thePlayer.sendQueue.addToSendQueue(
                        new C09PacketHeldItemChange(visibleSlot));
            }
        }
    }

    /** Consume mouse deltas into the detached camera while placement rotates the player. */
    @EventTarget
    public void onPlayerHeadRotation(EventPlayerHeadRotation event) {
        if(!silentRotationActive) {
            return;
        }

        updateDetachedCamera(event.getYaw(), event.getPitch());
        event.setCancelled(true);
    }

    /** Render first/third-person camera from the detached camera rotation. */
    @EventTarget
    public void onCameraRotation(EventCameraRotation event) {
        if(!silentRotationActive) {
            return;
        }

        event.setYaw(cameraYaw);
        event.setPitch(cameraPitch);
    }

    private boolean hasPlayerContext() {
        return mc.thePlayer != null && mc.theWorld != null;
    }

    private boolean canPlace() {
        if(mc.playerController == null || mc.currentScreen != null
                || !mc.inGameHasFocus) {
            return false;
        }
        if(mc.thePlayer.isSpectator() || mc.thePlayer.capabilities.isFlying
                || mc.thePlayer.ridingEntity != null) {
            return false;
        }
        return !mc.thePlayer.isInWater() && !mc.thePlayer.isInLava()
                && !mc.thePlayer.isOnLadder();
    }

    /**
     * LiquidBounce Normal Scaffold's findBlock flow with Expand disabled.
     * The current block is searched first, then a 3x2 clutch area ordered by
     * distance from the player's eyes.
     */
    private Placement findBlock() {
        BlockPos blockPosition = getBlockPositionBelowPlayer();

        if(!isReplaceable(blockPosition)) {
            return null;
        }

        Placement direct = search(blockPosition, true, true);
        if(direct != null) {
            return direct;
        }

        // EXTEND is deliberately zero, therefore no direction offset is added.
        if(EXTEND != 0) {
            return null;
        }

        List<BlockPos> positions = new ArrayList<BlockPos>();
        for(int x = -HORIZONTAL_CLUTCH_BLOCKS; x <= HORIZONTAL_CLUTCH_BLOCKS; x++) {
            for(int y = 0; y >= -VERTICAL_CLUTCH_BLOCKS; y--) {
                for(int z = -HORIZONTAL_CLUTCH_BLOCKS; z <= HORIZONTAL_CLUTCH_BLOCKS; z++) {
                    positions.add(blockPosition.add(x, y, z));
                }
            }
        }

        final Vec3 eyes = getEyes();
        Collections.sort(positions, new Comparator<BlockPos>() {
            @Override
            public int compare(BlockPos first, BlockPos second) {
                return Double.compare(distanceSqToCenter(eyes, first),
                        distanceSqToCenter(eyes, second));
            }
        });

        for(BlockPos candidate : positions) {
            // Mirrors LiquidBounce's `canBeClicked() || search(...)` early exit.
            if(isValidSupport(candidate)) {
                return null;
            }

            Placement placement = search(candidate, true, true);
            if(placement != null) {
                return placement;
            }
        }

        return null;
    }

    private BlockPos getBlockPositionBelowPlayer() {
        double roundedY = Math.rint(mc.thePlayer.posY);
        if(mc.thePlayer.posY == roundedY + 0.5D) {
            return new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY,
                    mc.thePlayer.posZ);
        }
        return new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY,
                mc.thePlayer.posZ).down();
    }

    /**
     * Port of LiquidBounce's Area search. Every face point from 0.1 through
     * 0.9 is tested and only a raytrace-valid candidate is accepted. The
     * candidate with the smallest rotation difference is selected.
     */
    private Placement search(BlockPos blockPosition, boolean raycast, boolean area) {
        if(!isReplaceable(blockPosition)) {
            return null;
        }

        Vec3 eyes = getEyes();
        float maxReach = mc.playerController.getBlockReachDistance();
        Placement best = null;

        for(EnumFacing side : EnumFacing.values()) {
            BlockPos neighbor = blockPosition.offset(side);
            if(!isValidSupport(neighbor)) {
                continue;
            }

            if(!area) {
                Placement candidate = findTargetPlace(blockPosition, neighbor,
                        0.5D, 0.5D, 0.5D, side, eyes, maxReach, raycast);
                best = compareDifferences(candidate, best);
                continue;
            }

            for(int xi = 1; xi <= 9; xi++) {
                double x = Math.min(MAX_AREA_SAMPLE, xi * AREA_SAMPLE_STEP);
                for(int yi = 1; yi <= 9; yi++) {
                    double y = Math.min(MAX_AREA_SAMPLE, yi * AREA_SAMPLE_STEP);
                    for(int zi = 1; zi <= 9; zi++) {
                        double z = Math.min(MAX_AREA_SAMPLE, zi * AREA_SAMPLE_STEP);
                        Placement candidate = findTargetPlace(blockPosition,
                                neighbor, x, y, z, side, eyes, maxReach, raycast);
                        best = compareDifferences(candidate, best);
                    }
                }
            }
        }

        return best;
    }

    private Placement findTargetPlace(BlockPos target, BlockPos support,
            double x, double y, double z, EnumFacing side, Vec3 eyes,
            float maxReach, boolean raycast) {
        Vec3 hitCandidate = new Vec3(
                target.getX() + x + side.getFrontOffsetX() * x,
                target.getY() + y + side.getFrontOffsetY() * y,
                target.getZ() + z + side.getFrontOffsetZ() * z);

        if(eyes.distanceTo(hitCandidate) > maxReach) {
            return null;
        }

        if(raycast && mc.theWorld.rayTraceBlocks(
                eyes, hitCandidate, false, true, false) != null) {
            return null;
        }

        float[] rotation = fixedSensitivity(rotationsTo(hitCandidate));
        MovingObjectPosition trace = performBlockRaytrace(
                rotation[0], rotation[1], maxReach);
        if(trace == null || trace.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
                || !support.equals(trace.getBlockPos())) {
            return null;
        }

        EnumFacing expectedFace = side.getOpposite();
        if(raycast && trace.sideHit != expectedFace) {
            return null;
        }

        return new Placement(target, support, expectedFace, trace.hitVec,
                rotation[0], rotation[1], rotationDifference(rotation[0], rotation[1]));
    }

    private Placement compareDifferences(Placement candidate, Placement current) {
        if(candidate == null) {
            return current;
        }
        if(current == null || candidate.rotationDifference < current.rotationDifference) {
            return candidate;
        }
        return current;
    }

    private MovingObjectPosition performBlockRaytrace(float yaw, float pitch,
            float maxReach) {
        Vec3 eyes = getEyes();
        Vec3 look = getVectorForRotation(yaw, pitch);
        Vec3 reach = eyes.addVector(look.xCoord * maxReach,
                look.yCoord * maxReach, look.zCoord * maxReach);
        return mc.theWorld.rayTraceBlocks(eyes, reach, false, false, true);
    }

    private boolean placeBlock(ItemStack stack, Placement placement) {
        ItemBlock itemBlock = (ItemBlock) stack.getItem();
        if(!itemBlock.canPlaceBlockOnSide(mc.theWorld, placement.support,
                placement.face, mc.thePlayer, stack)) {
            return false;
        }

        applySilentRotationToPlayer();
        mc.thePlayer.sendQueue.addToSendQueue(
                new C03PacketPlayer.C05PacketPlayerLook(
                        silentYaw, silentPitch, mc.thePlayer.onGround));

        return mc.playerController.onPlayerRightClick(
                mc.thePlayer,
                mc.theWorld,
                stack,
                placement.support,
                placement.face,
                placement.hitVec);
    }

    private Vec3 getEyes() {
        return new Vec3(mc.thePlayer.posX,
                mc.thePlayer.getEntityBoundingBox().minY
                        + mc.thePlayer.getEyeHeight(),
                mc.thePlayer.posZ);
    }

    private double distanceSqToCenter(Vec3 eyes, BlockPos pos) {
        double dx = eyes.xCoord - (pos.getX() + 0.5D);
        double dy = eyes.yCoord - (pos.getY() + 0.5D);
        double dz = eyes.zCoord - (pos.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz;
    }

    private boolean isValidSupport(BlockPos pos) {
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        Material material = block.getMaterial();
        return block != Blocks.air && material != null
                && material.isSolid() && !material.isReplaceable();
    }

    private boolean isReplaceable(BlockPos pos) {
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        Material material = block.getMaterial();
        return block == Blocks.air || (material != null && material.isReplaceable());
    }

    private int findBlockSlot() {
        int currentSlot = mc.thePlayer.inventory.currentItem;
        ItemStack currentStack = mc.thePlayer.inventory.getStackInSlot(currentSlot);
        if(isUsableBlock(currentStack)) {
            return currentSlot;
        }

        int bestSlot = -1;
        int bestCount = 0;
        for(int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            if(isUsableBlock(stack) && stack.stackSize > bestCount) {
                bestSlot = slot;
                bestCount = stack.stackSize;
            }
        }
        return bestSlot;
    }

    private boolean isUsableBlock(ItemStack stack) {
        if(stack == null || stack.stackSize <= 0
                || !(stack.getItem() instanceof ItemBlock)) {
            return false;
        }

        Block block = ((ItemBlock) stack.getItem()).getBlock();
        if(block == null || block == Blocks.air || block instanceof BlockBush) {
            return false;
        }

        Material material = block.getMaterial();
        return material != null && material.isSolid() && block.isFullCube();
    }

    private void restoreOriginalSlot() {
        if(mc.thePlayer == null || mc.playerController == null
                || originalSlot < 0 || originalSlot > 8) {
            originalSlot = -1;
            return;
        }
        if(mc.thePlayer.inventory.currentItem != originalSlot) {
            mc.thePlayer.inventory.currentItem = originalSlot;
            mc.playerController.updateController();
        }
        originalSlot = -1;
    }

    private void activateContinuousRotation() {
        if(silentRotationActive || mc.thePlayer == null) {
            return;
        }

        cameraYaw = mc.thePlayer.rotationYaw;
        cameraPitch = mc.thePlayer.rotationPitch;
        previousCameraYaw = mc.thePlayer.prevRotationYaw;
        previousCameraPitch = mc.thePlayer.prevRotationPitch;

        double[] direction = getCameraInputDirection();
        float baseYaw = direction == null
                ? cameraYaw + 180.0F : yawFromVector(direction[0], direction[1]) + 180.0F;
        float[] fixed = fixedSensitivityFromReference(baseYaw, HOLD_PITCH,
                cameraYaw, cameraPitch);
        silentYaw = fixed[0];
        silentPitch = fixed[1];
        silentRotationActive = true;
        applySilentRotationToPlayer();
    }

    private void updateHoldingRotation() {
        if(!silentRotationActive) {
            activateContinuousRotation();
        }
        if(!silentRotationActive) {
            return;
        }

        double[] direction = getCameraInputDirection();
        float targetYaw;
        if(direction != null) {
            targetYaw = yawFromVector(direction[0], direction[1]) + 180.0F;
        } else if(hasPlacementRotation) {
            targetYaw = lastPlacementYaw;
        } else {
            targetYaw = cameraYaw + 180.0F;
        }

        float targetPitch = hasPlacementRotation
                ? clamp(lastPlacementPitch, 77.0F, 84.5F) : HOLD_PITCH;

        // Sub-degree low-frequency variation gives the held stance a small
        // human-looking imperfection without changing actual placement rays.
        wobblePhase += 0.115D;
        targetYaw += (float) Math.sin(wobblePhase) * HOLD_YAW_WOBBLE;
        targetPitch += (float) Math.sin(wobblePhase * 0.73D) * HOLD_PITCH_WOBBLE;

        float nextYaw = approachAngle(silentYaw, targetYaw, HOLD_YAW_STEP);
        float nextPitch = approach(silentPitch, targetPitch, HOLD_PITCH_STEP);
        float[] fixed = fixedSensitivity(new float[] { nextYaw, nextPitch });
        lockSilentRotation(fixed[0], fixed[1]);
    }

    private void lockSilentRotation(float yaw, float pitch) {
        if(!silentRotationActive) {
            activateContinuousRotation();
        }
        if(!silentRotationActive) {
            return;
        }

        silentYaw += MathHelper.wrapAngleTo180_float(yaw - silentYaw);
        silentPitch = clamp(pitch, -90.0F, 90.0F);
        applySilentRotationToPlayer();
    }

    private float[] rotationsTo(Vec3 hitVec) {
        double deltaX = hitVec.xCoord - mc.thePlayer.posX;
        double deltaY = hitVec.yCoord
                - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double deltaZ = hitVec.zCoord - mc.thePlayer.posZ;
        double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        float yaw = (float) (Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0D);
        float pitch = (float) -Math.toDegrees(Math.atan2(deltaY, horizontal));
        return new float[] { yaw, clamp(pitch, -90.0F, 90.0F) };
    }

    private float[] fixedSensitivity(float[] rotation) {
        float referenceYaw = silentRotationActive ? silentYaw : mc.thePlayer.rotationYaw;
        float referencePitch = silentRotationActive ? silentPitch : mc.thePlayer.rotationPitch;
        return fixedSensitivityFromReference(rotation[0], rotation[1],
                referenceYaw, referencePitch);
    }

    private float[] fixedSensitivityFromReference(float yaw, float pitch,
            float referenceYaw, float referencePitch) {
        float sensitivity = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float gcd = sensitivity * sensitivity * sensitivity * 1.2F;

        float yawDelta = MathHelper.wrapAngleTo180_float(yaw - referenceYaw);
        float pitchDelta = pitch - referencePitch;
        float fixedYaw = referenceYaw + Math.round(yawDelta / gcd) * gcd;
        float fixedPitch = referencePitch + Math.round(pitchDelta / gcd) * gcd;
        return new float[] { fixedYaw, clamp(fixedPitch, -90.0F, 90.0F) };
    }

    private double rotationDifference(float yaw, float pitch) {
        float referenceYaw = silentRotationActive ? silentYaw : mc.thePlayer.rotationYaw;
        float referencePitch = silentRotationActive ? silentPitch : mc.thePlayer.rotationPitch;
        double yawDifference = MathHelper.wrapAngleTo180_float(yaw - referenceYaw);
        double pitchDifference = pitch - referencePitch;
        return Math.sqrt(yawDifference * yawDifference
                + pitchDifference * pitchDifference);
    }

    private static Vec3 getVectorForRotation(float yaw, float pitch) {
        float yawCos = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float yawSin = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float pitchCos = -MathHelper.cos(-pitch * 0.017453292F);
        float pitchSin = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3(yawSin * pitchCos, pitchSin, yawCos * pitchCos);
    }

    private static void updateDetachedCamera(float yawDelta, float pitchDelta) {
        float oldYaw = cameraYaw;
        float oldPitch = cameraPitch;
        cameraYaw += yawDelta * 0.15F;
        cameraPitch -= pitchDelta * 0.15F;
        cameraPitch = clamp(cameraPitch, -90.0F, 90.0F);
        previousCameraYaw += cameraYaw - oldYaw;
        previousCameraPitch += cameraPitch - oldPitch;
    }

    private void updateNaturalMovement() {
        boolean canVary = canPlace() && mc.thePlayer.onGround
                && getCameraInputDirection() != null && !edgeGuardActive;

        if(!canVary) {
            targetSpeedFactor = 1.0F;
            movementSpeedFactor += (1.0F - movementSpeedFactor) * 0.35F;
            if(Math.abs(1.0F - movementSpeedFactor) < 0.001F) {
                movementSpeedFactor = 1.0F;
            }
            speedVariationTicks = 0;
            return;
        }

        if(speedVariationTicks <= 0) {
            targetSpeedFactor = MIN_NATURAL_SPEED
                    + humanRandom.nextFloat() * (MAX_NATURAL_SPEED - MIN_NATURAL_SPEED);
            speedVariationTicks = 6 + humanRandom.nextInt(7);
        } else {
            speedVariationTicks--;
        }

        movementSpeedFactor += (targetSpeedFactor - movementSpeedFactor)
                * NATURAL_SPEED_LERP;
        movementSpeedFactor = clamp(movementSpeedFactor,
                MIN_NATURAL_SPEED, 1.0F);
    }

    private void updateEdgeGuard() {
        boolean shouldGuard = canPlace() && isEdgeInCameraDirection();
        edgeGuardActive = shouldGuard;
        if(shouldGuard) {
            forceSneak();
        } else {
            releaseForcedSneak();
        }
    }

    private boolean isEdgeInCameraDirection() {
        double[] direction = getCameraInputDirection();
        if(direction == null) {
            return false;
        }

        AxisAlignedBB bb = mc.thePlayer.getEntityBoundingBox();
        double centerX = (bb.minX + bb.maxX) * 0.5D;
        double centerZ = (bb.minZ + bb.maxZ) * 0.5D;
        double halfWidthX = (bb.maxX - bb.minX) * 0.5D;
        double halfWidthZ = (bb.maxZ - bb.minZ) * 0.5D;

        double edgeDistanceX = Math.abs(direction[0]) < INPUT_EPSILON
                ? Double.POSITIVE_INFINITY : halfWidthX / Math.abs(direction[0]);
        double edgeDistanceZ = Math.abs(direction[1]) < INPUT_EPSILON
                ? Double.POSITIVE_INFINITY : halfWidthZ / Math.abs(direction[1]);
        double edgeDistance = Math.min(edgeDistanceX, edgeDistanceZ)
                + EDGE_PROBE_EPSILON;

        double leadingX = centerX + direction[0] * edgeDistance;
        double leadingZ = centerZ + direction[1] * edgeDistance;
        double sideX = -direction[1];
        double sideZ = direction[0];
        double y = bb.minY - 0.01D;

        boolean centerGap = isGap(leadingX, y, leadingZ);
        boolean leftGap = isGap(leadingX + sideX * EDGE_SIDE_SAMPLE_OFFSET,
                y, leadingZ + sideZ * EDGE_SIDE_SAMPLE_OFFSET);
        boolean rightGap = isGap(leadingX - sideX * EDGE_SIDE_SAMPLE_OFFSET,
                y, leadingZ - sideZ * EDGE_SIDE_SAMPLE_OFFSET);
        return centerGap && leftGap && rightGap;
    }

    private boolean isGap(double x, double y, double z) {
        Block block = mc.theWorld.getBlockState(new BlockPos(x, y, z)).getBlock();
        Material material = block.getMaterial();
        return block == Blocks.air || material == null || material.isReplaceable();
    }

    private double[] getCameraInputDirection() {
        if(mc.thePlayer == null || mc.thePlayer.movementInput == null) {
            return null;
        }

        float strafe = sign(mc.thePlayer.movementInput.moveStrafe);
        float forward = sign(mc.thePlayer.movementInput.moveForward);
        if(strafe == 0.0F && forward == 0.0F) {
            return null;
        }
        return movementVector(cameraYaw, strafe, forward);
    }

    private void forceSneak() {
        if(isPhysicalSneakPressed()) {
            forcedSneak = false;
            return;
        }

        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
        forcedSneak = true;
    }

    private void releaseForcedSneak() {
        if(!forcedSneak) {
            return;
        }
        if(mc.gameSettings != null && !isPhysicalSneakPressed()) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
        }
        forcedSneak = false;
    }

    private boolean isPhysicalSneakPressed() {
        if(mc.gameSettings == null) {
            return false;
        }
        int keyCode = mc.gameSettings.keyBindSneak.getKeyCode();
        return keyCode < 0
                ? Mouse.isButtonDown(keyCode + 100) : Keyboard.isKeyDown(keyCode);
    }

    private static void deactivateSilentRotation() {
        if(silentRotationActive && mc.thePlayer != null) {
            mc.thePlayer.rotationYaw = cameraYaw;
            mc.thePlayer.rotationPitch = cameraPitch;
            mc.thePlayer.prevRotationYaw = previousCameraYaw;
            mc.thePlayer.prevRotationPitch = previousCameraPitch;
            mc.thePlayer.rotationYawHead = cameraYaw;
            mc.thePlayer.prevRotationYawHead = previousCameraYaw;
            mc.thePlayer.renderYawOffset = cameraYaw;
            mc.thePlayer.prevRenderYawOffset = previousCameraYaw;
        }
        silentRotationActive = false;
    }

    public static void applySilentRotationToPlayer() {
        if(!silentRotationActive || mc.thePlayer == null) {
            return;
        }

        mc.thePlayer.rotationYaw = silentYaw;
        mc.thePlayer.rotationPitch = silentPitch;
        mc.thePlayer.prevRotationYaw = silentYaw;
        mc.thePlayer.prevRotationPitch = silentPitch;
        mc.thePlayer.rotationYawHead = silentYaw;
        mc.thePlayer.prevRotationYawHead = silentYaw;
        mc.thePlayer.renderYawOffset = silentYaw;
        mc.thePlayer.prevRenderYawOffset = silentYaw;
    }

    public static boolean isSilentRotationActive() {
        return silentRotationActive;
    }

    /** Camera-relative movement is mandatory while the detached camera is active. */
    public static boolean shouldApplyMoveFix() {
        return silentRotationActive;
    }

    public static float getSilentYaw() {
        return silentYaw;
    }

    public static float getSilentPitch() {
        return silentPitch;
    }

    public static Vec3 getCameraLook(float partialTicks) {
        float yaw = previousCameraYaw
                + (cameraYaw - previousCameraYaw) * partialTicks;
        float pitch = previousCameraPitch
                + (cameraPitch - previousCameraPitch) * partialTicks;
        return getVectorForRotation(yaw, pitch);
    }

    /**
     * Preserve camera-relative intent while movement physics use the real
     * placement yaw. The result is restricted to vanilla's eight directions.
     * Natural speed variation only slows input by up to three percent and is
     * disabled at edges, in air, and in every non-standard movement state.
     */
    public static float[] getMoveFixedInput(float strafe, float forward) {
        float magnitude = Math.max(Math.abs(strafe), Math.abs(forward));
        if(magnitude < INPUT_EPSILON) {
            return new float[] { 0.0F, 0.0F };
        }

        float inputStrafe = sign(strafe);
        float inputForward = sign(forward);
        double[] desired = movementVector(cameraYaw, inputStrafe, inputForward);

        int bestStrafe = 0;
        int bestForward = 0;
        double bestDot = -Double.MAX_VALUE;

        for(int candidateForward = -1; candidateForward <= 1; candidateForward++) {
            for(int candidateStrafe = -1; candidateStrafe <= 1; candidateStrafe++) {
                if(candidateForward == 0 && candidateStrafe == 0) {
                    continue;
                }

                double[] candidate = movementVector(
                        silentYaw, candidateStrafe, candidateForward);
                double dot = desired[0] * candidate[0]
                        + desired[1] * candidate[1];
                if(dot > bestDot) {
                    bestDot = dot;
                    bestStrafe = candidateStrafe;
                    bestForward = candidateForward;
                }
            }
        }

        float speedFactor = edgeGuardActive ? 1.0F : movementSpeedFactor;
        return new float[] {
                bestStrafe * magnitude * speedFactor,
                bestForward * magnitude * speedFactor
        };
    }

    private static double[] movementVector(float yaw, float strafe, float forward) {
        double radians = Math.toRadians(yaw);
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        double x = strafe * cos - forward * sin;
        double z = forward * cos + strafe * sin;
        double length = Math.sqrt(x * x + z * z);
        if(length > 0.0D) {
            x /= length;
            z /= length;
        }
        return new double[] { x, z };
    }

    private static float yawFromVector(double x, double z) {
        return (float) (Math.toDegrees(Math.atan2(z, x)) - 90.0D);
    }

    private static float approachAngle(float current, float target, float maxStep) {
        float difference = MathHelper.wrapAngleTo180_float(target - current);
        return current + clamp(difference, -maxStep, maxStep);
    }

    private static float approach(float current, float target, float maxStep) {
        return current + clamp(target - current, -maxStep, maxStep);
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static float sign(float value) {
        return value > INPUT_EPSILON ? 1.0F
                : value < -INPUT_EPSILON ? -1.0F : 0.0F;
    }

    private static final class Placement {
        private final BlockPos target;
        private final BlockPos support;
        private final EnumFacing face;
        private final Vec3 hitVec;
        private final float yaw;
        private final float pitch;
        private final double rotationDifference;

        private Placement(BlockPos target, BlockPos support, EnumFacing face,
                Vec3 hitVec, float yaw, float pitch, double rotationDifference) {
            this.target = target;
            this.support = support;
            this.face = face;
            this.hitVec = hitVec;
            this.yaw = yaw;
            this.pitch = pitch;
            this.rotationDifference = rotationDifference;
        }
    }
}
