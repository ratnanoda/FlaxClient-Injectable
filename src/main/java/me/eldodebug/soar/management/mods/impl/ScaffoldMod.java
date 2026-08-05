package me.eldodebug.soar.management.mods.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventCameraRotation;
import me.eldodebug.soar.management.event.impl.EventPlayerHeadRotation;
import me.eldodebug.soar.management.event.impl.EventUpdate;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

/**
 * Places solid hotbar blocks below and ahead of the player. The real player
 * rotation is kept on the server-facing placement angle while only the camera
 * and camera-relative movement are detached from it.
 */
public class ScaffoldMod extends Mod {

    private static final EnumFacing[] SUPPORT_DIRECTIONS = {
            EnumFacing.DOWN,
            EnumFacing.NORTH,
            EnumFacing.SOUTH,
            EnumFacing.WEST,
            EnumFacing.EAST
    };

    private static final float INPUT_EPSILON = 0.0001F;
    private static final int MAX_PLACEMENTS_PER_TICK = 2;
    private static final int MAX_PLACEMENT_ATTEMPTS = 8;
    private static final int ROTATION_HOLD_TICKS = 2;
    private static final double MIN_LOOK_AHEAD = 0.62D;
    private static final double MAX_LOOK_AHEAD = 1.45D;

    private static boolean silentRotationActive;
    private static float cameraYaw;
    private static float cameraPitch;
    private static float previousCameraYaw;
    private static float previousCameraPitch;
    private static float silentYaw;
    private static float silentPitch;

    private int originalSlot = -1;
    private int rotationHoldTicks;

    public ScaffoldMod() {
        super(TranslateText.SCAFFOLD, TranslateText.SCAFFOLD_DESCRIPTION, ModCategory.BLATANT);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        originalSlot = mc.thePlayer == null ? -1 : mc.thePlayer.inventory.currentItem;
        rotationHoldTicks = 0;
        deactivateSilentRotation();
    }

    @Override
    public void onDisable() {
        deactivateSilentRotation();
        restoreOriginalSlot();
        rotationHoldTicks = 0;
        super.onDisable();
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if(!canRun()) {
            deactivateSilentRotation();
            return;
        }

        int slot = findBlockSlot();
        if(slot < 0) {
            deactivateSilentRotation();
            return;
        }

        List<BlockPos> targets = getTargetPositions();
        Placement firstPlacement = findFirstPlacement(targets);
        if(firstPlacement == null) {
            holdOrReleaseRotation();
            return;
        }

        selectSlot(slot);

        int successfulPlacements = 0;
        int attempts = 0;
        while(successfulPlacements < MAX_PLACEMENTS_PER_TICK
                && attempts < MAX_PLACEMENT_ATTEMPTS) {
            Placement placement = findFirstPlacement(targets);
            if(placement == null) {
                break;
            }

            targets.remove(placement.target);
            attempts++;

            lockSilentRotation(placement.hitVec);
            rotationHoldTicks = ROTATION_HOLD_TICKS;

            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            if(!isUsableBlock(stack)) {
                deactivateSilentRotation();
                break;
            }

            if(placeBlock(stack, placement)) {
                mc.thePlayer.swingItem();
                successfulPlacements++;
            }
        }

        applySilentRotationToPlayer();
    }

    /**
     * Minecraft's renderer exposes raw mouse deltas through this event before
     * it calls EntityPlayerSP#setAngles. While Scaffold rotates the real
     * player, consume those deltas into a detached camera instead.
     */
    @EventTarget
    public void onPlayerHeadRotation(EventPlayerHeadRotation event) {
        if(!silentRotationActive) {
            return;
        }

        updateDetachedCamera(event.getYaw(), event.getPitch());
        event.setCancelled(true);
    }

    /** Keeps the rendered first/third-person camera on the detached angle. */
    @EventTarget
    public void onCameraRotation(EventCameraRotation event) {
        if(!silentRotationActive) {
            return;
        }

        event.setYaw(cameraYaw);
        event.setPitch(cameraPitch);
    }

    private boolean canRun() {
        if(mc.thePlayer == null || mc.theWorld == null || mc.playerController == null) {
            return false;
        }
        if(mc.currentScreen != null || !mc.inGameHasFocus) {
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
     * Produces an ordered set of blocks underneath the current footprint and
     * ahead of the requested camera-relative movement. For diagonal movement,
     * an attachable axis block is tried before the diagonal corner so the
     * bridge never relies on an impossible corner-only connection.
     */
    private List<BlockPos> getTargetPositions() {
        Set<BlockPos> targets = new LinkedHashSet<BlockPos>();
        AxisAlignedBB bounds = mc.thePlayer.getEntityBoundingBox();
        int targetY = MathHelper.floor_double(bounds.minY - 1.0D);
        BlockPos center = new BlockPos(
                MathHelper.floor_double(mc.thePlayer.posX),
                targetY,
                MathHelper.floor_double(mc.thePlayer.posZ));

        targets.add(center);

        double[] direction = getRequestedMovementDirection();
        if(Math.abs(direction[0]) > INPUT_EPSILON
                || Math.abs(direction[1]) > INPUT_EPSILON) {
            int stepX = direction[0] > INPUT_EPSILON ? 1
                    : direction[0] < -INPUT_EPSILON ? -1 : 0;
            int stepZ = direction[1] > INPUT_EPSILON ? 1
                    : direction[1] < -INPUT_EPSILON ? -1 : 0;

            BlockPos axisX = center.add(stepX, 0, 0);
            BlockPos axisZ = center.add(0, 0, stepZ);
            BlockPos diagonal = center.add(stepX, 0, stepZ);

            if(stepX != 0 && stepZ != 0) {
                if(preferXFirst(direction[0], direction[1])) {
                    targets.add(axisX);
                    targets.add(diagonal);
                    targets.add(axisZ);
                } else {
                    targets.add(axisZ);
                    targets.add(diagonal);
                    targets.add(axisX);
                }
            } else if(stepX != 0) {
                targets.add(axisX);
            } else if(stepZ != 0) {
                targets.add(axisZ);
            }

            double speed = Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX
                    + mc.thePlayer.motionZ * mc.thePlayer.motionZ);
            double lookAhead = Math.max(MIN_LOOK_AHEAD,
                    Math.min(MAX_LOOK_AHEAD, 0.62D + speed * 4.0D));
            BlockPos lead = new BlockPos(
                    mc.thePlayer.posX + direction[0] * lookAhead,
                    targetY,
                    mc.thePlayer.posZ + direction[1] * lookAhead);

            if(lead.getX() != center.getX()) {
                targets.add(new BlockPos(lead.getX(), targetY, center.getZ()));
            }
            if(lead.getZ() != center.getZ()) {
                targets.add(new BlockPos(center.getX(), targetY, lead.getZ()));
            }
            targets.add(lead);

            double edgeDistance = mc.thePlayer.width * 0.5D + 0.28D;
            targets.add(new BlockPos(
                    mc.thePlayer.posX + direction[0] * edgeDistance,
                    targetY,
                    mc.thePlayer.posZ + direction[1] * edgeDistance));
        }

        // Footprint corners are fallbacks for edge cases caused by bounding-box
        // overlap, knockback, or very small position changes between ticks.
        targets.add(new BlockPos(bounds.minX + 0.01D, targetY, bounds.minZ + 0.01D));
        targets.add(new BlockPos(bounds.minX + 0.01D, targetY, bounds.maxZ - 0.01D));
        targets.add(new BlockPos(bounds.maxX - 0.01D, targetY, bounds.minZ + 0.01D));
        targets.add(new BlockPos(bounds.maxX - 0.01D, targetY, bounds.maxZ - 0.01D));

        return new ArrayList<BlockPos>(targets);
    }

    private boolean preferXFirst(double directionX, double directionZ) {
        double absoluteX = Math.abs(directionX);
        double absoluteZ = Math.abs(directionZ);
        if(Math.abs(absoluteX - absoluteZ) > 0.0001D) {
            return absoluteX > absoluteZ;
        }

        double frontX = mc.thePlayer.posX
                + Math.signum(directionX) * mc.thePlayer.width * 0.5D;
        double frontZ = mc.thePlayer.posZ
                + Math.signum(directionZ) * mc.thePlayer.width * 0.5D;
        double fractionX = frontX - Math.floor(frontX);
        double fractionZ = frontZ - Math.floor(frontZ);
        double distanceX = directionX > 0.0D ? 1.0D - fractionX : fractionX;
        double distanceZ = directionZ > 0.0D ? 1.0D - fractionZ : fractionZ;
        return distanceX / Math.max(absoluteX, 0.0001D)
                <= distanceZ / Math.max(absoluteZ, 0.0001D);
    }

    private double[] getRequestedMovementDirection() {
        float viewYaw = silentRotationActive ? cameraYaw : mc.thePlayer.rotationYaw;
        float strafe = mc.thePlayer.movementInput == null
                ? 0.0F : mc.thePlayer.movementInput.moveStrafe;
        float forward = mc.thePlayer.movementInput == null
                ? 0.0F : mc.thePlayer.movementInput.moveForward;

        if(Math.abs(strafe) > INPUT_EPSILON || Math.abs(forward) > INPUT_EPSILON) {
            return movementVector(viewYaw, sign(strafe), sign(forward));
        }

        double speed = Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX
                + mc.thePlayer.motionZ * mc.thePlayer.motionZ);
        if(speed > 0.01D) {
            return new double[] {
                    mc.thePlayer.motionX / speed,
                    mc.thePlayer.motionZ / speed
            };
        }

        return new double[] { 0.0D, 0.0D };
    }

    private Placement findFirstPlacement(List<BlockPos> targets) {
        for(BlockPos target : targets) {
            if(!isReplaceable(target)) {
                continue;
            }

            Placement placement = findPlacement(target);
            if(placement != null) {
                return placement;
            }
        }
        return null;
    }

    private Placement findPlacement(BlockPos target) {
        for(EnumFacing direction : SUPPORT_DIRECTIONS) {
            BlockPos support = target.offset(direction);
            if(!isValidSupport(support)) {
                continue;
            }

            EnumFacing face = direction.getOpposite();
            Vec3 hitVec = new Vec3(
                    support.getX() + 0.5D + face.getFrontOffsetX() * 0.5D,
                    support.getY() + 0.5D + face.getFrontOffsetY() * 0.5D,
                    support.getZ() + 0.5D + face.getFrontOffsetZ() * 0.5D);
            return new Placement(target, support, face, hitVec);
        }
        return null;
    }

    private boolean placeBlock(ItemStack stack, Placement placement) {
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
        if(stack == null || stack.stackSize <= 0 || !(stack.getItem() instanceof ItemBlock)) {
            return false;
        }

        Block block = ((ItemBlock) stack.getItem()).getBlock();
        if(block == null || block == Blocks.air) {
            return false;
        }

        Material material = block.getMaterial();
        return material != null && material.isSolid() && block.isFullCube();
    }

    private void selectSlot(int slot) {
        if(mc.thePlayer.inventory.currentItem == slot) {
            return;
        }
        mc.thePlayer.inventory.currentItem = slot;
        mc.playerController.updateController();
    }

    private void restoreOriginalSlot() {
        if(mc.thePlayer == null || mc.playerController == null
                || originalSlot < 0 || originalSlot > 8) {
            originalSlot = -1;
            return;
        }
        selectSlot(originalSlot);
        originalSlot = -1;
    }

    private void lockSilentRotation(Vec3 hitVec) {
        if(!silentRotationActive) {
            cameraYaw = mc.thePlayer.rotationYaw;
            cameraPitch = mc.thePlayer.rotationPitch;
            previousCameraYaw = mc.thePlayer.prevRotationYaw;
            previousCameraPitch = mc.thePlayer.prevRotationPitch;
        }

        float[] rotations = rotationsTo(hitVec);
        float referenceYaw = silentRotationActive ? silentYaw : cameraYaw;
        silentYaw = referenceYaw
                + MathHelper.wrapAngleTo180_float(rotations[0] - referenceYaw);
        silentPitch = rotations[1];
        silentRotationActive = true;
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
        pitch = Math.max(-90.0F, Math.min(90.0F, pitch));
        return new float[] { yaw, pitch };
    }

    private static void updateDetachedCamera(float yawDelta, float pitchDelta) {
        float oldYaw = cameraYaw;
        float oldPitch = cameraPitch;
        cameraYaw += yawDelta * 0.15F;
        cameraPitch -= pitchDelta * 0.15F;
        cameraPitch = Math.max(-90.0F, Math.min(90.0F, cameraPitch));
        previousCameraYaw += cameraYaw - oldYaw;
        previousCameraPitch += cameraPitch - oldPitch;
    }

    private void holdOrReleaseRotation() {
        if(!silentRotationActive) {
            return;
        }

        if(rotationHoldTicks > 0) {
            rotationHoldTicks--;
            applySilentRotationToPlayer();
        } else {
            deactivateSilentRotation();
        }
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

    public static boolean shouldApplyMoveFix() {
        return silentRotationActive && SettingsMod.isMoveFixEnabled();
    }

    public static float getSilentYaw() {
        return silentYaw;
    }

    public static float getSilentPitch() {
        return silentPitch;
    }

    public static Vec3 getCameraLook(float partialTicks) {
        float yaw = previousCameraYaw + (cameraYaw - previousCameraYaw) * partialTicks;
        float pitch = previousCameraPitch
                + (cameraPitch - previousCameraPitch) * partialTicks;
        float yawCos = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float yawSin = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float pitchCos = -MathHelper.cos(-pitch * 0.017453292F);
        float pitchSin = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3(yawSin * pitchCos, pitchSin, yawCos * pitchCos);
    }

    /**
     * Converts camera-relative intent to exactly one of vanilla's eight
     * digital forward/strafe combinations under the real silent player yaw.
     * The returned array is {strafe, forward}.
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

        return new float[] {
                bestStrafe * magnitude,
                bestForward * magnitude
        };
    }

    private static float sign(float value) {
        if(value > INPUT_EPSILON) {
            return 1.0F;
        }
        if(value < -INPUT_EPSILON) {
            return -1.0F;
        }
        return 0.0F;
    }

    private static double[] movementVector(float yaw, float strafe, float forward) {
        double radians = Math.toRadians(yaw);
        double x = strafe * Math.cos(radians) - forward * Math.sin(radians);
        double z = forward * Math.cos(radians) + strafe * Math.sin(radians);
        double length = Math.sqrt(x * x + z * z);
        if(length < INPUT_EPSILON) {
            return new double[] { 0.0D, 0.0D };
        }
        return new double[] { x / length, z / length };
    }

    private static final class Placement {
        private final BlockPos target;
        private final BlockPos support;
        private final EnumFacing face;
        private final Vec3 hitVec;

        private Placement(BlockPos target, BlockPos support,
                EnumFacing face, Vec3 hitVec) {
            this.target = target;
            this.support = support;
            this.face = face;
            this.hitVec = hitVec;
        }
    }
}
