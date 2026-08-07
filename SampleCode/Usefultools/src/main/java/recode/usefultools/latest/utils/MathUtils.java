/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.utils;

public class MathUtils {
    public static float lerp(float start, float end, float pct) {
        return start + (end - start) * pct;
    }

    public static float easeOutExpo(float t) {
        return t == 1.0f ? 1.0f : 1.0f - (float)Math.pow(2.0, -10.0f * t);
    }
}

