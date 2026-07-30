package org.lwjgl.opengl;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.lwjgl.system.FunctionProvider;

/**
 * Minimal LWJGL 3-style function-provider bridge backed by Minecraft 1.8.9's
 * active LWJGL 2 OpenGL context. NanoVG discovers this class reflectively.
 */
public final class GL implements FunctionProvider {

    private static final GL FUNCTION_PROVIDER = new GL();

    private GL() {
    }

    public static ContextCapabilities getCapabilities() {
        return GLContext.getCapabilities();
    }

    public static FunctionProvider getFunctionProvider() {
        return FUNCTION_PROVIDER;
    }

    @Override
    public long getFunctionAddress(CharSequence functionName) {
        return GLContext.getFunctionAddress(functionName.toString());
    }

    @Override
    public long getFunctionAddress(ByteBuffer functionName) {
        ByteBuffer value = functionName.duplicate();
        int length = 0;
        while (length < value.remaining()
                && value.get(value.position() + length) != 0) {
            length++;
        }
        byte[] bytes = new byte[length];
        value.get(bytes);
        return getFunctionAddress(new String(bytes, StandardCharsets.US_ASCII));
    }
}
