/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Misc.StaffDetector;

import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.setting.NumberSetting;

public class StaffDetector_h
extends ModuleHeader {
    public NumberSetting fontSize = new NumberSetting("Font Size", "Size of the alert text", 30.0, 10.0, 100.0, 1.0);
    public NumberSetting yOffset = new NumberSetting("Y Offset", "Vertical offset of the alert screen", -100.0, -500.0, 500.0, 1.0);

    public StaffDetector_h() {
        super("StaffDetector", "Detects vanished staff members on the server", Category.MISC, 0, false);
        this.addSettings(this.fontSize, this.yOffset);
    }
}

