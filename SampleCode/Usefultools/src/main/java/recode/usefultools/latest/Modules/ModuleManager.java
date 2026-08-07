/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules;

import java.util.ArrayList;
import java.util.List;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Combat.AntiBot.AntiBot;
import recode.usefultools.latest.Modules.Combat.KillAura.KillAura;
import recode.usefultools.latest.Modules.Misc.AngleFix.AngleFix;
import recode.usefultools.latest.Modules.Misc.BindBlocker.BindBlocker;
import recode.usefultools.latest.Modules.Misc.Disabler.Disabler;
import recode.usefultools.latest.Modules.Misc.RotationManager.RotationManager;
import recode.usefultools.latest.Modules.Misc.ServerRotation.ServerRotation;
import recode.usefultools.latest.Modules.Misc.ShotbowNexSound.ShotbowNexSound;
import recode.usefultools.latest.Modules.Misc.StaffDetector.StaffDetector;
import recode.usefultools.latest.Modules.Misc.ToggleSound.ToggleSound;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.Modules.Movement.Fly.Fly;
import recode.usefultools.latest.Modules.Movement.Jesus.Jesus;
import recode.usefultools.latest.Modules.Movement.NoJumpDelay.NoJumpDelay;
import recode.usefultools.latest.Modules.Movement.Speed.Speed;
import recode.usefultools.latest.Modules.Movement.Sprint.Sprint;
import recode.usefultools.latest.Modules.Movement.Velocity.Velocity;
import recode.usefultools.latest.Modules.Player.CivBreak.CivBreak;
import recode.usefultools.latest.Modules.Player.FastBreak.FastBreak;
import recode.usefultools.latest.Modules.Player.Fucker.Fucker;
import recode.usefultools.latest.Modules.Player.Regen.Regen;
import recode.usefultools.latest.Modules.Player.Scaffold.Scaffold;
import recode.usefultools.latest.Modules.Player.Timer.Timer;
import recode.usefultools.latest.Modules.Visual.Animations.Animations;
import recode.usefultools.latest.Modules.Visual.ArrayList.ArrayLists;
import recode.usefultools.latest.Modules.Visual.BlockESP.BlockESP;
import recode.usefultools.latest.Modules.Visual.ClickGui.ClickGui;
import recode.usefultools.latest.Modules.Visual.HurtCam.HurtCam;
import recode.usefultools.latest.Modules.Visual.Interface.Interface;
import recode.usefultools.latest.Modules.Visual.LevelInfo.LevelInfo;
import recode.usefultools.latest.Modules.Visual.Nametags.Nametags;
import recode.usefultools.latest.Modules.Visual.Watermark.Watermark;

public class ModuleManager {
    public final static ModuleManager INSTANCE = new ModuleManager();
    private final List<BaseModule<?>> modules = new ArrayList();

    public void init() {
        this.add(new Interface());
        this.add(new ClickGui());
        this.add(new Watermark());
        this.add(new ArrayLists());
        this.add(new HurtCam());
        this.add(new Nametags());
        this.add(new Animations());
        this.add(new BlockESP());
        this.add(new LevelInfo());
        this.add(new Fly());
        this.add(new Sprint());
        this.add(new Velocity());
        this.add(new NoJumpDelay());
        this.add(new Jesus());
        this.add(new Speed());
        this.add(new FastBreak());
        this.add(new Timer());
        this.add(new Fucker());
        this.add(new CivBreak());
        this.add(new Regen());
        this.add(new Scaffold());
        this.add(new KillAura());
        this.add(new AntiBot());
        this.add(new RotationManager());
        this.add(new ServerRotation());
        this.add(new Disabler());
        this.add(new StaffDetector());
        this.add(new ShotbowNexSound());
        this.add(new BindBlocker());
        this.add(new ToggleSound());
        this.add(new AngleFix());
    }

    private void add(BaseModule<?> m) {
        this.modules.add(m);
    }

    public List<BaseModule<?>> getModules() {
        return this.modules;
    }

    public BaseModule<?> getModuleByName(String name) {
        return this.modules.stream().filter(m -> ((ModuleHeader)m.h).name.equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public void onKey(int key) {
        if (key == 0) {
            return;
        }
        this.modules.forEach(m -> {
            if (((ModuleHeader)m.h).key == key) {
                m.toggle();
            }
        });
    }

    public void onUpdate() {
        this.modules.stream().filter(m -> ((ModuleHeader)m.h).enabled).forEach(BaseModule::onUpdate);
    }

    public void onRenderHUD() {
        this.modules.stream().filter(m -> ((ModuleHeader)m.h).enabled).forEach(BaseModule::onRenderHUD);
    }
}

