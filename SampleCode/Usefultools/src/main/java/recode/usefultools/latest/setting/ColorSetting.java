/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.setting;

import recode.usefultools.latest.setting.Setting;

public class ColorSetting
extends Setting {
    public float[] rgba;

    public ColorSetting(String name, String description, int color) {
        super(name, description);
        float a = (float)(color >> 24 & 0xFF) / 255.0f;
        float r = (float)(color >> 16 & 0xFF) / 255.0f;
        float g = (float)(color >> 8 & 0xFF) / 255.0f;
        float b = (float)(color & 0xFF) / 255.0f;
        this.rgba = new float[]{r, g, b, a};
    }
}

