package me.eldodebug.soar.injection.mixin.mixins.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.eldodebug.soar.management.event.impl.EventMotionUpdate;
import me.eldodebug.soar.management.event.impl.EventSendChat;
import me.eldodebug.soar.management.event.impl.EventUpdate;
import me.eldodebug.soar.utils.player.SilentRotationManager;
import net.minecraft.client.entity.EntityPlayerSP;

@Mixin(EntityPlayerSP.class)
public class MixinEntityPlayerSP {

    private final EventUpdate glide$eventUpdate = new EventUpdate();
    private final EventMotionUpdate glide$eventMotionUpdate = new EventMotionUpdate();

    @Unique
    private boolean glide$silentRotationApplied;
    @Unique
    private float glide$cameraYaw;
    @Unique
    private float glide$cameraPitch;

    @Inject(method = "onUpdate", at = @At("HEAD"))
    public void preOnUpdate(CallbackInfo ci) {
        glide$eventUpdate.call();
    }
    
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    public void preSendChatMessage(String message, CallbackInfo ci) {
        EventSendChat event = new EventSendChat(message);
        event.call();
        
        if(event.isCancelled()) {
            ci.cancel();
        }
    }
    
    @Inject(method = "onUpdateWalkingPlayer", at = @At("HEAD"))
    private void preOnUpdateWalkingPlayer(CallbackInfo ci) {
        glide$eventMotionUpdate.call();

        if(!SilentRotationManager.isActive()) {
            return;
        }

        EntityPlayerSP player = (EntityPlayerSP) (Object) this;
        glide$cameraYaw = player.rotationYaw;
        glide$cameraPitch = player.rotationPitch;
        player.rotationYaw = SilentRotationManager.getYaw();
        player.rotationPitch = SilentRotationManager.getPitch();
        glide$silentRotationApplied = true;
    }

    @Inject(method = "onUpdateWalkingPlayer", at = @At("RETURN"))
    private void postOnUpdateWalkingPlayer(CallbackInfo ci) {
        if(!glide$silentRotationApplied) {
            return;
        }

        EntityPlayerSP player = (EntityPlayerSP) (Object) this;
        player.rotationYaw = glide$cameraYaw;
        player.rotationPitch = glide$cameraPitch;
        glide$silentRotationApplied = false;
    }
}
