package me.eldodebug.soar.utils.player;

/**
 * Stores the server-facing rotation used by modules that need to aim without
 * taking control of the local camera.
 */
public final class SilentRotationManager {

    private static boolean active;
    private static float yaw;
    private static float pitch;
    private static float cameraYaw;
    private static float cameraPitch;

    private SilentRotationManager() {
    }

    public static void activate(float serverYaw, float serverPitch, float currentCameraYaw, float currentCameraPitch) {
        active = true;
        yaw = serverYaw;
        pitch = Math.max(-90.0F, Math.min(90.0F, serverPitch));
        cameraYaw = currentCameraYaw;
        cameraPitch = currentCameraPitch;
    }

    public static void updateCamera(float currentCameraYaw, float currentCameraPitch) {
        cameraYaw = currentCameraYaw;
        cameraPitch = currentCameraPitch;
    }

    public static void clear() {
        active = false;
    }

    public static boolean isActive() {
        return active;
    }

    public static float getYaw() {
        return yaw;
    }

    public static float getPitch() {
        return pitch;
    }

    public static float getCameraYaw() {
        return cameraYaw;
    }

    public static float getCameraPitch() {
        return cameraPitch;
    }
}
