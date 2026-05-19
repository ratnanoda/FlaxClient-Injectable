package me.eldodebug.flax.management.mods;

public class StubMod extends Mod {

	private boolean warned;

	public StubMod(ModDefinition definition) {
		super(definition.id(), definition.name(), definition.description(), definition.category());
	}

	@Override
	public void onEnable() {
		super.onEnable();
		if (!warned && mc.player != null) {
			mc.player.sendMessage(
					net.minecraft.text.Text.literal("[FlaxClient] " + getName() + " is queued for 1.21.11 porting."),
					false);
			warned = true;
		}
	}
}
