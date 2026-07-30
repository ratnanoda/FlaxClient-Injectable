package me.eldodebug.soar.attach;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.invoke.MethodHandles;
import java.security.ProtectionDomain;

import org.lwjgl.opengl.GLContext;

/**
 * Defines the small NanoVG function-provider bridge with LWJGL 2's protection
 * domain. The LWJGL 2 OpenGL package is sealed, so loading the same class
 * directly from the attached client jar would correctly be rejected.
 */
final class AttachOpenGLBridge {

    private static final String BRIDGE_NAME = "org.lwjgl.opengl.GL";
    private static volatile boolean prepared;

    private AttachOpenGLBridge() {
    }

    static synchronized void prepare() throws Exception {
        if (prepared) {
            return;
        }

        ClassLoader targetLoader = GLContext.class.getClassLoader();
        if (!isLoaded(targetLoader)) {
            byte[] bytes = readBridgeBytes();
            if (!defineWithJava9Lookup(bytes)) {
                defineWithJava8Reflection(targetLoader, bytes);
            }
        }
        prepared = true;
    }

    private static boolean isLoaded(ClassLoader targetLoader) {
        try {
            Class.forName(BRIDGE_NAME, false, targetLoader);
            return true;
        } catch (ClassNotFoundException missing) {
            return false;
        }
    }

    private static boolean defineWithJava9Lookup(byte[] bytes) throws Exception {
        try {
            Method privateLookupIn = MethodHandles.class.getMethod(
                    "privateLookupIn",
                    Class.class,
                    MethodHandles.Lookup.class);
            Object lookup = privateLookupIn.invoke(
                    null,
                    GLContext.class,
                    MethodHandles.lookup());
            Method defineClass = MethodHandles.Lookup.class.getMethod(
                    "defineClass",
                    byte[].class);
            try {
                defineClass.invoke(lookup, new Object[] {bytes});
            } catch (InvocationTargetException error) {
                Throwable cause = error.getCause();
                if (!(cause instanceof LinkageError)) {
                    throw error;
                }
            }
            return true;
        } catch (NoSuchMethodException java8) {
            return false;
        }
    }

    private static void defineWithJava8Reflection(
            ClassLoader targetLoader,
            byte[] bytes) throws Exception {
            Method defineClass = ClassLoader.class.getDeclaredMethod(
                    "defineClass",
                    String.class,
                    byte[].class,
                    int.class,
                    int.class,
                    ProtectionDomain.class);
            defineClass.setAccessible(true);
            try {
                defineClass.invoke(
                        targetLoader,
                        BRIDGE_NAME,
                        bytes,
                        0,
                        bytes.length,
                        GLContext.class.getProtectionDomain());
            } catch (InvocationTargetException error) {
                Throwable cause = error.getCause();
                if (!(cause instanceof LinkageError)) {
                    throw error;
                }
            }
    }

    private static byte[] readBridgeBytes() throws IOException {
        try (InputStream input = AttachOpenGLBridge.class.getResourceAsStream(
                "/org/lwjgl/opengl/GL.class")) {
            if (input == null) {
                throw new IOException("Embedded OpenGL function bridge is missing");
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }
}
