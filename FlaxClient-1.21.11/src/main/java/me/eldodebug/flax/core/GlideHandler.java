package me.eldodebug.flax.core;

import me.eldodebug.flax.management.event.EventTarget;
import me.eldodebug.flax.management.event.impl.EventTick;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.lwjgl.glfw.GLFW;

public final class GlideHandler {

	private static boolean menuKeyDown;

	private GlideHandler() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> new EventTick().call());
		Glide.getInstance().getEventManager().register(new MenuKeyHandler());
	}

	private static final class MenuKeyHandler {
		@EventTarget
		public void onTick(EventTick event) {
			if (Glide.getInstance().mc().currentScreen != null) {
				menuKeyDown = false;
				return;
			}

			long window = Glide.getInstance().mc().getWindow().getHandle();
			boolean pressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
			if (pressed && !menuKeyDown) {
				Glide.getInstance().openModMenu();
			}
			menuKeyDown = pressed;
		}
	}
}
