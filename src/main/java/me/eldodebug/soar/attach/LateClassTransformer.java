package me.eldodebug.soar.attach;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Transforms already-loaded Minecraft classes without changing their schema.
 * JVMTI calls this class from its ClassFileLoadHook.
 */
public final class LateClassTransformer {

    private static final String MINECRAFT_DEOBF = "net/minecraft/client/Minecraft";
    private static final String MINECRAFT_NOTCH = "ave";
    private static final String GUI_INGAME_DEOBF = "net/minecraft/client/gui/GuiIngame";
    private static final String GUI_INGAME_NOTCH = "avo";
    private static final String ENTITY_RENDERER_DEOBF =
            "net/minecraft/client/renderer/EntityRenderer";
    private static final String ENTITY_RENDERER_NOTCH = "bfk";
    private static final String PLAYER_SP_DEOBF =
            "net/minecraft/client/entity/EntityPlayerSP";
    private static final String PLAYER_SP_NOTCH = "bew";
    private static final String NETWORK_MANAGER_DEOBF =
            "net/minecraft/network/NetworkManager";
    private static final String NETWORK_MANAGER_NOTCH = "ek";
    private static final String ENTITY_PLAYER_DEOBF =
            "net/minecraft/entity/player/EntityPlayer";
    private static final String ENTITY_PLAYER_NOTCH = "wn";
    private static final String ENTITY_LIVING_DEOBF =
            "net/minecraft/entity/EntityLivingBase";
    private static final String ENTITY_LIVING_NOTCH = "pr";
    private static final String ABSTRACT_PLAYER_DEOBF =
            "net/minecraft/client/entity/AbstractClientPlayer";
    private static final String ABSTRACT_PLAYER_NOTCH = "bet";
    private static final String MODEL_PLAYER_DEOBF =
            "net/minecraft/client/model/ModelPlayer";
    private static final String MODEL_PLAYER_NOTCH = "bbr";
    private static final String LIVING_RENDERER_DEOBF =
            "net/minecraft/client/renderer/entity/RendererLivingEntity";
    private static final String LIVING_RENDERER_NOTCH = "bjl";
    private static final String PLAYER_RENDERER_DEOBF =
            "net/minecraft/client/renderer/entity/RenderPlayer";
    private static final String PLAYER_RENDERER_NOTCH = "bln";
    private static final String ITEM_RENDERER_DEOBF =
            "net/minecraft/client/renderer/ItemRenderer";
    private static final String ITEM_RENDERER_NOTCH = "bfn";
    private static final String PLAY_HANDLER_DEOBF =
            "net/minecraft/client/network/NetHandlerPlayClient";
    private static final String PLAY_HANDLER_NOTCH = "bcy";
    private static final String WORLD_CLIENT_DEOBF =
            "net/minecraft/client/multiplayer/WorldClient";
    private static final String WORLD_CLIENT_NOTCH = "bdb";
    private static final String FONT_RENDERER_DEOBF =
            "net/minecraft/client/gui/FontRenderer";
    private static final String FONT_RENDERER_NOTCH = "avn";
    private static final String TEXTURE_MAP_DEOBF =
            "net/minecraft/client/renderer/texture/TextureMap";
    private static final String TEXTURE_MAP_NOTCH = "bmh";
    private static final String RENDER_CHUNK_DEOBF =
            "net/minecraft/client/renderer/chunk/RenderChunk";
    private static final String RENDER_CHUNK_NOTCH = "bht";
    private static final String CHUNK_CONTAINER_DEOBF =
            "net/minecraft/client/renderer/ChunkRenderContainer";
    private static final String CHUNK_CONTAINER_NOTCH = "bfh";
    private static final String RENDER_MANAGER_DEOBF =
            "net/minecraft/client/renderer/entity/RenderManager";
    private static final String RENDER_MANAGER_NOTCH = "biu";
    private static final String TNT_RENDERER_DEOBF =
            "net/minecraft/client/renderer/entity/RenderTNTPrimed";
    private static final String TNT_RENDERER_NOTCH = "bkf";
    private static final String WORLD_DEOBF = "net/minecraft/world/World";
    private static final String WORLD_NOTCH = "adm";
    private static final String WORLD_INFO_DEOBF =
            "net/minecraft/world/storage/WorldInfo";
    private static final String WORLD_INFO_NOTCH = "ato";
    private static final String WORLD_CHUNK_MANAGER_DEOBF =
            "net/minecraft/world/biome/WorldChunkManager";
    private static final String WORLD_CHUNK_MANAGER_NOTCH = "aec";
    private static final String ENTITY_DEOBF = "net/minecraft/entity/Entity";
    private static final String ENTITY_NOTCH = "pk";
    private static final String SOUND_MANAGER_DEOBF =
            "net/minecraft/client/audio/SoundManager";
    private static final String SOUND_MANAGER_NOTCH = "bpx";
    private static final String CHUNK_DISPATCHER_DEOBF =
            "net/minecraft/client/renderer/chunk/ChunkRenderDispatcher";
    private static final String CHUNK_DISPATCHER_NOTCH = "bho";
    private static final String BLOCK_DEOBF = "net/minecraft/block/Block";
    private static final String BLOCK_NOTCH = "afh";
    private static final String SCREENSHOT_HELPER_DEOBF =
            "net/minecraft/util/ScreenShotHelper";
    private static final String SCREENSHOT_HELPER_NOTCH = "avj";
    private static final String TAB_OVERLAY_DEOBF =
            "net/minecraft/client/gui/GuiPlayerTabOverlay";
    private static final String TAB_OVERLAY_NOTCH = "awh";
    private static final String RENDER_ITEM_DEOBF =
            "net/minecraft/client/renderer/entity/RenderItem";
    private static final String RENDER_ITEM_NOTCH = "bjh";
    private static final String RENDER_ENTITY_ITEM_DEOBF =
            "net/minecraft/client/renderer/entity/RenderEntityItem";
    private static final String RENDER_ENTITY_ITEM_NOTCH = "bjf";
    private static final String DEADMAU5_LAYER_DEOBF =
            "net/minecraft/client/renderer/entity/layers/LayerDeadmau5Head";
    private static final String DEADMAU5_LAYER_NOTCH = "bkt";
    private static final String MOUSE_HELPER_DEOBF =
            "net/minecraft/util/MouseHelper";
    private static final String MOUSE_HELPER_NOTCH = "avf";
    private static final String HOOK_OWNER = "me/eldodebug/soar/attach/LateHooks";

    private LateClassTransformer() {
    }

    public static byte[] transform(String internalName, byte[] originalBytes) {
        if (!isTarget(internalName)) {
            return null;
        }

        ClassNode node = new ClassNode();
        new ClassReader(originalBytes).accept(node, ClassReader.EXPAND_FRAMES);

        boolean changed = false;
        for (Object methodObject : node.methods) {
            MethodNode method = (MethodNode) methodObject;
            if (isMinecraft(internalName)) {
                changed |= transformMinecraft(method);
            } else if (isGuiIngame(internalName)) {
                changed |= transformGuiIngame(method);
            } else if (isEntityRenderer(internalName)) {
                changed |= transformEntityRenderer(method);
            } else if (isPlayerSp(internalName)) {
                changed |= transformPlayerSp(method);
            } else if (isNetworkManager(internalName)) {
                changed |= transformNetworkManager(method);
            } else if (isEntityPlayer(internalName)) {
                changed |= transformEntityPlayer(method);
            } else if (isEntityLiving(internalName)) {
                changed |= transformEntityLiving(method);
            } else if (isAbstractPlayer(internalName)) {
                changed |= transformAbstractPlayer(internalName, method);
            } else if (isModelPlayer(internalName)) {
                changed |= transformModelPlayer(method);
            } else if (isLivingRenderer(internalName)) {
                changed |= transformLivingRenderer(method);
            } else if (isPlayerRenderer(internalName)) {
                changed |= transformPlayerRenderer(method);
            } else if (isItemRenderer(internalName)) {
                changed |= transformItemRenderer(method);
            } else if (isPlayHandler(internalName)) {
                changed |= transformPlayHandler(method);
            } else if (isWorldClient(internalName)) {
                changed |= transformWorldClient(method);
            } else if (isFontRenderer(internalName)) {
                changed |= transformFontRenderer(method);
            } else if (isTextureMap(internalName)) {
                changed |= transformTextureMap(method);
            } else if (isRenderChunk(internalName)) {
                changed |= transformRenderChunk(method);
            } else if (isChunkContainer(internalName)) {
                changed |= transformChunkContainer(method);
            } else if (isRenderManager(internalName)) {
                changed |= transformRenderManager(method);
            } else if (isTntRenderer(internalName)) {
                changed |= transformTntRenderer(method);
            } else if (isWorld(internalName)) {
                changed |= transformWorld(method);
            } else if (isWorldInfo(internalName)) {
                changed |= transformWorldInfo(method);
            } else if (isWorldChunkManager(internalName)) {
                changed |= transformWorldChunkManager(method);
            } else if (isEntity(internalName)) {
                changed |= transformEntity(method);
            } else if (isSoundManager(internalName)) {
                changed |= transformSoundManager(method);
            } else if (isChunkDispatcher(internalName)) {
                changed |= transformChunkDispatcher(method);
            } else if (isBlock(internalName)) {
                changed |= transformBlock(method);
            } else if (isScreenshotHelper(internalName)) {
                changed |= transformScreenshotHelper(method);
            } else if (isTabOverlay(internalName)) {
                changed |= transformTabOverlay(method);
            } else if (isRenderItem(internalName)) {
                changed |= transformRenderItem(method);
            } else if (isRenderEntityItem(internalName)) {
                changed |= transformRenderEntityItem(method);
            } else if (isDeadmau5Layer(internalName)) {
                changed |= transformDeadmau5Layer(method);
            } else if (isMouseHelper(internalName)) {
                changed |= transformMouseHelper(method);
            }
        }

        if (!changed) {
            return null;
        }

        ClassWriter writer = new SafeClassWriter(
                ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static boolean transformMinecraft(MethodNode method) {
        if (matches(method, "()V", "runTick", "s")
                && !containsHook(method, "onClientTick")) {
            wrapMouseWheelReads(method);
            injectBeforeReturns(method, hookCall("onClientTick", "()V"));
            return true;
        }
        if (matches(method, "()V", "runGameLoop", "av")
                && !containsHook(method, "onPreRenderTick")) {
            method.instructions.insert(hookCall("onPreRenderTick", "()V"));
            injectBeforeReturns(method, hookCall("onRenderTick", "()V"));
            return true;
        }
        if (matches(method, "()V", "updateDisplay", "h")
                && !containsHook(method, "onUpdateDisplay")) {
            method.instructions.insert(hookCall("onUpdateDisplay", "()V"));
            return true;
        }
        if (matches(method, "()V", "updateFramebufferSize", "ay")
                && !containsHook(method, "onUpdateFramebufferSize")) {
            method.instructions.insert(hookCall("onUpdateFramebufferSize", "()V"));
            return true;
        }
        if (matches(method, "()V", "dispatchKeypresses", "Z")
                && !containsHook(method, "onKeyEvent")) {
            injectBeforeReturns(method, hookCall("onKeyEvent", "()V"));
            return true;
        }
        if (matches(method, "()V", "clickMouse", "aw")
                && !containsHook(method, "onLeftClick")) {
            injectCancelableHead(method, "onLeftClick", "()Z", -1);
            return true;
        }
        if (matches(method, "()V", "rightClickMouse", "ax")
                && !containsHook(method, "onRightClick")) {
            injectCancelableHead(method, "onRightClick", "()Z", -1);
            return true;
        }
        if (matches(method, "()V", "toggleFullscreen", "q")
                && !containsHook(method, "onToggleFullscreen")) {
            injectCancelableHead(method, "onToggleFullscreen", "()Z", -1);
            return true;
        }
        if ((matches(method,
                        "(Lnet/minecraft/client/multiplayer/WorldClient;)V",
                        "loadWorld", "a")
                || matches(method, "(Lbdb;)V", "loadWorld", "a"))
                && !containsHook(method, "onLoadWorld")) {
            method.instructions.insert(hookCall("onLoadWorld", "()V"));
            return true;
        }
        if (matches(method, "()I", "getLimitFramerate", "j")
                && !containsHook(method, "onLimitFramerate")) {
            insertBeforeOpcode(
                    method,
                    Opcodes.IRETURN,
                    hookCall("onLimitFramerate", "(I)I"));
            return true;
        }
        if (matches(method, "()Z", "isFramerateLimitBelowMax", "k")
                && !containsHook(method, "onFramerateLimitBelowMax")) {
            insertBeforeOpcode(
                    method,
                    Opcodes.IRETURN,
                    hookCall("onFramerateLimitBelowMax", "(Z)Z"));
            return true;
        }
        if (matches(method, "()I", "getDebugFPS", "ai")
                && (method.access & Opcodes.ACC_STATIC) != 0
                && !containsHook(method, "onDebugFps")) {
            insertBeforeOpcode(
                    method,
                    Opcodes.IRETURN,
                    hookCall("onDebugFps", "(I)I"));
            return true;
        }
        return false;
    }

    private static void wrapMouseWheelReads(MethodNode method) {
        for (AbstractInsnNode instruction = method.instructions.getFirst();
                instruction != null;
                instruction = instruction.getNext()) {
            if (!(instruction instanceof MethodInsnNode)) {
                continue;
            }
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (call.getOpcode() == Opcodes.INVOKESTATIC
                    && "org/lwjgl/input/Mouse".equals(call.owner)
                    && "getEventDWheel".equals(call.name)
                    && "()I".equals(call.desc)) {
                method.instructions.insert(call, hookCall("onScrollMouse", "(I)I"));
            }
        }
    }

    private static boolean transformGuiIngame(MethodNode method) {
        if (matches(method, "(F)V", "renderGameOverlay", "a")
                && !containsHook(method, "onRender2D")) {
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.FLOAD, 1));
            hook.add(hookCall("onRender2D", "(F)V"));
            /*
             * Match MixinGuiIngame's ordinal-2 GlStateManager.color injection.
             * Calling at RETURN is too late: vanilla has already disabled blend
             * and restored its final colour, which breaks the 2D half of ghost
             * nametags and the NanoVG break-progress overlay.
             */
            MethodInsnNode insertionPoint = null;
            int colorOrdinal = 0;
            for (AbstractInsnNode instruction = method.instructions.getFirst();
                    instruction != null;
                    instruction = instruction.getNext()) {
                if (!(instruction instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode call = (MethodInsnNode) instruction;
                boolean colorCall =
                        ("net/minecraft/client/renderer/GlStateManager".equals(call.owner)
                                        && "color".equals(call.name)
                                        && "(FFFF)V".equals(call.desc))
                                || ("bfl".equals(call.owner)
                                        && "c".equals(call.name)
                                        && "(FFFF)V".equals(call.desc));
                if (colorCall && colorOrdinal++ == 2) {
                    insertionPoint = call;
                    break;
                }
            }
            if (insertionPoint != null) {
                method.instructions.insertBefore(insertionPoint, hook);
            } else {
                injectBeforeReturns(method, hook);
            }
            return true;
        }
        if (matches(method, "()Z", "showCrosshair", "b")
                && !containsHook(method, "onRenderCrosshair")) {
            insertBeforeOpcode(
                    method,
                    Opcodes.IRETURN,
                    hookCall("onRenderCrosshair", "(Z)Z"));
            return true;
        }
        if ((matches(method,
                        "(Lnet/minecraft/client/gui/ScaledResolution;)V",
                        "renderPlayerStats", "d")
                || matches(method, "(Lavr;)V", "renderPlayerStats", "d"))
                && !containsHook(method, "onRenderPlayerStats")) {
            injectBeforeReturns(method, hookCall("onRenderPlayerStats", "()V"));
            return true;
        }
        if ((matches(method,
                        "(Lnet/minecraft/client/gui/ScaledResolution;)V",
                        "renderPumpkinOverlay", "e")
                || matches(method, "(Lavr;)V", "renderPumpkinOverlay", "e"))
                && !containsHook(method, "onRenderPumpkinOverlay")) {
            injectCancelableHead(method, "onRenderPumpkinOverlay", "()Z", -1);
            return true;
        }
        if ((matches(method,
                        "(Lnet/minecraft/client/gui/ScaledResolution;F)V",
                        "renderTooltip", "a")
                || matches(method, "(Lavr;F)V", "renderTooltip", "a"))
                && !containsHook(method, "onRenderTooltip")) {
            injectCancelableFloatHead(method, "onRenderTooltip", "(F)Z", 2);
            return true;
        }
        if ((matches(method,
                        "(Lnet/minecraft/client/gui/ScaledResolution;I)V",
                        "renderExpBar", "b")
                || matches(method, "(Lavr;I)V", "renderExpBar", "b"))
                && !containsHook(method, "onRenderExpBar")) {
            injectCancelableHead(method, "onRenderExpBar", "()Z", -1);
            return true;
        }
        if ((matches(method,
                        "(Lnet/minecraft/scoreboard/ScoreObjective;Lnet/minecraft/client/gui/ScaledResolution;)V",
                        "renderScoreboard", "a")
                || matches(method, "(Lauk;Lavr;)V", "renderScoreboard", "a"))
                && !containsHook(method, "onRenderScoreboard")) {
            injectCancelableHead(
                    method,
                    "onRenderScoreboard",
                    "(Ljava/lang/Object;)Z",
                    1);
            return true;
        }
        if ((matches(method,
                        "(Lnet/minecraft/client/gui/ScaledResolution;)V",
                        "renderSelectedItem", "a")
                || matches(method, "(Lavr;)V", "renderSelectedItem", "a"))
                && !containsHook(method, "onRenderSelectedItem")) {
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
            hook.add(hookCall("onRenderSelectedItem", "(Ljava/lang/Object;)V"));
            method.instructions.insert(hook);
            return true;
        }
        return false;
    }

    private static boolean transformEntityRenderer(MethodNode method) {
        if (matches(method, "(IFJ)V", "renderWorldPass", "a")) {
            boolean changed = false;
            if (!containsHook(method, "onRender3D")) {
                InsnList hook = new InsnList();
                hook.add(new VarInsnNode(Opcodes.FLOAD, 2));
                hook.add(hookCall("onRender3D", "(F)V"));
                /*
                 * Preserve the original Mixin injection point immediately before
                 * EntityRenderer.renderHand is read. Ghost world overlays depend
                 * on the world camera matrices still being active here.
                 */
                FieldInsnNode insertionPoint = null;
                for (AbstractInsnNode instruction = method.instructions.getFirst();
                        instruction != null;
                        instruction = instruction.getNext()) {
                    if (!(instruction instanceof FieldInsnNode)) {
                        continue;
                    }
                    FieldInsnNode field = (FieldInsnNode) instruction;
                    boolean renderHand =
                            field.getOpcode() == Opcodes.GETFIELD
                                    && "Z".equals(field.desc)
                                    && ((ENTITY_RENDERER_DEOBF.equals(field.owner)
                                                    && "renderHand".equals(field.name))
                                            || (ENTITY_RENDERER_NOTCH.equals(field.owner)
                                                    && "C".equals(field.name)));
                    if (renderHand) {
                        insertionPoint = field;
                        break;
                    }
                }
                if (insertionPoint != null) {
                    method.instructions.insertBefore(insertionPoint, hook);
                } else {
                    injectBeforeReturns(method, hook);
                }
                changed = true;
            }
            if (!containsHook(method, "renderBlockHighlight")) {
                for (AbstractInsnNode instruction = method.instructions.getFirst();
                        instruction != null;
                        instruction = instruction.getNext()) {
                    if (!(instruction instanceof MethodInsnNode)) {
                        continue;
                    }
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    boolean deobfuscated = "net/minecraft/client/renderer/RenderGlobal"
                                    .equals(call.owner)
                            && "drawSelectionBox".equals(call.name)
                            && ("(Lnet/minecraft/entity/player/EntityPlayer;"
                                    + "Lnet/minecraft/util/MovingObjectPosition;IF)V")
                                    .equals(call.desc);
                    boolean obfuscated = "bfr".equals(call.owner)
                            && "a".equals(call.name)
                            && "(Lwn;Lauh;IF)V".equals(call.desc);
                    if (deobfuscated || obfuscated) {
                        call.setOpcode(Opcodes.INVOKESTATIC);
                        call.owner = HOOK_OWNER;
                        call.name = "renderBlockHighlight";
                        call.desc = "(Ljava/lang/Object;Ljava/lang/Object;"
                                + "Ljava/lang/Object;IF)V";
                        call.itf = false;
                        changed = true;
                    }
                }
            }
            return changed;
        }
        if (matches(method, "(FZ)F", "getFOVModifier", "a")
                && !containsHook(method, "onZoomFov")) {
            insertBeforeOpcode(
                    method,
                    Opcodes.FRETURN,
                    hookCall("onZoomFov", "(F)F"));
            return true;
        }
        if (matches(method, "(F)V", "hurtCameraEffect", "d")
                && !containsHook(method, "rotateHurtCamera")) {
            for (AbstractInsnNode instruction = method.instructions.getFirst();
                    instruction != null;
                    instruction = instruction.getNext()) {
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (("net/minecraft/client/renderer/GlStateManager".equals(call.owner)
                                    || "bfl".equals(call.owner))
                            && ("rotate".equals(call.name) || "b".equals(call.name))
                            && "(FFFF)V".equals(call.desc)) {
                        call.setOpcode(Opcodes.INVOKESTATIC);
                        call.owner = HOOK_OWNER;
                        call.name = "rotateHurtCamera";
                        call.itf = false;
                    }
                }
            }
            return containsHook(method, "rotateHurtCamera");
        }
        if (matches(method, "(FJ)V", "updateCameraAndRender", "a")
                && !containsHook(method, "onPlayerHeadRotation")) {
            boolean changed = false;
            for (AbstractInsnNode instruction = method.instructions.getFirst();
                    instruction != null;
                    instruction = instruction.getNext()) {
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (("setAngles".equals(call.name) || "c".equals(call.name))
                            && "(FF)V".equals(call.desc)) {
                        call.setOpcode(Opcodes.INVOKESTATIC);
                        call.owner = HOOK_OWNER;
                        call.name = "onPlayerHeadRotation";
                        call.desc = "(Ljava/lang/Object;FF)V";
                        call.itf = false;
                        changed = true;
                    }
                }
            }
            if (changed) {
                injectBeforeReturns(method, hookCall("onShader", "()V"));
            }
            return changed;
        }
        if (method.name.startsWith("redirect$")
                && method.name.contains("setAngles")
                && !containsHook(method, "onPlayerHeadRotation")) {
            boolean changed = false;
            for (AbstractInsnNode instruction = method.instructions.getFirst();
                    instruction != null;
                    instruction = instruction.getNext()) {
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (("net/minecraft/client/entity/EntityPlayerSP".equals(call.owner)
                                    || "bew".equals(call.owner))
                            && ("setAngles".equals(call.name) || "c".equals(call.name))
                            && "(FF)V".equals(call.desc)) {
                        call.setOpcode(Opcodes.INVOKESTATIC);
                        call.owner = HOOK_OWNER;
                        call.name = "onPlayerHeadRotation";
                        call.desc = "(Ljava/lang/Object;FF)V";
                        call.itf = false;
                        changed = true;
                    }
                }
            }
            return changed;
        }
        if (matches(method, "(F)V", "updateLightmap", "g")
                && !containsHook(method, "onGamma")) {
            boolean changed = false;
            for (AbstractInsnNode instruction = method.instructions.getFirst();
                    instruction != null;
                    instruction = instruction.getNext()) {
                if (instruction instanceof FieldInsnNode) {
                    FieldInsnNode field = (FieldInsnNode) instruction;
                    if (field.getOpcode() == Opcodes.GETFIELD
                            && "F".equals(field.desc)
                            && ("gammaSetting".equals(field.name)
                                    || "aJ".equals(field.name))) {
                        method.instructions.insert(
                                field,
                                hookCall("onGamma", "(F)F"));
                        changed = true;
                    }
                }
            }
            return changed;
        }
        if (matches(method, "(F)V", "orientCamera", "f")
                && !containsHook(method, "beginCameraRotation")) {
            method.instructions.insert(
                    hookCall("beginCameraRotation", "()V"));
            boolean changed = true;
            for (AbstractInsnNode instruction = method.instructions.getFirst();
                    instruction != null; ) {
                AbstractInsnNode next = instruction.getNext();
                if (instruction instanceof FieldInsnNode) {
                    FieldInsnNode field = (FieldInsnNode) instruction;
                    if (field.getOpcode() == Opcodes.GETFIELD
                            && "F".equals(field.desc)) {
                        String hookName = null;
                        if (("net/minecraft/entity/Entity".equals(field.owner)
                                        && ("rotationYaw".equals(field.name)
                                                || "prevRotationYaw".equals(field.name)))
                                || ("pk".equals(field.owner)
                                        && ("y".equals(field.name)
                                                || "A".equals(field.name)))) {
                            hookName = "cameraYaw";
                        } else if (("net/minecraft/entity/Entity".equals(field.owner)
                                        && ("rotationPitch".equals(field.name)
                                                || "prevRotationPitch".equals(field.name)))
                                || ("pk".equals(field.owner)
                                        && ("z".equals(field.name)
                                                || "B".equals(field.name)))) {
                            hookName = "cameraPitch";
                        } else if ((ENTITY_RENDERER_DEOBF.equals(field.owner)
                                        && "thirdPersonDistance".equals(field.name))
                                || (ENTITY_RENDERER_NOTCH.equals(field.owner)
                                        && "q".equals(field.name))) {
                            hookName = "cameraDistance";
                        }
                        if (hookName != null) {
                            method.instructions.insert(
                                    field,
                                    hookCall(hookName, "(F)F"));
                        }
                    }
                }
                instruction = next;
            }
            return changed;
        }
        return false;
    }

    private static boolean transformPlayerSp(MethodNode method) {
        if (matches(method, "()V", "onUpdate", "t_")
                && !containsHook(method, "onPlayerUpdate")) {
            method.instructions.insert(hookCall("onPlayerUpdate", "()V"));
            return true;
        }
        if (matches(method, "()V", "onUpdateWalkingPlayer", "p")
                && !containsHook(method, "onMotionUpdate")) {
            method.instructions.insert(hookCall("onMotionUpdate", "()V"));
            return true;
        }
        if (matches(method, "(Ljava/lang/String;)V", "sendChatMessage", "e")
                && !containsHook(method, "onSendChat")) {
            injectCancelableHead(
                    method,
                    "onSendChat",
                    "(Ljava/lang/String;)Z",
                    1);
            return true;
        }
        return false;
    }

    private static boolean transformNetworkManager(MethodNode method) {
        boolean singlePacketArgument =
                "(Lnet/minecraft/network/Packet;)V".equals(method.desc)
                        || "(Lff;)V".equals(method.desc);
        if (singlePacketArgument
                && ("sendPacket".equals(method.name) || "a".equals(method.name))
                && !containsHook(method, "onSendPacket")) {
            injectCancelableHead(
                    method,
                    "onSendPacket",
                    "(Ljava/lang/Object;)Z",
                    1);
            return true;
        }

        boolean packetRead =
                "(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;)V"
                                .equals(method.desc)
                        || "(Lio/netty/channel/ChannelHandlerContext;Lff;)V"
                                .equals(method.desc);
        if (packetRead
                && ("channelRead0".equals(method.name) || "a".equals(method.name))
                && !containsHook(method, "onReceivePacket")) {
            injectCancelableHead(
                    method,
                    "onReceivePacket",
                    "(Ljava/lang/Object;)Z",
                    2);
            return true;
        }
        return false;
    }

    private static boolean transformEntityPlayer(MethodNode method) {
        if ((matches(method, "(Lnet/minecraft/entity/Entity;)V",
                        "attackTargetEntityWithCurrentItem", "f")
                || matches(method, "(Lpk;)V",
                        "attackTargetEntityWithCurrentItem", "f"))
                && !containsHook(method, "onAttackEntity")) {
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
            hook.add(hookCall("onAttackEntity", "(Ljava/lang/Object;)V"));
            method.instructions.insert(hook);
            return true;
        }
        if (matches(method, "()V", "jump", "bF")
                && !containsHook(method, "onJump")) {
            method.instructions.insert(hookCall("onJump", "()V"));
            return true;
        }
        if (matches(method, "()V", "onUpdate", "t_")
                && !containsHook(method, "onEntityPlayerTick")) {
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
            hook.add(hookCall("onEntityPlayerTick", "(Ljava/lang/Object;)V"));
            method.instructions.insert(hook);
            return true;
        }
        return false;
    }

    private static boolean transformEntityLiving(MethodNode method) {
        if (matches(method, "()V", "onEntityUpdate", "K")
                && !containsHook(method, "onLivingUpdate")) {
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
            hook.add(hookCall("onLivingUpdate", "(Ljava/lang/Object;)V"));
            injectBeforeReturns(method, hook);
            return true;
        }
        if (matches(method, "()I", "getArmSwingAnimationEnd", "n")
                && !containsHook(method, "onArmSwingAnimation")) {
            insertBeforeOpcode(
                    method,
                    Opcodes.IRETURN,
                    hookCall("onArmSwingAnimation", "(I)I"));
            return true;
        }
        return false;
    }

    private static boolean transformAbstractPlayer(String internalName, MethodNode method) {
        if (matches(method, "()F", "getFovModifier", "o")
                && !containsHook(method, "onFovModifier")) {
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
            hook.add(hookCall("onFovModifier", "(FLjava/lang/Object;)F"));
            insertBeforeOpcode(method, Opcodes.FRETURN, hook);
            return true;
        }
        if (("getLocationSkin".equals(method.name) || "i".equals(method.name))
                && ("()Lnet/minecraft/util/ResourceLocation;".equals(method.desc)
                        || "()Ljy;".equals(method.desc))
                && !containsHook(method, "onLocationSkin")) {
            insertResourceHook(internalName, method, "onLocationSkin");
            return true;
        }
        if (("getLocationCape".equals(method.name) || "k".equals(method.name))
                && ("()Lnet/minecraft/util/ResourceLocation;".equals(method.desc)
                        || "()Ljy;".equals(method.desc))
                && !containsHook(method, "onLocationCape")) {
            insertResourceHook(internalName, method, "onLocationCape");
            return true;
        }
        return false;
    }

    private static boolean transformModelPlayer(MethodNode method) {
        if (matches(method, "(F)V", "renderCape", "c")
                && !containsHook(method, "onRenderVanillaCape")) {
            injectCancelableHead(method, "onRenderVanillaCape", "()Z", -1);
            return true;
        }
        boolean render =
                ("render".equals(method.name) || "a".equals(method.name))
                        && ("(Lnet/minecraft/entity/Entity;FFFFFF)V"
                                        .equals(method.desc)
                                || "(Lpk;FFFFFF)V".equals(method.desc));
        if (render && !containsHook(method, "onModelPlayerRender")) {
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
            hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
            hook.add(new VarInsnNode(Opcodes.FLOAD, 7));
            hook.add(hookCall(
                    "onModelPlayerRender",
                    "(Ljava/lang/Object;Ljava/lang/Object;F)V"));
            injectBeforeReturns(method, hook);
            return true;
        }
        return false;
    }

    private static boolean transformLivingRenderer(MethodNode method) {
        boolean renderLiving =
                ("doRender".equals(method.name) || "a".equals(method.name))
                        && ("(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V"
                                        .equals(method.desc)
                                || "(Lpr;DDDFF)V".equals(method.desc));
        if (renderLiving && !containsHook(method, "onRenderLivingEntity")) {
            LabelNode continueLabel = new LabelNode();
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
            hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
            hook.add(new VarInsnNode(Opcodes.DLOAD, 2));
            hook.add(new VarInsnNode(Opcodes.DLOAD, 4));
            hook.add(new VarInsnNode(Opcodes.DLOAD, 6));
            hook.add(hookCall(
                    "onRenderLivingEntity",
                    "(Ljava/lang/Object;Ljava/lang/Object;DDD)Z"));
            hook.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
            hook.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
            hook.add(continueLabel);
            method.instructions.insert(hook);
            return true;
        }
        boolean renderName =
                ("renderName".equals(method.name) || "b".equals(method.name))
                        && ("(Lnet/minecraft/entity/EntityLivingBase;DDD)V"
                                        .equals(method.desc)
                                || "(Lpr;DDD)V".equals(method.desc));
        if (renderName && !containsHook(method, "onRenderName")) {
            injectCancelableHead(
                    method,
                    "onRenderName",
                    "(Ljava/lang/Object;)Z",
                    1);
            return true;
        }
        boolean canRenderName =
                ("canRenderName".equals(method.name) || "a".equals(method.name))
                        && ("(Lnet/minecraft/entity/EntityLivingBase;)Z"
                                        .equals(method.desc)
                                || "(Lpr;)Z".equals(method.desc));
        if (canRenderName && !containsHook(method, "onCanRenderName")) {
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
            hook.add(hookCall(
                    "onCanRenderName",
                    "(ZLjava/lang/Object;)Z"));
            insertBeforeOpcode(method, Opcodes.IRETURN, hook);
            return true;
        }
        if ((matches(method,
                        "(Lnet/minecraft/entity/EntityLivingBase;FZ)Z",
                        "setBrightness", "a")
                || matches(method, "(Lpr;FZ)Z", "setBrightness", "a"))
                && !containsHook(method, "prepareHitOverlay")) {
            method.instructions.insert(hookCall("prepareHitOverlay", "()V"));
            int zeroOrdinal = 0;
            int oneOrdinal = 0;
            for (AbstractInsnNode instruction = method.instructions.getFirst();
                    instruction != null; ) {
                AbstractInsnNode next = instruction.getNext();
                String hookName = null;
                if (instruction.getOpcode() == Opcodes.FCONST_1) {
                    if (oneOrdinal++ == 0) {
                        hookName = "hitOverlayRed";
                    }
                } else if (instruction.getOpcode() == Opcodes.FCONST_0) {
                    if (zeroOrdinal == 0) {
                        hookName = "hitOverlayGreen";
                    } else if (zeroOrdinal == 1) {
                        hookName = "hitOverlayBlue";
                    }
                    zeroOrdinal++;
                } else if (instruction instanceof LdcInsnNode
                        && ((LdcInsnNode) instruction).cst instanceof Float
                        && Float.compare(
                                ((Float) ((LdcInsnNode) instruction).cst).floatValue(),
                                0.3F) == 0) {
                    hookName = "hitOverlayAlpha";
                }
                if (hookName != null) {
                    method.instructions.insert(
                            instruction,
                            hookCall(hookName, "(F)F"));
                }
                instruction = next;
            }
            return true;
        }
        return false;
    }

    private static boolean transformPlayerRenderer(MethodNode method) {
        boolean renderPlayer =
                ("doRender".equals(method.name) || "a".equals(method.name))
                        && ("(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V"
                                        .equals(method.desc)
                                || "(Lbet;DDDFF)V".equals(method.desc));
        if (renderPlayer && !containsHook(method, "onRenderPlayer")) {
            LabelNode continueLabel = new LabelNode();
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
            hook.add(new VarInsnNode(Opcodes.DLOAD, 2));
            hook.add(new VarInsnNode(Opcodes.DLOAD, 4));
            hook.add(new VarInsnNode(Opcodes.DLOAD, 6));
            hook.add(new VarInsnNode(Opcodes.FLOAD, 9));
            hook.add(hookCall(
                    "onRenderPlayer",
                    "(Ljava/lang/Object;DDDF)Z"));
            hook.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
            hook.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
            hook.add(continueLabel);
            method.instructions.insert(hook);
            return true;
        }
        return false;
    }

    private static boolean transformItemRenderer(MethodNode method) {
        if (matches(method, "(F)V", "renderItemInFirstPerson", "a")
                && !containsHook(method, "onRenderItemInFirstPerson")) {
            method.instructions.insert(hookCall("onRenderItemInFirstPerson", "()V"));
            return true;
        }
        if (matches(method, "(F)V", "renderWaterOverlayTexture", "e")
                && !containsHook(method, "onWaterOverlay")) {
            injectCancelableHead(method, "onWaterOverlay", "()Z", -1);
            return true;
        }
        if (matches(method, "(F)V", "renderFireInFirstPerson", "f")
                && !containsHook(method, "onFireOverlay")) {
            injectCancelableHead(method, "onFireOverlay", "()Z", -1);
            return true;
        }
        return false;
    }

    private static boolean transformPlayHandler(MethodNode method) {
        boolean entityStatus =
                ("handleEntityStatus".equals(method.name) || "a".equals(method.name))
                        && ("(Lnet/minecraft/network/play/server/S19PacketEntityStatus;)V"
                                        .equals(method.desc)
                                || "(Lgi;)V".equals(method.desc));
        if (entityStatus && !containsHook(method, "onEntityStatus")) {
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
            hook.add(hookCall("onEntityStatus", "(Ljava/lang/Object;)V"));
            injectBeforeReturns(method, hook);
            return true;
        }
        return false;
    }

    private static boolean transformWorldClient(MethodNode method) {
        if (matches(method, "()V", "sendQuittingDisconnectingPacket", "H")
                && !containsHook(method, "onLeaveServer")) {
            method.instructions.insert(hookCall("onLeaveServer", "()V"));
            return true;
        }
        if (matches(method,
                        "(DDDLjava/lang/String;FFZ)V",
                        "playSound", "a")
                && !containsHook(method, "onPlaySound")) {
            LabelNode continueLabel = new LabelNode();
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.DLOAD, 1));
            hook.add(new VarInsnNode(Opcodes.DLOAD, 3));
            hook.add(new VarInsnNode(Opcodes.DLOAD, 5));
            hook.add(new VarInsnNode(Opcodes.ALOAD, 7));
            hook.add(new VarInsnNode(Opcodes.FLOAD, 8));
            hook.add(new VarInsnNode(Opcodes.FLOAD, 9));
            hook.add(new VarInsnNode(Opcodes.ILOAD, 10));
            hook.add(hookCall(
                    "onPlaySound",
                    "(DDDLjava/lang/String;FFZ)Z"));
            hook.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
            hook.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
            hook.add(continueLabel);
            method.instructions.insert(hook);
            return true;
        }
        return false;
    }

    private static boolean transformFontRenderer(MethodNode method) {
        boolean renderString =
                ("renderString".equals(method.name) || "b".equals(method.name))
                        && "(Ljava/lang/String;FFIZ)I".equals(method.desc);
        boolean stringWidth =
                ("getStringWidth".equals(method.name) || "a".equals(method.name))
                        && "(Ljava/lang/String;)I".equals(method.desc);
        if ((renderString || stringWidth) && !containsHook(method, "onText")) {
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
            hook.add(hookCall(
                    "onText",
                    "(Ljava/lang/String;)Ljava/lang/String;"));
            hook.add(new VarInsnNode(Opcodes.ASTORE, 1));
            method.instructions.insert(hook);
            return true;
        }
        return false;
    }

    private static boolean transformTextureMap(MethodNode method) {
        boolean loadAtlas =
                ("loadTextureAtlas".equals(method.name) || "b".equals(method.name))
                        && ("(Lnet/minecraft/client/resources/IResourceManager;)V"
                                        .equals(method.desc)
                                || "(Lbni;)V".equals(method.desc));
        if (loadAtlas && !containsHook(method, "onSwitchTexture")) {
            injectBeforeReturns(method, hookCall("onSwitchTexture", "()V"));
            return true;
        }
        return false;
    }

    private static boolean transformRenderChunk(MethodNode method) {
        boolean setPosition =
                ("setPosition".equals(method.name) || "a".equals(method.name))
                        && ("(Lnet/minecraft/util/BlockPos;)V".equals(method.desc)
                                || "(Lcj;)V".equals(method.desc));
        if (setPosition && !containsHook(method, "onRenderChunkPosition")) {
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
            hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
            hook.add(hookCall(
                    "onRenderChunkPosition",
                    "(Ljava/lang/Object;Ljava/lang/Object;)V"));
            injectBeforeReturns(method, hook);
            return true;
        }
        return false;
    }

    private static boolean transformChunkContainer(MethodNode method) {
        boolean preRender =
                ("preRenderChunk".equals(method.name) || "a".equals(method.name))
                        && ("(Lnet/minecraft/client/renderer/chunk/RenderChunk;)V"
                                        .equals(method.desc)
                                || "(Lbht;)V".equals(method.desc));
        if (preRender && !containsHook(method, "onPreRenderChunk")) {
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
            hook.add(hookCall("onPreRenderChunk", "(Ljava/lang/Object;)V"));
            injectBeforeReturns(method, hook);
            return true;
        }
        return false;
    }

    private static boolean transformRenderManager(MethodNode method) {
        if ((matches(method,
                        "(Lnet/minecraft/entity/Entity;DDDFF)V",
                        "renderDebugBoundingBox", "b")
                || matches(method, "(Lpk;DDDFF)V", "renderDebugBoundingBox", "b"))
                && !containsHook(method, "onRenderHitbox")) {
            LabelNode continueLabel = new LabelNode();
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
            hook.add(new VarInsnNode(Opcodes.DLOAD, 2));
            hook.add(new VarInsnNode(Opcodes.DLOAD, 4));
            hook.add(new VarInsnNode(Opcodes.DLOAD, 6));
            hook.add(new VarInsnNode(Opcodes.FLOAD, 8));
            hook.add(new VarInsnNode(Opcodes.FLOAD, 9));
            hook.add(hookCall(
                    "onRenderHitbox",
                    "(Ljava/lang/Object;DDDFF)Z"));
            hook.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
            hook.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
            hook.add(continueLabel);
            method.instructions.insert(hook);
            return true;
        }
        return false;
    }

    private static boolean transformTntRenderer(MethodNode method) {
        if ((matches(method,
                        "(Lnet/minecraft/entity/item/EntityTNTPrimed;DDDFF)V",
                        "doRender", "a")
                || matches(method, "(Lvj;DDDFF)V", "doRender", "a"))
                && !containsHook(method, "onRenderTnt")) {
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
            hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
            hook.add(new VarInsnNode(Opcodes.DLOAD, 2));
            hook.add(new VarInsnNode(Opcodes.DLOAD, 4));
            hook.add(new VarInsnNode(Opcodes.DLOAD, 6));
            hook.add(new VarInsnNode(Opcodes.FLOAD, 8));
            hook.add(new VarInsnNode(Opcodes.FLOAD, 9));
            hook.add(hookCall(
                    "onRenderTnt",
                    "(Ljava/lang/Object;Ljava/lang/Object;DDDFF)V"));
            method.instructions.insert(hook);
            return true;
        }
        return false;
    }

    private static boolean transformWorld(MethodNode method) {
        if (matches(method, "(F)F", "getRainStrength", "j")
                && !containsHook(method, "onRainStrength")) {
            insertBeforeOpcode(
                    method,
                    Opcodes.FRETURN,
                    hookCall("onRainStrength", "(F)F"));
            return true;
        }
        if (matches(method, "(F)F", "getThunderStrength", "h")
                && !containsHook(method, "onThunderStrength")) {
            insertBeforeOpcode(
                    method,
                    Opcodes.FRETURN,
                    hookCall("onThunderStrength", "(F)F"));
            return true;
        }
        return false;
    }

    private static boolean transformWorldInfo(MethodNode method) {
        if (matches(method, "()J", "getWorldTime", "g")
                && !containsHook(method, "onWorldTime")) {
            insertBeforeOpcode(
                    method,
                    Opcodes.LRETURN,
                    hookCall("onWorldTime", "(J)J"));
            return true;
        }
        if (matches(method, "()Z", "isRaining", "p")
                && !containsHook(method, "onIsRaining")) {
            insertBeforeOpcode(
                    method,
                    Opcodes.IRETURN,
                    hookCall("onIsRaining", "(Z)Z"));
            return true;
        }
        if (matches(method, "()Z", "isThundering", "n")
                && !containsHook(method, "onIsThundering")) {
            insertBeforeOpcode(
                    method,
                    Opcodes.IRETURN,
                    hookCall("onIsThundering", "(Z)Z"));
            return true;
        }
        return false;
    }

    private static boolean transformWorldChunkManager(MethodNode method) {
        if (matches(method, "(FI)F", "getTemperatureAtHeight", "a")
                && !containsHook(method, "onTemperatureAtHeight")) {
            insertBeforeOpcode(
                    method,
                    Opcodes.FRETURN,
                    hookCall("onTemperatureAtHeight", "(F)F"));
            return true;
        }
        return false;
    }

    private static boolean transformEntity(MethodNode method) {
        if (matches(method, "(DDD)V", "setVelocity", "i")
                && !containsHook(method, "onSetVelocity")) {
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
            hook.add(new VarInsnNode(Opcodes.DLOAD, 1));
            hook.add(new VarInsnNode(Opcodes.DLOAD, 3));
            hook.add(new VarInsnNode(Opcodes.DLOAD, 5));
            hook.add(hookCall(
                    "onSetVelocity",
                    "(Ljava/lang/Object;DDD)V"));
            method.instructions.insert(hook);
            return true;
        }
        return false;
    }

    private static boolean transformSoundManager(MethodNode method) {
        boolean playSound =
                ("playSound".equals(method.name) || "c".equals(method.name))
                        && ("(Lnet/minecraft/client/audio/ISound;)V"
                                        .equals(method.desc)
                                || "(Lbpj;)V".equals(method.desc));
        if (playSound && !containsHook(method, "onSoundManagerPlay")) {
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
            hook.add(hookCall(
                    "onSoundManagerPlay",
                    "(Ljava/lang/Object;)V"));
            method.instructions.insert(hook);
            return true;
        }
        return false;
    }

    private static boolean transformChunkDispatcher(MethodNode method) {
        boolean nextUpdateName =
                "getNextChunkUpdate".equals(method.name)
                        || "d".equals(method.name);
        boolean nextUpdateDescriptor =
                ("()Lnet/minecraft/client/renderer/chunk/"
                                + "ChunkCompileTaskGenerator;").equals(method.desc)
                        || "()Lbhn;".equals(method.desc);
        if (nextUpdateName
                && nextUpdateDescriptor
                && !containsHook(method, "onNextChunkUpdate")) {
            method.instructions.insert(hookCall("onNextChunkUpdate", "()V"));
            return true;
        }
        return false;
    }

    private static boolean transformBlock(MethodNode method) {
        boolean shouldRenderName =
                ("shouldSideBeRendered".equals(method.name)
                        || "a".equals(method.name));
        boolean shouldRenderDescriptor =
                ("(Lnet/minecraft/world/IBlockAccess;"
                                + "Lnet/minecraft/util/BlockPos;"
                                + "Lnet/minecraft/util/EnumFacing;)Z")
                                .equals(method.desc)
                        || "(Ladq;Lcj;Lcq;)Z".equals(method.desc);
        if (shouldRenderName
                && shouldRenderDescriptor
                && !containsHook(method, "onShouldSideBeRendered")) {
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
            hook.add(hookCall(
                    "onShouldSideBeRendered",
                    "(ZLjava/lang/Object;)Z"));
            insertBeforeOpcode(method, Opcodes.IRETURN, hook);
            return true;
        }
        return false;
    }

    private static boolean transformScreenshotHelper(MethodNode method) {
        boolean screenshotName =
                "saveScreenshot".equals(method.name) || "a".equals(method.name);
        boolean screenshotDescriptor =
                ("(Ljava/io/File;Ljava/lang/String;II"
                                + "Lnet/minecraft/client/shader/Framebuffer;)"
                                + "Lnet/minecraft/util/IChatComponent;")
                                .equals(method.desc)
                        || "(Ljava/io/File;Ljava/lang/String;IILbfw;)Leu;"
                                .equals(method.desc);
        boolean target = screenshotName && screenshotDescriptor;
        if (!target || containsHook(method, "onAsyncScreenshot")) {
            return false;
        }

        LabelNode continueLabel = new LabelNode();
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
        hook.add(new VarInsnNode(Opcodes.ILOAD, 2));
        hook.add(new VarInsnNode(Opcodes.ILOAD, 3));
        hook.add(new VarInsnNode(Opcodes.ALOAD, 4));
        hook.add(hookCall(
                "onAsyncScreenshot",
                "(Ljava/io/File;Ljava/lang/String;II"
                        + "Ljava/lang/Object;)Ljava/lang/Object;"));
        hook.add(new org.objectweb.asm.tree.InsnNode(Opcodes.DUP));
        hook.add(new JumpInsnNode(Opcodes.IFNULL, continueLabel));
        hook.add(new TypeInsnNode(
                Opcodes.CHECKCAST,
                method.desc.endsWith("Leu;")
                        ? "eu"
                        : "net/minecraft/util/IChatComponent"));
        hook.add(new org.objectweb.asm.tree.InsnNode(Opcodes.ARETURN));
        hook.add(continueLabel);
        hook.add(new org.objectweb.asm.tree.InsnNode(Opcodes.POP));
        method.instructions.insert(hook);
        return true;
    }

    private static boolean transformTabOverlay(MethodNode method) {
        boolean playerListName =
                "renderPlayerlist".equals(method.name) || "a".equals(method.name);
        boolean playerListDescriptor =
                ("(ILnet/minecraft/scoreboard/Scoreboard;"
                                + "Lnet/minecraft/scoreboard/ScoreObjective;)V")
                                .equals(method.desc)
                        || "(ILauo;Lauk;)V".equals(method.desc);
        boolean renderPlayerList = playerListName && playerListDescriptor;
        if (!renderPlayerList) {
            return false;
        }

        boolean changed = false;
        for (AbstractInsnNode instruction = method.instructions.getFirst();
                instruction != null; ) {
            AbstractInsnNode next = instruction.getNext();
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                boolean deobfuscatedPlayerLookup =
                        "net/minecraft/client/multiplayer/WorldClient"
                                        .equals(call.owner)
                                && "getPlayerEntityByUUID".equals(call.name)
                                && ("(Ljava/util/UUID;)"
                                                + "Lnet/minecraft/entity/player/"
                                                + "EntityPlayer;")
                                                .equals(call.desc);
                boolean obfuscatedPlayerLookup =
                        "bdb".equals(call.owner)
                                && "b".equals(call.name)
                                && "(Ljava/util/UUID;)Lwn;".equals(call.desc);
                boolean playerLookup =
                        deobfuscatedPlayerLookup || obfuscatedPlayerLookup;
                if (playerLookup) {
                    InsnList hook = new InsnList();
                    hook.add(hookCall(
                            "onTabPlayerHead",
                            "(Ljava/lang/Object;)Ljava/lang/Object;"));
                    hook.add(new TypeInsnNode(
                            Opcodes.CHECKCAST,
                            call.desc.endsWith("Lwn;")
                                    ? "wn"
                                    : "net/minecraft/entity/player/EntityPlayer"));
                    method.instructions.insert(call, hook);
                    changed = true;
                }

                boolean integrated =
                        ("net/minecraft/client/Minecraft".equals(call.owner)
                                && "isIntegratedServerRunning".equals(call.name)
                                && "()Z".equals(call.desc))
                        || ("ave".equals(call.owner)
                                && "E".equals(call.name)
                                && "()Z".equals(call.desc));
                boolean encrypted =
                        ("net/minecraft/network/NetworkManager".equals(call.owner)
                                && "getIsencrypted".equals(call.name)
                                && "()Z".equals(call.desc))
                        || ("ek".equals(call.owner)
                                && "f".equals(call.name)
                                && "()Z".equals(call.desc));
                if (integrated || encrypted) {
                    method.instructions.insert(
                            call,
                            hookCall("onTabShowHeads", "(Z)Z"));
                    changed = true;
                }
            } else if (instruction instanceof LdcInsnNode) {
                Object constant = ((LdcInsnNode) instruction).cst;
                if (Integer.valueOf(Integer.MIN_VALUE).equals(constant)
                        || Integer.valueOf(553648127).equals(constant)) {
                    method.instructions.insert(
                            instruction,
                            hookCall("onTabBackgroundColor", "(I)I"));
                    changed = true;
                }
            }
            instruction = next;
        }
        return changed;
    }

    private static boolean transformRenderItem(MethodNode method) {
        boolean renderEffectName =
                "renderEffect".equals(method.name) || "a".equals(method.name);
        boolean renderEffectDescriptor =
                ("(Lnet/minecraft/client/resources/model/"
                                + "IBakedModel;)V").equals(method.desc)
                        || "(Lboq;)V".equals(method.desc);
        boolean renderEffect = renderEffectName && renderEffectDescriptor;
        if (renderEffect && !containsHook(method, "renderItemEffect")) {
            method.instructions.clear();
            method.tryCatchBlocks.clear();
            method.localVariables = null;
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            method.instructions.add(hookCall(
                    "renderItemEffect",
                    "(Ljava/lang/Object;Ljava/lang/Object;)V"));
            method.instructions.add(
                    new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
            return true;
        }

        boolean guiItemName =
                "renderItemAndEffectIntoGUI".equals(method.name)
                        || "b".equals(method.name);
        boolean guiItemDescriptor =
                "(Lnet/minecraft/item/ItemStack;II)V".equals(method.desc)
                        || "(Lzx;II)V".equals(method.desc);
        boolean guiItem = guiItemName && guiItemDescriptor;
        boolean guiOverlayName =
                "renderItemOverlayIntoGUI".equals(method.name)
                        || "a".equals(method.name);
        boolean guiOverlayDescriptor =
                ("(Lnet/minecraft/client/gui/FontRenderer;"
                                + "Lnet/minecraft/item/ItemStack;II"
                                + "Ljava/lang/String;)V").equals(method.desc)
                        || "(Lavn;Lzx;IILjava/lang/String;)V"
                                .equals(method.desc);
        boolean guiOverlay = guiOverlayName && guiOverlayDescriptor;
        if ((guiItem || guiOverlay)
                && !containsHook(method, "onRenderItemGui")) {
            method.instructions.insert(hookCall("onRenderItemGui", "()V"));
            return true;
        }
        return false;
    }

    private static boolean transformRenderEntityItem(MethodNode method) {
        boolean positionName =
                "func_177077_a".equals(method.name) || "a".equals(method.name);
        boolean positionDescriptor =
                ("(Lnet/minecraft/entity/item/EntityItem;DDDF"
                                + "Lnet/minecraft/client/resources/model/"
                                + "IBakedModel;)I").equals(method.desc)
                        || "(Luz;DDDFLboq;)I".equals(method.desc);
        boolean position = positionName && positionDescriptor;
        if (position && !containsHook(method, "positionDroppedItem")) {
            method.instructions.clear();
            method.tryCatchBlocks.clear();
            method.localVariables = null;
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            method.instructions.add(new VarInsnNode(Opcodes.DLOAD, 2));
            method.instructions.add(new VarInsnNode(Opcodes.DLOAD, 4));
            method.instructions.add(new VarInsnNode(Opcodes.DLOAD, 6));
            method.instructions.add(new VarInsnNode(Opcodes.FLOAD, 8));
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 9));
            method.instructions.add(hookCall(
                    "positionDroppedItem",
                    "(Ljava/lang/Object;Ljava/lang/Object;DDDF"
                            + "Ljava/lang/Object;)I"));
            method.instructions.add(
                    new org.objectweb.asm.tree.InsnNode(Opcodes.IRETURN));
            return true;
        }

        boolean doRender =
                ("doRender".equals(method.name) || "a".equals(method.name))
                        && ("(Lnet/minecraft/entity/item/EntityItem;DDDFF)V"
                                        .equals(method.desc)
                                || "(Luz;DDDFF)V".equals(method.desc));
        if (!doRender || containsHook(method, "renderDroppedItem")) {
            return false;
        }
        boolean changed = false;
        for (AbstractInsnNode instruction = method.instructions.getFirst();
                instruction != null;
                instruction = instruction.getNext()) {
            if (!(instruction instanceof MethodInsnNode)) {
                continue;
            }
            MethodInsnNode call = (MethodInsnNode) instruction;
            boolean render =
                    ("net/minecraft/client/renderer/entity/RenderItem"
                                            .equals(call.owner)
                                    && "renderItem".equals(call.name)
                                    && ("(Lnet/minecraft/item/ItemStack;"
                                                    + "Lnet/minecraft/client/"
                                                    + "resources/model/IBakedModel;)V")
                                                    .equals(call.desc))
                            || ("bjh".equals(call.owner)
                                    && "a".equals(call.name)
                                    && "(Lzx;Lboq;)V".equals(call.desc));
            if (render) {
                call.setOpcode(Opcodes.INVOKESTATIC);
                call.owner = HOOK_OWNER;
                call.name = "renderDroppedItem";
                call.desc = "(Ljava/lang/Object;Ljava/lang/Object;"
                        + "Ljava/lang/Object;)V";
                call.itf = false;
                changed = true;
            }
        }
        return changed;
    }

    private static boolean transformDeadmau5Layer(MethodNode method) {
        boolean renderName =
                "doRenderLayer".equals(method.name) || "a".equals(method.name);
        boolean renderDescriptor =
                ("(Lnet/minecraft/client/entity/"
                                + "AbstractClientPlayer;FFFFFFF)V")
                                .equals(method.desc)
                        || "(Lbet;FFFFFFF)V".equals(method.desc);
        boolean render = renderName && renderDescriptor;
        if (!render || containsHook(method, "renderEars")) {
            return false;
        }
        method.instructions.clear();
        method.tryCatchBlocks.clear();
        method.localVariables = null;
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        method.instructions.add(new VarInsnNode(Opcodes.FLOAD, 4));
        method.instructions.add(hookCall(
                "renderEars",
                "(Ljava/lang/Object;Ljava/lang/Object;F)V"));
        method.instructions.add(
                new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
        return true;
    }

    private static boolean transformMouseHelper(MethodNode method) {
        if (matches(method, "()V", "mouseXYChange", "c")
                && !containsHook(method, "onRawMouse")) {
            injectCancelableHead(
                    method,
                    "onRawMouse",
                    "(Ljava/lang/Object;)Z",
                    0);
            return true;
        }
        return false;
    }

    private static void insertResourceHook(
            String ownerName,
            MethodNode method,
            String hookName) {
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(hookCall(
                hookName,
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"));
        hook.add(new TypeInsnNode(
                Opcodes.CHECKCAST,
                ABSTRACT_PLAYER_NOTCH.equals(ownerName)
                        ? "jy"
                        : "net/minecraft/util/ResourceLocation"));
        insertBeforeOpcode(method, Opcodes.ARETURN, hook);
    }

    private static boolean matches(
            MethodNode method,
            String descriptor,
            String deobfuscatedName,
            String notchName) {
        return descriptor.equals(method.desc)
                && (deobfuscatedName.equals(method.name) || notchName.equals(method.name));
    }

    private static boolean containsHook(MethodNode method, String hookName) {
        for (AbstractInsnNode instruction = method.instructions.getFirst();
                instruction != null;
                instruction = instruction.getNext()) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (HOOK_OWNER.equals(call.owner) && hookName.equals(call.name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static MethodInsnNode hookCall(String hookName, String descriptor) {
        return new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                HOOK_OWNER,
                hookName,
                descriptor,
                false);
    }

    private static void injectBeforeReturns(MethodNode method, AbstractInsnNode hook) {
        InsnList list = new InsnList();
        list.add(hook);
        injectBeforeReturns(method, list);
    }

    private static void injectBeforeReturns(MethodNode method, InsnList hookTemplate) {
        for (AbstractInsnNode instruction = method.instructions.getFirst();
                instruction != null; ) {
            AbstractInsnNode next = instruction.getNext();
            if (instruction.getOpcode() == Opcodes.RETURN) {
                method.instructions.insertBefore(instruction, cloneInstructions(hookTemplate));
            }
            instruction = next;
        }
    }

    private static void insertBeforeOpcode(
            MethodNode method,
            int opcode,
            AbstractInsnNode hook) {
        InsnList list = new InsnList();
        list.add(hook);
        insertBeforeOpcode(method, opcode, list);
    }

    private static void insertBeforeOpcode(
            MethodNode method,
            int opcode,
            InsnList hookTemplate) {
        for (AbstractInsnNode instruction = method.instructions.getFirst();
                instruction != null; ) {
            AbstractInsnNode next = instruction.getNext();
            if (instruction.getOpcode() == opcode) {
                method.instructions.insertBefore(instruction, cloneInstructions(hookTemplate));
            }
            instruction = next;
        }
    }

    private static InsnList cloneInstructions(InsnList source) {
        InsnList clone = new InsnList();
        for (AbstractInsnNode instruction = source.getFirst();
                instruction != null;
                instruction = instruction.getNext()) {
            clone.add(instruction.clone(null));
        }
        return clone;
    }

    private static void injectCancelableHead(
            MethodNode method,
            String hookName,
            String descriptor,
            int argumentIndex) {
        LabelNode continueLabel = new LabelNode();
        InsnList hook = new InsnList();
        if (argumentIndex >= 0) {
            hook.add(new VarInsnNode(Opcodes.ALOAD, argumentIndex));
        }
        hook.add(hookCall(hookName, descriptor));
        hook.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
        hook.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
        hook.add(continueLabel);
        method.instructions.insert(hook);
    }

    private static void injectCancelableFloatHead(
            MethodNode method,
            String hookName,
            String descriptor,
            int argumentIndex) {
        LabelNode continueLabel = new LabelNode();
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.FLOAD, argumentIndex));
        hook.add(hookCall(hookName, descriptor));
        hook.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
        hook.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
        hook.add(continueLabel);
        method.instructions.insert(hook);
    }

    private static boolean isTarget(String name) {
        return isMinecraft(name)
                || isGuiIngame(name)
                || isEntityRenderer(name)
                || isPlayerSp(name)
                || isNetworkManager(name)
                || isEntityPlayer(name)
                || isEntityLiving(name)
                || isAbstractPlayer(name)
                || isModelPlayer(name)
                || isLivingRenderer(name)
                || isPlayerRenderer(name)
                || isItemRenderer(name)
                || isPlayHandler(name)
                || isWorldClient(name)
                || isFontRenderer(name)
                || isTextureMap(name)
                || isRenderChunk(name)
                || isChunkContainer(name)
                || isRenderManager(name)
                || isTntRenderer(name)
                || isWorld(name)
                || isWorldInfo(name)
                || isWorldChunkManager(name)
                || isEntity(name)
                || isSoundManager(name)
                || isChunkDispatcher(name)
                || isBlock(name)
                || isScreenshotHelper(name)
                || isTabOverlay(name)
                || isRenderItem(name)
                || isRenderEntityItem(name)
                || isDeadmau5Layer(name)
                || isMouseHelper(name);
    }

    private static boolean isMinecraft(String name) {
        return MINECRAFT_DEOBF.equals(name) || MINECRAFT_NOTCH.equals(name);
    }

    private static boolean isGuiIngame(String name) {
        return GUI_INGAME_DEOBF.equals(name) || GUI_INGAME_NOTCH.equals(name);
    }

    private static boolean isEntityRenderer(String name) {
        return ENTITY_RENDERER_DEOBF.equals(name) || ENTITY_RENDERER_NOTCH.equals(name);
    }

    private static boolean isPlayerSp(String name) {
        return PLAYER_SP_DEOBF.equals(name) || PLAYER_SP_NOTCH.equals(name);
    }

    private static boolean isNetworkManager(String name) {
        return NETWORK_MANAGER_DEOBF.equals(name) || NETWORK_MANAGER_NOTCH.equals(name);
    }

    private static boolean isEntityPlayer(String name) {
        return ENTITY_PLAYER_DEOBF.equals(name) || ENTITY_PLAYER_NOTCH.equals(name);
    }

    private static boolean isEntityLiving(String name) {
        return ENTITY_LIVING_DEOBF.equals(name) || ENTITY_LIVING_NOTCH.equals(name);
    }

    private static boolean isAbstractPlayer(String name) {
        return ABSTRACT_PLAYER_DEOBF.equals(name) || ABSTRACT_PLAYER_NOTCH.equals(name);
    }

    private static boolean isModelPlayer(String name) {
        return MODEL_PLAYER_DEOBF.equals(name) || MODEL_PLAYER_NOTCH.equals(name);
    }

    private static boolean isLivingRenderer(String name) {
        return LIVING_RENDERER_DEOBF.equals(name) || LIVING_RENDERER_NOTCH.equals(name);
    }

    private static boolean isPlayerRenderer(String name) {
        return PLAYER_RENDERER_DEOBF.equals(name) || PLAYER_RENDERER_NOTCH.equals(name);
    }

    private static boolean isItemRenderer(String name) {
        return ITEM_RENDERER_DEOBF.equals(name) || ITEM_RENDERER_NOTCH.equals(name);
    }

    private static boolean isPlayHandler(String name) {
        return PLAY_HANDLER_DEOBF.equals(name) || PLAY_HANDLER_NOTCH.equals(name);
    }

    private static boolean isWorldClient(String name) {
        return WORLD_CLIENT_DEOBF.equals(name) || WORLD_CLIENT_NOTCH.equals(name);
    }

    private static boolean isFontRenderer(String name) {
        return FONT_RENDERER_DEOBF.equals(name) || FONT_RENDERER_NOTCH.equals(name);
    }

    private static boolean isTextureMap(String name) {
        return TEXTURE_MAP_DEOBF.equals(name) || TEXTURE_MAP_NOTCH.equals(name);
    }

    private static boolean isRenderChunk(String name) {
        return RENDER_CHUNK_DEOBF.equals(name) || RENDER_CHUNK_NOTCH.equals(name);
    }

    private static boolean isChunkContainer(String name) {
        return CHUNK_CONTAINER_DEOBF.equals(name) || CHUNK_CONTAINER_NOTCH.equals(name);
    }

    private static boolean isRenderManager(String name) {
        return RENDER_MANAGER_DEOBF.equals(name) || RENDER_MANAGER_NOTCH.equals(name);
    }

    private static boolean isTntRenderer(String name) {
        return TNT_RENDERER_DEOBF.equals(name) || TNT_RENDERER_NOTCH.equals(name);
    }

    private static boolean isWorld(String name) {
        return WORLD_DEOBF.equals(name) || WORLD_NOTCH.equals(name);
    }

    private static boolean isWorldInfo(String name) {
        return WORLD_INFO_DEOBF.equals(name) || WORLD_INFO_NOTCH.equals(name);
    }

    private static boolean isWorldChunkManager(String name) {
        return WORLD_CHUNK_MANAGER_DEOBF.equals(name)
                || WORLD_CHUNK_MANAGER_NOTCH.equals(name);
    }

    private static boolean isEntity(String name) {
        return ENTITY_DEOBF.equals(name) || ENTITY_NOTCH.equals(name);
    }

    private static boolean isSoundManager(String name) {
        return SOUND_MANAGER_DEOBF.equals(name)
                || SOUND_MANAGER_NOTCH.equals(name);
    }

    private static boolean isChunkDispatcher(String name) {
        return CHUNK_DISPATCHER_DEOBF.equals(name)
                || CHUNK_DISPATCHER_NOTCH.equals(name);
    }

    private static boolean isBlock(String name) {
        return BLOCK_DEOBF.equals(name) || BLOCK_NOTCH.equals(name);
    }

    private static boolean isScreenshotHelper(String name) {
        return SCREENSHOT_HELPER_DEOBF.equals(name)
                || SCREENSHOT_HELPER_NOTCH.equals(name);
    }

    private static boolean isTabOverlay(String name) {
        return TAB_OVERLAY_DEOBF.equals(name)
                || TAB_OVERLAY_NOTCH.equals(name);
    }

    private static boolean isRenderItem(String name) {
        return RENDER_ITEM_DEOBF.equals(name)
                || RENDER_ITEM_NOTCH.equals(name);
    }

    private static boolean isRenderEntityItem(String name) {
        return RENDER_ENTITY_ITEM_DEOBF.equals(name)
                || RENDER_ENTITY_ITEM_NOTCH.equals(name);
    }

    private static boolean isDeadmau5Layer(String name) {
        return DEADMAU5_LAYER_DEOBF.equals(name)
                || DEADMAU5_LAYER_NOTCH.equals(name);
    }

    private static boolean isMouseHelper(String name) {
        return MOUSE_HELPER_DEOBF.equals(name)
                || MOUSE_HELPER_NOTCH.equals(name);
    }

    private static final class SafeClassWriter extends ClassWriter {

        private SafeClassWriter(int flags) {
            super(flags);
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            try {
                return super.getCommonSuperClass(type1, type2);
            } catch (LinkageError | RuntimeException unavailableType) {
                return "java/lang/Object";
            }
        }
    }
}
