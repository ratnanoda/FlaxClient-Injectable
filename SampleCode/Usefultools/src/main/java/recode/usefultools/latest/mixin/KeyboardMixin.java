/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.client.KeyboardHandler
 *  net.minecraft.client.input.KeyEvent
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package recode.usefultools.latest.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import recode.usefultools.latest.Modules.Misc.BindBlocker.BindBlocker;
import recode.usefultools.latest.Modules.ModuleManager;
import recode.usefultools.latest.Modules.Visual.ClickGui.ClickGui;
import recode.usefultools.latest.Modules.Visual.ClickGui.ClickGui_h;
import recode.usefultools.latest.utils.AccountManager;

@Mixin(value={KeyboardHandler.class})
public class KeyboardMixin {
    @Inject(method={"keyPress"}, at={@At(value="HEAD")}, cancellable=true)
    private void onKey(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (ClickGui.instance != null && ((ClickGui_h)ClickGui.instance.h).enabled && event.key() == 256) {
            if (action == 1) {
                ClickGui.instance.setEnabled(false);
            }
            ci.cancel();
            return;
        }
        if (AccountManager.INSTANCE.showScreen && event.key() == 256) {
            if (action == 1) {
                AccountManager.INSTANCE.showScreen = false;
            }
            ci.cancel();
            return;
        }
        if (action == 1) {
            if (BindBlocker.shouldBlockKeybinds()) {
                return;
            }
            ModuleManager.INSTANCE.onKey(event.key());
        }
    }
}

