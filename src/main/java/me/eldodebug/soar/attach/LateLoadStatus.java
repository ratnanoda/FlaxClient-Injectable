package me.eldodebug.soar.attach;

/**
 * Shared readiness gate for attach mode. It prevents the client managers from
 * starting while Minecraft classes are only partially transformed.
 */
public final class LateLoadStatus {

    private static volatile boolean coreHookReady;
    private static volatile boolean transformerReady;
    private static volatile boolean dejecting;

    private LateLoadStatus() {
    }

    public static boolean isTransformerReady() {
        return transformerReady && !dejecting;
    }

    public static boolean isDejecting() {
        return dejecting;
    }

    public static void beginDeject() {
        dejecting = true;
    }

    public static boolean isCoreHookReady() {
        return coreHookReady;
    }

    public static void markCoreHookReady() {
        coreHookReady = true;
    }

    public static void markTransformerReady() {
        transformerReady = true;
    }
}
