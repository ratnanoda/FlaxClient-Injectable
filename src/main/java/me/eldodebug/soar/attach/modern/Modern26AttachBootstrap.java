package me.eldodebug.soar.attach.modern;

import java.util.concurrent.atomic.AtomicBoolean;

/** Versioned native entry point for the Minecraft 26.2 compatibility layer. */
public final class Modern26AttachBootstrap {

    private static final AtomicBoolean ATTACHED = new AtomicBoolean();

    private Modern26AttachBootstrap() {
    }

    public static void attach() {
        if (ATTACHED.compareAndSet(false, true)) {
            Modern26LateHooks.onAttached();
        }
    }
}
