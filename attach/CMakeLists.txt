cmake_minimum_required(VERSION 3.20)

project(FlaxClientAttach LANGUAGES CXX)

if(NOT WIN32)
    message(FATAL_ERROR "The attach DLL currently targets Windows only.")
endif()

if(NOT CMAKE_SIZEOF_VOID_P EQUAL 8)
    message(FATAL_ERROR "FlaxClientAttach must be built for x64.")
endif()

find_package(JNI REQUIRED)

set(
    FLAX_CLIENT_LUNAR_JAR
    "${CMAKE_SOURCE_DIR}/../build/libs/FlaxClient-Lunar.jar"
    CACHE FILEPATH
    "Deobfuscated FlaxClient jar embedded for Lunar Client 1.8.9"
)
set(
    FLAX_CLIENT_DAWN_JAR
    "${CMAKE_SOURCE_DIR}/../build/libs/FlaxClient-Release.jar"
    CACHE FILEPATH
    "Reobfuscated FlaxClient jar embedded for Dawn/production Minecraft 1.8.9"
)

set(FLAX_CLIENT_SOURCES
    src/FlaxClient.cpp
)

if(EXISTS "${FLAX_CLIENT_LUNAR_JAR}" AND EXISTS "${FLAX_CLIENT_DAWN_JAR}")
    file(TO_CMAKE_PATH "${FLAX_CLIENT_LUNAR_JAR}" FLAX_CLIENT_LUNAR_JAR_RC)
    file(TO_CMAKE_PATH "${FLAX_CLIENT_DAWN_JAR}" FLAX_CLIENT_DAWN_JAR_RC)
    configure_file(
        resources/FlaxClient.rc.in
        "${CMAKE_CURRENT_BINARY_DIR}/FlaxClient.rc"
        @ONLY
    )
    list(APPEND FLAX_CLIENT_SOURCES "${CMAKE_CURRENT_BINARY_DIR}/FlaxClient.rc")
else()
    message(WARNING
        "One or both FlaxClient runtime jars were not found; the DLL will require a sidecar jar.")
endif()

add_library(FlaxClient SHARED
    ${FLAX_CLIENT_SOURCES}
)

# Release packages are built with the Visual Studio multi-config generator.
# Embed the finished DLL into the injector so users only receive one EXE.
file(TO_CMAKE_PATH
    "${CMAKE_BINARY_DIR}/bin/Release/FlaxClient.dll"
    FLAX_CLIENT_DLL_RC
)
file(TO_CMAKE_PATH
    "${CMAKE_CURRENT_SOURCE_DIR}/resources/FlaxInjector.manifest"
    FLAX_INJECTOR_MANIFEST_RC
)
configure_file(
    resources/FlaxInjector.rc.in
    "${CMAKE_CURRENT_BINARY_DIR}/FlaxInjector.rc"
    @ONLY
)

add_executable(FlaxInjector WIN32
    src/FlaxInjector.cpp
    "${CMAKE_CURRENT_BINARY_DIR}/FlaxInjector.rc"
)
add_dependencies(FlaxInjector FlaxClient)

target_compile_features(FlaxClient PRIVATE cxx_std_17)
target_compile_features(FlaxInjector PRIVATE cxx_std_17)
target_include_directories(FlaxClient PRIVATE ${JNI_INCLUDE_DIRS})
target_link_libraries(FlaxInjector PRIVATE Shell32)
target_compile_definitions(FlaxClient PRIVATE
    WIN32_LEAN_AND_MEAN
    NOMINMAX
    UNICODE
    _UNICODE
)
target_compile_definitions(FlaxInjector PRIVATE
    WIN32_LEAN_AND_MEAN
    NOMINMAX
    UNICODE
    _UNICODE
)

if(MSVC)
    # A static MSVC runtime keeps the final distribution genuinely self-contained.
    set_property(TARGET FlaxClient PROPERTY
        MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")
    set_property(TARGET FlaxInjector PROPERTY
        MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")

    # Use a stable LocalAppData runtime directory instead of random-looking
    # temporary files, and enable standard Windows exploit mitigations.
    set(FLAX_SECURITY_HEADER "${CMAKE_CURRENT_SOURCE_DIR}/src/FlaxSecurityCompat.h")
    target_compile_options(FlaxClient PRIVATE
        /W4 /permissive- /EHsc /sdl /guard:cf /Brepro
        "/FI${FLAX_SECURITY_HEADER}"
    )
    target_compile_options(FlaxInjector PRIVATE
        /W4 /permissive- /EHsc /sdl /guard:cf /Brepro
        "/FI${FLAX_SECURITY_HEADER}"
    )
    target_link_options(FlaxClient PRIVATE
        /DYNAMICBASE /NXCOMPAT /HIGHENTROPYVA /guard:cf /CETCOMPAT
        /INCREMENTAL:NO /OPT:REF /OPT:ICF
    )
    target_link_options(FlaxInjector PRIVATE
        /DYNAMICBASE /NXCOMPAT /HIGHENTROPYVA /guard:cf /CETCOMPAT
        /INCREMENTAL:NO /OPT:REF /OPT:ICF /MANIFEST:NO
    )
endif()

set_target_properties(FlaxClient PROPERTIES
    OUTPUT_NAME "FlaxClient"
    RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/bin"
)

set_target_properties(FlaxInjector PROPERTIES
    OUTPUT_NAME "FlaxClient"
    RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/bin"
)
