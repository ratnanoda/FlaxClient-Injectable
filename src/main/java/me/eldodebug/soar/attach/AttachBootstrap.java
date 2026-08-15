package me.eldodebug.soar.attach;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.logger.GlideLogger;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.notification.NotificationType;
import net.minecraft.client.Minecraft;

/**
 * Entry point called after FlaxClient.dll has added the client jar to the
 * running Minecraft LaunchClassLoader.
 *
 * The late transformer is installed before this class starts the full client.
 * Until that transformer is ready, attaching only verifies the native/JVM/JAR
 * bridge and deliberately leaves the existing game state untouched.
 */
public final class AttachBootstrap {

    private static final AtomicBoolean ATTACH_REQUESTED = new AtomicBoolean();
    private static final AtomicBoolean CLIENT_STARTED = new AtomicBoolean();
    private static final AtomicBoolean CLIENT_FAILED = new AtomicBoolean();

    private AttachBootstrap() {
    }

    public static void attach() {
        if (!ATTACH_REQUESTED.compareAndSet(false, true)) {
            GlideLogger.warn("FlaxClient attach was already requested");
            return;
        }

        if (isModernMinecraft()) {
            attachModern();
            return;
        }

        attachLegacy();
    }

    private static void attachLegacy() {
        final Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) {
            ATTACH_REQUESTED.set(false);
            throw new IllegalStateException("Minecraft has not created its client instance");
        }

        minecraft.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!LateLoadStatus.isTransformerReady()) {
                        GlideLogger.info(
                                "FlaxClient DLL bridge is ready; waiting for the late-load transformer");
                        return;
                    }
                    startClient();
                } catch (Throwable error) {
                    ATTACH_REQUESTED.set(false);
                    GlideLogger.error(
                            "Failed to initialize FlaxClient after attach",
                            error instanceof Exception ? (Exception) error : new Exception(error));
                }
            }
        });
    }

    private static boolean isModernMinecraft() {
        try {
            Minecraft.class.getMethod("getInstance");
            return true;
        } catch (NoSuchMethodException legacyClient) {
            return false;
        }
    }

    private static void attachModern() {
        try {
            Method getInstance = Minecraft.class.getMethod("getInstance");
            final Object minecraft = getInstance.invoke(null);
            if (minecraft == null) {
                throw new IllegalStateException("Minecraft has not created its client instance");
            }

            Method execute = minecraft.getClass().getMethod("execute", Runnable.class);
            execute.invoke(minecraft, new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!LateLoadStatus.isTransformerReady()) {
                            throw new IllegalStateException("late-load transformer is not ready");
                        }
                        ModernClientRuntime.start(minecraft);
                        CLIENT_STARTED.set(true);
                        GlideLogger.info("FlaxClient Lunar 26.1.2 initialization completed");
                    } catch (Throwable error) {
                        ModernClientRuntime.diagnostic("Modern bootstrap failed", error);
                        CLIENT_FAILED.set(true);
                        CLIENT_STARTED.set(false);
                        ATTACH_REQUESTED.set(false);
                        GlideLogger.error(
                                "Failed to initialize FlaxClient for Lunar 26.1.2",
                                error instanceof Exception
                                        ? (Exception) error
                                        : new Exception(error));
                    }
                }
            });
        } catch (Throwable error) {
            ModernClientRuntime.diagnostic("Could not schedule modern bootstrap", error);
            ATTACH_REQUESTED.set(false);
            throw new IllegalStateException("Could not schedule the Lunar 26.1.2 bootstrap", error);
        }
    }

    static void startClient() {
        if (CLIENT_FAILED.get()) {
            return;
        }
        if (CLIENT_STARTED.compareAndSet(false, true)) {
            try {
                AttachNativeLibraries.prepare();
                AttachOpenGLBridge.prepare();
                if (Glide.getInstance().getNanoVGManager() == null) {
                    Glide.getInstance().setNanoVGManager(new NanoVGManager());
                }
                Glide.getInstance().start();
                LateRuntimeInstaller.install();
                Glide.getInstance().getNotificationManager().post(
                        "Inject Successed!", "FlaxClient is ready", NotificationType.SUCCESS, 6000L);
                GlideLogger.info("FlaxClient late-load initialization completed");
            } catch (Throwable error) {
                CLIENT_STARTED.set(false);
                CLIENT_FAILED.set(true);
                GlideLogger.error(
                        "Failed to initialize FlaxClient after attach",
                        error instanceof Exception
                                ? (Exception) error
                                : new Exception(error));
            }
        }
    }
}
