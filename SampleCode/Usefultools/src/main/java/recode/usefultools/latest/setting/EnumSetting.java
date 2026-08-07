/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.setting;

import recode.usefultools.latest.setting.Setting;

public class EnumSetting<T extends Enum<T>>
extends Setting {
    public T value;
    public final String[] displayNames;

    public EnumSetting(String name, String description, T defaultValue, String ... displayNames) {
        super(name, description);
        this.value = defaultValue;
        this.displayNames = displayNames;
    }
}

