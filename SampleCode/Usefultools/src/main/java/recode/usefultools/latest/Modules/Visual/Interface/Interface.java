/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Visual.Interface;

import java.awt.Color;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Visual.Interface.Interface_h;
import recode.usefultools.latest.setting.ColorSetting;
import recode.usefultools.latest.utils.MathUtils;

public class Interface
extends BaseModule<Interface_h> {
    private final int[] transColors = new int[]{-10760454, -677448, -1, -677448};
    private final int[] bubblegumColors = new int[]{-39990, -15421, -7145985, -393324, -7864400};
    private final int[] watermelonColors = new int[]{-47546, -7667712, -7278960, -14513374, -3342388};
    private final int[] sunsetColors = new int[]{-2809856, -1083865, -26026, -1, -3054940, -4893040};
    private final int[] poisonColors = new int[]{-9183674, -12334759, -14031266, -15936941, -11021496, -12987780};

    public Interface() {
        super(new Interface_h());
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onUpdate() {
    }

    public int getCurrentColor(int index) {
        float elapsedSeconds;
        float speed = (float)((Interface_h)this.h).colorSpeed.value;
        if (((Interface_h)this.h).fix.value) {
            long rawTime = System.currentTimeMillis() % 3600000L;
            elapsedSeconds = (float)rawTime / 1000.0f * speed;
        } else {
            elapsedSeconds = (float)System.currentTimeMillis() / 1000.0f * speed;
        }
        float time = elapsedSeconds + (float)index * 0.15f;
        return switch ((Interface_h.ColorTheme)((Object)((Interface_h)this.h).theme.value)) {
            default -> throw new MatchException(null, null);
            case Interface_h.ColorTheme.Rainbow -> Color.HSBtoRGB(time * 0.1f % 1.0f, (float)((Interface_h)this.h).saturation.value, 1.0f);
            case Interface_h.ColorTheme.Trans -> this.getGradientColor(this.transColors, time);
            case Interface_h.ColorTheme.Bubblegum -> this.getGradientColor(this.bubblegumColors, time);
            case Interface_h.ColorTheme.Watermelon -> this.getGradientColor(this.watermelonColors, time);
            case Interface_h.ColorTheme.Sunset -> this.getGradientColor(this.sunsetColors, time);
            case Interface_h.ColorTheme.Poison -> this.getGradientColor(this.poisonColors, time);
            case Interface_h.ColorTheme.Custom -> {
                int[] customColors = new int[]{this.getIntFromColorSetting(((Interface_h)this.h).color1), this.getIntFromColorSetting(((Interface_h)this.h).color2)};
                yield this.getGradientColor(customColors, time);
            }
        };
    }

    private int getGradientColor(int[] colors, float time) {
        if (colors == null || colors.length == 0) {
            return -1;
        }
        if (colors.length == 1) {
            return colors[0];
        }
        float t = time % (float)colors.length;
        if (t < 0.0f) {
            t += (float)colors.length;
        }
        int index1 = (int)t;
        int index2 = (index1 + 1) % colors.length;
        float pct = t - (float)index1;
        return this.lerpColor(colors[index1], colors[index2], pct);
    }

    private int getIntFromColorSetting(ColorSetting c) {
        if (c == null || c.rgba == null || c.rgba.length != 4) {
            return -1;
        }
        int r = (int)(c.rgba[0] * 255.0f);
        int g = (int)(c.rgba[1] * 255.0f);
        int b = (int)(c.rgba[2] * 255.0f);
        int a = (int)(c.rgba[3] * 255.0f);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private int lerpColor(int c1, int c2, float pct) {
        int a = (int)MathUtils.lerp(c1 >> 24 & 0xFF, c2 >> 24 & 0xFF, pct);
        int r = (int)MathUtils.lerp(c1 >> 16 & 0xFF, c2 >> 16 & 0xFF, pct);
        int g = (int)MathUtils.lerp(c1 >> 8 & 0xFF, c2 >> 8 & 0xFF, pct);
        int b = (int)MathUtils.lerp(c1 & 0xFF, c2 & 0xFF, pct);
        return a << 24 | r << 16 | g << 8 | b;
    }
}

