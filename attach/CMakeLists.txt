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
    FLAX_CLIENT_JAR
    "${CMAKE_SOURCE_DIR}/../build/libs/FlaxClient-Release.jar"
    CACHE FILEPATH
    "FlaxClient jar embedded into the attach DLL"
)

set(FLAX_CLIENT_SOURCES
    src/FlaxClient.cpp
)

if(EXISTS "${FLAX_CLIENT_JAR}")
    file(TO_CMAKE_PATH "${FLAX_CLIENT_JAR}" FLAX_CLIENT_JAR_RC)
    configure_file(
        resources/FlaxClient.rc.in
        "${CMAKE_CURRENT_BINARY_DIR}/FlaxClient.rc"
        @ONLY
    )
    list(APPEND FLAX_CLIENT_SOURCES "${CMAKE_CURRENT_BINARY_DIR}/FlaxClient.rc")
else()
    message(WARNING "FlaxClient jar was not found; the DLL will require a sidecar jar.")
endif()

add_library(FlaxClient SHARED
    ${FLAX_CLIENT_SOURCES}
)

add_executable(FlaxInjector WIN32
    src/FlaxInjector.cpp
)

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
    target_compile_options(FlaxClient PRIVATE /W4 /permissive- /EHsc)
    target_compile_options(FlaxInjector PRIVATE /W4 /permissive- /EHsc)
endif()

set_target_properties(FlaxClient PROPERTIES
    OUTPUT_NAME "FlaxClient"
    RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/bin"
)

set_target_properties(FlaxInjector PROPERTIES
    OUTPUT_NAME "FlaxInjector"
    RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/bin"
)
