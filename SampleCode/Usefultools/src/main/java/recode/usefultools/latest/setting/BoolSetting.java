/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.setting;

import recode.usefultools.latest.setting.Setting;

public class BoolSetting
extends Setting {
    public boolean value;

    public BoolSetting(String name, String description, boolean defaultValue) {
        super(name, description);
        this.value = defaultValue;
    }
}

