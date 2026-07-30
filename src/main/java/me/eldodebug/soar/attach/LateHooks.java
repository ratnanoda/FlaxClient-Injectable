package me.eldodebug.soar.attach;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.IntBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.gui.GuiEditHUD;
import me.eldodebug.soar.gui.modmenu.GuiModMenu;
import me.eldodebug.soar.gui.mainmenu.GuiGlideMainMenu;
import me.eldodebug.soar.hooks.RenderEntityItemHook;
import me.eldodebug.soar.logger.GlideLogger;
import me.eldodebug.soar.management.event.impl.EventClickMouse;
import me.eldodebug.soar.management.event.impl.EventCameraRotation;
import me.eldodebug.soar.management.event.impl.EventDamageEntity;
import me.eldodebug.soar.management.event.impl.EventAttackEntity;
import me.eldodebug.soar.management.event.impl.EventBlockHighlightRender;
import me.eldodebug.soar.management.event.impl.EventFovUpdate;
import me.eldodebug.soar.management.event.impl.EventFireOverlay;
import me.eldodebug.soar.management.event.impl.EventGamma;
import me.eldodebug.soar.management.event.impl.EventHurtCamera;
import me.eldodebug.soar.management.event.impl.EventHitOverlay;
import me.eldodebug.soar.management.event.impl.EventJump;
import me.eldodebug.soar.management.event.impl.EventKey;
import me.eldodebug.soar.management.event.impl.EventLivingUpdate;
import me.eldodebug.soar.management.event.impl.EventLeaveServer;
import me.eldodebug.soar.management.event.impl.EventLoadWorld;
import me.eldodebug.soar.management.event.impl.EventLocationCape;
import me.eldodebug.soar.management.event.impl.EventLocationSkin;
import me.eldodebug.soar.management.event.impl.EventMotionUpdate;
import me.eldodebug.soar.management.event.impl.EventPlaySound;
import me.eldodebug.soar.management.event.impl.EventPlayerHeadRotation;
import me.eldodebug.soar.management.event.impl.EventPreRenderTick;
import me.eldodebug.soar.management.event.impl.EventPreRenderChunk;
import me.eldodebug.soar.management.event.impl.EventReceivePacket;
import me.eldodebug.soar.management.event.impl.EventRender2D;
import me.eldodebug.soar.management.event.impl.EventRender3D;
import me.eldodebug.soar.management.event.impl.EventRenderCrosshair;
import me.eldodebug.soar.management.event.impl.EventRenderChunkPosition;
import me.eldodebug.soar.management.event.impl.EventRenderDamageTint;
import me.eldodebug.soar.management.event.impl.EventRenderExpBar;
import me.eldodebug.soar.management.event.impl.EventRenderHitbox;
import me.eldodebug.soar.management.event.impl.EventRenderNotification;
import me.eldodebug.soar.management.event.impl.EventRendererLivingEntity;
import me.eldodebug.soar.management.event.impl.EventRenderPlayer;
import me.eldodebug.soar.management.event.impl.EventRenderItemInFirstPerson;
import me.eldodebug.soar.management.event.impl.EventRenderPlayerStats;
import me.eldodebug.soar.management.event.impl.EventRenderPumpkinOverlay;
import me.eldodebug.soar.management.event.impl.EventRenderScoreboard;
import me.eldodebug.soar.management.event.impl.EventRenderSelectedItem;
import me.eldodebug.soar.management.event.impl.EventRenderTooltip;
import me.eldodebug.soar.management.event.impl.EventRenderVisualizer;
import me.eldodebug.soar.management.event.impl.EventRenderTick;
import me.eldodebug.soar.management.event.impl.EventRenderTNT;
import me.eldodebug.soar.management.event.impl.EventScrollMouse;
import me.eldodebug.soar.management.event.impl.EventSendChat;
import me.eldodebug.soar.management.event.impl.EventSendPacket;
import me.eldodebug.soar.management.event.impl.EventShader;
import me.eldodebug.soar.management.event.impl.EventSwitchTexture;
import me.eldodebug.soar.management.event.impl.EventTick;
import me.eldodebug.soar.management.event.impl.EventText;
import me.eldodebug.soar.management.event.impl.EventToggleFullscreen;
import me.eldodebug.soar.management.event.impl.EventUpdate;
import me.eldodebug.soar.management.event.impl.EventUpdateDisplay;
import me.eldodebug.soar.management.event.impl.EventUpdateFramebufferSize;
import me.eldodebug.soar.management.event.impl.EventWaterOverlay;
import me.eldodebug.soar.management.event.impl.EventZoomFov;
import me.eldodebug.soar.management.mods.impl.SlowSwingMod;
import me.eldodebug.soar.management.mods.impl.WaveyCapesMod;
import me.eldodebug.soar.management.mods.impl.FPSLimiterMod;
import me.eldodebug.soar.management.mods.impl.FPSSpooferMod;
import me.eldodebug.soar.management.mods.impl.HitDelayFixMod;
import me.eldodebug.soar.management.mods.impl.TimeChangerMod;
import me.eldodebug.soar.management.mods.impl.WeatherChangerMod;
import me.eldodebug.soar.management.mods.impl.ClearGlassMod;
import me.eldodebug.soar.management.mods.impl.ClientSpooferMod;
import me.eldodebug.soar.management.mods.impl.DamageTiltMod;
import me.eldodebug.soar.management.mods.impl.FPSBoostMod;
import me.eldodebug.soar.management.mods.impl.SoundSubtitlesMod;
import me.eldodebug.soar.management.mods.impl.AsyncScreenshotMod;
import me.eldodebug.soar.management.mods.impl.GlintColorMod;
import me.eldodebug.soar.management.mods.impl.Items2DMod;
import me.eldodebug.soar.management.mods.impl.ShinyPotsMod;
import me.eldodebug.soar.management.mods.impl.TabEditorMod;
import me.eldodebug.soar.management.mods.impl.EarsMod;
import me.eldodebug.soar.management.mods.impl.FemaleGenderMod;
import me.eldodebug.soar.management.mods.impl.GhostNametagsMod;
import me.eldodebug.soar.management.mods.impl.GhostFreelookMod;
import me.eldodebug.soar.management.mods.impl.NametagMod;
import me.eldodebug.soar.management.mods.impl.RawInputMod;
import me.eldodebug.soar.management.mods.impl.asyncscreenshot.AsyncScreenshots;
import me.eldodebug.soar.management.language.TranslateText;
import net.minecraft.block.BlockGlass;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.RenderTNTPrimed;
import net.minecraft.client.renderer.entity.layers.LayerDeadmau5Head;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MouseHelper;
import net.minecraft.scoreboard.ScoreObjective;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.Display;

/**
 * Schema-preserving hook targets called from retransformed Minecraft methods.
 * State lives here instead of being added to already-loaded Minecraft classes.
 */
public final class LateHooks {

    private static final AtomicBoolean CORE_HOOK_REPORTED = new AtomicBoolean();
    private static final ThreadLocal<float[]> CAMERA_STATE = new ThreadLocal<float[]>();
    private static final ThreadLocal<float[]> HIT_OVERLAY_STATE =
            new ThreadLocal<float[]>();
    private static IntBuffer screenshotPixelBuffer;
    private static Method renderModelColoredMethod;
    private static Method droppedItemCountMethod;
    private static final Map<ModelPlayer, ModelRenderer> FEMALE_MODELS =
            Collections.synchronizedMap(
                    new WeakHashMap<ModelPlayer, ModelRenderer>());
    private static final EventTick EVENT_TICK = new EventTick();
    private static final EventUpdate EVENT_UPDATE = new EventUpdate();
    private static final EventMotionUpdate EVENT_MOTION_UPDATE = new EventMotionUpdate();
    private static final EventPreRenderTick EVENT_PRE_RENDER_TICK = new EventPreRenderTick();
    private static final EventRenderTick EVENT_RENDER_TICK = new EventRenderTick();
    private static final EventUpdateDisplay EVENT_UPDATE_DISPLAY = new EventUpdateDisplay();
    private static final EventUpdateFramebufferSize EVENT_UPDATE_FRAMEBUFFER_SIZE =
            new EventUpdateFramebufferSize();

    private LateHooks() {
    }

    public static void onClientTick() {
        if (CORE_HOOK_REPORTED.compareAndSet(false, true)) {
            GlideLogger.info("FlaxClient late-load tick hook is active");
        }

        if (!LateLoadStatus.isTransformerReady()) {
            return;
        }

        AttachBootstrap.startClient();
        if (Glide.getInstance().getEventManager() != null && Display.isActive()) {
            EVENT_TICK.call();
        }
    }

    public static void onKeyEvent() {
        if (!isRuntimeReady()) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (Keyboard.getEventKeyState() && minecraft.currentScreen == null) {
            new EventKey(
                    Keyboard.getEventKey() == 0
                            ? Keyboard.getEventCharacter() + 256
                            : Keyboard.getEventKey())
                    .call();
        }
    }

    public static void onPreRenderTick() {
        if (isRuntimeReady()) {
            EVENT_PRE_RENDER_TICK.call();
        }
    }

    public static void onRenderTick() {
        if (isRuntimeReady()) {
            EVENT_RENDER_TICK.call();
        }
    }

    public static void onUpdateDisplay() {
        if (isRuntimeReady()) {
            EVENT_UPDATE_DISPLAY.call();
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft.currentScreen != null
                    && !(minecraft.currentScreen instanceof GuiModMenu)
                    && !(minecraft.currentScreen instanceof GuiEditHUD)
                    && !(minecraft.currentScreen instanceof GuiGlideMainMenu)) {
                new EventRenderNotification().call();
            }
        }
    }

    public static void onUpdateFramebufferSize() {
        if (isRuntimeReady()) {
            EVENT_UPDATE_FRAMEBUFFER_SIZE.call();
        }
    }

    public static int onScrollMouse(int wheel) {
        if (!isRuntimeReady() || wheel == 0) {
            return wheel;
        }
        EventScrollMouse event = new EventScrollMouse(wheel);
        event.call();
        return event.isCancelled() ? 0 : wheel;
    }

    public static boolean onLeftClick() {
        HitDelayFixMod hitDelay = HitDelayFixMod.getInstance();
        if (isRuntimeReady() && hitDelay != null && hitDelay.isToggled()) {
            MinecraftAccess.setLeftClickCounter(Minecraft.getMinecraft(), 0);
        }
        return callClickEvent(0);
    }

    public static boolean onRightClick() {
        return callClickEvent(1);
    }

    public static boolean onToggleFullscreen() {
        if (!isRuntimeReady()) {
            return false;
        }
        EventToggleFullscreen event =
                new EventToggleFullscreen(!Minecraft.getMinecraft().isFullScreen());
        event.call();
        return event.isCancelled() || !event.isApplyState();
    }

    public static void onLoadWorld() {
        if (isRuntimeReady()) {
            new EventLoadWorld().call();
        }
    }

    public static int onLimitFramerate(int vanillaValue) {
        if (!isRuntimeReady()) {
            return vanillaValue;
        }
        FPSLimiterMod limiter = FPSLimiterMod.getInstance();
        if (limiter == null || !limiter.isToggled()) {
            return vanillaValue;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.currentScreen == null
                && limiter.getLimitMaxFpsSetting().isToggled()) {
            return limiter.getMaxFpsSetting().getValueInt();
        }
        if (minecraft.currentScreen != null
                && limiter.getLimitGuiFps().isToggled()) {
            return limiter.getGuiFpsSetting().getValueInt();
        }
        return vanillaValue;
    }

    public static boolean onFramerateLimitBelowMax(boolean vanillaValue) {
        if (!isRuntimeReady()) {
            return vanillaValue;
        }
        FPSLimiterMod limiter = FPSLimiterMod.getInstance();
        return limiter != null
                && limiter.isToggled()
                && limiter.getLimitMaxFpsSetting().isToggled()
                || vanillaValue;
    }

    public static int onDebugFps(int vanillaValue) {
        if (!isRuntimeReady()) {
            return vanillaValue;
        }
        FPSSpooferMod spoofer = FPSSpooferMod.getInstance();
        return spoofer != null && spoofer.isToggled()
                ? vanillaValue * spoofer.getMultiplierSetting().getValueInt()
                : vanillaValue;
    }

    public static long onWorldTime(long vanillaValue) {
        if (!isRuntimeReady()) {
            return vanillaValue;
        }
        TimeChangerMod timeChanger = TimeChangerMod.getInstance();
        return timeChanger != null && timeChanger.isToggled()
                ? (long) (timeChanger.getTimeSetting().getValueFloat() * 1000L)
                        + 18000L
                : vanillaValue;
    }

    public static boolean onIsRaining(boolean vanillaValue) {
        WeatherChangerMod weather = activeWeatherChanger();
        return weather == null
                ? vanillaValue
                : !weatherOptionEquals(weather, TranslateText.CLEAR);
    }

    public static boolean onIsThundering(boolean vanillaValue) {
        WeatherChangerMod weather = activeWeatherChanger();
        return weather == null
                ? vanillaValue
                : weatherOptionEquals(weather, TranslateText.STORM);
    }

    public static float onRainStrength(float vanillaValue) {
        WeatherChangerMod weather = activeWeatherChanger();
        if (weather == null) {
            return vanillaValue;
        }
        return weatherOptionEquals(weather, TranslateText.CLEAR)
                ? 0.0F
                : weather.getRainStrength().getValueFloat();
    }

    public static float onThunderStrength(float vanillaValue) {
        WeatherChangerMod weather = activeWeatherChanger();
        if (weather == null) {
            return vanillaValue;
        }
        return weatherOptionEquals(weather, TranslateText.STORM)
                ? weather.getThunderStrength().getValueFloat()
                : 0.0F;
    }

    public static float onTemperatureAtHeight(float vanillaValue) {
        WeatherChangerMod weather = activeWeatherChanger();
        return weather != null && weatherOptionEquals(weather, TranslateText.SNOW)
                ? 0.0F
                : vanillaValue;
    }

    public static void onSetVelocity(
            Object entityObject,
            double x,
            double y,
            double z) {
        if (!isRuntimeReady() || !(entityObject instanceof Entity)) {
            return;
        }
        DamageTiltMod mod = DamageTiltMod.getInstance();
        Minecraft minecraft = Minecraft.getMinecraft();
        Entity entity = (Entity) entityObject;
        if (mod == null
                || !mod.isToggled()
                || minecraft.thePlayer == null
                || entity != minecraft.thePlayer) {
            return;
        }

        float result = (float) (Math.atan2(
                minecraft.thePlayer.motionZ - z,
                minecraft.thePlayer.motionX - x)
                * (180.0D / Math.PI)
                - minecraft.thePlayer.rotationYaw);
        if (Float.isFinite(result)) {
            minecraft.thePlayer.attackedAtYaw = result;
        }
    }

    public static void onSoundManagerPlay(Object soundObject) {
        if (!isRuntimeReady() || !(soundObject instanceof ISound)) {
            return;
        }
        SoundSubtitlesMod mod = SoundSubtitlesMod.getInstance();
        if (mod != null) {
            mod.soundPlay((ISound) soundObject);
        }
    }

    public static void onNextChunkUpdate() {
        if (!isRuntimeReady()) {
            return;
        }
        FPSBoostMod mod = FPSBoostMod.getInstance();
        if (mod == null
                || !mod.isToggled()
                || !mod.getChunkDelaySetting().isToggled()) {
            return;
        }
        try {
            Thread.sleep(mod.getDelaySetting().getValueLong() * 15L);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    public static boolean onShouldSideBeRendered(
            boolean vanillaValue,
            Object block) {
        if (!isRuntimeReady()) {
            return vanillaValue;
        }
        ClearGlassMod mod = ClearGlassMod.getInstance();
        if (mod == null || !mod.isToggled()) {
            return vanillaValue;
        }
        if (block instanceof BlockGlass && mod.getNormalSetting().isToggled()) {
            return false;
        }
        if (block instanceof BlockStainedGlass
                && mod.getStainedSetting().isToggled()) {
            return false;
        }
        return vanillaValue;
    }

    public static Object onAsyncScreenshot(
            File gameDirectory,
            String screenshotName,
            int width,
            int height,
            Object framebufferObject) {
        if (!isRuntimeReady() || !(framebufferObject instanceof Framebuffer)) {
            return null;
        }
        AsyncScreenshotMod mod = AsyncScreenshotMod.getInstance();
        if (mod == null || !mod.isToggled()) {
            return null;
        }

        Framebuffer framebuffer = (Framebuffer) framebufferObject;
        if (OpenGlHelper.isFramebufferEnabled()) {
            width = framebuffer.framebufferTextureWidth;
            height = framebuffer.framebufferTextureHeight;
        }

        int pixelCount = width * height;
        if (screenshotPixelBuffer == null
                || screenshotPixelBuffer.capacity() < pixelCount) {
            screenshotPixelBuffer = BufferUtils.createIntBuffer(pixelCount);
        }
        int[] pixels = new int[pixelCount];
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        screenshotPixelBuffer.clear();

        if (OpenGlHelper.isFramebufferEnabled()) {
            GlStateManager.bindTexture(framebuffer.framebufferTexture);
            GL11.glGetTexImage(
                    GL11.GL_TEXTURE_2D,
                    0,
                    GL12.GL_BGRA,
                    GL12.GL_UNSIGNED_INT_8_8_8_8_REV,
                    screenshotPixelBuffer);
        } else {
            GL11.glReadPixels(
                    0,
                    0,
                    width,
                    height,
                    GL12.GL_BGRA,
                    GL12.GL_UNSIGNED_INT_8_8_8_8_REV,
                    screenshotPixelBuffer);
        }
        screenshotPixelBuffer.get(pixels);
        new AsyncScreenshots(width, height, pixels).start();
        return new ChatComponentText("Capturing screenshot...");
    }

    public static Object onTabPlayerHead(Object player) {
        return showTabHeads() ? player : null;
    }

    public static boolean onTabShowHeads(boolean vanillaValue) {
        return vanillaValue && showTabHeads();
    }

    public static int onTabBackgroundColor(int vanillaColor) {
        if (!isRuntimeReady()) {
            return vanillaColor;
        }
        TabEditorMod mod = TabEditorMod.getInstance();
        return mod != null
                        && mod.isToggled()
                        && !mod.getBackgroundSetting().isToggled()
                ? 0
                : vanillaColor;
    }

    public static void onRenderItemGui() {
        GlStateManager.enableDepth();
    }

    public static void renderItemEffect(
            Object rendererObject,
            Object modelObject) {
        if (!(rendererObject instanceof RenderItem)
                || !(modelObject instanceof IBakedModel)) {
            return;
        }
        RenderItem renderer = (RenderItem) rendererObject;
        IBakedModel model = (IBakedModel) modelObject;

        int color = -8372020;
        GlintColorMod glint = GlintColorMod.getInstance();
        if (isRuntimeReady() && glint != null && glint.isToggled()) {
            color = glint.getGlintColor().getRGB();
        }
        ShinyPotsMod shiny = ShinyPotsMod.getInstance();
        boolean shinyPots =
                isRuntimeReady() && shiny != null && shiny.isToggled();

        GlStateManager.depthMask(false);
        if (!shinyPots) {
            GlStateManager.depthFunc(514);
        }
        GlStateManager.disableLighting();
        GlStateManager.blendFunc(768, 1);
        Minecraft.getMinecraft().getTextureManager().bindTexture(
                new ResourceLocation(
                        "textures/misc/enchanted_item_glint.png"));
        GlStateManager.matrixMode(5890);
        GlStateManager.pushMatrix();
        GlStateManager.scale(8.0F, 8.0F, 8.0F);
        float offset =
                (float) (Minecraft.getSystemTime() % 3000L)
                        / 3000.0F
                        / 8.0F;
        GlStateManager.translate(offset, 0.0F, 0.0F);
        GlStateManager.rotate(-50.0F, 0.0F, 0.0F, 1.0F);
        renderModelColored(renderer, model, color);
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        GlStateManager.scale(8.0F, 8.0F, 8.0F);
        float reverseOffset =
                (float) (Minecraft.getSystemTime() % 4873L)
                        / 4873.0F
                        / 8.0F;
        GlStateManager.translate(-reverseOffset, 0.0F, 0.0F);
        GlStateManager.rotate(10.0F, 0.0F, 0.0F, 1.0F);
        renderModelColored(renderer, model, color);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(5888);
        GlStateManager.blendFunc(770, 771);
        GlStateManager.enableLighting();
        if (!shinyPots) {
            GlStateManager.depthFunc(515);
        }
        GlStateManager.depthMask(true);
        Minecraft.getMinecraft().getTextureManager().bindTexture(
                TextureMap.locationBlocksTexture);
    }

    public static int positionDroppedItem(
            Object rendererObject,
            Object entityObject,
            double x,
            double y,
            double z,
            float partialTicks,
            Object modelObject) {
        RenderEntityItem renderer = (RenderEntityItem) rendererObject;
        EntityItem entity = (EntityItem) entityObject;
        IBakedModel model = (IBakedModel) modelObject;
        return RenderEntityItemHook.func_177077_a(
                entity,
                x,
                y,
                z,
                partialTicks,
                model,
                getDroppedItemCount(renderer, entity.getEntityItem()));
    }

    public static void renderDroppedItem(
            Object rendererObject,
            Object stackObject,
            Object modelObject) {
        RenderItem renderer = (RenderItem) rendererObject;
        ItemStack stack = (ItemStack) stackObject;
        IBakedModel model = (IBakedModel) modelObject;
        Items2DMod mod = Items2DMod.getInstance();
        if (isRuntimeReady()
                && mod != null
                && mod.isToggled()
                && !model.isGui3d()) {
            RenderEntityItemHook.oldItemRender(renderer, model, stack);
        } else {
            renderer.renderItem(stack, model);
        }
    }

    public static void onRender2D(float partialTicks) {
        if (isRuntimeReady()) {
            Minecraft minecraft = Minecraft.getMinecraft();
            new EventRenderVisualizer(partialTicks).call();
            new EventRenderDamageTint(partialTicks).call();
            if (!(minecraft.currentScreen instanceof GuiEditHUD)) {
                new EventRender2D(partialTicks).call();
                if (minecraft.currentScreen == null) {
                    new EventRenderNotification().call();
                }
            }
        }
    }

    public static void onRender3D(float partialTicks) {
        if (isRuntimeReady()) {
            new EventRender3D(partialTicks).call();
        }
    }

    public static void renderBlockHighlight(
            Object renderGlobal,
            Object player,
            Object hit,
            int subId,
            float partialTicks) {
        if (renderGlobal instanceof RenderGlobal
                && player instanceof EntityPlayer
                && hit instanceof MovingObjectPosition) {
            EventBlockHighlightRender event =
                    new EventBlockHighlightRender(
                            (MovingObjectPosition) hit, partialTicks);
            if (isRuntimeReady()) {
                event.call();
            }
            if (!event.isCancelled()) {
                ((RenderGlobal) renderGlobal).drawSelectionBox(
                        (EntityPlayer) player,
                        (MovingObjectPosition) hit,
                        subId,
                        partialTicks);
            }
        }
    }

    public static void onPlayerUpdate() {
        if (isRuntimeReady()) {
            EVENT_UPDATE.call();
        }
    }

    public static void onMotionUpdate() {
        if (isRuntimeReady()) {
            EVENT_MOTION_UPDATE.call();
        }
    }

    public static boolean onSendChat(String message) {
        if (!isRuntimeReady()) {
            return false;
        }
        EventSendChat event = new EventSendChat(message);
        event.call();
        return event.isCancelled();
    }

    public static boolean onSendPacket(Object packet) {
        if (!isRuntimeReady() || !(packet instanceof Packet)) {
            return false;
        }
        applyClientBrand((Packet<?>) packet);
        EventSendPacket event = new EventSendPacket((Packet<?>) packet);
        event.call();
        return event.isCancelled();
    }

    public static boolean onReceivePacket(Object packet) {
        if (!isRuntimeReady() || !(packet instanceof Packet)) {
            return false;
        }
        EventReceivePacket event = new EventReceivePacket((Packet<?>) packet);
        event.call();
        return event.isCancelled();
    }

    public static void onAttackEntity(Object entity) {
        if (isRuntimeReady() && entity instanceof Entity
                && ((Entity) entity).canAttackWithItem()) {
            new EventAttackEntity((Entity) entity).call();
        }
    }

    public static void onJump() {
        if (isRuntimeReady()) {
            new EventJump().call();
        }
    }

    public static void onEntityPlayerTick(Object player) {
        if (isRuntimeReady() && player instanceof EntityPlayer) {
            PlayerState.simulate((EntityPlayer) player);
        }
    }

    public static void onLivingUpdate(Object entity) {
        if (isRuntimeReady() && entity instanceof EntityLivingBase) {
            new EventLivingUpdate((EntityLivingBase) entity).call();
        }
    }

    public static int onArmSwingAnimation(int vanillaValue) {
        if (!isRuntimeReady()) {
            return vanillaValue;
        }
        SlowSwingMod mod = SlowSwingMod.getInstance();
        return mod != null && mod.isToggled()
                ? mod.getDelaySetting().getValueInt()
                : vanillaValue;
    }

    public static float onFovModifier(float vanillaValue, Object player) {
        if (!isRuntimeReady() || !(player instanceof AbstractClientPlayer)) {
            return vanillaValue;
        }
        EventFovUpdate event =
                new EventFovUpdate((AbstractClientPlayer) player, vanillaValue);
        event.call();
        return event.getFov();
    }

    public static Object onLocationSkin(Object vanillaValue, Object player) {
        if (!isRuntimeReady() || !(player instanceof AbstractClientPlayer)) {
            return vanillaValue;
        }
        EventLocationSkin event = new EventLocationSkin(
                MinecraftAccess.getPlayerInfo((AbstractClientPlayer) player));
        event.call();
        return event.isCancelled() && event.getSkin() != null
                ? event.getSkin()
                : vanillaValue;
    }

    public static Object onLocationCape(Object vanillaValue, Object player) {
        if (!isRuntimeReady() || !(player instanceof AbstractClientPlayer)) {
            return vanillaValue;
        }
        EventLocationCape event = new EventLocationCape(
                MinecraftAccess.getPlayerInfo((AbstractClientPlayer) player));
        event.call();
        return event.isCancelled()
                ? event.getCape()
                : vanillaValue;
    }

    public static boolean onRenderCrosshair(boolean vanillaValue) {
        if (!isRuntimeReady()) {
            return vanillaValue;
        }
        EventRenderCrosshair event = new EventRenderCrosshair();
        event.call();
        return vanillaValue && !event.isCancelled();
    }

    public static void onRenderPlayerStats() {
        if (!isRuntimeReady()) {
            return;
        }
        Entity renderEntity = Minecraft.getMinecraft().getRenderViewEntity();
        if (renderEntity instanceof EntityPlayer && renderEntity.ridingEntity == null) {
            new EventRenderPlayerStats().call();
        }
    }

    public static boolean onRenderPumpkinOverlay() {
        if (!isRuntimeReady()) {
            return false;
        }
        EventRenderPumpkinOverlay event = new EventRenderPumpkinOverlay();
        event.call();
        return event.isCancelled();
    }

    public static boolean onRenderTooltip(float partialTicks) {
        if (!isRuntimeReady()) {
            return false;
        }
        EventRenderTooltip event = new EventRenderTooltip(partialTicks);
        event.call();
        return event.isCancelled();
    }

    public static boolean onRenderExpBar() {
        if (!isRuntimeReady()) {
            return false;
        }
        EventRenderExpBar event = new EventRenderExpBar();
        event.call();
        return event.isCancelled();
    }

    public static boolean onRenderScoreboard(Object objective) {
        if (!isRuntimeReady() || !(objective instanceof ScoreObjective)) {
            return false;
        }
        EventRenderScoreboard event =
                new EventRenderScoreboard((ScoreObjective) objective);
        event.call();
        return event.isCancelled();
    }

    public static void onRenderSelectedItem(Object gui) {
        if (!isRuntimeReady() || !(gui instanceof GuiIngame)) {
            return;
        }
        GuiIngame guiIngame = (GuiIngame) gui;
        int ticks = MinecraftAccess.getRemainingHighlightTicks(guiIngame);
        if (ticks <= 0 || MinecraftAccess.getHighlightingItemStack(guiIngame) == null) {
            return;
        }
        int alpha = Math.min(255, (int) ((float) ticks * 256.0F / 10.0F));
        new EventRenderSelectedItem(0xFFFFFF + (alpha << 24)).call();
    }

    public static boolean onRenderVanillaCape() {
        return isRuntimeReady()
                && WaveyCapesMod.getInstance() != null
                && WaveyCapesMod.getInstance().isToggled();
    }

    public static void onModelPlayerRender(
            Object modelObject,
            Object entityObject,
            float scale) {
        if (!isRuntimeReady()
                || !(modelObject instanceof ModelPlayer)
                || !(entityObject instanceof Entity)) {
            return;
        }
        FemaleGenderMod mod = FemaleGenderMod.getInstance();
        Entity entity = (Entity) entityObject;
        if (mod == null
                || !mod.isToggled()
                || entity != Minecraft.getMinecraft().thePlayer) {
            return;
        }

        ModelPlayer model = (ModelPlayer) modelObject;
        ModelRenderer female = FEMALE_MODELS.get(model);
        if (female == null) {
            female = new ModelRenderer(model, 16, 20);
            female.addBox(-4.0F, -1.5F, -5.0F, 8, 4, 4, 0.0F);
            FEMALE_MODELS.put(model, female);
        }
        female.showModel = true;
        female.offsetY = entity.isSneaking() ? 0.25F : 0.0F;
        female.offsetZ = entity.isSneaking() ? 0.1F : 0.0F;
        female.rotateAngleX = 45.0F;
        GlStateManager.pushMatrix();
        female.render(scale);
        GlStateManager.popMatrix();
    }

    public static void renderEars(
            Object layerObject,
            Object playerObject,
            float partialTicks) {
        if (!isRuntimeReady()
                || !(layerObject instanceof LayerDeadmau5Head)
                || !(playerObject instanceof AbstractClientPlayer)) {
            return;
        }
        EarsMod mod = EarsMod.getInstance();
        AbstractClientPlayer player =
                (AbstractClientPlayer) playerObject;
        if (mod == null
                || !mod.isToggled()
                || player != Minecraft.getMinecraft().thePlayer
                || player.isInvisible()) {
            return;
        }
        RenderPlayer renderer = MinecraftAccess.getDeadmau5PlayerRenderer(
                (LayerDeadmau5Head) layerObject);
        EarsMod.drawLeft(player, partialTicks, renderer);
        EarsMod.drawRight(player, partialTicks, renderer);
    }

    public static boolean onRawMouse(Object helperObject) {
        if (!isRuntimeReady() || !(helperObject instanceof MouseHelper)) {
            return false;
        }
        RawInputMod mod = RawInputMod.getInstance();
        if (mod == null
                || !mod.isToggled()
                || !org.lwjgl.input.Mouse.isGrabbed()
                || !mod.isAvailable()
                || mod.getThread() == null) {
            return false;
        }
        MouseHelper helper = (MouseHelper) helperObject;
        helper.deltaX = (int) mod.getDx();
        helper.deltaY = (int) -mod.getDy();
        mod.getThread().reset();
        return true;
    }

    @SuppressWarnings("unchecked")
    public static boolean onRenderLivingEntity(
            Object renderer,
            Object entity,
            double x,
            double y,
            double z) {
        if (!isRuntimeReady()
                || !(renderer instanceof RendererLivingEntity)
                || !(entity instanceof EntityLivingBase)) {
            return false;
        }
        EventRendererLivingEntity event = new EventRendererLivingEntity(
                (RendererLivingEntity<EntityLivingBase>) renderer,
                (EntityLivingBase) entity,
                x,
                y,
                z);
        event.call();
        return event.isCancelled();
    }

    public static boolean onRenderName(Object entity) {
        if (!isRuntimeReady()) {
            return false;
        }
        GhostNametagsMod mod = GhostNametagsMod.getInstance();
        return entity instanceof EntityPlayer
                && mod != null
                && mod.isToggled();
    }

    public static boolean onCanRenderName(
            boolean vanillaValue,
            Object entity) {
        if (!isRuntimeReady()) {
            return vanillaValue;
        }
        NametagMod mod = NametagMod.getInstance();
        return mod != null
                        && mod.isToggled()
                        && entity == Minecraft.getMinecraft().thePlayer
                ? true
                : vanillaValue;
    }

    public static void prepareHitOverlay() {
        if (!isRuntimeReady()) {
            HIT_OVERLAY_STATE.remove();
            return;
        }
        EventHitOverlay event = new EventHitOverlay(1.0F, 0.0F, 0.0F, 0.3F);
        event.call();
        HIT_OVERLAY_STATE.set(new float[] {
                event.getRed(),
                event.getGreen(),
                event.getBlue(),
                event.getAlpha()
        });
    }

    public static float hitOverlayRed(float vanillaValue) {
        float[] state = HIT_OVERLAY_STATE.get();
        return state == null ? vanillaValue : state[0];
    }

    public static float hitOverlayGreen(float vanillaValue) {
        float[] state = HIT_OVERLAY_STATE.get();
        return state == null ? vanillaValue : state[1];
    }

    public static float hitOverlayBlue(float vanillaValue) {
        float[] state = HIT_OVERLAY_STATE.get();
        return state == null ? vanillaValue : state[2];
    }

    public static float hitOverlayAlpha(float vanillaValue) {
        float[] state = HIT_OVERLAY_STATE.get();
        return state == null ? vanillaValue : state[3];
    }

    public static boolean onRenderPlayer(
            Object entity,
            double x,
            double y,
            double z,
            float partialTicks) {
        if (!isRuntimeReady() || !(entity instanceof Entity)) {
            return false;
        }
        EventRenderPlayer event =
                new EventRenderPlayer((Entity) entity, x, y, z, partialTicks);
        event.call();
        return event.isCancelled();
    }

    public static void onRenderItemInFirstPerson() {
        if (isRuntimeReady()) {
            new EventRenderItemInFirstPerson().call();
        }
    }

    public static boolean onWaterOverlay() {
        if (!isRuntimeReady()) {
            return false;
        }
        EventWaterOverlay event = new EventWaterOverlay();
        event.call();
        return event.isCancelled();
    }

    public static boolean onFireOverlay() {
        if (!isRuntimeReady()) {
            return false;
        }
        EventFireOverlay event = new EventFireOverlay();
        event.call();
        return event.isCancelled();
    }

    public static void onEntityStatus(Object packet) {
        if (!isRuntimeReady() || !(packet instanceof S19PacketEntityStatus)) {
            return;
        }
        S19PacketEntityStatus status = (S19PacketEntityStatus) packet;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (status.getOpCode() == 2 && minecraft.theWorld != null) {
            new EventDamageEntity(status.getEntity(minecraft.theWorld)).call();
        }
    }

    public static void onLeaveServer() {
        if (isRuntimeReady()) {
            new EventLeaveServer().call();
        }
    }

    public static boolean onPlaySound(
            double x,
            double y,
            double z,
            String soundName,
            float volume,
            float pitch,
            boolean distanceDelay) {
        if (!isRuntimeReady()) {
            return false;
        }
        EventPlaySound event =
                new EventPlaySound(soundName, volume, pitch, volume, pitch);
        event.call();
        if (event.getPitch() == pitch && event.getVolume() == volume) {
            return false;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.getRenderViewEntity() == null) {
            return false;
        }
        double distanceSq =
                minecraft.getRenderViewEntity().getDistanceSq(x, y, z);
        PositionedSoundRecord sound = new PositionedSoundRecord(
                new ResourceLocation(soundName),
                event.getVolume(),
                event.getPitch(),
                (float) x,
                (float) y,
                (float) z);
        if (distanceDelay && distanceSq > 100.0D) {
            minecraft.getSoundHandler().playDelayedSound(
                    sound,
                    (int) (Math.sqrt(distanceSq) / 40.0D * 20.0D));
        } else {
            minecraft.getSoundHandler().playSound(sound);
        }
        return true;
    }

    public static String onText(String text) {
        if (!isRuntimeReady() || text == null) {
            return text;
        }
        EventText event = new EventText(text);
        event.call();
        return event.getOutputText();
    }

    public static float onZoomFov(float vanillaValue) {
        if (!isRuntimeReady()) {
            return vanillaValue;
        }
        EventZoomFov event = new EventZoomFov(vanillaValue);
        event.call();
        return event.getFov();
    }

    public static void beginCameraRotation() {
        Minecraft minecraft = Minecraft.getMinecraft();
        Entity viewEntity = minecraft.getRenderViewEntity();
        if (!isRuntimeReady() || viewEntity == null) {
            CAMERA_STATE.remove();
            return;
        }
        EventCameraRotation event = new EventCameraRotation(
                viewEntity.rotationYaw,
                viewEntity.rotationPitch,
                0.0F,
                4.0F);
        event.call();
        GhostFreelookMod freelook = GhostFreelookMod.getInstance();
        if (freelook != null && freelook.isCameraActive()) {
            minecraft.getRenderManager().playerViewY = event.getYaw();
            minecraft.getRenderManager().playerViewX = event.getPitch();
        }
        CAMERA_STATE.set(new float[] {
                event.getYaw(),
                event.getPitch(),
                event.getThirdPersonDistance()
        });
        GlStateManager.rotate(event.getRoll(), 0.0F, 0.0F, 1.0F);
    }

    public static float cameraYaw(float vanillaValue) {
        float[] state = CAMERA_STATE.get();
        return state == null ? vanillaValue : state[0];
    }

    public static float cameraPitch(float vanillaValue) {
        float[] state = CAMERA_STATE.get();
        return state == null ? vanillaValue : state[1];
    }

    public static float cameraDistance(float vanillaValue) {
        float[] state = CAMERA_STATE.get();
        return state == null ? vanillaValue : state[2];
    }

    public static void rotateHurtCamera(float angle, float x, float y, float z) {
        if (isRuntimeReady()) {
            EventHurtCamera event = new EventHurtCamera();
            event.call();
            angle *= event.getIntensity();
        }
        GlStateManager.rotate(angle, x, y, z);
    }

    public static void onPlayerHeadRotation(Object player, float yaw, float pitch) {
        if (!(player instanceof Entity)) {
            return;
        }
        if (isRuntimeReady()) {
            EventPlayerHeadRotation event = new EventPlayerHeadRotation(yaw, pitch);
            event.call();
            if (event.isCancelled()) {
                return;
            }
            yaw = event.getYaw();
            pitch = event.getPitch();
        }
        ((Entity) player).setAngles(yaw, pitch);
    }

    public static float onGamma(float vanillaValue) {
        if (!isRuntimeReady()) {
            return vanillaValue;
        }
        EventGamma event = new EventGamma(vanillaValue);
        event.call();
        return event.getGamma();
    }

    public static void onShader() {
        if (!isRuntimeReady()) {
            return;
        }
        EventShader event = new EventShader();
        event.call();
        for (ShaderGroup group : event.getGroups()) {
            GlStateManager.matrixMode(5890);
            GlStateManager.pushMatrix();
            GlStateManager.loadIdentity();
            group.loadShaderGroup(
                    MinecraftAccess.getTimer(Minecraft.getMinecraft()).renderPartialTicks);
            GlStateManager.popMatrix();
        }
    }

    public static void onSwitchTexture() {
        if (isRuntimeReady()) {
            new EventSwitchTexture().call();
        }
    }

    public static void onRenderChunkPosition(Object chunk, Object position) {
        if (isRuntimeReady()
                && chunk instanceof RenderChunk
                && position instanceof BlockPos) {
            new EventRenderChunkPosition(
                    (RenderChunk) chunk,
                    (BlockPos) position).call();
        }
    }

    public static void onPreRenderChunk(Object chunk) {
        if (isRuntimeReady() && chunk instanceof RenderChunk) {
            new EventPreRenderChunk((RenderChunk) chunk).call();
        }
    }

    public static boolean onRenderHitbox(
            Object entity,
            double x,
            double y,
            double z,
            float entityYaw,
            float partialTicks) {
        if (!isRuntimeReady() || !(entity instanceof Entity)) {
            return false;
        }
        EventRenderHitbox event = new EventRenderHitbox(
                (Entity) entity, x, y, z, entityYaw, partialTicks);
        event.call();
        return event.isCancelled();
    }

    public static void onRenderTnt(
            Object renderer,
            Object entity,
            double x,
            double y,
            double z,
            float entityYaw,
            float partialTicks) {
        if (isRuntimeReady()
                && renderer instanceof RenderTNTPrimed
                && entity instanceof EntityTNTPrimed) {
            new EventRenderTNT(
                    (RenderTNTPrimed) renderer,
                    (EntityTNTPrimed) entity,
                    x,
                    y,
                    z,
                    entityYaw,
                    partialTicks).call();
        }
    }

    private static boolean callClickEvent(int button) {
        if (!isRuntimeReady()) {
            return false;
        }
        EventClickMouse event = new EventClickMouse(button);
        event.call();
        return event.isCancelled();
    }

    private static WeatherChangerMod activeWeatherChanger() {
        if (!isRuntimeReady()) {
            return null;
        }
        WeatherChangerMod weather = WeatherChangerMod.getInstance();
        return weather != null && weather.isToggled() ? weather : null;
    }

    private static void applyClientBrand(Packet<?> packet) {
        if (!(packet instanceof C17PacketCustomPayload)) {
            return;
        }
        C17PacketCustomPayload custom = (C17PacketCustomPayload) packet;
        if (!"MC|Brand".equals(custom.getChannelName())) {
            return;
        }

        String brand = "FlaxClient";
        ClientSpooferMod mod = ClientSpooferMod.getInstance();
        if (mod != null && mod.isToggled()) {
            TranslateText option =
                    mod.getTypeSetting().getOption().getTranslate();
            if (TranslateText.VANILLA.equals(option)) {
                brand = ClientBrandRetriever.getClientModName();
            } else if (TranslateText.FORGE.equals(option)) {
                brand = "FML";
            }
        }

        PacketBuffer data = custom.getBufferData();
        data.clear();
        data.writeString(brand);
    }

    private static boolean showTabHeads() {
        if (!isRuntimeReady()) {
            return true;
        }
        TabEditorMod mod = TabEditorMod.getInstance();
        return mod == null
                || !mod.isToggled()
                || mod.getHeadSetting().isToggled();
    }

    private static void renderModelColored(
            RenderItem renderer,
            IBakedModel model,
            int color) {
        try {
            Method method = renderModelColoredMethod;
            if (method == null) {
                method = RenderItem.class.getDeclaredMethod(
                        runtimeMethodName(
                                RenderItem.class,
                                "renderModel",
                                IBakedModel.class,
                                Integer.TYPE),
                        IBakedModel.class,
                        Integer.TYPE);
                method.setAccessible(true);
                renderModelColoredMethod = method;
            }
            method.invoke(renderer, model, Integer.valueOf(color));
        } catch (Exception error) {
            throw new IllegalStateException(
                    "Unable to invoke RenderItem color renderer",
                    error);
        }
    }

    private static int getDroppedItemCount(
            RenderEntityItem renderer,
            ItemStack stack) {
        try {
            Method method = droppedItemCountMethod;
            if (method == null) {
                method = RenderEntityItem.class.getDeclaredMethod(
                        runtimeMethodName(
                                RenderEntityItem.class,
                                "func_177078_a",
                                ItemStack.class),
                        ItemStack.class);
                method.setAccessible(true);
                droppedItemCountMethod = method;
            }
            return ((Integer) method.invoke(renderer, stack)).intValue();
        } catch (Exception error) {
            throw new IllegalStateException(
                    "Unable to invoke dropped-item count renderer",
                    error);
        }
    }

    private static String runtimeMethodName(
            Class<?> owner,
            String preferredName,
            Class<?>... parameterTypes) {
        try {
            owner.getDeclaredMethod(preferredName, parameterTypes);
            return preferredName;
        } catch (NoSuchMethodException ignored) {
            for (Method method : owner.getDeclaredMethods()) {
                Class<?>[] actual = method.getParameterTypes();
                if (actual.length != parameterTypes.length) {
                    continue;
                }
                boolean matches = true;
                for (int index = 0; index < actual.length; index++) {
                    if (actual[index] != parameterTypes[index]) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    return method.getName();
                }
            }
            throw new IllegalStateException(
                    "No matching runtime method in " + owner.getName());
        }
    }

    private static boolean weatherOptionEquals(
            WeatherChangerMod weather,
            TranslateText translationKey) {
        return weather.getWeatherSetting().getOption().getTranslate()
                .equals(translationKey);
    }

    private static boolean isRuntimeReady() {
        return LateLoadStatus.isTransformerReady()
                && Glide.getInstance().getEventManager() != null;
    }
}
