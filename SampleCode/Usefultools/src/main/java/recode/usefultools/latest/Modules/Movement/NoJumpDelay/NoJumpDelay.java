/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Movement.NoJumpDelay;

import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Movement.NoJumpDelay.NoJumpDelay_h;
import recode.usefultools.latest.mixin.LivingEntityAccessor;

public class NoJumpDelay
extends BaseModule<NoJumpDelay_h> {
    public NoJumpDelay() {
        super(new NoJumpDelay_h());
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onUpdate() {
        if (NoJumpDelay.mc.player != null) {
            ((LivingEntityAccessor)NoJumpDelay.mc.player).setNoJumpDelay(0);
        }
    }
}

