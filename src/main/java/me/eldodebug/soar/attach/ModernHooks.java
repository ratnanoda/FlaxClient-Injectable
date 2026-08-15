package me.eldodebug.soar.attach;

/** Hooks injected into unobfuscated Minecraft 26.1.2 classes. */
public final class ModernHooks {

    private ModernHooks() {
    }

    public static void onClientTick() {
        ModernClientRuntime.onClientTick();
    }

    public static void onRenderFrame() {
        ModernClientRuntime.onRenderFrame();
    }

    public static boolean onMouseTurn(Object mouseHandler) {
        return ModernClientRuntime.onMouseTurn(mouseHandler);
    }

    public static void onCameraUpdate(Object camera) {
        ModernClientRuntime.onCameraUpdate(camera);
    }

    public static boolean shouldBlockGameInput() {
        return ModernClientRuntime.shouldBlockGameInput();
    }

    public static boolean onKeyboardInput(Object event, int action) {
        return ModernClientRuntime.onKeyboardInput(event, action);
    }

    public static Object onPlayerRenderType(Object original, Object renderState) {
        return ModernClientRuntime.onPlayerRenderType(original, renderState);
    }

    public static void onLocalPlayerTick(Object player) {
        ModernClientRuntime.onLocalPlayerTick(player);
    }

    public static void onLocalPlayerAiStepHead(Object player) {
        ModernClientRuntime.onLocalPlayerAiStepHead(player);
    }

    public static void onLocalPlayerAiStepTail(Object player) {
        ModernClientRuntime.onLocalPlayerAiStepTail(player);
    }

    public static void onLocalPlayerSendPositionHead(Object player) {
        ModernClientRuntime.onLocalPlayerSendPositionHead(player);
    }

    public static void onLocalPlayerSendPositionTail(Object player) {
        ModernClientRuntime.onLocalPlayerSendPositionTail(player);
    }

    public static Object onLivingEntityTravel(Object entity, Object movement) {
        return ModernClientRuntime.onLivingEntityTravel(entity, movement);
    }
}
