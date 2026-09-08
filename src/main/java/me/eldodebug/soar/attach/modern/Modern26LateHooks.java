package me.eldodebug.soar.attach.modern;

/** Versioned linkage surface for Minecraft 26.2 transformed classes. */
public final class Modern26LateHooks {

    private Modern26LateHooks() {
    }

    public static void onAttached() {
        ModernLateHooks.onAttached();
    }

    public static void onClientTick() {
        ModernLateHooks.onClientTick();
    }

    public static void onHudRender(Object gui) {
        ModernLateHooks.onHudRender(gui);
    }

    public static void onCameraBeforeExtract(Object camera) {
        ModernLateHooks.onCameraBeforeExtract(camera);
    }
}
