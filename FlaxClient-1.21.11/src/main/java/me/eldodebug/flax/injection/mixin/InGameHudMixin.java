package me.eldodebug.flax.injection.mixin;

import me.eldodebug.flax.management.event.impl.EventRender2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

	@Inject(method = "render", at = @At("RETURN"))
	private void flax$onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo callbackInfo) {
		new EventRender2D(context).call();
	}
}
