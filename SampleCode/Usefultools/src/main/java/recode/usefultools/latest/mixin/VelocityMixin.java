/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientPacketListener
 *  net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
 *  net.minecraft.world.phys.Vec3
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package recode.usefultools.latest.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import recode.usefultools.latest.Modules.ModuleManager;
import recode.usefultools.latest.Modules.Movement.Velocity.Velocity;
import recode.usefultools.latest.Modules.Movement.Velocity.Velocity_h;

@Mixin(value={ClientPacketListener.class})
public class VelocityMixin {
    @Inject(method={"handleSetEntityMotion"}, at={@At(value="HEAD")}, cancellable=true)
    private void onHandleSetEntityMotion(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
        Velocity velModule;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (packet.id() == mc.player.getId() && (velModule = (Velocity)ModuleManager.INSTANCE.getModuleByName("Velocity")) != null && ((Velocity_h)velModule.h).enabled) {
            if (velModule.shouldBypass()) {
                return;
            }
            Velocity_h vh = (Velocity_h)velModule.h;
            if (vh.mode.value == Velocity_h.Mode.FullCancel) {
                ci.cancel();
            } else if (vh.mode.value == Velocity_h.Mode.SetVel) {
                boolean rY;
                boolean rXZ;
                double vPct;
                double hPct;
                if (vh.separateOnGround.value && mc.player.onGround()) {
                    hPct = vh.horizontalOnGround.value / 100.0;
                    vPct = vh.verticalOnGround.value / 100.0;
                    rXZ = vh.resetXZOnGround.value;
                    rY = vh.resetYOnGround.value;
                } else {
                    hPct = vh.horizontal.value / 100.0;
                    vPct = vh.vertical.value / 100.0;
                    rXZ = vh.resetXZ.value;
                    rY = vh.resetY.value;
                }
                if (hPct == 0.0 && vPct == 0.0) {
                    ci.cancel();
                    return;
                }
                Vec3 motion = packet.movement();
                Vec3 currentVel = mc.player.getDeltaMovement();
                double newX = rXZ ? motion.x * hPct : currentVel.x + (motion.x - currentVel.x) * hPct;
                double newZ = rXZ ? motion.z * hPct : currentVel.z + (motion.z - currentVel.z) * hPct;
                double newY = rY ? motion.y * vPct : currentVel.y + (motion.y - currentVel.y) * vPct;
                mc.player.lerpMotion(new Vec3(newX, newY, newZ));
                ci.cancel();
            }
        }
    }
}

