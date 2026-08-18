package me.eldodebug.soar.attach;

/**
 * Native-facing transformer entry point shared by Lunar and Dawn builds.
 *
 * <p>Lunar keeps the existing MCP/readable transformation path. The Dawn jar
 * contains an MCP-to-SRG mapping resource, which activates SrgMappingAdapter
 * and lets the same LateClassTransformer operate against SRG member names.</p>
 */
public final class CompatibleLateClassTransformer {

    private CompatibleLateClassTransformer() {
    }

    public static byte[] transform(String internalName, byte[] originalBytes) {
        if (SrgMappingAdapter.isAvailable()) {
            return SrgMappingAdapter.transform(internalName, originalBytes);
        }
        return LateClassTransformer.transform(internalName, originalBytes);
    }
}
