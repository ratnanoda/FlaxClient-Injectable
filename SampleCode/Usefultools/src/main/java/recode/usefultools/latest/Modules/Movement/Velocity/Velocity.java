/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.world.item.FishingRodItem
 */
package recode.usefultools.latest.Modules.Movement.Velocity;

import net.minecraft.world.item.FishingRodItem;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Movement.Velocity.Velocity_h;

public class Velocity
extends BaseModule<Velocity_h> {
    public static Velocity instance;
    public int bypassTicks = 0;
    private boolean wasRightClickDown = false;

    public Velocity() {
        super(new Velocity_h());
        instance = this;
    }

    @Override
    public void onEnable() {
        this.bypassTicks = 0;
        this.wasRightClickDown = false;
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onUpdate() {
        if (Velocity.mc.player == null) {
            return;
        }
        if (((Velocity_h)this.h).nekozoAnni.value) {
            boolean holdingRod;
            boolean bl = holdingRod = Velocity.mc.player.getMainHandItem().getItem() instanceof FishingRodItem || Velocity.mc.player.getOffhandItem().getItem() instanceof FishingRodItem;
            if (holdingRod && Velocity.mc.player.fishing != null && Velocity.mc.options.keyUse.isDown() && !this.wasRightClickDown) {
                this.bypassTicks = 5;
            }
            this.wasRightClickDown = Velocity.mc.options.keyUse.isDown();
            if (this.bypassTicks > 0) {
                --this.bypassTicks;
            }
        } else {
            this.bypassTicks = 0;
        }
    }

    public boolean shouldBypass() {
        return ((Velocity_h)this.h).enabled && ((Velocity_h)this.h).nekozoAnni.value && this.bypassTicks > 0;
    }
}

