package me.eldodebug.soar.management.mods.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventUpdate;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.utils.player.SilentRotationManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

/**
 * Emergency clutch builder. When the vertical column from directly below the
 * player's feet through three blocks down is empty, it silently aims at a
 * visible placement face and builds a connected path back to the feet.
 */
public class ClutchMod extends Mod {

    private static final int TRIGGER_DEPTH = 3;
    private static final int MAX_SEARCH_DEPTH = 10;
    private static final int MAX_HORIZONTAL_SEARCH = 5;
    private static final int MAX_VERTICAL_SEARCH = 7;
    private static final int MAX_PLACEMENTS_PER_TICK = 4;
    private static final double FACE_SAMPLE_OFFSET = 0.22D;
    private static final double RAY_TRACE_INSET = 0.025D;

    private static final EnumFacing[] SEARCH_DIRECTIONS = {
            EnumFacing.DOWN,
            EnumFacing.NORTH,
            EnumFacing.SOUTH,
            EnumFacing.WEST,
            EnumFacing.EAST
    };

    private static final EnumFacing[] PLACE_FACES = {
            EnumFacing.UP,
            EnumFacing.NORTH,
            EnumFacing.SOUTH,
            EnumFacing.WEST,
            EnumFacing.EAST,
            EnumFacing.DOWN
    };

    private boolean clutching;
    private boolean previousMoveFixState;
    private int originalHotbarSlot = -1;
    private int retriggerCooldown;

    public ClutchMod() {
        super(TranslateText.NONE, TranslateText.NONE, ModCategory.BLATANT);
    }

    @Override
    public String getName() {
        return "Clutch";
    }

    @Override
    public String getDescription() {
        return "Silently builds a reachable block path under the player to prevent a fall.";
    }

    @Override
    public String getNameKey() {
        return "text.clutch";
    }

    @Override
    public void onEnable() {
        super.onEnable();
        previousMoveFixState = SettingsMod.isMoveFixEnabled();
        forceMoveFix();
        resetRuntimeState();
    }

    @Override
    public void onDisable() {
        stopClutch(true);
        SettingsMod settings = SettingsMod.getInstance();
        if(settings != null) {
            settings.getMoveFixSetting().setToggled(previousMoveFixState);
        }
        super.onDisable();
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        forceMoveFix();

        if(retriggerCooldown > 0) {
            retriggerCooldown--;
        }

        if(!canRun()) {
            if(clutching) {
                stopClutch(false);
            }
            return;
        }

        if(clutching) {
            SilentRotationManager.updateCamera(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);

            if(hasStableSupportAtFeet() && mc.thePlayer.onGround) {
                stopClutch(false);
                retriggerCooldown = 2;
                return;
            }

            performClutchTick();
            return;
        }

        if(retriggerCooldown == 0 && shouldStartClutch()) {
            startClutch();
            performClutchTick();
        }
    }

    private void forceMoveFix() {
        SettingsMod settings = SettingsMod.getInstance();
        if(settings != null && !settings.getMoveFixSetting().isToggled()) {
            settings.getMoveFixSetting().setToggled(true);
        }
    }

    private boolean canRun() {
        if(mc.thePlayer == null || mc.theWorld == null || mc.playerController == null
                || mc.currentScreen != null || !mc.inGameHasFocus) {
            return false;
        }

        if(mc.thePlayer.capabilities.isFlying || mc.thePlayer.ridingEntity != null) {
            return false;
        }

        return !mc.thePlayer.isInWater() && !mc.thePlayer.isInLava() && !mc.thePlayer.isOnLadder();
    }

    private boolean shouldStartClutch() {
        return !mc.thePlayer.onGround
                && findBlockSlot() >= 0
                && !hasBlockInVerticalColumn(TRIGGER_DEPTH);
    }

    private void startClutch() {
        clutching = true;
        originalHotbarSlot = mc.thePlayer.inventory.currentItem;
    }

    private void performClutchTick() {
        int blockSlot = findBlockSlot();
        if(blockSlot < 0) {
            return;
        }

        selectHotbarSlot(blockSlot);

        for(int placed = 0; placed < MAX_PLACEMENTS_PER_TICK; placed++) {
            BlockPos target = getTargetUnderFeet();
            if(!isReplaceable(target)) {
                break;
            }

            List<BlockPos> path = findPlacementPath(target);
            if(path.isEmpty()) {
                break;
            }

            BlockPos placePos = path.get(0);
            PlacementData placement = findPlacementData(placePos);
            if(placement == null || !placeBlock(placement)) {
                break;
            }

            if(isReplaceable(placePos)) {
                break;
            }

            blockSlot = findBlockSlot();
            if(blockSlot < 0) {
                break;
            }
            selectHotbarSlot(blockSlot);
        }
    }

    private List<BlockPos> findPlacementPath(BlockPos target) {
        Queue<PathNode> queue = new ArrayDeque<PathNode>();
        Set<BlockPos> visited = new HashSet<BlockPos>();
        queue.add(new PathNode(target, null, 0));
        visited.add(target);

        while(!queue.isEmpty()) {
            PathNode node = queue.poll();
            if(!isReplaceable(node.pos)) {
                continue;
            }

            if(findPlacementData(node.pos) != null) {
                ArrayList<BlockPos> result = new ArrayList<BlockPos>();
                PathNode current = node;
                while(current != null) {
                    result.add(current.pos);
                    current = current.parent;
                }
                return result;
            }

            if(node.depth >= MAX_SEARCH_DEPTH) {
                continue;
            }

            for(EnumFacing direction : SEARCH_DIRECTIONS) {
                BlockPos next = node.pos.offset(direction);
                if(visited.contains(next) || !isWithinSearchBounds(next, target) || !isReplaceable(next)) {
                    continue;
                }

                visited.add(next);
                queue.add(new PathNode(next, node, node.depth + 1));
            }
        }

        return new ArrayList<BlockPos>();
    }

    private boolean isWithinSearchBounds(BlockPos pos, BlockPos target) {
        return pos.getY() <= target.getY()
                && pos.getY() >= target.getY() - MAX_VERTICAL_SEARCH
                && Math.abs(pos.getX() - target.getX()) <= MAX_HORIZONTAL_SEARCH
                && Math.abs(pos.getZ() - target.getZ()) <= MAX_HORIZONTAL_SEARCH;
    }

    private PlacementData findPlacementData(BlockPos placePos) {
        PlacementData closest = null;
        double closestDistance = Double.MAX_VALUE;
        Vec3 eyes = getEyePosition();

        for(EnumFacing face : PLACE_FACES) {
            BlockPos supportPos = placePos.offset(face.getOpposite());
            if(!isSolid(supportPos)) {
                continue;
            }

            Vec3 hitVec = findVisiblePointOnFace(eyes, supportPos, face);
            if(hitVec == null) {
                continue;
            }

            double distance = eyes.squareDistanceTo(hitVec);
            double reach = mc.playerController.getBlockReachDistance() + 0.15D;
            if(distance <= reach * reach && distance < closestDistance) {
                closestDistance = distance;
                closest = new PlacementData(supportPos, face, hitVec);
            }
        }

        return closest;
    }

    /**
     * Finds a point that is actually ray-visible on the requested block face.
     * The exact face centre is preferred; small inset samples are used only
     * when the centre is hidden by neighbouring geometry.
     */
    private Vec3 findVisiblePointOnFace(Vec3 eyes, BlockPos supportPos, EnumFacing face) {
        double[] offsets = { 0.0D, FACE_SAMPLE_OFFSET, -FACE_SAMPLE_OFFSET };

        for(double first : offsets) {
            for(double second : offsets) {
                Vec3 hitVec = getFacePoint(supportPos, face, first, second);
                if(isVisibleFaceHit(eyes, supportPos, face, hitVec)) {
                    return hitVec;
                }
            }
        }

        return null;
    }

    private Vec3 getFacePoint(BlockPos supportPos, EnumFacing face, double first, double second) {
        double x = supportPos.getX() + 0.5D + face.getFrontOffsetX() * 0.5D;
        double y = supportPos.getY() + 0.5D + face.getFrontOffsetY() * 0.5D;
        double z = supportPos.getZ() + 0.5D + face.getFrontOffsetZ() * 0.5D;

        if(face == EnumFacing.UP || face == EnumFacing.DOWN) {
            x += first;
            z += second;
        } else if(face == EnumFacing.NORTH || face == EnumFacing.SOUTH) {
            x += first;
            y += second;
        } else {
            z += first;
            y += second;
        }

        return new Vec3(x, y, z);
    }

    private boolean isVisibleFaceHit(Vec3 eyes, BlockPos supportPos, EnumFacing face, Vec3 hitVec) {
        Vec3 traceEnd = hitVec.addVector(
                -face.getFrontOffsetX() * RAY_TRACE_INSET,
                -face.getFrontOffsetY() * RAY_TRACE_INSET,
                -face.getFrontOffsetZ() * RAY_TRACE_INSET);
        MovingObjectPosition ray = mc.theWorld.rayTraceBlocks(eyes, traceEnd, false, true, false);
        return ray != null
                && ray.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                && supportPos.equals(ray.getBlockPos())
                && ray.sideHit == face;
    }

    private boolean placeBlock(PlacementData placement) {
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if(!isUsableBlockStack(heldItem)) {
            return false;
        }

        float[] rotations = getRotations(placement.hitVec);
        SilentRotationManager.activate(
                rotations[0],
                rotations[1],
                mc.thePlayer.rotationYaw,
                mc.thePlayer.rotationPitch);

        if(mc.getNetHandler() != null) {
            mc.getNetHandler().addToSendQueue(
                    new C03PacketPlayer.C05PacketPlayerLook(
                            rotations[0],
                            rotations[1],
                            mc.thePlayer.onGround));
        }

        boolean placed = mc.playerController.onPlayerRightClick(
                mc.thePlayer,
                mc.theWorld,
                heldItem,
                placement.supportPos,
                placement.face,
                placement.hitVec);

        if(placed) {
            mc.thePlayer.swingItem();
            mc.playerController.updateController();
        }

        return placed;
    }

    private float[] getRotations(Vec3 hitVec) {
        Vec3 eyes = getEyePosition();
        double deltaX = hitVec.xCoord - eyes.xCoord;
        double deltaY = hitVec.yCoord - eyes.yCoord;
        double deltaZ = hitVec.zCoord - eyes.zCoord;
        double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        float yaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) -(Math.atan2(deltaY, horizontal) * 180.0D / Math.PI);
        return new float[] { yaw, Math.max(-90.0F, Math.min(90.0F, pitch)) };
    }

    private Vec3 getEyePosition() {
        return new Vec3(
                mc.thePlayer.posX,
                mc.thePlayer.posY + mc.thePlayer.getEyeHeight(),
                mc.thePlayer.posZ);
    }

    private int findBlockSlot() {
        int current = mc.thePlayer.inventory.currentItem;
        if(isUsableBlockStack(mc.thePlayer.inventory.getStackInSlot(current))) {
            return current;
        }

        for(int slot = 0; slot < 9; slot++) {
            if(isUsableBlockStack(mc.thePlayer.inventory.getStackInSlot(slot))) {
                return slot;
            }
        }

        return -1;
    }

    private boolean isUsableBlockStack(ItemStack stack) {
        if(stack == null || stack.stackSize <= 0 || !(stack.getItem() instanceof ItemBlock)) {
            return false;
        }

        Block block = ((ItemBlock) stack.getItem()).getBlock();
        Material material = block.getMaterial();
        return !(block instanceof BlockFalling) && material.isSolid() && !material.isReplaceable();
    }

    private void selectHotbarSlot(int slot) {
        if(slot < 0 || slot > 8 || mc.thePlayer.inventory.currentItem == slot) {
            return;
        }

        mc.thePlayer.inventory.currentItem = slot;
        mc.playerController.updateController();
    }

    private BlockPos getTargetUnderFeet() {
        AxisAlignedBB bb = mc.thePlayer.getEntityBoundingBox();
        int y = MathHelper.floor_double(bb.minY - 0.01D);
        return new BlockPos(mc.thePlayer.posX, y, mc.thePlayer.posZ);
    }

    /**
     * Checks only the single vertical X/Z column directly under the player's
     * feet. Nearby blocks to the side or on a diagonal do not suppress Clutch.
     */
    private boolean hasBlockInVerticalColumn(int depth) {
        BlockPos top = getTargetUnderFeet();

        for(int offset = 0; offset < depth; offset++) {
            BlockPos pos = new BlockPos(top.getX(), top.getY() - offset, top.getZ());
            if(isSolid(pos)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasStableSupportAtFeet() {
        AxisAlignedBB bb = mc.thePlayer.getEntityBoundingBox();
        return hasSupportAtY(bb, MathHelper.floor_double(bb.minY - 0.01D));
    }

    private boolean hasSupportAtY(AxisAlignedBB bb, int y) {
        double minX = bb.minX + 0.05D;
        double maxX = bb.maxX - 0.05D;
        double minZ = bb.minZ + 0.05D;
        double maxZ = bb.maxZ - 0.05D;
        double centerX = (bb.minX + bb.maxX) * 0.5D;
        double centerZ = (bb.minZ + bb.maxZ) * 0.5D;

        return isSolid(new BlockPos(centerX, y, centerZ))
                || isSolid(new BlockPos(minX, y, minZ))
                || isSolid(new BlockPos(minX, y, maxZ))
                || isSolid(new BlockPos(maxX, y, minZ))
                || isSolid(new BlockPos(maxX, y, maxZ));
    }

    private boolean isSolid(BlockPos pos) {
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        Material material = block.getMaterial();
        return material.isSolid() && !material.isReplaceable();
    }

    private boolean isReplaceable(BlockPos pos) {
        return mc.theWorld.getBlockState(pos).getBlock().getMaterial().isReplaceable();
    }

    private void stopClutch(boolean resetAll) {
        if(mc.thePlayer != null && mc.getNetHandler() != null && SilentRotationManager.isActive()) {
            mc.getNetHandler().addToSendQueue(
                    new C03PacketPlayer.C05PacketPlayerLook(
                            mc.thePlayer.rotationYaw,
                            mc.thePlayer.rotationPitch,
                            mc.thePlayer.onGround));
        }

        SilentRotationManager.clear();
        restoreHotbarSlot();
        clutching = false;

        if(resetAll) {
            retriggerCooldown = 0;
        }
    }

    private void restoreHotbarSlot() {
        if(mc.thePlayer != null && originalHotbarSlot >= 0 && originalHotbarSlot <= 8) {
            mc.thePlayer.inventory.currentItem = originalHotbarSlot;
            if(mc.playerController != null) {
                mc.playerController.updateController();
            }
        }
        originalHotbarSlot = -1;
    }

    private void resetRuntimeState() {
        clutching = false;
        originalHotbarSlot = -1;
        retriggerCooldown = 0;
        SilentRotationManager.clear();
    }

    private static final class PathNode {
        private final BlockPos pos;
        private final PathNode parent;
        private final int depth;

        private PathNode(BlockPos pos, PathNode parent, int depth) {
            this.pos = pos;
            this.parent = parent;
            this.depth = depth;
        }
    }

    private static final class PlacementData {
        private final BlockPos supportPos;
        private final EnumFacing face;
        private final Vec3 hitVec;

        private PlacementData(BlockPos supportPos, EnumFacing face, Vec3 hitVec) {
            this.supportPos = supportPos;
            this.face = face;
            this.hitVec = hitVec;
        }
    }
}
