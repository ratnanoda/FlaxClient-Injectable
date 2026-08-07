/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.client.multiplayer.ClientPacketListener
 *  net.minecraft.network.protocol.game.ClientboundSoundPacket
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package recode.usefultools.latest.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Misc.ShotbowNexSound.ShotbowNexSound;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.Modules.ModuleManager;
import recode.usefultools.latest.mixin.ClientboundSoundPacketAccessor;

@Mixin(value={ClientPacketListener.class})
public class ClientboundSoundPacketMixin {
    @Inject(method={"handleSoundEvent"}, at={@At(value="HEAD")})
    private void onHandleSoundEvent(ClientboundSoundPacket packet, CallbackInfo ci) {
        SoundEvent sound;
        BaseModule<?> mod = ModuleManager.INSTANCE.getModuleByName("ShotbowNexSound");
        if (mod != null && ((ModuleHeader)mod.h).enabled && ((sound = (SoundEvent)packet.getSound().value()).equals((Object)SoundEvents.ANVIL_PLACE) || sound.equals((Object)SoundEvents.ANVIL_LAND))) {
            float newPitch = ((ShotbowNexSound)mod).getRandomPitch();
            ((ClientboundSoundPacketAccessor)packet).setPitch(newPitch);
        }
    }
}

