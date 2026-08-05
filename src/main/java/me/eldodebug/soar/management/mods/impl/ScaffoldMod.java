package me.eldodebug.soar.management.mods.impl;

import me.eldodebug.soar.gui.modmenu.category.impl.BlatantCategory;
import me.eldodebug.soar.management.event.EventTarget;
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
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

/**
 * Places a solid hotbar block below the player's feet.
 *
 * The implementation follows the same high-level split used by modern
 * Scaffold implementations (target selection, support-face search, block
 * selection, rotation and placement), but is written against Minecraft 1.8.9
 * and FlaxClient's own event API.
 */
public class ScaffoldMod extends Mod {

    private static final EnumFacing[] SUPPORT_DIRECTIONS = {
            EnumFacing.DOWN,
            EnumFacing.NORTH,
            EnumFacing.SOUTH,
            EnumFacing.WEST,
            EnumFacing.EAST
    };

    private int originalSlot = -1;
    private int placeCooldown;

    public ScaffoldMod() {
        super(TranslateText.SCAFFOLD, TranslateText.SCAFFOLD_DESCRIPTION, ModCategory.BLATANT);
    }

    @Override
    public void setup() {
        BlatantCategory.install();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        originalSlot = mc.thePlayer == null ? -1 : mc.thePlayer.inventory.currentItem;
        placeCooldown = 0;
    }

    @Override
    public void onDisable() {
        restoreOriginalSlot();
        placeCooldown = 0;
        super.onDisable();
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if(!canRun()) {
            return;
        }

        if(placeCooldown > 0) {
            placeCooldown--;
            return;
        }

        BlockPos target = getTargetPosition();
        if(!isReplaceable(target)) {
            return;
        }

        Placement placement = findPlacement(target);
        int slot = findBlockSlot();
        if(placement == null || slot < 0) {
            return;
        }

        selectSlot(slot);
        ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
        if(!isUsableBlock(stack)) {
            return;
        }

        float[] rotations = rotationsTo(placement.hitVec);
        mc.thePlayer.sendQueue.addToSendQueue(
                new C03PacketPlayer.C05PacketPlayerLook(rotations[0], rotations[1], mc.thePlayer.onGround));

        boolean placed = mc.playerController.onPlayerRightClick(
                mc.thePlayer,
                mc.theWorld,
                stack,
                placement.support,
                placement.face,
                placement.hitVec);

        if(placed) {
            mc.thePlayer.swingItem();
            mc.rightClickDelayTimer = 1;
            placeCooldown = 1;
        }
    }

    private boolean canRun() {
        if(mc.thePlayer == null || mc.theWorld == null || mc.playerController == null) {
            return false;
        }
        if(mc.currentScreen != null || !mc.inGameHasFocus) {
            return false;
        }
        if(mc.thePlayer.isSpectator() || mc.thePlayer.capabilities.isFlying || mc.thePlayer.ridingEntity != null) {
            return false;
        }
        return !mc.thePlayer.isInWater() && !mc.thePlayer.isInLava() && !mc.thePlayer.isOnLadder();
    }

    private BlockPos getTargetPosition() {
        return new BlockPos(
                mc.thePlayer.posX,
                mc.thePlayer.getEntityBoundingBox().minY - 1.0D,
                mc.thePlayer.posZ);
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
            return new Placement(support, face, hitVec);
        }
        return null;
    }

    private boolean isValidSupport(BlockPos pos) {
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        Material material = block.getMaterial();
        return block != Blocks.air && material != null && material.isSolid() && !material.isReplaceable();
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
        if(mc.thePlayer == null || mc.playerController == null || originalSlot < 0 || originalSlot > 8) {
            originalSlot = -1;
            return;
        }
        selectSlot(originalSlot);
        originalSlot = -1;
    }

    private float[] rotationsTo(Vec3 hitVec) {
        double deltaX = hitVec.xCoord - mc.thePlayer.posX;
        double deltaY = hitVec.yCoord - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double deltaZ = hitVec.zCoord - mc.thePlayer.posZ;
        double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        float yaw = (float) (Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0D);
        float pitch = (float) -Math.toDegrees(Math.atan2(deltaY, horizontal));
        pitch = Math.max(-90.0F, Math.min(90.0F, pitch));
        return new float[] { yaw, pitch };
    }

    private static final class Placement {
        private final BlockPos support;
        private final EnumFacing face;
        private final Vec3 hitVec;

        private Placement(BlockPos support, EnumFacing face, Vec3 hitVec) {
            this.support = support;
            this.face = face;
            this.hitVec = hitVec;
        }
    }
}
