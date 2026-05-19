package me.eldodebug.flax.injection.mixin;

import me.eldodebug.flax.core.Glide;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

	@Inject(method = "stop", at = @At("HEAD"))
	private void flax$onStop(CallbackInfo callbackInfo) {
		Glide.getInstance().stop();
	}
}
