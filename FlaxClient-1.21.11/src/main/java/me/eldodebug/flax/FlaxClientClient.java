package me.eldodebug.flax;

import me.eldodebug.flax.core.Glide;
import net.fabricmc.api.ClientModInitializer;

public class FlaxClientClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		FlaxClient.LOGGER.info("FlaxClient 1.21.11 client entrypoint loading...");
		Glide.getInstance().start();
	}
}
