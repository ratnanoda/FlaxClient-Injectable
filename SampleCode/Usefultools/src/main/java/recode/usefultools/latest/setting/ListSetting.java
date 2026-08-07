/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.setting;

import java.util.ArrayList;
import java.util.List;
import recode.usefultools.latest.setting.Setting;

public class ListSetting
extends Setting {
    public final List<String> value = new ArrayList<String>();

    public ListSetting(String name, String description) {
        super(name, description);
    }
}

