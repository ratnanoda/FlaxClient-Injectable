package me.eldodebug.soar.forge.render;

import me.eldodebug.soar.management.mods.impl.ShaderMod;

/** Runtime-configurable distances for the Forge-only volumetric fog. */
public final class FogSettings {

    public static final float fogStartDistance = 30.0F;
    private static volatile float fallbackFogEndDistance = 65.0F;
    private static volatile float fallbackFogDensity = 0.75F;

    private FogSettings() {
    }

    public static float getFogEndDistance() {
        ShaderMod shaderMod = ShaderMod.getInstance();
        return shaderMod == null ? fallbackFogEndDistance : shaderMod.getFogEndDistance();
    }

    public static void setFogEndDistance(float distance) {
        float clampedDistance = Math.max(40.0F, Math.min(120.0F, distance));
        fallbackFogEndDistance = clampedDistance;
        ShaderMod shaderMod = ShaderMod.getInstance();
        if (shaderMod != null) {
            shaderMod.setFogEndDistance(clampedDistance);
        }
    }

    /**
     * The common module owns this setting. Reflection keeps this small Forge
     * bridge source compatible while the common runtime is also used by the
     * Injectable build.
     */
    public static float getFogDensity() {
        ShaderMod shaderMod = ShaderMod.getInstance();
        if (shaderMod != null) {
            try {
                Object value = shaderMod.getClass().getMethod("getFogDensity").invoke(shaderMod);
                if (value instanceof Number) {
                    return clampDensity(((Number) value).floatValue());
                }
            } catch (ReflectiveOperationException ignored) {
                // The common module may not be initialized yet.
            }
        }
        return fallbackFogDensity;
    }

    public static void setFogDensity(float density) {
        float clampedDensity = clampDensity(density);
        fallbackFogDensity = clampedDensity;

        ShaderMod shaderMod = ShaderMod.getInstance();
        if (shaderMod != null) {
            try {
                shaderMod.getClass().getMethod("setFogDensity", Float.TYPE).invoke(shaderMod, clampedDensity);
            } catch (ReflectiveOperationException ignored) {
                // The value remains available as the Forge GUI fallback.
            }
        }
    }

    private static float clampDensity(float density) {
        return Math.max(0.0F, Math.min(1.0F, density));
    }
}
