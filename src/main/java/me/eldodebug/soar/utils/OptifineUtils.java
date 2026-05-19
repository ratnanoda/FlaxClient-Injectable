package me.eldodebug.soar.utils;

import java.lang.reflect.Field;

import me.eldodebug.soar.injection.mixin.GlideTweaker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;

public class OptifineUtils {

    private static Field gameSettings_ofFastRender;
    private static Minecraft mc = Minecraft.getMinecraft();
    private static long nextApplyMillis;
    
    static {
        try {
            Class.forName("Config");

            gameSettings_ofFastRender = GameSettings.class.getDeclaredField("ofFastRender");
            gameSettings_ofFastRender.setAccessible(true);
        } catch (ClassNotFoundException ignore) {
        } catch (NoSuchFieldException e) {}
    }
    
    public static void disableFastRender() {
		long now = System.currentTimeMillis();
		if(now < nextApplyMillis) {
			return;
		}
		nextApplyMillis = now + 1000L;
    	
		if(GlideTweaker.hasOptifine) {
			try {
				if(OptifineUtils.gameSettings_ofFastRender != null && OptifineUtils.gameSettings_ofFastRender.getBoolean(mc.gameSettings)) {
					OptifineUtils.gameSettings_ofFastRender.setBoolean(mc.gameSettings, false);
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {}
		}
		
		if(!mc.gameSettings.useVbo) {
			mc.gameSettings.useVbo = true;
		}
		if(!mc.gameSettings.fboEnable) {
			mc.gameSettings.fboEnable = true;
		}
    }
}
