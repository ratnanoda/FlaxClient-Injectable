#include <Windows.h>
#include <jni.h>
#include <jvmti.h>

#include <cstring>

namespace flax_compat {

using GetCreatedJavaVMs = jint(JNICALL*)(JavaVM**, jsize, jsize*);

constexpr int mcp_jar_resource_id = 101;
constexpr int srg_jar_resource_id = 102;
constexpr int notch_jar_resource_id = 103;

enum class MappingNamespace {
    unknown,
    mcp,
    srg,
    notch,
};

MappingNamespace detected_namespace = MappingNamespace::unknown;

MappingNamespace readable_minecraft_namespace(jvmtiEnv* jvmti, jclass minecraft_class) {
    jint field_count = 0;
    jfieldID* fields = nullptr;
    if (jvmti->GetClassFields(minecraft_class, &field_count, &fields) != JVMTI_ERROR_NONE || fields == nullptr) {
        return MappingNamespace::unknown;
    }

    bool mcp = false;
    bool srg = false;
    for (jint index = 0; index < field_count; ++index) {
        char* name = nullptr;
        char* signature = nullptr;
        char* generic = nullptr;
        if (jvmti->GetFieldName(minecraft_class, fields[index], &name, &signature, &generic) == JVMTI_ERROR_NONE && name != nullptr) {
            // MCP 1.8.9 stable_22 names and their SRG counterparts. These are
            // taken from the 1.8.9 mappings rather than from a client brand.
            if (std::strcmp(name, "thePlayer") == 0 ||
                std::strcmp(name, "theWorld") == 0 ||
                std::strcmp(name, "timer") == 0) {
                mcp = true;
            } else if (std::strcmp(name, "field_71439_g") == 0 ||
                       std::strcmp(name, "field_71441_e") == 0 ||
                       std::strcmp(name, "field_71428_T") == 0) {
                srg = true;
            }
        }
        if (name != nullptr) jvmti->Deallocate(reinterpret_cast<unsigned char*>(name));
        if (signature != nullptr) jvmti->Deallocate(reinterpret_cast<unsigned char*>(signature));
        if (generic != nullptr) jvmti->Deallocate(reinterpret_cast<unsigned char*>(generic));
    }

    jvmti->Deallocate(reinterpret_cast<unsigned char*>(fields));
    if (srg) return MappingNamespace::srg;
    if (mcp) return MappingNamespace::mcp;
    return MappingNamespace::unknown;
}

int select_client_jar_resource() {
    detected_namespace = MappingNamespace::unknown;

    HMODULE jvm_module = GetModuleHandleW(L"jvm.dll");
    if (jvm_module == nullptr) return 0;

    const auto get_created_vms = reinterpret_cast<GetCreatedJavaVMs>(
        GetProcAddress(jvm_module, "JNI_GetCreatedJavaVMs"));
    if (get_created_vms == nullptr) return 0;

    JavaVM* vm = nullptr;
    jsize vm_count = 0;
    if (get_created_vms(&vm, 1, &vm_count) != JNI_OK || vm == nullptr || vm_count == 0) {
        return 0;
    }

    jvmtiEnv* jvmti = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&jvmti), JVMTI_VERSION_1_2) != JNI_OK || jvmti == nullptr) {
        return 0;
    }

    jint class_count = 0;
    jclass* classes = nullptr;
    if (jvmti->GetLoadedClasses(&class_count, &classes) != JVMTI_ERROR_NONE || classes == nullptr) {
        return 0;
    }

    int selected = 0;
    for (jint index = 0; index < class_count; ++index) {
        char* signature = nullptr;
        if (jvmti->GetClassSignature(classes[index], &signature, nullptr) != JVMTI_ERROR_NONE || signature == nullptr) {
            continue;
        }

        const bool readable_minecraft =
            std::strcmp(signature, "Lnet/minecraft/client/Minecraft;") == 0;
        const bool notch_minecraft = std::strcmp(signature, "Lave;") == 0;
        jvmti->Deallocate(reinterpret_cast<unsigned char*>(signature));

        if (notch_minecraft) {
            detected_namespace = MappingNamespace::notch;
            selected = notch_jar_resource_id;
            break;
        }
        if (!readable_minecraft) continue;

        detected_namespace = readable_minecraft_namespace(jvmti, classes[index]);
        if (detected_namespace == MappingNamespace::srg) {
            selected = srg_jar_resource_id;
        } else if (detected_namespace == MappingNamespace::mcp) {
            selected = mcp_jar_resource_id;
        }
        break;
    }

    jvmti->Deallocate(reinterpret_cast<unsigned char*>(classes));
    return selected;
}

bool runtime_supported() {
    return select_client_jar_resource() != 0;
}

MappingNamespace mapping_namespace() {
    select_client_jar_resource();
    return detected_namespace;
}

const char* runtime_name() {
    switch (mapping_namespace()) {
        case MappingNamespace::mcp:
            return "Minecraft 1.8.9 (MCP)";
        case MappingNamespace::srg:
            return "Minecraft 1.8.9 (SRG)";
        case MappingNamespace::notch:
            return "Minecraft 1.8.9 (Notch)";
        default:
            return "Unsupported Minecraft runtime";
    }
}

LPWSTR make_int_resource(WORD id) {
    const WORD selected = static_cast<WORD>(
        id == mcp_jar_resource_id ? select_client_jar_resource() : id);
    return reinterpret_cast<LPWSTR>(static_cast<ULONG_PTR>(selected));
}

}  // namespace flax_compat

#ifdef MAKEINTRESOURCEW
#undef MAKEINTRESOURCEW
#endif
#define MAKEINTRESOURCEW(id) \
    (::flax_compat::make_int_resource(static_cast<WORD>(id)))

#include "FlaxClientPatched.cpp"