package me.eldodebug.flax.management.event;

import me.eldodebug.flax.FlaxClient;
import me.eldodebug.flax.core.Glide;

import java.lang.reflect.InvocationTargetException;

public abstract class Event {

	private boolean cancelled;

	public Event call() {
		this.cancelled = false;
		callEvent(this);
		return this;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	private static void callEvent(Event event) {
		EventManager eventManager = Glide.getInstance().getEventManager();
		ArrayHelper<Data> listeners = eventManager.get(event.getClass());
		if (listeners == null) {
			return;
		}

		for (Data data : listeners) {
			try {
				data.target.invoke(data.source, event);
			} catch (IllegalAccessException | InvocationTargetException error) {
				FlaxClient.LOGGER.error("Failed to dispatch {}", event.getClass().getSimpleName(), error);
			}
		}
	}
}
