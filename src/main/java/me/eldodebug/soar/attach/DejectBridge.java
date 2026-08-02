package me.eldodebug.soar.attach;

import java.util.concurrent.atomic.AtomicBoolean;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.logger.GlideLogger;

public final class DejectBridge {

    private static final AtomicBoolean REQUESTED = new AtomicBoolean();

    private DejectBridge() {
    }

    public static void requestDeject() {
        if(!REQUESTED.compareAndSet(false, true)) {
            return;
        }
        LateLoadStatus.beginDeject();
        try {
            Glide.getInstance().shutdownForDeject();
        } catch(Throwable error) {
            GlideLogger.error("Failed to stop the Java client cleanly before deject",
                    error instanceof Exception ? (Exception) error : new Exception(error));
        }
        try {
            requestNativeDeject();
        } catch(Throwable error) {
            GlideLogger.error("Failed to request native Minecraft class restoration",
                    error instanceof Exception ? (Exception) error : new Exception(error));
        }
    }

    private static native void requestNativeDeject();
}
