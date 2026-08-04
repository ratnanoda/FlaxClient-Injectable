package me.eldodebug.soar.management.mods.impl;

import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventUpdate;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.BooleanSetting;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class SafeWalkMod extends Mod {

	private final BooleanSetting blocksOnlySetting = new BooleanSetting(TranslateText.BLOCK, this, true);

	// Only cross the player's real bounding-box edge by a floating-point
	// epsilon. SafeWalk must not engage while the player is merely approaching
	// the edge.
	private static final double EDGE_PROBE_EPSILON = 0.001D;
	private static final double INPUT_EPSILON = 0.001D;
	private static final double SIDE_SAMPLE_OFFSET = 0.22D;

	private boolean forcedSneak;
	private BlockPos edgeGapPos;

	public SafeWalkMod() {
		super(TranslateText.SAFE_WALK, TranslateText.SAFE_WALK_DESCRIPTION, ModCategory.GHOST);
	}

	@Override
	public void onEnable() {
		super.onEnable();
		forcedSneak = false;
		edgeGapPos = null;
	}

	@EventTarget
	public void onUpdate(EventUpdate event) {
		if(!canRun() || !settingsMet()) {
			releaseForcedSneak();
			edgeGapPos = null;
			return;
		}

		if(wasGapJustFilled()) {
			releaseForcedSneak();
			edgeGapPos = null;
			return;
		}

		if(isEdgeOfBlock()) {
			forceSneak();
		} else {
			// Do not retain sneak for a debounce/delay tick. This is deliberately
			// immediate so SafeWalk cannot remain active away from an edge.
			releaseForcedSneak();
			edgeGapPos = null;
		}
	}

	@Override
	public void onDisable() {
		super.onDisable();
		releaseForcedSneak();
		edgeGapPos = null;
	}

	private boolean canRun() {
		if(mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null || !mc.inGameHasFocus) {
			return false;
		}

		if(!mc.thePlayer.onGround || mc.thePlayer.capabilities.isFlying || mc.thePlayer.ridingEntity != null) {
			return false;
		}

		return !mc.thePlayer.isInWater() && !mc.thePlayer.isInLava() && !mc.thePlayer.isOnLadder();
	}

	private boolean settingsMet() {
		if(!blocksOnlySetting.isToggled()) {
			return true;
		}

		ItemStack heldItem = mc.thePlayer.getHeldItem();
		return heldItem != null && heldItem.getItem() instanceof ItemBlock;
	}

	private boolean isEdgeOfBlock() {
		AxisAlignedBB bb = mc.thePlayer.getEntityBoundingBox();
		float forward = mc.thePlayer.movementInput.moveForward;
		float strafe = mc.thePlayer.movementInput.moveStrafe;

		if(Math.abs(forward) < INPUT_EPSILON && Math.abs(strafe) < INPUT_EPSILON) {
			return false;
		}

		// Convert the actual keyboard movement input into a horizontal world
		// direction. motionX/motionZ can retain inertia after input stops and was
		// one source of SafeWalk appearing to stick.
		double yaw = Math.toRadians(mc.thePlayer.rotationYaw);
		double directionX = strafe * Math.cos(yaw) - forward * Math.sin(yaw);
		double directionZ = forward * Math.cos(yaw) + strafe * Math.sin(yaw);
		double length = Math.sqrt(directionX * directionX + directionZ * directionZ);
		if(length < INPUT_EPSILON) {
			return false;
		}
		directionX /= length;
		directionZ /= length;

		double centerX = (bb.minX + bb.maxX) * 0.5D;
		double centerZ = (bb.minZ + bb.maxZ) * 0.5D;
		double halfWidthX = (bb.maxX - bb.minX) * 0.5D;
		double halfWidthZ = (bb.maxZ - bb.minZ) * 0.5D;

		double edgeDistanceX = Math.abs(directionX) < INPUT_EPSILON
				? Double.POSITIVE_INFINITY : halfWidthX / Math.abs(directionX);
		double edgeDistanceZ = Math.abs(directionZ) < INPUT_EPSILON
				? Double.POSITIVE_INFINITY : halfWidthZ / Math.abs(directionZ);
		double edgeDistance = Math.min(edgeDistanceX, edgeDistanceZ) + EDGE_PROBE_EPSILON;

		double leadingX = centerX + directionX * edgeDistance;
		double leadingZ = centerZ + directionZ * edgeDistance;
		double sideX = -directionZ;
		double sideZ = directionX;
		double y = bb.minY - 0.01D;

		// A single unsupported corner is not a real edge: it happens frequently
		// while walking diagonally or near neighbouring blocks. Require the
		// centre and both sides of the leading edge to be unsupported.
		boolean centerGap = isGap(leadingX, y, leadingZ);
		boolean leftGap = isGap(leadingX + sideX * SIDE_SAMPLE_OFFSET, y,
				leadingZ + sideZ * SIDE_SAMPLE_OFFSET);
		boolean rightGap = isGap(leadingX - sideX * SIDE_SAMPLE_OFFSET, y,
				leadingZ - sideZ * SIDE_SAMPLE_OFFSET);

		if(centerGap && leftGap && rightGap) {
			edgeGapPos = new BlockPos(leadingX, y, leadingZ);
			return true;
		}
		return false;
	}

	private boolean isGap(double x, double y, double z) {
		return mc.theWorld.isAirBlock(new BlockPos(x, y, z));
	}

	private boolean wasGapJustFilled() {
		return forcedSneak && edgeGapPos != null && !mc.theWorld.isAirBlock(edgeGapPos);
	}

	private void forceSneak() {
		if(isPhysicalSneakPressed()) {
			return;
		}

		if(!forcedSneak) {
			setSneak(true);
			forcedSneak = true;
		}
	}

	private boolean isPhysicalSneakPressed() {
		int keyCode = mc.gameSettings.keyBindSneak.getKeyCode();
		if(keyCode < 0) {
			return Mouse.isButtonDown(keyCode + 100);
		}
		return Keyboard.isKeyDown(keyCode);
	}

	private void setSneak(boolean state) {
		KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), state);
	}

	private void releaseForcedSneak() {
		// Synchronise the key state even when our ownership flag was lost during
		// a physical sneak press. This prevents an injected true state from
		// surviving after SafeWalk has left the edge.
		if(!isPhysicalSneakPressed()) {
			setSneak(false);
		}
		forcedSneak = false;
	}
}
