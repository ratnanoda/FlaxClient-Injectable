package me.eldodebug.flax.management.event;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class EventManager {

	private final Map<Class<?>, ArrayHelper<Data>> registry = new HashMap<>();

	public void register(Object listener) {
		for (Method method : listener.getClass().getDeclaredMethods()) {
			if (!isMethodBad(method)) {
				register(method, listener);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void register(Method method, Object listener) {
		Class<?> eventType = method.getParameterTypes()[0];
		Data data = new Data(listener, method, method.getAnnotation(EventTarget.class).value());
		if (!data.target.canAccess(listener)) {
			data.target.setAccessible(true);
		}

		ArrayHelper<Data> listeners = registry.computeIfAbsent(eventType, key -> new ArrayHelper<>());
		if (!listeners.contains(data)) {
			listeners.add(data);
			sort(eventType);
		}
	}

	public void unregister(Object listener) {
		for (ArrayHelper<Data> listeners : registry.values()) {
			for (Data data : listeners) {
				if (data.source.equals(listener)) {
					listeners.remove(data);
				}
			}
		}
		cleanEmpty();
	}

	private void cleanEmpty() {
		Iterator<Map.Entry<Class<?>, ArrayHelper<Data>>> iterator = registry.entrySet().iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getValue().isEmpty()) {
				iterator.remove();
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void sort(Class<?> eventType) {
		ArrayHelper<Data> sorted = new ArrayHelper<>();
		for (byte priority : Priority.VALUE_ARRAY) {
			for (Data data : registry.get(eventType)) {
				if (data.priority == priority) {
					sorted.add(data);
				}
			}
		}
		registry.put(eventType, sorted);
	}

	private boolean isMethodBad(Method method) {
		return method.getParameterTypes().length != 1 || !method.isAnnotationPresent(EventTarget.class);
	}

	public ArrayHelper<Data> get(Class<? extends Event> eventType) {
		return registry.get(eventType);
	}
}
