#include <Windows.h>
#include <jni.h>
#include <jvmti.h>

#include <cstring>

namespace flax_compat {

using GetCreatedJavaVMs = jint(JNICALL*)(JavaVM**, jsize, jsize*);

constexpr int lunar_jar_resource_id = 101;
constexpr int dawn_jar_resource_id = 102;

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

        if (std::strcmp(signature, "Lave;") == 0) {
            // Production/notch 1.8.9 names (used by Dawn-style runtimes)
            // need the ForgeGradle reobfuscated client jar.
            selected = dawn_jar_resource_id;
        } else if (std::strcmp(
                       signature,
                       "Lnet/minecraft/client/Minecraft;") == 0) {
            // Lunar's currently supported runtime exposes the deobfuscated
            // Minecraft class name, so retain the existing Lunar jar.
            selected = lunar_jar_resource_id;
        }

        jvmti->Deallocate(reinterpret_cast<unsigned char*>(signature));
        if (selected == dawn_jar_resource_id) {
            break;
        }
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
// This avoids forking the injection path while allowing one DLL to carry both
// the Lunar/deobfuscated and Dawn/production 1.8.9 jars.
#ifdef MAKEINTRESOURCEW
#undef MAKEINTRESOURCEW
#endif
#define MAKEINTRESOURCEW(id) \
    (::flax_compat::make_int_resource(static_cast<WORD>(id)))

#include "FlaxClient.cpp"
