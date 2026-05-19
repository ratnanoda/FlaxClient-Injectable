package me.eldodebug.flax.injection.mixin;

import me.eldodebug.flax.management.mods.impl.ZoomMod;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

	@Inject(method = "getFov(Lnet/minecraft/client/render/Camera;FZ)F", at = @At("RETURN"), cancellable = true)
	private void flax$applyZoomFovMultiplier(Camera camera, float tickProgress, boolean changingFov, CallbackInfoReturnable<Float> cir) {
		float multiplier = ZoomMod.getFovMultiplier();
		if (Math.abs(multiplier - 1.0F) > 0.0001F) {
			cir.setReturnValue(cir.getReturnValueF() * multiplier);
		}
	}
}
