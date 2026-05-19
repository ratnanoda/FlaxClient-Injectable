package me.eldodebug.soar.management.mods.impl;

import java.util.concurrent.ThreadLocalRandom;

import org.lwjgl.input.Mouse;

import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventRenderTick;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

public class AimAssistMod extends Mod {

	private final NumberSetting rangeSetting = new NumberSetting(TranslateText.RANGE, this, 4.2D, 2.0D, 6.0D, false);
	private final NumberSetting fovSetting = new NumberSetting(TranslateText.FOV, this, 48.0D, 10.0D, 140.0D, false);
	private final NumberSetting smoothnessSetting = new NumberSetting(TranslateText.SMOOTH_SPEED, this, 7.5D, 1.5D, 26.0D, false);
	private final NumberSetting accuracySetting = new NumberSetting(TranslateText.ACCURACY, this, 84.0D, 0.0D, 100.0D, false);
	private final NumberSetting strengthSetting = new NumberSetting(TranslateText.STRENGTH, this, 0.9D, 0.05D, 2.6D, false);
	private float smoothYawStep;
	private float smoothPitchStep;
	private EntityPlayer targetLock;
	private int targetLockTicks;

	public AimAssistMod() {
		super(TranslateText.AIM_ASSIST, TranslateText.AIM_ASSIST_DESCRIPTION, ModCategory.GHOST);
	}

	@EventTarget
	public void onRenderTick(EventRenderTick event) {
		if(!canAssist()) {
			releaseAssistMotion(0.6F);
			targetLock = null;
			targetLockTicks = 0;
			return;
		}

		// Don't assist if looking at a block very closely to avoid jitter when mining/placing
		if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK) {
			if (mc.objectMouseOver.hitVec.distanceTo(mc.thePlayer.getPositionEyes(1.0F)) < 2.5D) {
				releaseAssistMotion(0.5F);
				return;
			}
		}

		maintainSprintInput();

		EntityPlayer target = selectTarget();
		if(target == null) {
			releaseAssistMotion(0.72F);
			return;
		}

		float[] rotations = getTargetRotations(target);
		float yawDiff = MathHelper.wrapAngleTo180_float(rotations[0] - mc.thePlayer.rotationYaw);
		float pitchDiff = MathHelper.wrapAngleTo180_float(rotations[1] - mc.thePlayer.rotationPitch);

		float strength = (float) strengthSetting.getValue();
		float accuracy = clamp((float) accuracySetting.getValue() / 100.0F, 0.0F, 1.0F);
		float strengthNorm = clamp(strength / 2.6F, 0.0F, 1.0F);

		float precision = Math.max(0.012F, (0.34F - (strength * 0.095F)) * (1.0F - (accuracy * 0.35F)));
		if(Math.abs(yawDiff) <= precision && Math.abs(pitchDiff) <= precision) {
			releaseAssistMotion(0.74F);
			return;
		}

		float fovHalf = (float) fovSetting.getValue() / 2.0F;
		float totalError = (float) Math.sqrt((yawDiff * yawDiff) + (pitchDiff * pitchDiff));
		float manualThreshold = Math.max(8.0F, fovHalf * 0.42F);
		float closeness = clamp((manualThreshold - totalError) / manualThreshold, 0.0F, 1.0F);
		float assistBlend = (0.18F + 0.82F * smoothStep(closeness)) * (0.67F + (accuracy * 0.33F));

		float smoothing = (float) smoothnessSetting.getValue();
		float errorScale = clamp(totalError / Math.max(1.0F, fovHalf), 0.0F, 1.0F);
		float maxStep = 0.06F + (0.36F * errorScale) + (strength * 0.60F);

		float trackingScale = 0.42F + (strength * 0.58F);
		float desiredYawStep = (yawDiff / smoothing) * trackingScale * assistBlend;
		float desiredPitchStep = (pitchDiff / (smoothing * 1.12F)) * trackingScale * assistBlend;

		desiredYawStep = clamp(desiredYawStep, -maxStep, maxStep);
		desiredPitchStep = clamp(desiredPitchStep, -maxStep, maxStep);

		float response = 0.20F + (strengthNorm * 0.24F) + (accuracy * 0.08F);
		smoothYawStep += (desiredYawStep - smoothYawStep) * response;
		smoothPitchStep += (desiredPitchStep - smoothPitchStep) * response;

		float jitterScale = (1.0F - accuracy);
		jitterScale = jitterScale * jitterScale;
		float yawStep = smoothYawStep + (float) ThreadLocalRandom.current().nextDouble(-0.014D, 0.014D) * jitterScale * (1.0F - assistBlend * 0.42F);
		float pitchStep = smoothPitchStep + (float) ThreadLocalRandom.current().nextDouble(-0.010D, 0.010D) * jitterScale * (1.0F - assistBlend * 0.42F);

		mc.thePlayer.rotationYaw += yawStep;
		mc.thePlayer.rotationPitch = clamp(mc.thePlayer.rotationPitch + pitchStep, -90.0F, 90.0F);
	}

	private boolean canAssist() {
		return mc.thePlayer != null
				&& mc.theWorld != null
				&& mc.inGameHasFocus
				&& mc.currentScreen == null
				&& (Mouse.isButtonDown(0) || mc.gameSettings.keyBindAttack.isKeyDown());
	}

	private void maintainSprintInput() {
		if(mc.thePlayer == null || mc.thePlayer.isSprinting() || !mc.gameSettings.keyBindSprint.isKeyDown()) {
			return;
		}

		if(mc.thePlayer.isSneaking() || mc.thePlayer.isUsingItem() || mc.thePlayer.isCollidedHorizontally) {
			return;
		}

		if(mc.thePlayer.moveForward <= 0.0F) {
			return;
		}

		if(mc.thePlayer.getFoodStats().getFoodLevel() <= 6 && !mc.thePlayer.capabilities.allowFlying) {
			return;
		}

		mc.thePlayer.setSprinting(true);
	}

	private void releaseAssistMotion(float damping) {
		smoothYawStep *= damping;
		smoothPitchStep *= damping;
	}

	private EntityPlayer selectTarget() {
		if(isValidTarget(targetLock, true)) {
			targetLockTicks = Math.min(targetLockTicks + 1, 12);
			return targetLock;
		}

		EntityPlayer next = findBestTarget();
		if(next != targetLock) {
			targetLockTicks = 0;
		}
		targetLock = next;
		return targetLock;
	}

	private boolean isValidTarget(EntityPlayer player, boolean relaxedFov) {
		if(player == null || mc.thePlayer == null || mc.theWorld == null || player == mc.thePlayer || player.isDead || player.isInvisible()) {
			return false;
		}

		if(!mc.thePlayer.canEntityBeSeen(player)) {
			return false;
		}

		float maxRange = (float) rangeSetting.getValue();
		float distance = mc.thePlayer.getDistanceToEntity(player);
		if(distance > maxRange) {
			return false;
		}

		float[] rotations = getTargetRotations(player);
		float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(rotations[0] - mc.thePlayer.rotationYaw));
		float pitchDiff = Math.abs(MathHelper.wrapAngleTo180_float(rotations[1] - mc.thePlayer.rotationPitch));
		float fovHalf = (float) fovSetting.getValue() / 2.0F;
		float checkFov = relaxedFov ? fovHalf * 1.18F : fovHalf;

		return yawDiff <= checkFov && pitchDiff <= checkFov;
	}

	private EntityPlayer findBestTarget() {
		EntityPlayer bestTarget = null;
		float bestScore = Float.MAX_VALUE;
		float maxRange = (float) rangeSetting.getValue();
		float fovHalf = (float) fovSetting.getValue() / 2.0F;

		for(EntityPlayer player : mc.theWorld.playerEntities) {
			if(player == null || player == mc.thePlayer || player.isDead || player.isInvisible()) {
				continue;
			}

			if(!mc.thePlayer.canEntityBeSeen(player)) {
				continue;
			}

			float distance = mc.thePlayer.getDistanceToEntity(player);
			if(distance > maxRange) {
				continue;
			}

			float[] rotations = getTargetRotations(player);
			float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(rotations[0] - mc.thePlayer.rotationYaw));
			float pitchDiff = Math.abs(MathHelper.wrapAngleTo180_float(rotations[1] - mc.thePlayer.rotationPitch));

			if(yawDiff > fovHalf || pitchDiff > fovHalf) {
				continue;
			}

			float targetStickBonus = (player == targetLock ? 3.2F : 0.0F) + (targetLockTicks * 0.08F);
			float score = yawDiff + (pitchDiff * 0.72F) + (distance * 0.23F) - targetStickBonus;
			if(score < bestScore) {
				bestScore = score;
				bestTarget = player;
			}
		}

		return bestTarget;
	}

	private float[] getTargetRotations(EntityPlayer target) {
		double x = target.posX - mc.thePlayer.posX;
		double y = (target.posY + (target.getEyeHeight() * 0.9D)) - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
		double z = target.posZ - mc.thePlayer.posZ;

		double horizontal = Math.sqrt((x * x) + (z * z));
		float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90.0F;
		float pitch = (float) (-Math.toDegrees(Math.atan2(y, horizontal)));

		float accuracy = clamp((float) (accuracySetting.getValue() / 100.0D), 0.0F, 1.0F);
		float noise = (1.0F - accuracy);
		noise = noise * noise;

		if(noise > 0.0F) {
			yaw += (float) ThreadLocalRandom.current().nextDouble(-noise * 1.7D, noise * 1.7D);
			pitch += (float) ThreadLocalRandom.current().nextDouble(-noise * 1.3D, noise * 1.3D);
		}

		return new float[] { yaw, pitch };
	}

	private float clamp(float value, float min, float max) {
		return value < min ? min : Math.min(value, max);
	}

	private float smoothStep(float value) {
		return value * value * (3.0F - (2.0F * value));
	}
}
