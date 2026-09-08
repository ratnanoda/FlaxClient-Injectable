package me.eldodebug.soar.utils.render;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import me.eldodebug.soar.attach.MinecraftAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

/**
 * Captures the world render matrices once per 3D frame and projects world
 * coordinates to 2D screen-space (ScaledResolution) coordinates, so labels can
 * be drawn flat as a HUD overlay - always camera-aligned and never occluded.
 */
public class WorldToScreen {

	private static final Minecraft mc = Minecraft.getMinecraft();

	private static final FloatBuffer modelviewBuffer = BufferUtils.createFloatBuffer(16);
	private static final FloatBuffer projectionBuffer = BufferUtils.createFloatBuffer(16);
	private static final IntBuffer viewportBuffer = BufferUtils.createIntBuffer(16);

	private static final float[] modelview = new float[16];
	private static final float[] projection = new float[16];

	private static double viewerX, viewerY, viewerZ;
	private static boolean captured;

	// Call during the 3D world pass (EventRender3D) while the camera matrices
	// are active.
	public static void capture() {
		modelviewBuffer.clear();
		projectionBuffer.clear();
		viewportBuffer.clear();

		GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelviewBuffer);
		GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projectionBuffer);
		GL11.glGetInteger(GL11.GL_VIEWPORT, viewportBuffer);

		modelviewBuffer.get(modelview).rewind();
		projectionBuffer.get(projection).rewind();

		viewerX = MinecraftAccess.getRenderPosX(mc.getRenderManager());
		viewerY = MinecraftAccess.getRenderPosY(mc.getRenderManager());
		viewerZ = MinecraftAccess.getRenderPosZ(mc.getRenderManager());
		captured = viewportBuffer.get(2) > 0 && viewportBuffer.get(3) > 0;
	}

	// Projects an absolute world position. Returns {screenX, screenY} in
	// ScaledResolution coordinates, or null when the point is behind the camera.
	public static float[] project(double worldX, double worldY, double worldZ) {
		if(!captured) return null;

		// Vanilla/OptiFine normally leave a camera-relative modelview matrix at
		// this hook, while recent Lunar builds can leave the absolute camera
		// translation in it. Try both conventions and select the point that is
		// actually inside the current clip volume.
		float[] relative = projectCandidate(
				(float) (worldX - viewerX),
				(float) (worldY - viewerY),
				(float) (worldZ - viewerZ));
		float[] absolute = projectCandidate((float) worldX, (float) worldY, (float) worldZ);
		if(relative == null) return absolute == null ? null : new float[] { absolute[0], absolute[1] };
		if(absolute == null) return new float[] { relative[0], relative[1] };
		float[] best = relative[2] <= absolute[2] ? relative : absolute;
		return new float[] { best[0], best[1] };
	}

	private static float[] projectCandidate(float x, float y, float z) {

		// eye = modelview * (x, y, z, 1)
		float ex = modelview[0] * x + modelview[4] * y + modelview[8] * z + modelview[12];
		float ey = modelview[1] * x + modelview[5] * y + modelview[9] * z + modelview[13];
		float ez = modelview[2] * x + modelview[6] * y + modelview[10] * z + modelview[14];
		float ew = modelview[3] * x + modelview[7] * y + modelview[11] * z + modelview[15];

		// clip = projection * eye
		float cx = projection[0] * ex + projection[4] * ey + projection[8] * ez + projection[12] * ew;
		float cy = projection[1] * ex + projection[5] * ey + projection[9] * ez + projection[13] * ew;
		float cz = projection[2] * ex + projection[6] * ey + projection[10] * ez + projection[14] * ew;
		float cw = projection[3] * ex + projection[7] * ey + projection[11] * ez + projection[15] * ew;

		if(cw <= 0.0F) {
			return null;
		}

		float ndcX = cx / cw;
		float ndcY = cy / cw;
		float ndcZ = cz / cw;
		if(!Float.isFinite(ndcX) || !Float.isFinite(ndcY) || !Float.isFinite(ndcZ)
				|| Math.abs(ndcX) > 1.15F || Math.abs(ndcY) > 1.15F
				|| ndcZ < -1.1F || ndcZ > 1.1F) {
			return null;
		}

		int vpX = viewportBuffer.get(0);
		int vpY = viewportBuffer.get(1);
		int vpW = viewportBuffer.get(2);
		int vpH = viewportBuffer.get(3);

		float winX = vpX + vpW * (ndcX + 1.0F) / 2.0F;
		float winY = vpY + vpH * (ndcY + 1.0F) / 2.0F;

		int scaleFactor = new ScaledResolution(mc).getScaleFactor();
		if(scaleFactor <= 0) {
			return null;
		}

		float screenX = winX / scaleFactor;
		float screenY = (vpY + vpH - winY) / scaleFactor;

		return new float[] { screenX, screenY, Math.abs(ndcX) + Math.abs(ndcY) };
	}
}
