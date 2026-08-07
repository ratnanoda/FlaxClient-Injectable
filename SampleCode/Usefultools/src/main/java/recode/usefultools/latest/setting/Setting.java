/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.setting;

import java.util.function.Supplier;

public abstract class Setting {
    public final String name;
    public final String description;
    public Supplier<Boolean> visibility = () -> true;

    public Setting(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public boolean isVisible() {
        return this.visibility.get();
    }
}

