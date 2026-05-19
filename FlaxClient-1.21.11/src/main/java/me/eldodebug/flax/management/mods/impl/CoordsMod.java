package me.eldodebug.flax.management.mods.impl;

import me.eldodebug.flax.management.event.EventTarget;
import me.eldodebug.flax.management.event.impl.EventRender2D;
import me.eldodebug.flax.management.mods.Mod;
import me.eldodebug.flax.management.mods.ModCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.registry.entry.RegistryEntry;

public final class CoordsMod extends Mod {

	public CoordsMod() {
		super("coords", "Coords", "Shows XYZ and biome", ModCategory.HUD);
	}

	@EventTarget
	public void onRender(EventRender2D event) {
		if (mc.player == null || mc.world == null) {
			return;
		}

		BlockPos pos = mc.player.getBlockPos();
		RegistryEntry<Biome> biomeEntry = mc.world.getBiome(pos);
		String biome = biomeEntry.matchesKey(BiomeKeys.PLAINS)
				? "plains"
				: biomeEntry.getKey().map(key -> key.getValue().getPath()).orElse("unknown");

		String text = "X: " + pos.getX() + " Y: " + pos.getY() + " Z: " + pos.getZ() + " | " + biome;
		HudRenderUtil.drawHudLine(event.getContext(), mc, 4, 32, text, 0xFF4A90E2, 0xFFFFFFFF);
	}
}
