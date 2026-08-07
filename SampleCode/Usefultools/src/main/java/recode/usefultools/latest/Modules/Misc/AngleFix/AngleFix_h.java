/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Misc.AngleFix;

import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.setting.BoolSetting;

public class AngleFix_h
extends ModuleHeader {
    public BoolSetting invertSimple = new BoolSetting("Invert Simple", "Inverts Simple MoveFix angle sign", false);
    public BoolSetting invertSilent = new BoolSetting("Invert Silent", "Inverts Silent MoveFix relative angle sign", false);
    public BoolSetting swapSilentSides = new BoolSetting("Swap Silent Sides", "Swaps A and D keys in Silent mode", false);
    public BoolSetting swapSilentUpDown = new BoolSetting("Swap Silent UpDown", "Swaps W and S keys in Silent mode", false);
    public BoolSetting invertClientYaw = new BoolSetting("Invert ClientYaw", "Inverts ClientYaw sign in calculations", false);
    public BoolSetting invertServerYaw = new BoolSetting("Invert ServerYaw", "Inverts ServerYaw sign in calculations", false);
    public BoolSetting forceSilent = new BoolSetting("Force Silent", "Forcibly uses Silent MoveFix globally", false);
    public BoolSetting debug = new BoolSetting("Debug", "Displays calculated angles and mapped keys on screen", false);

    public AngleFix_h() {
        super("AngleFix", "Fine-tunes and debugs MoveFix angles and parameters", Category.MISC, 0, false);
        this.addSettings(this.invertSimple, this.invertSilent, this.swapSilentSides, this.swapSilentUpDown, this.invertClientYaw, this.invertServerYaw, this.forceSilent, this.debug);
    }
}

