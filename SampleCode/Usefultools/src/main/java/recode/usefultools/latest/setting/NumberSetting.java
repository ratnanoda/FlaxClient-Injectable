/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.setting;

import recode.usefultools.latest.setting.Setting;

public class NumberSetting
extends Setting {
    public double value;
    public double min;
    public double max;
    public double step;

    public NumberSetting(String name, String description, double value, double min, double max, double step) {
        super(name, description);
        this.value = value;
        this.min = min;
        this.max = max;
        this.step = step;
    }
}

