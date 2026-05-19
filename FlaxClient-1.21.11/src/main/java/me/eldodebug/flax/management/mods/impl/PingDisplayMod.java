package me.eldodebug.flax.management.mods.impl;

import me.eldodebug.flax.management.event.EventTarget;
import me.eldodebug.flax.management.event.impl.EventRender2D;
import me.eldodebug.flax.management.mods.Mod;
import me.eldodebug.flax.management.mods.ModCategory;
import net.minecraft.client.network.PlayerListEntry;

public final class PingDisplayMod extends Mod {

	public PingDisplayMod() {
		super("ping_display", "Ping Display", "Shows your current server latency", ModCategory.HUD);
	}

	@EventTarget
	public void onRender(EventRender2D event) {
		if (mc.player == null || mc.getNetworkHandler() == null) {
			return;
		}

		PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
		int ping = entry != null ? entry.getLatency() : 0;
		HudRenderUtil.drawHudLine(event.getContext(), mc, 4, 60, "Ping: " + ping + " ms", 0xFF7E57C2, 0xFFFFFFFF);
	}
}
