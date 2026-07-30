package me.eldodebug.soar.utils;

import java.lang.reflect.Field;

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
    	
		if(gameSettings_ofFastRender != null) {
			try {
				if(OptifineUtils.gameSettings_ofFastRender != null && OptifineUtils.gameSettings_ofFastRender.getBoolean(mc.gameSettings)) {
					OptifineUtils.gameSettings_ofFastRender.setBoolean(mc.gameSettings, false);
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {}
		}
		
		/*
		 * Never switch VBO/FBO render paths after Minecraft has entered a
		 * world. Chunk render tasks are built for the render path that was
		 * active when they were queued. Changing useVbo here during a late
		 * attach can make Lunar upload a display-list task through the VBO
		 * uploader, whose VertexBuffer is consequently null.
		 *
		 * NanoVG works with Minecraft's existing framebuffer configuration;
		 * only OptiFine Fast Render needs to be disabled for compatibility.
		 */
    }
}
