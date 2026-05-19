package me.eldodebug.flax;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlaxClient implements ModInitializer {

	public static final String MOD_ID = "flaxclient";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("FlaxClient 1.21.11 initialized.");
	}
}
