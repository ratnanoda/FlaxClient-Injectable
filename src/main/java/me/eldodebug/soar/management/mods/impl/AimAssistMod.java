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
	private final NumberSetting smoothnessSetting = new NumberSetting(TranslateText.SMOOTH_SPEED, this, 7.0D, 2.0D, 20.0D, false);
	private final NumberSetting accuracySetting = new NumberSetting(TranslateText.ACCURACY, this, 82.0D, 40.0D, 100.0D, false);
	private final NumberSetting strengthSetting = new NumberSetting(TranslateText.STRENGTH, this, 0.55D, 0.05D, 1.0D, false);
	private float smoothYawStep;
	private float smoothPitchStep;

	public AimAssistMod() {
		super(TranslateText.AIM_ASSIST, TranslateText.AIM_ASSIST_DESCRIPTION, ModCategory.GHOST);
	}

	@EventTarget
	public void onRenderTick(EventRenderTick event) {
		if(!canAssist()) {
			smoothYawStep *= 0.72F;
			smoothPitchStep *= 0.72F;
			return;
		}

		maintainSprintInput();

		EntityPlayer target = findBestTarget();
		if(target == null) {
			return;
		}

		float[] rotations = getTargetRotations(target);
		float yawDiff = MathHelper.wrapAngleTo180_float(rotations[0] - mc.thePlayer.rotationYaw);
		float pitchDiff = MathHelper.wrapAngleTo180_float(rotations[1] - mc.thePlayer.rotationPitch);

		float precision = (float) Math.max(0.02D, (1.2D - strengthSetting.getValue()) * 0.9D);
		if(Math.abs(yawDiff) <= precision && Math.abs(pitchDiff) <= precision) {
			smoothYawStep *= 0.7F;
			smoothPitchStep *= 0.7F;
			return;
		}

		float fovHalf = (float) fovSetting.getValue() / 2.0F;
		float totalError = (float) Math.sqrt((yawDiff * yawDiff) + (pitchDiff * pitchDiff));
		float manualThreshold = Math.max(8.0F, fovHalf * 0.42F);
		float closeness = clamp((manualThreshold - totalError) / manualThreshold, 0.0F, 1.0F);
		float assistBlend = 0.16F + 0.84F * smoothStep(closeness);

		float smoothing = (float) smoothnessSetting.getValue();
		float strength = (float) strengthSetting.getValue();
		float errorScale = clamp(totalError / Math.max(1.0F, fovHalf), 0.0F, 1.0F);
		float maxStep = 0.055F + (0.38F * errorScale) + (strength * 0.72F);

		float desiredYawStep = (yawDiff / smoothing) * strength * assistBlend;
		float desiredPitchStep = (pitchDiff / (smoothing * 1.1F)) * strength * assistBlend;

		desiredYawStep = clamp(desiredYawStep, -maxStep, maxStep);
		desiredPitchStep = clamp(desiredPitchStep, -maxStep, maxStep);

		smoothYawStep += (desiredYawStep - smoothYawStep) * 0.35F;
		smoothPitchStep += (desiredPitchStep - smoothPitchStep) * 0.35F;

		// Tiny micro-jitter for less robotic adjustment.
		float yawStep = smoothYawStep + (float) ThreadLocalRandom.current().nextDouble(-0.011D, 0.011D) * (1.0F - assistBlend * 0.45F);
		float pitchStep = smoothPitchStep + (float) ThreadLocalRandom.current().nextDouble(-0.008D, 0.008D) * (1.0F - assistBlend * 0.45F);

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

			float score = yawDiff + (pitchDiff * 0.72F) + (distance * 0.23F);
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

		float accuracy = (float) (accuracySetting.getValue() / 100.0D);
		float noise = (1.0F - accuracy);

		if(noise > 0.0F) {
			yaw += (float) ThreadLocalRandom.current().nextDouble(-noise * 1.2D, noise * 1.2D);
			pitch += (float) ThreadLocalRandom.current().nextDouble(-noise * 0.95D, noise * 0.95D);
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
