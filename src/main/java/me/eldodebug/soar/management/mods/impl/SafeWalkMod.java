package me.eldodebug.soar.management.mods.impl;

import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventUpdate;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class SafeWalkMod extends Mod {

	// Only cross the player's real bounding-box edge by a floating-point
	// epsilon. SafeWalk must not engage while the player is merely approaching
	// the edge.
	private static final double EDGE_PROBE_EPSILON = 0.001D;
	private static final double INPUT_EPSILON = 0.001D;
	private static final double SIDE_SAMPLE_OFFSET = 0.22D;

	private boolean forcedSneak;
	private boolean hasEdgeDirection;
	private double lastEdgeDirectionX;
	private double lastEdgeDirectionZ;
	private BlockPos edgeGapPos;

	public SafeWalkMod() {
		super(TranslateText.SAFE_WALK, TranslateText.SAFE_WALK_DESCRIPTION, ModCategory.GHOST);
	}

	@Override
	public void onEnable() {
		super.onEnable();
		forcedSneak = false;
		clearEdgeState();
	}

	@EventTarget
	public void onUpdate(EventUpdate event) {
		if(!canRun()) {
			releaseForcedSneak();
			clearEdgeState();
			return;
		}

		if(wasGapJustFilled()) {
			releaseForcedSneak();
			clearEdgeState();
			return;
		}

		if(isEdgeOfBlock()) {
			// Reassert the injected key state every tick. A physical key-release
			// event can clear KeyBinding.pressed even while SafeWalk still owns
			// sneak, so setting it only on the first edge tick is not sufficient.
			forceSneak();
		} else {
			releaseForcedSneak();
			clearEdgeState();
		}
	}

	@Override
	public void onDisable() {
		super.onDisable();
		releaseForcedSneak();
		clearEdgeState();
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

	private boolean isEdgeOfBlock() {
		AxisAlignedBB bb = mc.thePlayer.getEntityBoundingBox();
		double[] direction = getInputDirection();

		// When movement input is released at the lip, continue checking the last
		// direction that actually reached an edge. Releasing W/A/S/D must not
		// drop forced sneak while the player's feet are still over that lip.
		if(direction == null) {
			if(!hasEdgeDirection) {
				return false;
			}
			direction = new double[] { lastEdgeDirectionX, lastEdgeDirectionZ };
		}

		if(isEdgeInDirection(bb, direction[0], direction[1])) {
			lastEdgeDirectionX = direction[0];
			lastEdgeDirectionZ = direction[1];
			hasEdgeDirection = true;
			return true;
		}
		return false;
	}

	private double[] getInputDirection() {
		float forward = mc.thePlayer.movementInput.moveForward;
		float strafe = mc.thePlayer.movementInput.moveStrafe;
		if(Math.abs(forward) < INPUT_EPSILON && Math.abs(strafe) < INPUT_EPSILON) {
			return null;
		}

		double yaw = Math.toRadians(mc.thePlayer.rotationYaw);
		double directionX = strafe * Math.cos(yaw) - forward * Math.sin(yaw);
		double directionZ = forward * Math.cos(yaw) + strafe * Math.sin(yaw);
		double length = Math.sqrt(directionX * directionX + directionZ * directionZ);
		if(length < INPUT_EPSILON) {
			return null;
		}
		return new double[] { directionX / length, directionZ / length };
	}

	private boolean isEdgeInDirection(AxisAlignedBB bb, double directionX, double directionZ) {
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

		// A single unsupported corner is not a real edge. Require the centre and
		// both side samples of the leading edge to be unsupported.
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
			forcedSneak = false;
			return;
		}

		setSneak(true);
		forcedSneak = true;
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
		if(!isPhysicalSneakPressed()) {
			setSneak(false);
		}
		forcedSneak = false;
	}

	private void clearEdgeState() {
		hasEdgeDirection = false;
		lastEdgeDirectionX = 0.0D;
		lastEdgeDirectionZ = 0.0D;
		edgeGapPos = null;
	}
}
