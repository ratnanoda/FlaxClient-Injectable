/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Visual.Animations;

import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.setting.BoolSetting;
import recode.usefultools.latest.setting.EnumSetting;
import recode.usefultools.latest.setting.NumberSetting;

public class Animations_h
extends ModuleHeader {
    public BoolSetting fluxSwing = new BoolSetting("Flux Swing", "Stops item translation bobbing during swing", true);
    public EnumSetting<BlockMode> fakeBlockMode = new EnumSetting<BlockMode>("Block Mode", "Fake sword blocking animation", BlockMode.NONE, "None", "SwordOnly", "Always");
    public EnumSetting<BlockStyleMode> blockStyleMode = new EnumSetting<BlockStyleMode>("Block Style", "Blocking swing style", BlockStyleMode.Solstice, "Solstice", "Java");
    public NumberSetting blockY = new NumberSetting("1.7Y", "Y-offset of item when blocking", 0.08, -1.0, 1.0, 0.01);
    public BoolSetting onlyOnBlock = new BoolSetting("Only On Block", "Applies custom transformations only while blocking", true);
    public NumberSetting customX = new NumberSetting("Custom X", "X-offset of the held item", 0.0, -2.0, 2.0, 0.01);
    public NumberSetting customY = new NumberSetting("Custom Y", "Y-offset of the held item", 0.0, -2.0, 2.0, 0.01);
    public NumberSetting customZ = new NumberSetting("Custom Z", "Z-offset of the held item", 0.0, -2.0, 2.0, 0.01);
    public NumberSetting customRotX = new NumberSetting("Custom Rot X", "X-axis rotation degrees", 0.0, -180.0, 180.0, 0.5);
    public NumberSetting customRotY = new NumberSetting("Custom Rot Y", "Y-axis rotation degrees", 0.0, -180.0, 180.0, 0.5);
    public NumberSetting customRotZ = new NumberSetting("Custom Rot Z", "Z-axis rotation degrees", 0.0, -180.0, 180.0, 0.5);
    public NumberSetting customScale = new NumberSetting("Custom Scale", "Scale multiplier of the held item", 1.0, 0.1, 3.0, 0.01);
    public BoolSetting noCooldown = new BoolSetting("Disable Cooldown", "Disables visual item cooldown drop", true);
    public BoolSetting oldSwap = new BoolSetting("Old Item Swap", "Triggers swap animation when durability changes", false);
    public NumberSetting swingDuration = new NumberSetting("Swing Duration", "Visual hand swing speed duration", 6.0, 1.0, 30.0, 1.0);

    public Animations_h() {
        super("Animations", "Customizes held item swing animations", Category.VISUAL, 0, false);
        this.blockStyleMode.visibility = () -> this.fakeBlockMode.value != BlockMode.NONE;
        this.blockY.visibility = () -> this.fakeBlockMode.value != BlockMode.NONE;
        this.onlyOnBlock.visibility = () -> this.fakeBlockMode.value != BlockMode.NONE;
        this.customX.visibility = () -> !this.onlyOnBlock.value || this.fakeBlockMode.value != BlockMode.NONE;
        this.customY.visibility = () -> !this.onlyOnBlock.value || this.fakeBlockMode.value != BlockMode.NONE;
        this.customZ.visibility = () -> !this.onlyOnBlock.value || this.fakeBlockMode.value != BlockMode.NONE;
        this.customRotX.visibility = () -> !this.onlyOnBlock.value || this.fakeBlockMode.value != BlockMode.NONE;
        this.customRotY.visibility = () -> !this.onlyOnBlock.value || this.fakeBlockMode.value != BlockMode.NONE;
        this.customRotZ.visibility = () -> !this.onlyOnBlock.value || this.fakeBlockMode.value != BlockMode.NONE;
        this.customScale.visibility = () -> !this.onlyOnBlock.value || this.fakeBlockMode.value != BlockMode.NONE;
        this.addSettings(this.fluxSwing, this.fakeBlockMode, this.blockStyleMode, this.blockY, this.onlyOnBlock, this.customX, this.customY, this.customZ, this.customRotX, this.customRotY, this.customRotZ, this.customScale, this.noCooldown, this.oldSwap, this.swingDuration);
    }

    public static enum BlockMode {
        NONE,
        SWORD_ONLY,
        ALWAYS;

    }

    public static enum BlockStyleMode {
        Solstice,
        Java;

    }
}

