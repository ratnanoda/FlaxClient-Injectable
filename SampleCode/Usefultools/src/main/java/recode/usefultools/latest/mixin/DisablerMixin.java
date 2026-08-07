/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
 *  net.minecraft.network.protocol.game.ServerboundPlayerActionPacket$Action
 *  net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket
 *  net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket$Action
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package recode.usefultools.latest.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Misc.Disabler.Disabler;
import recode.usefultools.latest.Modules.Misc.Disabler.Disabler_h;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.Modules.ModuleManager;

@Mixin(value={ClientCommonPacketListenerImpl.class})
public class DisablerMixin {
    @Inject(method={"send"}, at={@At(value="HEAD")}, cancellable=true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        BaseModule<?> mod;
        if (packet instanceof ServerboundPlayerActionPacket actionPacket && actionPacket.getAction() == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                Disabler.lastStopBreakTick = mc.player.tickCount;
                BaseModule<?> mod2 = ModuleManager.INSTANCE.getModuleByName("Disabler");
                if (mod2 != null && ((ModuleHeader)mod2.h).enabled && ((Disabler_h)mod2.h).debugLog.value) {
                    mc.player.sendSystemMessage((Component)Component.literal((String)("§7[§bUT§7] §c[Event] Sent STOP_DESTROY_BLOCK (GameTick: " + mc.player.tickCount + ")")));
                }
            }
        }
        if (packet instanceof ServerboundPlayerCommandPacket commandPacket && commandPacket.getAction() == ServerboundPlayerCommandPacket.Action.START_SPRINTING && (mod = ModuleManager.INSTANCE.getModuleByName("Disabler")) != null && ((ModuleHeader)mod.h).enabled) {
            Disabler_h h = (Disabler_h)mod.h;
            if (h.sprintD.value) {
                ci.cancel();
            }
        }
    }
}

