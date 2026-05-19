package me.eldodebug.flax.injection.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class FlaxTitleScreenMixin {

	@Inject(method = "render", at = @At("TAIL"))
	private void flax$renderBrand(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo callbackInfo) {
		TitleScreen screen = (TitleScreen) (Object) this;
		int width = screen.width;
		context.fill(width / 2 - 86, 2, width / 2 + 86, 18, 0x7F101820);
		context.fill(width / 2 - 86, 2, width / 2 - 84, 18, 0xFF2EC4B6);
		context.drawCenteredTextWithShadow(
				screen.getTextRenderer(),
				"FlaxClient 1.21.11",
				width / 2,
				6,
				0xFFFFFFFF);
	}
}
