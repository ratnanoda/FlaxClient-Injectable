package me.eldodebug.soar.management.mods.impl;

import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventCameraRotation;
import me.eldodebug.soar.management.event.impl.EventPlayerHeadRotation;
import me.eldodebug.soar.management.event.impl.EventUpdate;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

/**
 * Original simple Scaffold placement with a detached camera.
 *
 * While enabled, the real player and server rotation stay locked toward the
 * placement point. When no placement point exists, the player faces opposite
 * the camera-relative movement direction with a downward placement pitch.
 * Movement is remapped so the resulting motion remains camera-relative while
 * the server observes backward movement. Sprint is forcibly disabled.
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
    private static final float HOLD_PITCH = 80.0F;

    private static boolean silentRotationActive;
    private static boolean stopSprintPacketSent;
    private static float cameraYaw;
    private static float cameraPitch;
    private static float previousCameraYaw;
    private static float previousCameraPitch;
    private static float silentYaw;
    private static float silentPitch;

    private int originalSlot = -1;
    private int placeCooldown;

    public ScaffoldMod() {
        super(TranslateText.SCAFFOLD, TranslateText.SCAFFOLD_DESCRIPTION,
                ModCategory.BLATANT);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        originalSlot = mc.thePlayer == null
                ? -1 : mc.thePlayer.inventory.currentItem;
        placeCooldown = 0;
        stopSprintPacketSent = false;

        if(hasPlayerContext()) {
            activateDetachedCamera();
            updateBackwardHoldingRotation();
            enforceNoSprint();
        }
    }

    @Override
    public void onDisable() {
        enforceNoSprint();
        deactivateDetachedCamera();
        restoreOriginalSlot();
        placeCooldown = 0;
        stopSprintPacketSent = false;
        super.onDisable();
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if(!hasPlayerContext()) {
            deactivateDetachedCamera();
            return;
        }

        activateDetachedCamera();
        enforceNoSprint();

        Placement placement = null;
        int slot = -1;

        if(canPlace()) {
            BlockPos target = getTargetPosition();
            if(isReplaceable(target)) {
                float preferredBackwardYaw = getDesiredBackwardYaw();
                placement = findPlacement(target, preferredBackwardYaw);
                slot = findBlockSlot();
            }
        }

        if(placement != null) {
            lockSilentRotation(placement.yaw, placement.pitch);
        } else {
            updateBackwardHoldingRotation();
        }

        applySilentRotationToPlayer();
        sendCurrentLookPacket();

        if(!canPlace()) {
            return;
        }

        if(placeCooldown > 0) {
            placeCooldown--;
            return;
        }

        if(placement == null || slot < 0) {
            return;
        }

        selectSlot(slot);
        ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
        if(!isUsableBlock(stack)) {
            return;
        }

        boolean placed = mc.playerController.onPlayerRightClick(
                mc.thePlayer,
                mc.theWorld,
                stack,
                placement.support,
                placement.face,
                placement.hitVec);

        if(placed) {
            mc.thePlayer.swingItem();
            placeCooldown = 1;
        }

        applySilentRotationToPlayer();
        enforceNoSprint();
    }

    /** Keep mouse input on a detached camera instead of rotating the player. */
    @EventTarget
    public void onPlayerHeadRotation(EventPlayerHeadRotation event) {
        if(!silentRotationActive) {
            return;
        }

        updateDetachedCamera(event.getYaw(), event.getPitch());
        event.setCancelled(true);
    }

    /** Render the first/third-person camera from the detached camera angles. */
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
        if(mc.thePlayer == null || mc.theWorld == null
                || mc.playerController == null) {
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

    private BlockPos getTargetPosition() {
        return new BlockPos(
                mc.thePlayer.posX,
                mc.thePlayer.getEntityBoundingBox().minY - 1.0D,
                mc.thePlayer.posZ);
    }

    /**
     * Uses the original immediate support search, but chooses the valid face
     * whose exact hit rotation is closest to the desired backward yaw.
     */
    private Placement findPlacement(BlockPos target, float preferredYaw) {
        Placement best = null;
        float bestDifference = Float.MAX_VALUE;

        for(EnumFacing direction : SUPPORT_DIRECTIONS) {
            BlockPos support = target.offset(direction);
            if(!isValidSupport(support)) {
                continue;
            }

            EnumFacing face = direction.getOpposite();
            Vec3 hitVec = new Vec3(
                    support.getX() + 0.5D
                            + face.getFrontOffsetX() * 0.5D,
                    support.getY() + 0.5D
                            + face.getFrontOffsetY() * 0.5D,
                    support.getZ() + 0.5D
                            + face.getFrontOffsetZ() * 0.5D);
            float[] rotation = rotationsTo(hitVec);
            float difference = Math.abs(MathHelper.wrapAngleTo180_float(
                    rotation[0] - preferredYaw));

            if(best == null || difference < bestDifference) {
                best = new Placement(support, face, hitVec,
                        rotation[0], rotation[1]);
                bestDifference = difference;
            }
        }

        return best;
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
        return block == Blocks.air
                || (material != null && material.isReplaceable());
    }

    private int findBlockSlot() {
        int currentSlot = mc.thePlayer.inventory.currentItem;
        ItemStack currentStack = mc.thePlayer.inventory
                .getStackInSlot(currentSlot);
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
        if(block == null || block == Blocks.air) {
            return false;
        }

        Material material = block.getMaterial();
        return material != null && material.isSolid()
                && block.isFullCube();
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

    private float[] rotationsTo(Vec3 hitVec) {
        double deltaX = hitVec.xCoord - mc.thePlayer.posX;
        double deltaY = hitVec.yCoord
                - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double deltaZ = hitVec.zCoord - mc.thePlayer.posZ;
        double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        float yaw = (float) (Math.toDegrees(
                Math.atan2(deltaZ, deltaX)) - 90.0D);
        float pitch = (float) -Math.toDegrees(
                Math.atan2(deltaY, horizontal));
        return new float[] { yaw, clampPitch(pitch) };
    }

    private static void activateDetachedCamera() {
        if(silentRotationActive || mc.thePlayer == null) {
            return;
        }

        cameraYaw = mc.thePlayer.rotationYaw;
        cameraPitch = mc.thePlayer.rotationPitch;
        previousCameraYaw = mc.thePlayer.prevRotationYaw;
        previousCameraPitch = mc.thePlayer.prevRotationPitch;
        silentYaw = cameraYaw + 180.0F;
        silentPitch = HOLD_PITCH;
        silentRotationActive = true;
        applySilentRotationToPlayer();
    }

    private static void deactivateDetachedCamera() {
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

    private static void updateDetachedCamera(float yawDelta,
            float pitchDelta) {
        float oldYaw = cameraYaw;
        float oldPitch = cameraPitch;
        cameraYaw += yawDelta * 0.15F;
        cameraPitch = clampPitch(cameraPitch - pitchDelta * 0.15F);
        previousCameraYaw += cameraYaw - oldYaw;
        previousCameraPitch += cameraPitch - oldPitch;
    }

    private static void updateBackwardHoldingRotation() {
        if(!silentRotationActive) {
            activateDetachedCamera();
        }
        silentYaw = getDesiredBackwardYaw();
        silentPitch = HOLD_PITCH;
        applySilentRotationToPlayer();
    }

    private static float getDesiredBackwardYaw() {
        if(mc.thePlayer == null || mc.thePlayer.movementInput == null) {
            return cameraYaw + 180.0F;
        }

        float strafe = mc.thePlayer.movementInput.moveStrafe;
        float forward = mc.thePlayer.movementInput.moveForward;
        if(Math.abs(strafe) < INPUT_EPSILON
                && Math.abs(forward) < INPUT_EPSILON) {
            return cameraYaw + 180.0F;
        }

        double[] desired = movementVector(cameraYaw, strafe, forward);
        return yawFromVector(desired[0], desired[1]) + 180.0F;
    }

    private static void lockSilentRotation(float yaw, float pitch) {
        if(!silentRotationActive) {
            activateDetachedCamera();
        }
        silentYaw += MathHelper.wrapAngleTo180_float(yaw - silentYaw);
        silentPitch = clampPitch(pitch);
        applySilentRotationToPlayer();
    }

    private static void sendCurrentLookPacket() {
        if(!silentRotationActive || mc.thePlayer == null
                || mc.thePlayer.sendQueue == null) {
            return;
        }
        mc.thePlayer.sendQueue.addToSendQueue(
                new C03PacketPlayer.C05PacketPlayerLook(
                        silentYaw, silentPitch, mc.thePlayer.onGround));
    }

    /**
     * Disables the sprint key, the local sprint flag and the server sprint
     * state. The explicit stop packet is sent on activation and whenever a
     * transient sprint state is observed.
     */
    public static void enforceNoSprint() {
        if(mc.thePlayer == null) {
            return;
        }

        boolean wasSprinting = mc.thePlayer.isSprinting();
        mc.thePlayer.setSprinting(false);

        if(mc.gameSettings != null) {
            KeyBinding.setKeyBindState(
                    mc.gameSettings.keyBindSprint.getKeyCode(), false);
        }

        if(mc.thePlayer.sendQueue != null
                && (!stopSprintPacketSent || wasSprinting)) {
            mc.thePlayer.sendQueue.addToSendQueue(
                    new C0BPacketEntityAction(
                            mc.thePlayer,
                            C0BPacketEntityAction.Action.STOP_SPRINTING));
            stopSprintPacketSent = true;
        }
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

    public static Vec3 getCameraLook(float partialTicks) {
        float yaw = previousCameraYaw
                + (cameraYaw - previousCameraYaw) * partialTicks;
        float pitch = previousCameraPitch
                + (cameraPitch - previousCameraPitch) * partialTicks;
        return getVectorForRotation(yaw, pitch);
    }

    /**
     * Converts camera-relative input to backward/back-diagonal input under the
     * server-facing yaw. Forward input is never emitted while Scaffold is on.
     */
    public static float[] getMoveFixedInput(float strafe, float forward) {
        float magnitude = Math.max(Math.abs(strafe), Math.abs(forward));
        if(magnitude < INPUT_EPSILON) {
            return new float[] { 0.0F, 0.0F };
        }

        double[] desired = movementVector(cameraYaw, strafe, forward);
        int bestStrafe = 0;
        double bestDot = -Double.MAX_VALUE;

        for(int candidateStrafe = -1; candidateStrafe <= 1;
                candidateStrafe++) {
            double[] candidate = movementVector(
                    silentYaw, candidateStrafe, -1.0F);
            double dot = desired[0] * candidate[0]
                    + desired[1] * candidate[1];
            if(dot > bestDot) {
                bestDot = dot;
                bestStrafe = candidateStrafe;
            }
        }

        return new float[] {
                bestStrafe * magnitude,
                -magnitude
        };
    }

    private static double[] movementVector(float yaw, float strafe,
            float forward) {
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

    private static Vec3 getVectorForRotation(float yaw, float pitch) {
        float yawCos = MathHelper.cos(
                -yaw * 0.017453292F - (float) Math.PI);
        float yawSin = MathHelper.sin(
                -yaw * 0.017453292F - (float) Math.PI);
        float pitchCos = -MathHelper.cos(-pitch * 0.017453292F);
        float pitchSin = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3(yawSin * pitchCos, pitchSin, yawCos * pitchCos);
    }

    private static float clampPitch(float pitch) {
        return Math.max(-90.0F, Math.min(90.0F, pitch));
    }

    private static final class Placement {
        private final BlockPos support;
        private final EnumFacing face;
        private final Vec3 hitVec;
        private final float yaw;
        private final float pitch;

        private Placement(BlockPos support, EnumFacing face, Vec3 hitVec,
                float yaw, float pitch) {
            this.support = support;
            this.face = face;
            this.hitVec = hitVec;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}
