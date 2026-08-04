package me.eldodebug.soar.utils.player;

import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;

/**
 * Helpers for constraining analogue or module-generated movement input to the
 * eight directions obtainable with vanilla WASD controls.
 */
public final class MoveFixUtils {

    private static final float INPUT_EPSILON = 1.0E-4F;
    private static final float DIRECTION_EPSILON = 1.0E-3F;
    private static final double DIRECTION_STEP = Math.PI / 4.0D;

    private MoveFixUtils() {
    }

    public static boolean isEightDirectionInput(float forward, float strafe) {
        float strength = Math.max(Math.abs(forward), Math.abs(strafe));
        if(strength < INPUT_EPSILON) {
            return true;
        }

        float normalizedForward = forward / strength;
        float normalizedStrafe = strafe / strength;
        return isVanillaComponent(normalizedForward) && isVanillaComponent(normalizedStrafe);
    }

    /**
     * Snaps an arbitrary local movement vector to the nearest 45-degree sector.
     * The returned array is ordered as [forward, strafe].
     */
    public static float[] snapToEightDirections(float forward, float strafe) {
        float strength = Math.max(Math.abs(forward), Math.abs(strafe));
        if(strength < INPUT_EPSILON) {
            return new float[] { 0.0F, 0.0F };
        }

        double angle = Math.atan2(strafe, forward);
        double snappedAngle = Math.round(angle / DIRECTION_STEP) * DIRECTION_STEP;
        float snappedForward = Math.round(Math.cos(snappedAngle)) * strength;
        float snappedStrafe = Math.round(Math.sin(snappedAngle)) * strength;
        return new float[] { snappedForward, snappedStrafe };
    }

    public static void applySnappedMoveFlying(Entity entity, float strafe, float forward, float friction) {
        float[] snapped = snapToEightDirections(forward, strafe);
        float fixedForward = snapped[0];
        float fixedStrafe = snapped[1];
        float lengthSquared = fixedStrafe * fixedStrafe + fixedForward * fixedForward;

        if(lengthSquared < INPUT_EPSILON) {
            return;
        }

        float length = MathHelper.sqrt_float(lengthSquared);
        if(length < 1.0F) {
            length = 1.0F;
        }

        float scale = friction / length;
        fixedStrafe *= scale;
        fixedForward *= scale;

        float yawSin = MathHelper.sin(entity.rotationYaw * (float) Math.PI / 180.0F);
        float yawCos = MathHelper.cos(entity.rotationYaw * (float) Math.PI / 180.0F);
        entity.motionX += fixedStrafe * yawCos - fixedForward * yawSin;
        entity.motionZ += fixedForward * yawCos + fixedStrafe * yawSin;
    }

    private static boolean isVanillaComponent(float value) {
        return Math.abs(value) < DIRECTION_EPSILON
                || Math.abs(Math.abs(value) - 1.0F) < DIRECTION_EPSILON;
    }
}
