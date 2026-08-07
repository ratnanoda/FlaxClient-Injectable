/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.client.multiplayer.ClientPacketListener
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package recode.usefultools.latest.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import recode.usefultools.latest.utils.CommandManager;

@Mixin(value={ClientPacketListener.class})
public class ChatCommandMixin {
    @Inject(method={"sendChat"}, at={@At(value="HEAD")}, cancellable=true)
    private void onSendChat(String message, CallbackInfo ci) {
        if (message.startsWith(".") && CommandManager.INSTANCE.handleCommand(message)) {
            ci.cancel();
        }
    }
}

