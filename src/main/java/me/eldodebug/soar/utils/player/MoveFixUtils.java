package me.eldodebug.soar.utils.player;

import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;

/**
 * Helpers for constraining analogue or rotation-corrected movement input to
 * the eight directions obtainable with vanilla WASD controls.
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

    /**
     * Finds the vanilla W/A/S/D combination, relative to targetYaw, whose world
     * direction is closest to the direction requested relative to sourceYaw.
     * This is the movement correction used while the server/player rotation is
     * decoupled from the local camera.
     */
    public static float[] remapForYaw(float forward, float strafe, float sourceYaw, float targetYaw) {
        float strength = Math.max(Math.abs(forward), Math.abs(strafe));
        if(strength < INPUT_EPSILON) {
            return new float[] { 0.0F, 0.0F };
        }

        double intendedAngle = getWorldAngle(sourceYaw, forward, strafe);
        float bestForward = 0.0F;
        float bestStrafe = 0.0F;
        double bestDifference = Double.MAX_VALUE;

        for(int candidateForward = -1; candidateForward <= 1; candidateForward++) {
            for(int candidateStrafe = -1; candidateStrafe <= 1; candidateStrafe++) {
                if(candidateForward == 0 && candidateStrafe == 0) {
                    continue;
                }

                double candidateAngle = getWorldAngle(targetYaw, candidateForward, candidateStrafe);
                double difference = Math.abs(wrapRadians(candidateAngle - intendedAngle));
                if(difference < bestDifference - 1.0E-8D) {
                    bestDifference = difference;
                    bestForward = candidateForward;
                    bestStrafe = candidateStrafe;
                }
            }
        }

        return new float[] { bestForward * strength, bestStrafe * strength };
    }

    public static void applySnappedMoveFlying(Entity entity, float strafe, float forward, float friction) {
        float[] snapped = snapToEightDirections(forward, strafe);
        applyMoveFlying(entity, snapped[1], snapped[0], friction, entity.rotationYaw);
    }

    /**
     * Minecraft 1.8's horizontal moveFlying calculation using an explicit yaw.
     * Input strength and vanilla diagonal normalization are retained.
     */
    public static void applyMoveFlying(Entity entity, float strafe, float forward, float friction, float yaw) {
        float lengthSquared = strafe * strafe + forward * forward;
        if(lengthSquared < INPUT_EPSILON) {
            return;
        }

        float length = MathHelper.sqrt_float(lengthSquared);
        if(length < 1.0F) {
            length = 1.0F;
        }

        float scale = friction / length;
        strafe *= scale;
        forward *= scale;

        float yawSin = MathHelper.sin(yaw * (float) Math.PI / 180.0F);
        float yawCos = MathHelper.cos(yaw * (float) Math.PI / 180.0F);
        entity.motionX += strafe * yawCos - forward * yawSin;
        entity.motionZ += forward * yawCos + strafe * yawSin;
    }

    private static double getWorldAngle(float yaw, float forward, float strafe) {
        double yawRadians = Math.toRadians(yaw);
        double x = strafe * Math.cos(yawRadians) - forward * Math.sin(yawRadians);
        double z = forward * Math.cos(yawRadians) + strafe * Math.sin(yawRadians);
        return Math.atan2(z, x);
    }

    private static double wrapRadians(double angle) {
        while(angle <= -Math.PI) {
            angle += Math.PI * 2.0D;
        }
        while(angle > Math.PI) {
            angle -= Math.PI * 2.0D;
        }
        return angle;
    }

    private static boolean isVanillaComponent(float value) {
        return Math.abs(value) < DIRECTION_EPSILON
                || Math.abs(Math.abs(value) - 1.0F) < DIRECTION_EPSILON;
    }
}
