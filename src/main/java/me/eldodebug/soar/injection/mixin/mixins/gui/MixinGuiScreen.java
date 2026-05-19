package me.eldodebug.soar.injection.mixin.mixins.gui;

import java.io.IOException;
import java.awt.Color;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.mods.impl.InternalSettingsMod;
import me.eldodebug.soar.utils.Sound;
import me.eldodebug.soar.utils.render.RenderUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen {

	@Shadow
    protected Minecraft mc;
    
	@Shadow
    public abstract void keyTyped(char typedChar, int keyCode);
    
	@Inject(method = "drawScreen", at = @At("HEAD"))
    public void preDrawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
		ensureVisibleCursorForFullscreenGui();
	}

	@Inject(method = "drawScreen", at = @At("TAIL"))
    public void postDrawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
		if(InternalSettingsMod.getInstance().getClickEffectsSetting().isToggled()) {
			Glide.getInstance().getClickEffects().drawClickEffects();
		}

		drawFullscreenCursorFallback(mouseX, mouseY);
	}
	
	@Inject(method = "mouseClicked", at = @At("HEAD"))
	public void preMouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
		if(InternalSettingsMod.getInstance().getClickEffectsSetting().isToggled()) {
			Glide.getInstance().getClickEffects().addClickEffect(mouseX, mouseY);
		}
		Sound.play("soar/audio/click.wav", true);
	}
	
	@Overwrite
    public void handleKeyboardInput() throws IOException {
        char c = Keyboard.getEventCharacter();
        
        if ((Keyboard.getEventKey() == 0 && c >= ' ') || Keyboard.getEventKeyState()) {
            this.keyTyped(c, Keyboard.getEventKey());
        }
        
        mc.dispatchKeypresses();
    }

	private void ensureVisibleCursorForFullscreenGui() {
		if(mc == null || mc.currentScreen == null || !mc.isFullScreen()) {
			return;
		}

		try {
			if(Mouse.isGrabbed()) {
				mc.mouseHelper.ungrabMouseCursor();
			}

			if(Mouse.isGrabbed()) {
				Mouse.setGrabbed(false);
			}

			Mouse.setNativeCursor(null);
		}
		catch(LWJGLException ignored) {
			try {
				Mouse.setGrabbed(false);
			}
			catch(Exception ignored2) {}
		}
	}

	private void drawFullscreenCursorFallback(int mouseX, int mouseY) {
		if(mc == null || mc.currentScreen == null || !mc.isFullScreen()) {
			return;
		}

		final int x = mouseX;
		final int y = mouseY;

		// Software cursor fallback for drivers/OS setups where native cursor can disappear
		// in exclusive fullscreen (LWJGL2 limitation).
		RenderUtils.drawRect(x, y, 1, 1, new Color(255, 255, 255, 255));
		RenderUtils.drawRect(x + 1, y + 1, 1, 1, new Color(255, 255, 255, 255));
		RenderUtils.drawRect(x + 2, y + 2, 1, 1, new Color(255, 255, 255, 255));
		RenderUtils.drawRect(x + 1, y, 1, 1, new Color(0, 0, 0, 220));
		RenderUtils.drawRect(x + 2, y + 1, 1, 1, new Color(0, 0, 0, 220));
		RenderUtils.drawRect(x + 3, y + 2, 1, 1, new Color(0, 0, 0, 220));
	}
}
