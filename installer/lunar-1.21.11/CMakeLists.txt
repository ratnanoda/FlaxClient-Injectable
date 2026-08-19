cmake_minimum_required(VERSION 3.20)
project(FlaxClientLunarInstaller LANGUAGES CXX RC)

add_executable(FlaxClient-Lunar-1.21.11-Installer WIN32
    main.cpp
    resources.rc
)

target_compile_features(FlaxClient-Lunar-1.21.11-Installer PRIVATE cxx_std_20)
target_compile_options(FlaxClient-Lunar-1.21.11-Installer PRIVATE /W4 /permissive- /utf-8)
target_compile_definitions(FlaxClient-Lunar-1.21.11-Installer PRIVATE UNICODE _UNICODE WIN32_LEAN_AND_MEAN NOMINMAX)
target_link_libraries(FlaxClient-Lunar-1.21.11-Installer PRIVATE ole32 shell32 shlwapi)

set_target_properties(FlaxClient-Lunar-1.21.11-Installer PROPERTIES
    OUTPUT_NAME "FlaxClient-Lunar-1.21.11-Installer"
)
