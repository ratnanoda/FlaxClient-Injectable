/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.phys.Vec3
 *  org.spongepowered.asm.mixin.Unique
 */
package recode.usefultools.latest.Modules.Movement.Jesus;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Unique;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Movement.Jesus.Jesus_h;

public class Jesus
extends BaseModule<Jesus_h> {
    public static Jesus instance;
    @Unique
    private int dolphinTicks = 0;

    public Jesus() {
        super(new Jesus_h());
        instance = this;
    }

    @Override
    public void onEnable() {
        this.dolphinTicks = 0;
    }

    @Override
    public void onDisable() {
        this.dolphinTicks = 0;
    }

    @Override
    public void onUpdate() {
        if (Jesus.mc.player == null || Jesus.mc.level == null) {
            return;
        }
        boolean inLiquid = Jesus.mc.player.isInWater() || Jesus.mc.player.isInLava();
        BlockPos belowPos = BlockPos.containing((double)Jesus.mc.player.getX(), (double)(Jesus.mc.player.getY() - 0.1), (double)Jesus.mc.player.getZ());
        boolean onLiquidSurface = Jesus.mc.level.getFluidState(belowPos).isSource();
        if (inLiquid || onLiquidSurface) {
            Vec3 vel = Jesus.mc.player.getDeltaMovement();
            boolean isMovingForward = Jesus.mc.options.keyUp.isDown() || Jesus.mc.options.keyDown.isDown();
            boolean isMovingSideways = Jesus.mc.options.keyLeft.isDown() || Jesus.mc.options.keyRight.isDown();
            boolean isMoving = isMovingForward || isMovingSideways;
            switch ((Jesus_h.Mode)((Object)((Jesus_h)this.h).mode.value)) {
                case Normal: {
                    if (inLiquid) {
                        Jesus.mc.player.setDeltaMovement(vel.x * ((Jesus_h)this.h).speed.value, 0.12, vel.z * ((Jesus_h)this.h).speed.value);
                        break;
                    }
                    if (!onLiquidSurface) break;
                    Jesus.mc.player.setDeltaMovement(vel.x * ((Jesus_h)this.h).speed.value, 0.0, vel.z * ((Jesus_h)this.h).speed.value);
                    break;
                }
                case OnGroundSpoof: {
                    if (inLiquid) {
                        Jesus.mc.player.setDeltaMovement(vel.x * ((Jesus_h)this.h).speed.value, 0.12, vel.z * ((Jesus_h)this.h).speed.value);
                    } else if (onLiquidSurface) {
                        Jesus.mc.player.setDeltaMovement(vel.x * ((Jesus_h)this.h).speed.value, 0.0, vel.z * ((Jesus_h)this.h).speed.value);
                    }
                    Jesus.mc.player.setOnGround(true);
                    break;
                }
                case Dolphin: {
                    if (inLiquid) {
                        if (isMoving) {
                            ++this.dolphinTicks;
                            if (this.dolphinTicks < (int)((Jesus_h)this.h).dolphinDelay.value) break;
                            Jesus.mc.player.setDeltaMovement(vel.x * ((Jesus_h)this.h).speed.value, ((Jesus_h)this.h).dolphinMotion.value, vel.z * ((Jesus_h)this.h).speed.value);
                            this.dolphinTicks = 0;
                            break;
                        }
                        this.dolphinTicks = 0;
                        break;
                    }
                    this.dolphinTicks = 0;
                }
            }
        } else {
            this.dolphinTicks = 0;
        }
    }
}

