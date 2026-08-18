package me.eldodebug.soar.attach;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

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
    private static final String AMBIGUOUS = "\u0000";
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

        byte[] mcpBytes = remap(originalBytes, MAPPINGS.srgToMcp);
        byte[] transformed = LateClassTransformer.transform(internalName, mcpBytes);
        if (transformed == null) {
            return null;
        }
        return remap(transformed, MAPPINGS.mcpToSrg);
    }

    private static byte[] remap(byte[] input, Remapper remapper) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, 0);
        reader.accept(new ClassRemapper(writer, remapper), 0);
        return writer.toByteArray();
    }

    private static final class MappingData {
        private final MemberRemapper srgToMcp;
        private final MemberRemapper mcpToSrg;

        private MappingData(MemberRemapper srgToMcp, MemberRemapper mcpToSrg) {
            this.srgToMcp = srgToMcp;
            this.mcpToSrg = mcpToSrg;
        }

        private static MappingData load() {
            ClassLoader loader = SrgMappingAdapter.class.getClassLoader();
            InputStream stream = loader == null
                    ? ClassLoader.getSystemResourceAsStream(MAPPING_RESOURCE)
                    : loader.getResourceAsStream(MAPPING_RESOURCE);
            if (stream == null) {
                return null;
            }

            MemberRemapper srgToMcp = new MemberRemapper();
            MemberRemapper mcpToSrg = new MemberRemapper();
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
                return new MappingData(srgToMcp, mcpToSrg);
            } catch (IOException | RuntimeException error) {
                System.err.println("FlaxClient: failed to load Dawn SRG mappings: " + error);
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
            String mapped = exactFields.get(fieldKey(owner, name));
            if (mapped == null) {
                mapped = uniqueValue(globalFields.get(name));
            }
            return mapped == null ? name : mapped;
        }

        @Override
        public String mapMethodName(String owner, String name, String descriptor) {
            String mapped = exactMethods.get(methodKey(owner, name, descriptor));
            if (mapped == null) {
                mapped = uniqueValue(globalMethods.get(methodGlobalKey(name, descriptor)));
            }
            return mapped == null ? name : mapped;
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
