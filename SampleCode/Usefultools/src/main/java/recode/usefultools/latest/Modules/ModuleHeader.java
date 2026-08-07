/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules;

import java.util.ArrayList;
import java.util.List;
import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.setting.Setting;

public abstract class ModuleHeader {
    public final String name;
    public final String description;
    public final Category category;
    public int key;
    public boolean enabled;
    public final List<Setting> settings = new ArrayList<Setting>();

    public ModuleHeader(String name, String description, Category category, int key, boolean enabled) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.key = key;
        this.enabled = enabled;
    }

    protected void addSettings(Setting ... settings) {
        for (Setting s : settings) {
            this.settings.add(s);
        }
    }
}

