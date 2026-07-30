package me.eldodebug.soar.attach;

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
