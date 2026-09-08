package me.eldodebug.soar.attach.modern;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Current Minecraft 26.2 transformer entry point.
 *
 * <p>The versioned name also lets a repaired runtime be loaded into a JVM in
 * which an older compatibility transformer was already defined.
 */
public final class Modern26LateClassTransformer {

    private static final String HOOKS =
            "me/eldodebug/soar/attach/modern/Modern26LateHooks";

    private Modern26LateClassTransformer() {
    }

    public static byte[] transform(String internalName, byte[] classBytes) {
        if (classBytes == null ||
                !("net/minecraft/client/Minecraft".equals(internalName) ||
                  "net/minecraft/client/gui/Gui".equals(internalName) ||
                  "net/minecraft/client/Camera".equals(internalName))) {
            return classBytes;
        }
        try {
            ClassReader reader = new ClassReader(classBytes);
            ClassNode node = new ClassNode(Opcodes.ASM9);
            reader.accept(node, 0);
            boolean changed = false;
            for (MethodNode method : node.methods) {
                if ("net/minecraft/client/Minecraft".equals(internalName) &&
                        "tick".equals(method.name) && "()V".equals(method.desc)) {
                    changed |= injectTick(method);
                } else if ("net/minecraft/client/gui/Gui".equals(internalName) &&
                        "extractRenderState".equals(method.name) &&
                        method.desc.endsWith("ZZ)V")) {
                    changed |= injectHud(method);
                } else if ("net/minecraft/client/Camera".equals(internalName) &&
                        "extractRenderState".equals(method.name) &&
                        method.desc.startsWith("(Lnet/minecraft/client/renderer/state/level/CameraRenderState;")) {
                    changed |= injectCamera(method);
                }
            }
            if (!changed) {
                return classBytes;
            }
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            return writer.toByteArray();
        } catch (Throwable error) {
            System.err.println("FlaxClient 26.2 transform failed for " +
                    internalName + ": " + error);
            return classBytes;
        }
    }

    private static boolean injectTick(MethodNode method) {
        if (containsHook(method, "onClientTick")) {
            return false;
        }
        method.instructions.insert(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                HOOKS,
                "onClientTick",
                "()V",
                false));
        return true;
    }

    private static boolean injectHud(MethodNode method) {
        if (containsHook(method, "onHudRender")) {
            return false;
        }
        boolean changed = false;
        for (AbstractInsnNode instruction = method.instructions.getFirst();
             instruction != null;) {
            AbstractInsnNode next = instruction.getNext();
            if (instruction.getOpcode() == Opcodes.RETURN) {
                InsnList hook = new InsnList();
                hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
                hook.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        HOOKS,
                        "onHudRender",
                        "(Ljava/lang/Object;)V",
                        false));
                method.instructions.insertBefore(instruction, hook);
                changed = true;
            }
            instruction = next;
        }
        return changed;
    }

    private static boolean injectCamera(MethodNode method) {
        if (containsHook(method, "onCameraBeforeExtract")) {
            return false;
        }
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                HOOKS,
                "onCameraBeforeExtract",
                "(Ljava/lang/Object;)V",
                false));
        method.instructions.insert(hook);
        return true;
    }

    private static boolean containsHook(MethodNode method, String hookName) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (HOOKS.equals(call.owner) && hookName.equals(call.name)) {
                    return true;
                }
            }
        }
        return false;
    }
}
