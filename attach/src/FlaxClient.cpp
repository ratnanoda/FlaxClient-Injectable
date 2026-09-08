#include <Windows.h>
#include <jni.h>
#include <jvmti.h>

#include <cstdarg>
#include <cstdio>
#include <cstring>
#include <filesystem>
#include <fstream>
#include <mutex>
#include <string>

namespace {

using GetCreatedJavaVMs = jint(JNICALL*)(JavaVM**, jsize, jsize*);

HMODULE g_module = nullptr;
std::mutex g_log_mutex;
jvmtiEnv* g_jvmti = nullptr;
jclass g_transformer_class = nullptr;
jmethodID g_transform_method = nullptr;

void log_message(const char* format, ...);

bool is_transform_target(const char* internal_name) {
    if (internal_name == nullptr) {
        return false;
    }
    static const char* targets[] = {
        "ave",
        "net/minecraft/client/Minecraft",
        "avo",
        "net/minecraft/client/gui/GuiIngame",
        "bfk",
        "net/minecraft/client/renderer/EntityRenderer",
        "bew",
        "net/minecraft/client/entity/EntityPlayerSP",
        "ek",
        "net/minecraft/network/NetworkManager",
        "wn",
        "net/minecraft/entity/player/EntityPlayer",
        "pr",
        "net/minecraft/entity/EntityLivingBase",
        "bet",
        "net/minecraft/client/entity/AbstractClientPlayer",
        "bjl",
        "net/minecraft/client/renderer/entity/RendererLivingEntity",
        "bbr",
        "net/minecraft/client/model/ModelPlayer",
        "bln",
        "net/minecraft/client/renderer/entity/RenderPlayer",
        "bfn",
        "net/minecraft/client/renderer/ItemRenderer",
        "bcy",
        "net/minecraft/client/network/NetHandlerPlayClient",
        "bdb",
        "net/minecraft/client/multiplayer/WorldClient",
        "avn",
        "net/minecraft/client/gui/FontRenderer",
        "bmh",
        "net/minecraft/client/renderer/texture/TextureMap",
        "bht",
        "net/minecraft/client/renderer/chunk/RenderChunk",
        "bfh",
        "net/minecraft/client/renderer/ChunkRenderContainer",
        "biu",
        "net/minecraft/client/renderer/entity/RenderManager",
        "bkf",
        "net/minecraft/client/renderer/entity/RenderTNTPrimed",
        "adm",
        "net/minecraft/world/World",
        "ato",
        "net/minecraft/world/storage/WorldInfo",
        "aec",
        "net/minecraft/world/biome/WorldChunkManager",
        "pk",
        "net/minecraft/entity/Entity",
        "bpx",
        "net/minecraft/client/audio/SoundManager",
        "bho",
        "net/minecraft/client/renderer/chunk/ChunkRenderDispatcher",
        "afh",
        "net/minecraft/block/Block",
        "avj",
        "net/minecraft/util/ScreenShotHelper",
        "awh",
        "net/minecraft/client/gui/GuiPlayerTabOverlay",
        "bjh",
        "net/minecraft/client/renderer/entity/RenderItem",
        "bjf",
        "net/minecraft/client/renderer/entity/RenderEntityItem",
        "bkt",
        "net/minecraft/client/renderer/entity/layers/LayerDeadmau5Head",
        "avf",
        "net/minecraft/util/MouseHelper",
        "net/minecraft/client/gui/Gui",
        "net/minecraft/client/Camera",
    };
    for (const char* target : targets) {
        if (std::strcmp(internal_name, target) == 0) {
            return true;
        }
    }
    return false;
}

std::filesystem::path module_path() {
    std::wstring buffer(32768, L'\0');
    const DWORD length =
        GetModuleFileNameW(g_module, buffer.data(), static_cast<DWORD>(buffer.size()));
    if (length == 0 || length >= buffer.size()) {
        return {};
    }
    buffer.resize(length);
    return std::filesystem::path(buffer);
}

std::filesystem::path log_path() {
    wchar_t temp_path[MAX_PATH]{};
    const DWORD length = GetTempPathW(MAX_PATH, temp_path);
    std::filesystem::path directory =
        length > 0 ? std::filesystem::path(temp_path) : module_path().parent_path();
    directory /= L"FlaxClient";
    std::error_code error;
    std::filesystem::create_directories(directory, error);
    return directory / L"attach.log";
}

std::filesystem::path materialize_client_jar() {
    HRSRC resource = FindResourceW(
        g_module,
        MAKEINTRESOURCEW(101),
        RT_RCDATA);
    if (resource != nullptr) {
        HGLOBAL loaded_resource = LoadResource(g_module, resource);
        const DWORD resource_size = SizeofResource(g_module, resource);
        const void* resource_bytes =
            loaded_resource == nullptr ? nullptr : LockResource(loaded_resource);
        if (resource_bytes != nullptr && resource_size > 0) {
            std::filesystem::path target = log_path().parent_path();
            target /= flax_compat::is_modern_dawn()
                          ? L"FlaxClient-26.2-bridge-"
                          : L"FlaxClient-";
            target += std::to_wstring(GetCurrentProcessId());
            target += L".jar";

            std::ofstream output(target, std::ios::binary | std::ios::trunc);
            output.write(
                static_cast<const char*>(resource_bytes),
                static_cast<std::streamsize>(resource_size));
            output.close();
            if (output && std::filesystem::is_regular_file(target) &&
                std::filesystem::file_size(target) == resource_size) {
                log_message(
                    "Extracted the embedded client jar to %s.",
                    target.u8string().c_str());
                return target;
            }
            log_message("Could not extract the embedded FlaxClient jar.");
        }
    }

    const std::filesystem::path sidecar =
        module_path().parent_path() / L"FlaxClient-Release.jar";
    if (std::filesystem::is_regular_file(sidecar)) {
        log_message("Using sidecar client jar %s.", sidecar.u8string().c_str());
        return sidecar;
    }
    return {};
}

void log_message(const char* format, ...) {
    char message[4096]{};
    va_list args;
    va_start(args, format);
    vsnprintf_s(message, sizeof(message), _TRUNCATE, format, args);
    va_end(args);

    SYSTEMTIME time{};
    GetLocalTime(&time);

    std::lock_guard<std::mutex> lock(g_log_mutex);
    std::ofstream output(log_path(), std::ios::app);
    if (output) {
        output << '[' << time.wYear << '-';
        output.width(2);
        output.fill('0');
        output << time.wMonth << '-';
        output.width(2);
        output << time.wDay << ' ';
        output.width(2);
        output << time.wHour << ':';
        output.width(2);
        output << time.wMinute << ':';
        output.width(2);
        output << time.wSecond << "] " << message << '\n';
    }
}

bool clear_java_exception(JNIEnv* env, const char* operation) {
    if (!env->ExceptionCheck()) {
        return false;
    }
    jthrowable exception = env->ExceptionOccurred();
    env->ExceptionClear();
    std::string detail;
    if (exception != nullptr) {
        jclass exception_class = env->GetObjectClass(exception);
        jmethodID to_string =
            exception_class == nullptr
                ? nullptr
                : env->GetMethodID(
                      exception_class,
                      "toString",
                      "()Ljava/lang/String;");
        if (to_string != nullptr && !env->ExceptionCheck()) {
            jstring description = static_cast<jstring>(
                env->CallObjectMethod(exception, to_string));
            if (!env->ExceptionCheck() && description != nullptr) {
                const char* utf8 =
                    env->GetStringUTFChars(description, nullptr);
                if (utf8 != nullptr) {
                    detail = utf8;
                    env->ReleaseStringUTFChars(description, utf8);
                }
                env->DeleteLocalRef(description);
            }
        }
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
        }
        if (exception_class != nullptr) {
            env->DeleteLocalRef(exception_class);
        }
        env->DeleteLocalRef(exception);
    }
    if (detail.empty()) {
        log_message("Java exception while %s.", operation);
    } else {
        log_message(
            "Java exception while %s: %s",
            operation,
            detail.c_str());
    }
    return true;
}

void JNICALL class_file_load_hook(
    jvmtiEnv* jvmti,
    JNIEnv* env,
    jclass,
    jobject,
    const char* name,
    jobject,
    jint class_data_length,
    const unsigned char* class_data,
    jint* new_class_data_length,
    unsigned char** new_class_data) {
    if (!is_transform_target(name) || g_transformer_class == nullptr ||
        g_transform_method == nullptr) {
        return;
    }

    jstring class_name = env->NewStringUTF(name);
    jbyteArray input = env->NewByteArray(class_data_length);
    if (class_name == nullptr || input == nullptr) {
        clear_java_exception(env, "allocating late-transform inputs");
        if (class_name != nullptr) {
            env->DeleteLocalRef(class_name);
        }
        if (input != nullptr) {
            env->DeleteLocalRef(input);
        }
        return;
    }

    env->SetByteArrayRegion(
        input,
        0,
        class_data_length,
        reinterpret_cast<const jbyte*>(class_data));
    auto output = static_cast<jbyteArray>(
        env->CallStaticObjectMethod(
            g_transformer_class,
            g_transform_method,
            class_name,
            input));
    if (clear_java_exception(env, "transforming a loaded Minecraft class") ||
        output == nullptr) {
        env->DeleteLocalRef(class_name);
        env->DeleteLocalRef(input);
        if (output != nullptr) {
            env->DeleteLocalRef(output);
        }
        return;
    }

    const jsize output_length = env->GetArrayLength(output);
    unsigned char* output_bytes = nullptr;
    const jvmtiError allocation_result =
        jvmti->Allocate(output_length, &output_bytes);
    if (allocation_result == JVMTI_ERROR_NONE && output_bytes != nullptr) {
        env->GetByteArrayRegion(
            output,
            0,
            output_length,
            reinterpret_cast<jbyte*>(output_bytes));
        if (!clear_java_exception(env, "copying transformed Minecraft bytecode")) {
            *new_class_data_length = output_length;
            *new_class_data = output_bytes;
            output_bytes = nullptr;
            log_message("Retransformed Minecraft class %s.", name);
        }
    } else {
        log_message(
            "JVMTI could not allocate transformed bytecode for %s (error %d).",
            name,
            allocation_result);
    }

    if (output_bytes != nullptr) {
        jvmti->Deallocate(output_bytes);
    }
    env->DeleteLocalRef(output);
    env->DeleteLocalRef(class_name);
    env->DeleteLocalRef(input);
}

jobject create_jar_url(JNIEnv* env, const std::filesystem::path& jar_path) {
    const std::string utf8_path = jar_path.u8string();
    jstring path = env->NewStringUTF(utf8_path.c_str());
    if (path == nullptr || clear_java_exception(env, "creating the client jar path")) {
        return nullptr;
    }

    jclass file_class = env->FindClass("java/io/File");
    if (file_class == nullptr || clear_java_exception(env, "finding java.io.File")) {
        env->DeleteLocalRef(path);
        return nullptr;
    }

    jmethodID file_constructor =
        env->GetMethodID(file_class, "<init>", "(Ljava/lang/String;)V");
    jmethodID to_uri =
        env->GetMethodID(file_class, "toURI", "()Ljava/net/URI;");
    if (file_constructor == nullptr || to_uri == nullptr ||
        clear_java_exception(env, "resolving java.io.File methods")) {
        env->DeleteLocalRef(path);
        env->DeleteLocalRef(file_class);
        return nullptr;
    }

    jobject file = env->NewObject(file_class, file_constructor, path);
    jobject uri = file == nullptr ? nullptr : env->CallObjectMethod(file, to_uri);
    if (file == nullptr || uri == nullptr ||
        clear_java_exception(env, "converting the client jar path to a URI")) {
        env->DeleteLocalRef(path);
        env->DeleteLocalRef(file_class);
        if (file != nullptr) {
            env->DeleteLocalRef(file);
        }
        return nullptr;
    }

    jclass uri_class = env->FindClass("java/net/URI");
    jmethodID to_url =
        uri_class == nullptr
            ? nullptr
            : env->GetMethodID(uri_class, "toURL", "()Ljava/net/URL;");
    jobject url =
        to_url == nullptr ? nullptr : env->CallObjectMethod(uri, to_url);
    if (url == nullptr || clear_java_exception(env, "converting the client jar URI to a URL")) {
        url = nullptr;
    }

    env->DeleteLocalRef(path);
    env->DeleteLocalRef(file_class);
    env->DeleteLocalRef(file);
    env->DeleteLocalRef(uri);
    if (uri_class != nullptr) {
        env->DeleteLocalRef(uri_class);
    }
    return url;
}

jobject create_jar_nio_path(JNIEnv* env, const std::filesystem::path& jar_path) {
    const std::string utf8_path = jar_path.u8string();
    jstring path = env->NewStringUTF(utf8_path.c_str());
    jclass paths_class = env->FindClass("java/nio/file/Paths");
    jclass string_class = env->FindClass("java/lang/String");
    jobjectArray empty_more = string_class == nullptr
                                  ? nullptr
                                  : env->NewObjectArray(0, string_class, nullptr);
    jmethodID get_path = paths_class == nullptr
                             ? nullptr
                             : env->GetStaticMethodID(
                                   paths_class,
                                   "get",
                                   "(Ljava/lang/String;[Ljava/lang/String;)Ljava/nio/file/Path;");
    jobject result = path == nullptr || empty_more == nullptr || get_path == nullptr
                         ? nullptr
                         : env->CallStaticObjectMethod(paths_class, get_path, path, empty_more);
    if (clear_java_exception(env, "creating the Knot class-path entry")) {
        result = nullptr;
    }
    if (path != nullptr) {
        env->DeleteLocalRef(path);
    }
    if (empty_more != nullptr) {
        env->DeleteLocalRef(empty_more);
    }
    if (string_class != nullptr) {
        env->DeleteLocalRef(string_class);
    }
    if (paths_class != nullptr) {
        env->DeleteLocalRef(paths_class);
    }
    return result;
}

bool add_jar_to_knot_loader(
    JNIEnv* env,
    jobject class_loader,
    const std::filesystem::path& jar_path) {
    jclass loader_class = env->GetObjectClass(class_loader);
    if (loader_class == nullptr) {
        clear_java_exception(env, "identifying the Fabric Knot class loader");
        return false;
    }
    jmethodID get_delegate = env->GetMethodID(
        loader_class,
        "getDelegate",
        "()Lnet/fabricmc/loader/impl/launch/knot/KnotClassDelegate;");
    if (get_delegate == nullptr) {
        // This is the expected path for LaunchWrapper/Lunar.
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
        }
        env->DeleteLocalRef(loader_class);
        return false;
    }

    jobject delegate = env->CallObjectMethod(class_loader, get_delegate);
    if (delegate == nullptr ||
        clear_java_exception(env, "getting the Knot class loader delegate")) {
        if (delegate != nullptr) {
            env->DeleteLocalRef(delegate);
        }
        env->DeleteLocalRef(loader_class);
        return false;
    }

    jobject path = create_jar_nio_path(env, jar_path);
    jclass delegate_class = env->GetObjectClass(delegate);
    jmethodID add_code_source = delegate_class == nullptr
                                    ? nullptr
                                    : env->GetMethodID(
                                          delegate_class,
                                          "addCodeSource",
                                          "(Ljava/nio/file/Path;)V");
    bool success = path != nullptr && add_code_source != nullptr;
    if (success) {
        env->CallVoidMethod(delegate, add_code_source, path);
        success = !clear_java_exception(
            env,
            "adding FlaxClient to the Knot class path");
    } else {
        clear_java_exception(env, "resolving KnotClassDelegate.addCodeSource");
    }

    if (delegate_class != nullptr) {
        env->DeleteLocalRef(delegate_class);
    }
    if (path != nullptr) {
        env->DeleteLocalRef(path);
    }
    env->DeleteLocalRef(delegate);
    env->DeleteLocalRef(loader_class);
    if (success) {
        log_message("Added the client jar through Fabric KnotClassDelegate.");
    }
    return success;
}

jobject find_loaded_minecraft_class_loader(JavaVM* vm) {
    jvmtiEnv* jvmti = nullptr;
    if (vm->GetEnv(
            reinterpret_cast<void**>(&jvmti),
            JVMTI_VERSION_1_2) != JNI_OK ||
        jvmti == nullptr) {
        return nullptr;
    }

    jint class_count = 0;
    jclass* classes = nullptr;
    if (jvmti->GetLoadedClasses(&class_count, &classes)
            != JVMTI_ERROR_NONE ||
        classes == nullptr) {
        return nullptr;
    }

    jobject result = nullptr;
    for (jint index = 0; index < class_count; ++index) {
        char* signature = nullptr;
        if (jvmti->GetClassSignature(
                classes[index],
                &signature,
                nullptr) == JVMTI_ERROR_NONE &&
            signature != nullptr) {
            const bool minecraft =
                std::strcmp(
                    signature,
                    "Lnet/minecraft/client/Minecraft;") == 0 ||
                std::strcmp(signature, "Lave;") == 0;
            jvmti->Deallocate(
                reinterpret_cast<unsigned char*>(signature));
            if (minecraft) {
                jobject loader = nullptr;
                if (jvmti->GetClassLoader(
                        classes[index],
                        &loader) == JVMTI_ERROR_NONE &&
                    loader != nullptr) {
                    result = loader;
                }
                break;
            }
        }
    }
    jvmti->Deallocate(
        reinterpret_cast<unsigned char*>(classes));

    if (result != nullptr) {
        log_message(
            "Using the defining class loader of the loaded Minecraft class.");
    }
    return result;
}

jobject find_system_class_loader(JNIEnv* env) {
    jclass class_loader_class = env->FindClass("java/lang/ClassLoader");
    jmethodID get_system_loader = class_loader_class == nullptr
                                      ? nullptr
                                      : env->GetStaticMethodID(
                                            class_loader_class,
                                            "getSystemClassLoader",
                                            "()Ljava/lang/ClassLoader;");
    if (get_system_loader == nullptr ||
        clear_java_exception(env, "resolving ClassLoader.getSystemClassLoader")) {
        if (class_loader_class != nullptr) {
            env->DeleteLocalRef(class_loader_class);
        }
        return nullptr;
    }
    jobject loader = env->CallStaticObjectMethod(class_loader_class, get_system_loader);
    if (clear_java_exception(env, "getting the Java system class loader")) {
        loader = nullptr;
    }
    env->DeleteLocalRef(class_loader_class);
    return loader;
}

jobject find_minecraft_class_loader(
    JNIEnv* env,
    JavaVM* vm) {
    jobject loaded_loader =
        find_loaded_minecraft_class_loader(vm);
    if (loaded_loader != nullptr) {
        return loaded_loader;
    }

    jclass launch_class = env->FindClass("net/minecraft/launchwrapper/Launch");
    if (launch_class != nullptr && !env->ExceptionCheck()) {
        jfieldID class_loader_field = env->GetStaticFieldID(
            launch_class,
            "classLoader",
            "Lnet/minecraft/launchwrapper/LaunchClassLoader;");
        if (class_loader_field != nullptr && !env->ExceptionCheck()) {
            jobject class_loader =
                env->GetStaticObjectField(launch_class, class_loader_field);
            if (!clear_java_exception(env, "reading Launch.classLoader") &&
                class_loader != nullptr) {
                env->DeleteLocalRef(launch_class);
                log_message("Using the Minecraft LaunchClassLoader.");
                return class_loader;
            }
        }
        clear_java_exception(env, "resolving Launch.classLoader");
        env->DeleteLocalRef(launch_class);
    } else {
        clear_java_exception(env, "probing for LaunchWrapper");
    }

    jclass class_loader_class = env->FindClass("java/lang/ClassLoader");
    jmethodID get_system_loader =
        class_loader_class == nullptr
            ? nullptr
            : env->GetStaticMethodID(
                  class_loader_class,
                  "getSystemClassLoader",
                  "()Ljava/lang/ClassLoader;");
    if (get_system_loader == nullptr ||
        clear_java_exception(env, "resolving ClassLoader.getSystemClassLoader")) {
        if (class_loader_class != nullptr) {
            env->DeleteLocalRef(class_loader_class);
        }
        return nullptr;
    }

    jobject class_loader =
        env->CallStaticObjectMethod(class_loader_class, get_system_loader);
    if (clear_java_exception(env, "getting the system class loader")) {
        class_loader = nullptr;
    }
    env->DeleteLocalRef(class_loader_class);
    if (class_loader != nullptr) {
        log_message("LaunchWrapper was not present; using the Java system class loader.");
    }
    return class_loader;
}

bool add_jar_to_launch_loader(
    JNIEnv* env,
    JavaVM* vm,
    jobject class_loader,
    const std::filesystem::path& jar_path) {
    if (flax_compat::is_modern_dawn()) {
        jvmtiEnv* jvmti = nullptr;
        if (vm->GetEnv(
                reinterpret_cast<void**>(&jvmti),
                JVMTI_VERSION_1_2) != JNI_OK ||
            jvmti == nullptr) {
            log_message("JVMTI was unavailable for the Dawn 26.2 class path.");
            return false;
        }
        const std::string utf8_path = jar_path.u8string();
        const jvmtiError result =
            jvmti->AddToSystemClassLoaderSearch(utf8_path.c_str());
        if (result != JVMTI_ERROR_NONE) {
            log_message(
                "Dawn 26.2 system class-path extension failed (error %d).",
                result);
            return false;
        }
        log_message("Added the Dawn 26.2 bridge to the Java system class path.");
        // Fabric deliberately isolates game classes from arbitrary parent
        // code sources. The transformer itself stays in the system loader so
        // it shares Fabric's ASM, while the static hook linkage is also made
        // visible to Knot through its supported code-source path.
        if (!add_jar_to_knot_loader(env, class_loader, jar_path)) {
            log_message("Could not expose the Dawn 26.2 hooks to Fabric Knot.");
            return false;
        }
        return true;
    }

    if (add_jar_to_knot_loader(env, class_loader, jar_path)) {
        return true;
    }

    jobject url = create_jar_url(env, jar_path);
    if (url == nullptr) {
        return false;
    }

    jclass loader_class = env->GetObjectClass(class_loader);
    jmethodID add_url = loader_class == nullptr
                            ? nullptr
                            : env->GetMethodID(loader_class, "addURL", "(Ljava/net/URL;)V");
    if (add_url != nullptr &&
        !clear_java_exception(env, "resolving LaunchClassLoader.addURL")) {
        env->CallVoidMethod(class_loader, add_url, url);
        const bool failed =
            clear_java_exception(env, "adding the FlaxClient jar");
        if (!failed) {
            env->DeleteLocalRef(loader_class);
            env->DeleteLocalRef(url);
            return true;
        }
    } else {
        clear_java_exception(env, "resolving LaunchClassLoader.addURL");
    }

    if (loader_class != nullptr) {
        env->DeleteLocalRef(loader_class);
    }
    env->DeleteLocalRef(url);

    /*
     * Java 9+ no longer exposes URLClassLoader.addURL on AppClassLoader.
     * JVMTI provides the supported equivalent and keeps the jar visible to
     * Lunar's Java 17 system loader.
     */
    jvmtiEnv* jvmti = nullptr;
    if (vm->GetEnv(
            reinterpret_cast<void**>(&jvmti),
            JVMTI_VERSION_1_2) != JNI_OK ||
        jvmti == nullptr) {
        log_message("JVMTI was unavailable for system classpath extension.");
        return false;
    }
    const std::string utf8_path = jar_path.u8string();
    const jvmtiError result =
        jvmti->AddToSystemClassLoaderSearch(utf8_path.c_str());
    if (result != JVMTI_ERROR_NONE) {
        log_message(
            "JVMTI AddToSystemClassLoaderSearch failed (error %d).",
            result);
        return false;
    }
    log_message("Added the client jar through the Java 9+ JVMTI class path.");
    return true;
}

jclass load_class_from(
    JNIEnv* env,
    jobject class_loader,
    const char* binary_name,
    const char* operation) {
    jclass loader_class = env->GetObjectClass(class_loader);
    jmethodID load_class =
        loader_class == nullptr
            ? nullptr
            : env->GetMethodID(
                  loader_class,
                  "loadClass",
                  "(Ljava/lang/String;)Ljava/lang/Class;");
    if (load_class == nullptr || clear_java_exception(env, operation)) {
        if (loader_class != nullptr) {
            env->DeleteLocalRef(loader_class);
        }
        return nullptr;
    }

    jstring class_name = env->NewStringUTF(binary_name);
    jobject class_object =
        class_name == nullptr
            ? nullptr
            : env->CallObjectMethod(class_loader, load_class, class_name);
    if (class_object == nullptr || clear_java_exception(env, operation)) {
        if (class_name != nullptr) {
            env->DeleteLocalRef(class_name);
        }
        env->DeleteLocalRef(loader_class);
        return nullptr;
    }

    env->DeleteLocalRef(class_name);
    env->DeleteLocalRef(loader_class);
    return static_cast<jclass>(class_object);
}

bool mark_late_load_ready(JNIEnv* env, jobject class_loader) {
    jclass status_class = load_class_from(
        env,
        class_loader,
        "me.eldodebug.soar.attach.LateLoadStatus",
        "loading LateLoadStatus");
    if (status_class == nullptr) {
        return false;
    }

    jmethodID mark_core_ready =
        env->GetStaticMethodID(status_class, "markCoreHookReady", "()V");
    jmethodID mark_transformer_ready =
        env->GetStaticMethodID(status_class, "markTransformerReady", "()V");
    if (mark_core_ready == nullptr || mark_transformer_ready == nullptr ||
        clear_java_exception(env, "resolving LateLoadStatus.markCoreHookReady")) {
        env->DeleteLocalRef(status_class);
        return false;
    }

    env->CallStaticVoidMethod(status_class, mark_core_ready);
    bool failed =
        clear_java_exception(env, "marking the core late-load hook ready");
    if (!failed) {
        env->CallStaticVoidMethod(status_class, mark_transformer_ready);
        failed = clear_java_exception(env, "marking the late-load transformer ready");
    }
    env->DeleteLocalRef(status_class);
    return !failed;
}

bool retransform_loaded_minecraft_classes(jvmtiEnv* jvmti) {
    jint class_count = 0;
    jclass* classes = nullptr;
    const jvmtiError loaded_result =
        jvmti->GetLoadedClasses(&class_count, &classes);
    if (loaded_result != JVMTI_ERROR_NONE) {
        log_message("JVMTI GetLoadedClasses failed (error %d).", loaded_result);
        return false;
    }

    int found = 0;
    int transformed = 0;
    for (jint index = 0; index < class_count; ++index) {
        char* signature = nullptr;
        if (jvmti->GetClassSignature(classes[index], &signature, nullptr) !=
                JVMTI_ERROR_NONE ||
            signature == nullptr) {
            continue;
        }

        std::string internal_name(signature);
        const bool valid_signature =
            internal_name.size() > 2 && internal_name.front() == 'L' &&
            internal_name.back() == ';';
        if (valid_signature) {
            internal_name =
                internal_name.substr(1, internal_name.size() - 2);
        }
        const bool target =
            valid_signature && is_transform_target(internal_name.c_str());
        jvmti->Deallocate(reinterpret_cast<unsigned char*>(signature));
        if (!target) {
            continue;
        }

        ++found;
        const jvmtiError transform_result =
            jvmti->RetransformClasses(1, &classes[index]);
        if (transform_result == JVMTI_ERROR_NONE) {
            ++transformed;
        } else {
            log_message(
                "JVMTI RetransformClasses failed for %s (error %d).",
                internal_name.c_str(),
                transform_result);
        }
    }

    if (classes != nullptr) {
        jvmti->Deallocate(reinterpret_cast<unsigned char*>(classes));
    }
    if (found == 0) {
        log_message("Minecraft hook targets are not loaded yet; hooks will apply on load.");
        return true;
    }
    log_message(
        "Retransformed %d of %d currently loaded Minecraft hook targets.",
        transformed,
        found);

    /*
     * Forge/Dawn may have already changed the schema of a small number of
     * vanilla classes before a late attach. HotSpot then rejects those
     * individual retransforms with errors 63/64 even though the remaining
     * classes were installed successfully. Treating that partial result as a
     * total failure prevented AttachBootstrap from ever running, so the user
     * saw a successful DLL injection but no client. Continue when at least one
     * hook was installed; LateHooks is deliberately defensive when a specific
     * optional hook is unavailable.
     */
    if (transformed == 0) {
        return false;
    }
    if (transformed != found) {
        log_message(
            "Continuing with %d unsupported Dawn-modified hook targets disabled.",
            found - transformed);
    }
    return true;
}

bool install_late_transformer(
    JNIEnv* env,
    JavaVM* vm,
    jobject class_loader) {
    jvmtiEnv* jvmti = nullptr;
    if (vm->GetEnv(
            reinterpret_cast<void**>(&jvmti),
            JVMTI_VERSION_1_2) != JNI_OK ||
        jvmti == nullptr) {
        log_message("The running JVM did not provide JVMTI 1.2.");
        return false;
    }

    jvmtiCapabilities capabilities{};
    capabilities.can_retransform_classes = 1;
    capabilities.can_retransform_any_class = 1;
    const jvmtiError capability_result = jvmti->AddCapabilities(&capabilities);
    if (capability_result != JVMTI_ERROR_NONE) {
        log_message("JVMTI AddCapabilities failed (error %d).", capability_result);
        return false;
    }

    const bool modern_dawn = flax_compat::is_modern_dawn();
    const char* transformer_name = modern_dawn
                                       ? "me.eldodebug.soar.attach.modern.Modern26LateClassTransformer"
                                       : "me.eldodebug.soar.attach.CompatibleLateClassTransformer";
    jclass transformer_class = load_class_from(
        env,
        class_loader,
        transformer_name,
        "loading the FlaxClient late transformer");
    if (transformer_class == nullptr) {
        return false;
    }

    jmethodID transform_method = env->GetStaticMethodID(
        transformer_class,
        "transform",
        "(Ljava/lang/String;[B)[B");
    if (transform_method == nullptr ||
        clear_java_exception(env, "resolving the FlaxClient late transformer")) {
        env->DeleteLocalRef(transformer_class);
        return false;
    }

    g_transformer_class =
        static_cast<jclass>(env->NewGlobalRef(transformer_class));
    g_transform_method = transform_method;
    g_jvmti = jvmti;
    env->DeleteLocalRef(transformer_class);
    if (g_transformer_class == nullptr ||
        clear_java_exception(env, "retaining the FlaxClient late transformer")) {
        return false;
    }

    jvmtiEventCallbacks callbacks{};
    callbacks.ClassFileLoadHook = &class_file_load_hook;
    const jvmtiError callback_result =
        jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
    if (callback_result != JVMTI_ERROR_NONE) {
        log_message("JVMTI SetEventCallbacks failed (error %d).", callback_result);
        return false;
    }

    const jvmtiError enable_result = jvmti->SetEventNotificationMode(
        JVMTI_ENABLE,
        JVMTI_EVENT_CLASS_FILE_LOAD_HOOK,
        nullptr);
    if (enable_result != JVMTI_ERROR_NONE) {
        log_message(
            "Could not enable JVMTI ClassFileLoadHook (error %d).",
            enable_result);
        return false;
    }

    if (!retransform_loaded_minecraft_classes(jvmti)) {
        return false;
    }
    if (!modern_dawn && !mark_late_load_ready(env, class_loader)) {
        return false;
    }

    log_message("JVMTI late-load transformer installed.");
    return true;
}

bool invoke_bootstrap(JNIEnv* env, jobject class_loader) {
    const bool modern_dawn = flax_compat::is_modern_dawn();
    jclass bootstrap_class = load_class_from(
        env,
        class_loader,
        modern_dawn
            ? "me.eldodebug.soar.attach.modern.Modern26AttachBootstrap"
            : "me.eldodebug.soar.attach.AttachBootstrap",
        "loading the FlaxClient attach bootstrap");
    if (bootstrap_class == nullptr) {
        return false;
    }
    jmethodID attach =
        env->GetStaticMethodID(bootstrap_class, "attach", "()V");
    if (attach == nullptr ||
        clear_java_exception(env, "resolving AttachBootstrap.attach")) {
        env->DeleteLocalRef(bootstrap_class);
        return false;
    }

    env->CallStaticVoidMethod(bootstrap_class, attach);
    const bool failed = clear_java_exception(env, "starting the FlaxClient attach bootstrap");

    env->DeleteLocalRef(bootstrap_class);
    return !failed;
}

DWORD WINAPI attach_worker(void*) {
    log_message("FlaxClient DLL loaded; waiting for the Java VM.");

    HMODULE jvm_module = nullptr;
    for (int attempt = 0; attempt < 100 && jvm_module == nullptr; ++attempt) {
        jvm_module = GetModuleHandleW(L"jvm.dll");
        if (jvm_module == nullptr) {
            Sleep(100);
        }
    }
    if (jvm_module == nullptr) {
        log_message("jvm.dll was not found.");
        return 1;
    }

    const auto get_created_vms = reinterpret_cast<GetCreatedJavaVMs>(
        GetProcAddress(jvm_module, "JNI_GetCreatedJavaVMs"));
    if (get_created_vms == nullptr) {
        log_message("JNI_GetCreatedJavaVMs was not exported by jvm.dll.");
        return 1;
    }

    JavaVM* vm = nullptr;
    jsize vm_count = 0;
    if (get_created_vms(&vm, 1, &vm_count) != JNI_OK || vm == nullptr ||
        vm_count == 0) {
        log_message("No running Java VM was available.");
        return 1;
    }

    JNIEnv* env = nullptr;
    bool attached_here = false;
    const jint environment_result =
        vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_8);
    if (environment_result == JNI_EDETACHED) {
        if (vm->AttachCurrentThread(reinterpret_cast<void**>(&env), nullptr) != JNI_OK) {
            log_message("Could not attach the FlaxClient worker to the Java VM.");
            return 1;
        }
        attached_here = true;
    } else if (environment_result != JNI_OK || env == nullptr) {
        log_message("Could not acquire JNIEnv (result %d).", environment_result);
        return 1;
    }

    const std::filesystem::path jar_path = materialize_client_jar();
    if (jar_path.empty()) {
        log_message("Neither an embedded nor a sidecar FlaxClient jar was available.");
        if (attached_here) {
            vm->DetachCurrentThread();
        }
        return 1;
    }

    jobject class_loader = find_minecraft_class_loader(env, vm);
    log_message("Detected runtime: %s.", flax_compat::runtime_name());
    bool success = class_loader != nullptr;
    if (success) {
        success = add_jar_to_launch_loader(env, vm, class_loader, jar_path);
    }
    jobject client_loader = class_loader;
    if (success && flax_compat::is_modern_dawn()) {
        client_loader = find_system_class_loader(env);
        success = client_loader != nullptr;
        if (success) {
            log_message("Using the Java system loader for the isolated 26.2 bridge.");
        }
    }
    if (success) {
        success = install_late_transformer(env, vm, client_loader);
    }
    if (success) {
        success = invoke_bootstrap(env, client_loader);
    }
    if (client_loader != nullptr && class_loader != nullptr &&
        !env->IsSameObject(client_loader, class_loader)) {
        env->DeleteLocalRef(client_loader);
    }
    if (class_loader != nullptr) {
        env->DeleteLocalRef(class_loader);
    }

    log_message(
        success ? "FlaxClient Java bootstrap loaded successfully."
                : "FlaxClient Java bootstrap failed.");

    if (attached_here) {
        vm->DetachCurrentThread();
    }
    return success ? 0 : 1;
}

}  // namespace

BOOL APIENTRY DllMain(HMODULE module, DWORD reason, LPVOID) {
    if (reason == DLL_PROCESS_ATTACH) {
        g_module = module;
        DisableThreadLibraryCalls(module);
        HANDLE thread = CreateThread(nullptr, 0, attach_worker, nullptr, 0, nullptr);
        if (thread != nullptr) {
            CloseHandle(thread);
        }
    }
    return TRUE;
}
