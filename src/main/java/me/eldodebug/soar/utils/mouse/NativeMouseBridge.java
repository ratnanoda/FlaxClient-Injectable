package me.eldodebug.soar.utils.mouse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * JNI bridge for injecting real OS-level mouse events through a virtual
 * kernel input device (Linux uinput). Events emitted here reach LWJGL
 * through the same XI2 raw-input path as a physical mouse, so Minecraft
 * cannot distinguish them from user input.
 */
public final class NativeMouseBridge {

	private static volatile boolean available;
	private static volatile String errorDetail;
	private static volatile int leftButtonState = -1;
	private static final ScheduledExecutorService CLICK_RELEASE_EXECUTOR =
			Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
				@Override
				public Thread newThread(Runnable runnable) {
					Thread thread = new Thread(runnable, "FlaxClient-MouseClick");
					thread.setDaemon(true);
					return thread;
				}
			});

	static {
		String detail;
		try {
			detail = tryLoad();
		} catch(Throwable t) {
			detail = t.getClass().getSimpleName() + ": " + t.getMessage();
		}
		if(detail == null) {
			available = true;
			startButtonPoller();
			try {
				Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
					@Override
					public void run() {
						try { nativeShutdown(); } catch(Throwable ignored) {}
					}
				}, "FlaxClient-NativeMouse-Shutdown"));
			} catch(Throwable ignored) {}
		} else {
			errorDetail = detail;
		}
	}

	private NativeMouseBridge() {}

	private static String tryLoad() throws Exception {
		String os = System.getProperty("os.name", "").toLowerCase();
		String arch = System.getProperty("os.arch", "").toLowerCase();
		String resource;
		if(os.contains("linux") && (arch.contains("amd64") || arch.contains("x86_64"))) {
			resource = "/native/linux-x64/libflaxmouse.so";
		} else {
			return "unsupported platform: " + os + "/" + arch;
		}

		InputStream in = NativeMouseBridge.class.getResourceAsStream(resource);
		if(in == null) {
			return "native resource missing: " + resource;
		}

		Path tmp = Files.createTempFile("libflaxmouse", ".so");
		tmp.toFile().deleteOnExit();
		try {
			Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
		} finally {
			try { in.close(); } catch(IOException ignored) {}
		}

		System.load(tmp.toAbsolutePath().toString());

		int rc = nativeInit();
		if(rc != 0) {
			if(rc == -13 || rc == -1) {
				return "cannot open /dev/uinput (permission denied). See FlaxClientLauncher/setup-uinput.sh";
			}
			return "nativeInit failed (code " + rc + ")";
		}
		return null;
	}

	public static boolean isAvailable() {
		return available;
	}

	public static String getErrorDetail() {
		return errorDetail;
	}

	public static boolean click() {
		if(!available) return false;
		try {
			if(nativeButton(0, true) != 0) return false;
			// A real mouse never emits press and release at exactly the same instant.
			// Release from a daemon thread so the four-millisecond pulse is visible to
			// LWJGL without stalling Minecraft's render thread.
			CLICK_RELEASE_EXECUTOR.schedule(new Runnable() {
				@Override
				public void run() {
					try { nativeButton(0, false); } catch(Throwable ignored) {}
				}
			}, 4L, TimeUnit.MILLISECONDS);
			return true;
		} catch(Throwable t) {
			try { nativeButton(0, false); } catch(Throwable ignored) {}
			return false;
		}
	}

	public static boolean move(int dx, int dy) {
		if(!available) return false;
		if(dx == 0 && dy == 0) return true;
		try {
			return nativeMove(dx, dy) == 0;
		} catch(Throwable t) {
			return false;
		}
	}

	/**
	 * Returns the physical button state via the X master pointer (aggregate of
	 * all input devices). Used to keep AutoClicker engaged across our own
	 * injected release events. -1 when the query isn't available.
	 */
	public static int queryButton(int button) {
		if(!available) return -1;
		if(button == 0) return leftButtonState;
		return -1;
	}

	public static boolean canTrackPhysicalLeftButton() {
		return available && leftButtonState >= 0;
	}

	/** Keep EVIOCGKEY off Minecraft's latency-sensitive render thread. */
	private static void startButtonPoller() {
		Thread poller = new Thread(new Runnable() {
			@Override
			public void run() {
				while(available) {
					try {
						leftButtonState = nativeQueryButton(0);
						Thread.sleep(4L);
					} catch(InterruptedException ignored) {
						Thread.currentThread().interrupt();
						break;
					} catch(Throwable ignored) {
						leftButtonState = -1;
						try { Thread.sleep(50L); } catch(InterruptedException interrupted) {
							Thread.currentThread().interrupt();
							break;
						}
					}
				}
			}
		}, "FlaxClient-MouseState");
		poller.setDaemon(true);
		poller.start();
	}

	private static native int nativeInit();
	private static native int nativeClick(int button);
	private static native int nativeButton(int button, boolean down);
	private static native int nativeMove(int dx, int dy);
	private static native int nativeQueryButton(int button);
	private static native void nativeShutdown();
}
