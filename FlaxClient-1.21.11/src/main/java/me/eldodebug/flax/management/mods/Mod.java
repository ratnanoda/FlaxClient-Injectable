package me.eldodebug.flax.management.mods;

import me.eldodebug.flax.FlaxClient;
import me.eldodebug.flax.common.animation.SimpleAnimation;
import me.eldodebug.flax.core.Glide;
import net.minecraft.client.MinecraftClient;

public class Mod {

	protected final MinecraftClient mc = MinecraftClient.getInstance();
	private final String id;
	private final String name;
	private final String description;
	private final ModCategory category;
	private boolean toggled;
	private final SimpleAnimation animation = new SimpleAnimation();

	public Mod(String id, String name, String description, ModCategory category) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.category = category;
	}

	public void setup() {
	}

	public void onEnable() {
		Glide.getInstance().getEventManager().register(this);
		FlaxClient.LOGGER.info("[MODULE] {} enabled", name);
	}

	public void onDisable() {
		Glide.getInstance().getEventManager().unregister(this);
		FlaxClient.LOGGER.info("[MODULE] {} disabled", name);
	}

	public void toggle() {
		setToggled(!toggled);
	}

	public void setToggled(boolean toggled) {
		if (this.toggled == toggled) {
			return;
		}
		this.toggled = toggled;
		if (toggled) {
			onEnable();
		} else {
			onDisable();
		}
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public ModCategory getCategory() {
		return category;
	}

	public boolean isToggled() {
		return toggled;
	}

	public SimpleAnimation getAnimation() {
		return animation;
	}
}
