package me.eldodebug.soar.injection.mixin.mixins.gui;

import java.awt.Color;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.gui.GuiFixConnecting;
import me.eldodebug.soar.gui.altmanager.GuiAltManager;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.utils.mouse.MouseUtils;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.ServerData;

@Mixin(GuiMultiplayer.class)
public abstract class MixinGuiMultiplayer extends GuiScreen {

	private static final float ALT_BTN_W = 50F;
	private static final float ALT_BTN_H = 15F;

	@Overwrite
	private void connectToServer(ServerData server){
		mc.displayGuiScreen(new GuiFixConnecting(this, mc, server));
	}

	@Inject(method = "drawScreen", at = @At("TAIL"))
	private void flax$drawAltButton(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {

		NanoVGManager nvg = Glide.getInstance().getNanoVGManager();
		float bx = width - ALT_BTN_W - 6F;
		float by = 6F;
		boolean hover = MouseUtils.isInside(mouseX, mouseY, bx, by, ALT_BTN_W, ALT_BTN_H);
		Color accent = Glide.getInstance().getColorManager().getCurrentColor().getInterpolateColor();

		nvg.setupAndDraw(() -> {
			nvg.drawRoundedRect(bx, by, ALT_BTN_W, ALT_BTN_H, 5,
					new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), hover ? 255 : 210));
			nvg.drawCenteredText("Alts", bx + ALT_BTN_W / 2F, by + ALT_BTN_H / 2F - 3.5F, Color.WHITE, 8.5F, Fonts.MEDIUM);
		});
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void flax$altButtonClick(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {

		if(mouseButton != 0) {
			return;
		}

		float bx = width - ALT_BTN_W - 6F;
		float by = 6F;

		if(MouseUtils.isInside(mouseX, mouseY, bx, by, ALT_BTN_W, ALT_BTN_H)) {
			mc.displayGuiScreen(new GuiAltManager(this));
			ci.cancel();
		}
	}
}
