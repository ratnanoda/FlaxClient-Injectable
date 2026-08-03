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

    public static synchronized void beginAttach() {
        dejecting = false;
        coreHookReady = false;
        transformerReady = false;
    }

    public static synchronized void beginDeject() {
        dejecting = true;
        coreHookReady = false;
        transformerReady = false;
    }

    public static boolean isCoreHookReady() {
        return coreHookReady && !dejecting;
    }

    public static void markCoreHookReady() {
        coreHookReady = true;
    }

    public static void markTransformerReady() {
        transformerReady = true;
        dejecting = false;
    }
}
