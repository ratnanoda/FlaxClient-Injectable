package me.eldodebug.soar.attach;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

/**
 * Adapts the existing MCP/notch late transformer to Forge/Feather-style SRG
 * member names without duplicating the transformer implementation.
 *
 * <p>The Dawn jar contains ForgeGradle's generated MCP -> SRG mapping as a
 * resource. Before LateClassTransformer sees a Minecraft class, this adapter
 * temporarily maps SRG member names back to MCP. The transformed bytes are then
 * mapped to SRG again before JVMTI returns them to the running game.</p>
 */
final class SrgMappingAdapter {

    private static final String MAPPING_RESOURCE = "flax/mappings/mcp-srg.srg";
    private static final String CLASS_MAPPING_RESOURCE = "flax/mappings/mcp-notch.srg";
    private static final String AMBIGUOUS = "\u0000";
    private static final Map<String, String> SUPER_CACHE = new HashMap<String, String>();
    private static final MappingData MAPPINGS = MappingData.load();

    private SrgMappingAdapter() {
    }

    static boolean isAvailable() {
        return MAPPINGS != null;
    }

    static byte[] transform(String internalName, byte[] originalBytes) {
        if (MAPPINGS == null) {
            return LateClassTransformer.transform(internalName, originalBytes);
        }

        boolean srgInput = hasSrgNames(originalBytes);
        byte[] mcpBytes = remap(originalBytes, MAPPINGS, srgInput, false);
        byte[] transformed = LateClassTransformer.transform(internalName, mcpBytes);
        if (transformed == null) {
            return null;
        }
        return remap(transformed, MAPPINGS, srgInput, true);
    }

    private static boolean hasSrgNames(byte[] input) {
        ClassReader reader = new ClassReader(input);
        org.objectweb.asm.tree.ClassNode node = new org.objectweb.asm.tree.ClassNode();
        reader.accept(node, 0);
        for (org.objectweb.asm.tree.MethodNode method : node.methods) {
            if (method.name.startsWith("func_")
                    || method.name.startsWith("field_")) {
                return true;
            }
            for (org.objectweb.asm.tree.AbstractInsnNode instruction = method.instructions.getFirst();
                    instruction != null;
                    instruction = instruction.getNext()) {
                if (instruction instanceof org.objectweb.asm.tree.MethodInsnNode
                        && ((org.objectweb.asm.tree.MethodInsnNode) instruction).name.startsWith("func_")) {
                    return true;
                }
                if (instruction instanceof org.objectweb.asm.tree.FieldInsnNode
                        && ((org.objectweb.asm.tree.FieldInsnNode) instruction).name.startsWith("field_")) {
                    return true;
                }
            }
        }
        for (org.objectweb.asm.tree.FieldNode field : node.fields) {
            if (field.name.startsWith("field_")) {
                return true;
            }
        }
        return false;
    }

    private static byte[] remap(
            byte[] input,
            MappingData mappings,
            boolean srgNamespace,
            boolean toRuntime) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, 0);
        reader.accept(new ClassRemapper(
                writer,
                new CompatibilityRemapper(mappings, srgNamespace, toRuntime)), 0);
        return writer.toByteArray();
    }

    private static final class MappingData {
        private final MemberRemapper srgToMcp;
        private final MemberRemapper mcpToSrg;
        private final MemberRemapper notchMembersToMcp;
        private final MemberRemapper mcpMembersToNotch;
        private final Map<String, String> notchClassesToMcp;
        private final Map<String, String> mcpClassesToNotch;

        private MappingData(
                MemberRemapper srgToMcp,
                MemberRemapper mcpToSrg,
                MemberRemapper notchMembersToMcp,
                MemberRemapper mcpMembersToNotch,
                Map<String, String> notchClassesToMcp,
                Map<String, String> mcpClassesToNotch) {
            this.srgToMcp = srgToMcp;
            this.mcpToSrg = mcpToSrg;
            this.notchMembersToMcp = notchMembersToMcp;
            this.mcpMembersToNotch = mcpMembersToNotch;
            this.notchClassesToMcp = notchClassesToMcp;
            this.mcpClassesToNotch = mcpClassesToNotch;
        }

        private static MappingData load() {
            ClassLoader loader = SrgMappingAdapter.class.getClassLoader();
            InputStream stream = loader == null
                    ? ClassLoader.getSystemResourceAsStream(MAPPING_RESOURCE)
                    : loader.getResourceAsStream(MAPPING_RESOURCE);
            if (stream == null) {
                return null;
            }

            InputStream classStream = loader == null
                    ? ClassLoader.getSystemResourceAsStream(CLASS_MAPPING_RESOURCE)
                    : loader.getResourceAsStream(CLASS_MAPPING_RESOURCE);
            if (classStream == null) {
                return null;
            }

            MemberRemapper srgToMcp = new MemberRemapper();
            MemberRemapper mcpToSrg = new MemberRemapper();
            MemberRemapper notchMembersToMcp = new MemberRemapper();
            MemberRemapper mcpMembersToNotch = new MemberRemapper();
            Map<String, String> notchClassesToMcp = new HashMap<String, String>();
            Map<String, String> mcpClassesToNotch = new HashMap<String, String>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("FD: ")) {
                        parseField(line, srgToMcp, mcpToSrg);
                    } else if (line.startsWith("MD: ")) {
                        parseMethod(line, srgToMcp, mcpToSrg);
                    }
                }
                try (BufferedReader classReader = new BufferedReader(
                        new InputStreamReader(classStream, StandardCharsets.UTF_8))) {
                    while ((line = classReader.readLine()) != null) {
                        if (line.startsWith("CL: ")) {
                            String[] parts = line.split("\\s+");
                            if (parts.length >= 3) {
                                notchClassesToMcp.put(parts[2], parts[1]);
                                mcpClassesToNotch.put(parts[1], parts[2]);
                            }
                        } else if (line.startsWith("FD: ")) {
                            parseField(line, notchMembersToMcp, mcpMembersToNotch);
                        } else if (line.startsWith("MD: ")) {
                            parseMethod(line, notchMembersToMcp, mcpMembersToNotch);
                        }
                    }
                }
                return new MappingData(
                        srgToMcp,
                        mcpToSrg,
                        notchMembersToMcp,
                        mcpMembersToNotch,
                        notchClassesToMcp,
                        mcpClassesToNotch);
            } catch (IOException | RuntimeException error) {
                System.err.println("FlaxClient: failed to load Dawn SRG mappings: " + error);
                return null;
            }
        }

        /**
         * Returns the owner and its obfuscated superclass chain in the
         * namespace used by the supplied member mapping. Minecraft frequently
         * emits invokevirtual owners for a subclass even when the method is
         * declared on a superclass (BlockPos -> Vec3i is the common case).
         */
        private List<String> resolveOwners(String owner, boolean ownerIsMcp) {
            String notchOwner = ownerIsMcp
                    ? mcpClassesToNotch.get(owner)
                    : owner;
            if (notchOwner == null) {
                return java.util.Collections.singletonList(owner);
            }
            List<String> result = new ArrayList<String>();
            Set<String> visited = new HashSet<String>();
            String current = notchOwner;
            while (current != null && visited.add(current)) {
                String mapped = ownerIsMcp
                        ? notchClassesToMcp.get(current)
                        : current;
                result.add(mapped == null ? current : mapped);
                current = readSuperName(current);
            }
            return result;
        }

        private static String readSuperName(String owner) {
            synchronized (SUPER_CACHE) {
                if (SUPER_CACHE.containsKey(owner)) {
                    return SUPER_CACHE.get(owner);
                }
            }
            String resource = owner + ".class";
            InputStream stream = null;
            ClassLoader loader = SrgMappingAdapter.class.getClassLoader();
            if (loader != null) {
                stream = loader.getResourceAsStream(resource);
            }
            if (stream == null) {
                stream = ClassLoader.getSystemResourceAsStream(resource);
            }
            if (stream == null) {
                synchronized (SUPER_CACHE) {
                    SUPER_CACHE.put(owner, null);
                }
                return null;
            }
            try (InputStream input = stream) {
                String superName = new ClassReader(input).getSuperName();
                synchronized (SUPER_CACHE) {
                    SUPER_CACHE.put(owner, superName);
                }
                return superName;
            } catch (IOException | RuntimeException ignored) {
                synchronized (SUPER_CACHE) {
                    SUPER_CACHE.put(owner, null);
                }
                return null;
            }
        }

        private static void parseField(
                String line,
                MemberRemapper srgToMcp,
                MemberRemapper mcpToSrg) {
            String[] parts = line.split("\\s+");
            if (parts.length < 3) {
                return;
            }
            MemberName mcp = MemberName.parse(parts[1]);
            MemberName srg = MemberName.parse(parts[2]);
            if (mcp == null || srg == null) {
                return;
            }
            srgToMcp.addField(srg.owner, srg.name, mcp.name);
            mcpToSrg.addField(mcp.owner, mcp.name, srg.name);
        }

        private static void parseMethod(
                String line,
                MemberRemapper srgToMcp,
                MemberRemapper mcpToSrg) {
            String[] parts = line.split("\\s+");
            if (parts.length < 5) {
                return;
            }
            MemberName mcp = MemberName.parse(parts[1]);
            MemberName srg = MemberName.parse(parts[3]);
            if (mcp == null || srg == null) {
                return;
            }
            String mcpDesc = parts[2];
            String srgDesc = parts[4];
            srgToMcp.addMethod(srg.owner, srg.name, srgDesc, mcp.name);
            mcpToSrg.addMethod(mcp.owner, mcp.name, mcpDesc, srg.name);
        }
    }

    /** Maps obfuscated class owners while adapting SRG member names. */
    private static final class CompatibilityRemapper extends Remapper {
        private final MappingData mappings;
        private final boolean srgNamespace;
        private final boolean toNotch;

        private CompatibilityRemapper(
                MappingData mappings,
                boolean srgNamespace,
                boolean toRuntime) {
            this.mappings = mappings;
            this.srgNamespace = srgNamespace;
            this.toNotch = toRuntime;
        }

        @Override
        public String map(String internalName) {
            // Forge/Dawn's SRG namespace only changes member names. Its class
            // names remain net/minecraft/... at runtime. Mapping those class
            // names through the notch table changes the class being
            // retransformed (for example Minecraft -> ave), which JVMTI
            // rejects with JVMTI_ERROR_NAMES_DONT_MATCH (69).
            if (srgNamespace) {
                return internalName;
            }
            Map<String, String> classes = toNotch
                    ? mappings.mcpClassesToNotch
                    : mappings.notchClassesToMcp;
            String mapped = classes.get(internalName);
            return mapped == null ? internalName : mapped;
        }

        @Override
        public String mapFieldName(String owner, String name, String descriptor) {
            // Never apply MCP's global-name fallback to JDK/LWJGL/mod
            // members. Common names such as Display.update otherwise collide
            // with an unrelated Minecraft mapping and become a bogus func_*
            // call at runtime.
            if (!isMinecraftOwner(owner)) {
                return name;
            }
            String memberOwner = !toNotch && srgNamespace ? map(owner) : owner;
            String memberDescriptor = !toNotch && srgNamespace
                    ? mapDesc(descriptor)
                    : descriptor;
            MemberRemapper members;
            if (toNotch) {
                members = srgNamespace
                        ? mappings.mcpToSrg
                        : mappings.mcpMembersToNotch;
            } else {
                members = srgNamespace
                        ? mappings.srgToMcp
                        : mappings.notchMembersToMcp;
            }
            boolean ownerIsMcp = toNotch || srgNamespace;
            for (String candidate : mappings.resolveOwners(memberOwner, ownerIsMcp)) {
                String mapped = members.findFieldName(candidate, name, memberDescriptor);
                if (mapped != null) {
                    return mapped;
                }
            }
            return members.mapFieldName(memberOwner, name, memberDescriptor);
        }

        @Override
        public String mapMethodName(String owner, String name, String descriptor) {
            if (!isMinecraftOwner(owner)) {
                return name;
            }
            String memberOwner = !toNotch && srgNamespace ? map(owner) : owner;
            String memberDescriptor = !toNotch && srgNamespace
                    ? mapMethodDesc(descriptor)
                    : descriptor;
            MemberRemapper members;
            if (toNotch) {
                members = srgNamespace
                        ? mappings.mcpToSrg
                        : mappings.mcpMembersToNotch;
            } else {
                members = srgNamespace
                        ? mappings.srgToMcp
                        : mappings.notchMembersToMcp;
            }
            boolean ownerIsMcp = toNotch || srgNamespace;
            for (String candidate : mappings.resolveOwners(memberOwner, ownerIsMcp)) {
                String mapped = members.findMethodName(candidate, name, memberDescriptor);
                if (mapped != null) {
                    return mapped;
                }
            }
            return members.mapMethodName(memberOwner, name, memberDescriptor);
        }

        private boolean isMinecraftOwner(String owner) {
            return owner != null
                    && (owner.startsWith("net/minecraft/")
                            || mappings.notchClassesToMcp.containsKey(owner)
                            || mappings.mcpClassesToNotch.containsKey(owner));
        }
    }

    private static final class MemberName {
        private final String owner;
        private final String name;

        private MemberName(String owner, String name) {
            this.owner = owner;
            this.name = name;
        }

        private static MemberName parse(String qualifiedName) {
            int separator = qualifiedName.lastIndexOf('/');
            if (separator <= 0 || separator >= qualifiedName.length() - 1) {
                return null;
            }
            return new MemberName(
                    qualifiedName.substring(0, separator),
                    qualifiedName.substring(separator + 1));
        }
    }

    private static final class MemberRemapper extends Remapper {
        private final Map<String, String> exactFields = new HashMap<String, String>();
        private final Map<String, String> globalFields = new HashMap<String, String>();
        private final Map<String, String> exactMethods = new HashMap<String, String>();
        private final Map<String, String> globalMethods = new HashMap<String, String>();

        private void addField(String owner, String name, String mappedName) {
            exactFields.put(fieldKey(owner, name), mappedName);
            putUnique(globalFields, name, mappedName);
        }

        private void addMethod(
                String owner,
                String name,
                String descriptor,
                String mappedName) {
            exactMethods.put(methodKey(owner, name, descriptor), mappedName);
            putUnique(globalMethods, methodGlobalKey(name, descriptor), mappedName);
        }

        @Override
        public String mapFieldName(String owner, String name, String descriptor) {
            String mapped = findFieldName(owner, name, descriptor);
            if (mapped == null) {
                mapped = uniqueValue(globalFields.get(name));
            }
            return mapped == null ? name : mapped;
        }

        private String findFieldName(String owner, String name, String descriptor) {
            return uniqueValue(exactFields.get(fieldKey(owner, name)));
        }

        @Override
        public String mapMethodName(String owner, String name, String descriptor) {
            String mapped = findMethodName(owner, name, descriptor);
            if (mapped == null) {
                mapped = uniqueValue(globalMethods.get(methodGlobalKey(name, descriptor)));
            }
            return mapped == null ? name : mapped;
        }

        private String findMethodName(String owner, String name, String descriptor) {
            return uniqueValue(exactMethods.get(methodKey(owner, name, descriptor)));
        }

        private static String fieldKey(String owner, String name) {
            return owner + '\u0001' + name;
        }

        private static String methodKey(String owner, String name, String descriptor) {
            return owner + '\u0001' + name + '\u0001' + descriptor;
        }

        private static String methodGlobalKey(String name, String descriptor) {
            return name + '\u0001' + descriptor;
        }

        private static void putUnique(
                Map<String, String> map,
                String key,
                String value) {
            String previous = map.get(key);
            if (previous == null) {
                map.put(key, value);
            } else if (!previous.equals(value)) {
                map.put(key, AMBIGUOUS);
            }
        }

        private static String uniqueValue(String value) {
            return AMBIGUOUS.equals(value) ? null : value;
        }
    }
}
