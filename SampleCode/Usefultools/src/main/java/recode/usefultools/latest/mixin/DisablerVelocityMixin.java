/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientPacketListener
 *  net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package recode.usefultools.latest.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import recode.usefultools.latest.Modules.Misc.Disabler.Disabler;
import recode.usefultools.latest.Modules.Misc.Disabler.Disabler_h;
import recode.usefultools.latest.Modules.ModuleManager;

@Mixin(value={ClientPacketListener.class})
public class DisablerVelocityMixin {
    @Inject(method={"handleSetEntityMotion"}, at={@At(value="HEAD")})
    private void onHandleSetEntityMotion(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
        Disabler mod;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (packet.id() == mc.player.getId() && (mod = (Disabler)ModuleManager.INSTANCE.getModuleByName("Disabler")) != null && ((Disabler_h)mod.h).enabled && ((Disabler_h)mod.h).velocityD.value) {
            int currentTick;
            if (((Disabler_h)mod.h).onGroundOnly.value && !mc.player.onGround()) {
                return;
            }
            if (((Disabler_h)mod.h).veloBypass.value == Disabler_h.VeloBypass.Break_Delay && (currentTick = mc.player.tickCount) - Disabler.lastStopBreakTick <= 1) {
                mod.hasDelayedPacket = true;
                mod.delayedMotion = packet.movement();
                return;
            }
            mod.triggerVelocityD(packet.movement());
        }
    }
}

