package me.eldodebug.soar.attach;

import me.eldodebug.soar.logger.GlideLogger;

/**
 * Installs objects that a premain Mixin would normally add in constructors.
 */
public final class LateRuntimeInstaller {

    private LateRuntimeInstaller() {
    }

    public static void install() {
        // Ghost-only builds do not install cosmetic player layers.
        GlideLogger.info("Ghost-only runtime installer completed");
    }
}
