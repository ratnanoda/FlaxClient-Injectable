package me.eldodebug.soar.management.mods.impl.rearview;

import java.nio.IntBuffer;

import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.GL11;

import me.eldodebug.soar.attach.MinecraftAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;

public class RearviewCamera {

	private Minecraft mc = Minecraft.getMinecraft();
	
	private int mirrorFBO;
	private int mirrorTex;
	private int mirrorDepth;
	private long renderEndNanoTime;
	private RenderGlobalHelper mirrorRenderGlobal;
	private float fov;
	private boolean firstUpdate, recording, lockCamera;
	
	public RearviewCamera() {
        mirrorFBO = ARBFramebufferObject.glGenFramebuffers();
        mirrorTex = GL11.glGenTextures();
        mirrorDepth = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, mirrorTex);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB8, 800, 600, 0, GL11.GL_RGBA, GL11.GL_INT,
                (IntBuffer) null);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, mirrorDepth);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_DEPTH_COMPONENT, 800, 600, 0, GL11.GL_DEPTH_COMPONENT,
                GL11.GL_INT, (IntBuffer) null);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        
        mirrorRenderGlobal = new RenderGlobalHelper();
        fov = 70;
        lockCamera = true;
	}
	
    public void updateMirror() {
    	
        int w, h;
        float y, py, p, pp;
        boolean hide;
        int view, limit;
        long endTime = 0;
        
        GuiScreen currentScreen;
        
        if (!this.firstUpdate) {
            mc.renderGlobal.loadRenderers();
            this.firstUpdate = true;
        }
        
        w = mc.displayWidth;
        h = mc.displayHeight;
        y = MinecraftAccess.getRenderViewEntity(mc).rotationYaw;
        py = MinecraftAccess.getRenderViewEntity(mc).prevRotationYaw;
        p = MinecraftAccess.getRenderViewEntity(mc).rotationPitch;
        pp = MinecraftAccess.getRenderViewEntity(mc).prevRotationPitch;
        hide = mc.gameSettings.hideGUI;
        view = mc.gameSettings.thirdPersonView;
        limit = mc.gameSettings.limitFramerate;
        fov = mc.gameSettings.fovSetting;
        currentScreen = mc.currentScreen;
    	
        switchToFB();

        if (limit != 0) {
        	endTime = renderEndNanoTime;
        }

        mc.currentScreen = null;
        mc.displayHeight = 600;
        mc.displayWidth = 800;
        mc.gameSettings.hideGUI = true;
        mc.gameSettings.thirdPersonView = 0;
        mc.gameSettings.limitFramerate = 0;
        mc.gameSettings.fovSetting = fov;

        MinecraftAccess.getRenderViewEntity(mc).rotationYaw += 180;
        MinecraftAccess.getRenderViewEntity(mc).prevRotationYaw += 180;
        
        if(lockCamera) {
            MinecraftAccess.getRenderViewEntity(mc).rotationPitch = 0;
            MinecraftAccess.getRenderViewEntity(mc).prevRotationPitch = 0;
        }else {
            MinecraftAccess.getRenderViewEntity(mc).rotationPitch = -p + 18;
            MinecraftAccess.getRenderViewEntity(mc).prevRotationPitch = -pp + 18;
        }

        recording = true;
        mirrorRenderGlobal.switchTo();

        GL11.glPushAttrib(272393);
        
        mc.entityRenderer.renderWorld(MinecraftAccess.getTimer(mc).renderPartialTicks, System.nanoTime());
        mc.entityRenderer.setupOverlayRendering();
        
        if (limit != 0) {
        	renderEndNanoTime = endTime;
        }
        
        GL11.glPopAttrib();
        
        mirrorRenderGlobal.switchFrom();
        recording = false;
        
        mc.currentScreen = currentScreen;
        MinecraftAccess.getRenderViewEntity(mc).rotationYaw = y;
        MinecraftAccess.getRenderViewEntity(mc).prevRotationYaw = py;
        MinecraftAccess.getRenderViewEntity(mc).rotationPitch = p;
        MinecraftAccess.getRenderViewEntity(mc).prevRotationPitch = pp;
        mc.gameSettings.limitFramerate = limit;
        mc.gameSettings.thirdPersonView = view;
        mc.gameSettings.hideGUI = hide;
        mc.displayWidth = w;
        mc.displayHeight = h;
        mc.gameSettings.fovSetting = fov;

        switchFromFB();
    }
	
    private void switchToFB() {
    	
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
		GlStateManager.disableDepth();
		GlStateManager.disableLighting();
		
    	OpenGlHelper.glBindFramebuffer(ARBFramebufferObject.GL_DRAW_FRAMEBUFFER, mirrorFBO);
        OpenGlHelper.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER,
        		OpenGlHelper.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D,
                mirrorTex, 0);
        OpenGlHelper.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER,
        		OpenGlHelper.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D,
                mirrorDepth, 0);
    }

    private void switchFromFB() {
    	OpenGlHelper.glBindFramebuffer(ARBFramebufferObject.GL_DRAW_FRAMEBUFFER, 0);
    }
    
    public int getTexture() {
    	return mirrorTex;
    }

	public boolean isRecording() {
		return recording;
	}

	public void setFov(float fov) {
		this.fov = fov;
	}

	public void setLockCamera(boolean lockCamera) {
		this.lockCamera = lockCamera;
	}
}
