#include <Windows.h>
#include <jni.h>
#include <jvmti.h>

#include <cstring>

namespace flax_compat {

using GetCreatedJavaVMs = jint(JNICALL*)(JavaVM**, jsize, jsize*);

constexpr int lunar_jar_resource_id = 101;
constexpr int dawn_jar_resource_id = 102;

bool minecraft_class_uses_srg_members(jvmtiEnv* jvmti, jclass minecraft_class) {
    jint field_count = 0;
    jfieldID* fields = nullptr;
    if (jvmti->GetClassFields(minecraft_class, &field_count, &fields) !=
            JVMTI_ERROR_NONE ||
        fields == nullptr) {
        return false;
    }

    bool srg = false;
    for (jint index = 0; index < field_count; ++index) {
        char* name = nullptr;
        char* signature = nullptr;
        char* generic = nullptr;
        if (jvmti->GetFieldName(
                minecraft_class,
                fields[index],
                &name,
                &signature,
                &generic) == JVMTI_ERROR_NONE &&
            name != nullptr) {
            if (std::strcmp(name, "field_71439_g") == 0 ||
                std::strcmp(name, "field_71441_e") == 0 ||
                std::strcmp(name, "field_71428_T") == 0) {
                srg = true;
            }
        }
        if (name != nullptr) {
            jvmti->Deallocate(reinterpret_cast<unsigned char*>(name));
        }
        if (signature != nullptr) {
            jvmti->Deallocate(reinterpret_cast<unsigned char*>(signature));
        }
        if (generic != nullptr) {
            jvmti->Deallocate(reinterpret_cast<unsigned char*>(generic));
        }
        if (srg) {
            break;
        }
    }

    jvmti->Deallocate(reinterpret_cast<unsigned char*>(fields));
    return srg;
}

int select_client_jar_resource() {
    HMODULE jvm_module = GetModuleHandleW(L"jvm.dll");
    if (jvm_module == nullptr) {
        return lunar_jar_resource_id;
    }

    const auto get_created_vms = reinterpret_cast<GetCreatedJavaVMs>(
        GetProcAddress(jvm_module, "JNI_GetCreatedJavaVMs"));
    if (get_created_vms == nullptr) {
        return lunar_jar_resource_id;
    }

    JavaVM* vm = nullptr;
    jsize vm_count = 0;
    if (get_created_vms(&vm, 1, &vm_count) != JNI_OK || vm == nullptr ||
        vm_count == 0) {
        return lunar_jar_resource_id;
    }

    jvmtiEnv* jvmti = nullptr;
    if (vm->GetEnv(
            reinterpret_cast<void**>(&jvmti),
            JVMTI_VERSION_1_2) != JNI_OK ||
        jvmti == nullptr) {
        return lunar_jar_resource_id;
    }

    jint class_count = 0;
    jclass* classes = nullptr;
    if (jvmti->GetLoadedClasses(&class_count, &classes) != JVMTI_ERROR_NONE ||
        classes == nullptr) {
        return lunar_jar_resource_id;
    }

    int selected = lunar_jar_resource_id;
    for (jint index = 0; index < class_count; ++index) {
        char* signature = nullptr;
        if (jvmti->GetClassSignature(classes[index], &signature, nullptr) !=
                JVMTI_ERROR_NONE ||
            signature == nullptr) {
            continue;
        }

        const bool minecraft =
            std::strcmp(signature, "Lnet/minecraft/client/Minecraft;") == 0;
        jvmti->Deallocate(reinterpret_cast<unsigned char*>(signature));
        if (!minecraft) {
            continue;
        }

        // Lunar 1.8.9 exposes MCP/readable member names, while the
        // Forge/Feather-style 1.8.9 runtime exposes the same readable class
        // name with SRG members such as field_71439_g. Dawn is the successor
        // to Feather, so select the SRG-reobfuscated jar for that layout.
        selected = minecraft_class_uses_srg_members(jvmti, classes[index])
                       ? dawn_jar_resource_id
                       : lunar_jar_resource_id;
        break;
    }

    jvmti->Deallocate(reinterpret_cast<unsigned char*>(classes));
    return selected;
}

LPWSTR make_int_resource(WORD id) {
    const WORD selected = static_cast<WORD>(
        id == lunar_jar_resource_id ? select_client_jar_resource() : id);
    return reinterpret_cast<LPWSTR>(static_cast<ULONG_PTR>(selected));
}

}  // namespace flax_compat

// FlaxClient.cpp already has a stable attach/JVMTI implementation. Keep that
// implementation intact and only intercept its embedded JAR resource lookup.
// CMake generates FlaxClientPatched.cpp from it with one additional change:
// the Java transformer entry point is routed through CompatibleLateClassTransformer.
#ifdef MAKEINTRESOURCEW
#undef MAKEINTRESOURCEW
#endif
#define MAKEINTRESOURCEW(id) \
    (::flax_compat::make_int_resource(static_cast<WORD>(id)))

#include "FlaxClientPatched.cpp"
