/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Visual.Animations;

import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Visual.Animations.Animations_h;

public class Animations
extends BaseModule<Animations_h> {
    public static Animations instance;
    public boolean wasRightClickDown = false;

    public Animations() {
        super(new Animations_h());
        instance = this;
    }

    @Override
    public void onEnable() {
        this.wasRightClickDown = false;
    }

    @Override
    public void onDisable() {
        this.wasRightClickDown = false;
        if (Animations.mc.gameRenderer != null && Animations.mc.gameRenderer.itemInHandRenderer != null) {
            Animations.mc.gameRenderer.itemInHandRenderer.tick();
        }
    }

    @Override
    public void onUpdate() {
        if (Animations.mc.options == null) {
            return;
        }
        if (!Animations.mc.options.keyUse.isDown()) {
            this.wasRightClickDown = false;
        }
    }
}

