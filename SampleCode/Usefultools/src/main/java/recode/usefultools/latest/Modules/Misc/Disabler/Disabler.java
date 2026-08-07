/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ServerboundMovePlayerPacket$Pos
 *  net.minecraft.world.phys.Vec3
 */
package recode.usefultools.latest.Modules.Misc.Disabler;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Misc.Disabler.Disabler_h;
import recode.usefultools.latest.mixin.EntityAccessor;

public class Disabler
extends BaseModule<Disabler_h> {
    public static Disabler instance;
    public static int lastStopBreakTick;
    public boolean hasDelayedPacket = false;
    public Vec3 delayedMotion = Vec3.ZERO;
    public int ticksToSend = 0;
    public Vec3 spoofVel = Vec3.ZERO;
    private Vec3 accumulatedOffset = Vec3.ZERO;

    public Disabler() {
        super(new Disabler_h());
        instance = this;
    }

    @Override
    public void onEnable() {
        this.ticksToSend = 0;
        this.spoofVel = Vec3.ZERO;
        this.accumulatedOffset = Vec3.ZERO;
        this.hasDelayedPacket = false;
        this.delayedMotion = Vec3.ZERO;
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onUpdate() {
        if (Disabler.mc.player == null) {
            return;
        }
        if (this.hasDelayedPacket && ((Disabler_h)this.h).enabled && ((Disabler_h)this.h).velocityD.value) {
            this.triggerVelocityD(this.delayedMotion);
            this.hasDelayedPacket = false;
        }
        if (this.ticksToSend > 0 && ((Disabler_h)this.h).enabled && ((Disabler_h)this.h).velocityD.value) {
            double totalTicks = ((Disabler_h)this.h).velocityTicks.value;
            Vec3 stepVel = new Vec3(this.spoofVel.x / totalTicks, this.spoofVel.y / totalTicks, this.spoofVel.z / totalTicks);
            Vec3 allowedStep = ((EntityAccessor)Disabler.mc.player).callCollide(stepVel);
            this.accumulatedOffset = this.accumulatedOffset.add(allowedStep);
            Vec3 spoofPos = Disabler.mc.player.position().add(this.accumulatedOffset);
            this.sendPosPacket(spoofPos);
            --this.ticksToSend;
        } else if (((Disabler_h)this.h).enabled && ((Disabler_h)this.h).velocityD.value && ((Disabler_h)this.h).smoothReturn.value && this.accumulatedOffset.lengthSqr() > 1.0E-4) {
            this.accumulatedOffset = this.accumulatedOffset.scale(0.1);
            Vec3 spoofPos = Disabler.mc.player.position().add(this.accumulatedOffset);
            this.sendPosPacket(spoofPos);
        } else if (this.accumulatedOffset.lengthSqr() > 0.0) {
            Disabler.mc.player.connection.send((Packet)new ServerboundMovePlayerPacket.Pos(Disabler.mc.player.position(), true, Disabler.mc.player.horizontalCollision));
            if (((Disabler_h)this.h).debugLog.value) {
                Disabler.mc.player.sendSystemMessage((Component)Component.literal((String)String.format("§7[§bUT§7] §a[Packet] Force Land -> X: %.4f, Y: %.4f, Z: %.4f (onGround: true)", Disabler.mc.player.getX(), Disabler.mc.player.getY(), Disabler.mc.player.getZ())));
            }
            this.accumulatedOffset = Vec3.ZERO;
        }
    }

    public boolean isMoving() {
        if (Disabler.mc.player == null) {
            return false;
        }
        Vec3 movement = Disabler.mc.player.getDeltaMovement();
        return movement.x * movement.x + movement.z * movement.z > 6.25E-4;
    }

    private void sendPosPacket(Vec3 pos) {
        int currentTick;
        boolean useSemi = false;
        boolean isMoving = this.isMoving();
        if (((Disabler_h)this.h).veloBypass.value == Disabler_h.VeloBypass.Semi_Full) {
            useSemi = !isMoving;
        } else if (((Disabler_h)this.h).veloBypass.value == Disabler_h.VeloBypass.Break_Semi && (currentTick = Disabler.mc.player.tickCount) - lastStopBreakTick <= 1) {
            boolean bl = useSemi = !isMoving;
        }
        if (useSemi) {
            if (((Disabler_h)this.h).debugLog.value) {
                Disabler.mc.player.sendSystemMessage((Component)Component.literal((String)String.format("§7[§bUT§7] §e[Packet] Pos (1/2) -> X: %.4f, Y: %.4f, Z: %.4f (onGround: false)", pos.x, pos.y, pos.z)));
                Disabler.mc.player.sendSystemMessage((Component)Component.literal((String)String.format("§7[§bUT§7] §e[Packet] Pos (2/2) -> X: %.4f, Y: %.4f, Z: %.4f (onGround: true)", Disabler.mc.player.getX(), Disabler.mc.player.getY(), Disabler.mc.player.getZ())));
            }
            Disabler.mc.player.connection.send((Packet)new ServerboundMovePlayerPacket.Pos(pos, false, Disabler.mc.player.horizontalCollision));
            Disabler.mc.player.connection.send((Packet)new ServerboundMovePlayerPacket.Pos(Disabler.mc.player.position(), true, Disabler.mc.player.horizontalCollision));
            this.accumulatedOffset = Vec3.ZERO;
        } else {
            if (((Disabler_h)this.h).debugLog.value) {
                Disabler.mc.player.sendSystemMessage((Component)Component.literal((String)String.format("§7[§bUT§7] §e[Packet] Pos (1/1) -> X: %.4f, Y: %.4f, Z: %.4f (onGround: false)", pos.x, pos.y, pos.z)));
            }
            Disabler.mc.player.connection.send((Packet)new ServerboundMovePlayerPacket.Pos(pos, false, Disabler.mc.player.horizontalCollision));
        }
    }

    public void triggerVelocityD(Vec3 motion) {
        if (Disabler.mc.player == null) {
            return;
        }
        double hPct = ((Disabler_h)this.h).horizontalPct.value / 100.0;
        double sX = motion.x * hPct;
        double sY = ((Disabler_h)this.h).velocityMode.value == Disabler_h.VelMode.Vertical_Only || ((Disabler_h)this.h).velocityMode.value == Disabler_h.VelMode.Both ? motion.y : 0.0;
        double sZ = motion.z * hPct;
        this.spoofVel = new Vec3(sX, sY, sZ);
        this.ticksToSend = (int)((Disabler_h)this.h).velocityTicks.value;
    }

    static {
        lastStopBreakTick = 0;
    }
}

