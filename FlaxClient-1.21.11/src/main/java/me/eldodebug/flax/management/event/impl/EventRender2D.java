package me.eldodebug.flax.management.event.impl;

import me.eldodebug.flax.management.event.Event;
import net.minecraft.client.gui.DrawContext;

public class EventRender2D extends Event {

	private final DrawContext context;

	public EventRender2D(DrawContext context) {
		this.context = context;
	}

	public DrawContext getContext() {
		return context;
	}
}
