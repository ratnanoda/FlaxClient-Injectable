from pathlib import Path


def replace_once(path, old, new):
    text = path.read_text(encoding="utf-8")
    if old not in text:
        raise RuntimeError("Expected text was not found in %s:\n%s" % (path, old[:180]))
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


root = Path(__file__).resolve().parents[1]

gui = root / "src/main/java/me/eldodebug/soar/gui/modmenu/GuiModMenu.java"
replace_once(
    gui,
    "\tprivate final CompSearchBox searchBox = new CompSearchBox();\n",
    "\tprivate final CompSearchBox searchBox = new CompSearchBox();\n"
    "\tprivate final BeginnerGuideOverlay beginnerGuide = new BeginnerGuideOverlay(this);\n",
)
replace_once(
    gui,
    "\t\tcontentHeight = Math.min(360, Math.max(280, scaledHeight - 44));",
    "\t\tcontentHeight = Math.min(360, Math.max(250, scaledHeight - 76));",
)
replace_once(
    gui,
    "\t\tdraggingSidebar = false;\n\t\tinitSnow();\n",
    "\t\tdraggingSidebar = false;\n\t\tbeginnerGuide.initGui();\n\t\tinitSnow();\n",
)
replace_once(
    gui,
    "\t\tdrawCurrentCategory(nvg, mouseX, mouseY, partialTicks, modulePage);\n\n"
    "\t\tif(MouseUtils.isInside(mouseX, mouseY, contentX, contentY + 31, contentWidth, contentHeight - 31)) {",
    "\t\tdrawCurrentCategory(nvg, mouseX, mouseY, partialTicks, modulePage);\n"
    "\t\tbeginnerGuide.draw(mouseX, mouseY, partialTicks);\n\n"
    "\t\tif(!beginnerGuide.isOpen() && MouseUtils.isInside(mouseX, mouseY, contentX, contentY + 31, contentWidth, contentHeight - 31)) {",
)
replace_once(
    gui,
    "\t@Override\n\tpublic void mouseClicked(int mouseX, int mouseY, int mouseButton) {\n",
    "\t@Override\n\tpublic void mouseClicked(int mouseX, int mouseY, int mouseButton) {\n"
    "\t\tif(beginnerGuide.mouseClicked(mouseX, mouseY, mouseButton)) return;\n",
)
replace_once(
    gui,
    "\t@Override\n\tpublic void keyTyped(char typedChar, int keyCode) {\n",
    "\t@Override\n\tpublic void keyTyped(char typedChar, int keyCode) {\n"
    "\t\tif(beginnerGuide.keyTyped(typedChar, keyCode)) return;\n",
)

cmake = root / "attach/CMakeLists.txt"
replace_once(
    cmake,
    "add_executable(FlaxInjector WIN32\n    src/FlaxInjector.cpp\n)\n",
    "# Release packages are built with the Visual Studio multi-config generator.\n"
    "# Embed the finished DLL into the injector so users only receive one EXE.\n"
    "file(TO_CMAKE_PATH\n"
    "    \"${CMAKE_BINARY_DIR}/bin/Release/FlaxClient.dll\"\n"
    "    FLAX_CLIENT_DLL_RC\n"
    ")\n"
    "configure_file(\n"
    "    resources/FlaxInjector.rc.in\n"
    "    \"${CMAKE_CURRENT_BINARY_DIR}/FlaxInjector.rc\"\n"
    "    @ONLY\n"
    ")\n\n"
    "add_executable(FlaxInjector WIN32\n"
    "    src/FlaxInjector.cpp\n"
    "    \"${CMAKE_CURRENT_BINARY_DIR}/FlaxInjector.rc\"\n"
    ")\n"
    "add_dependencies(FlaxInjector FlaxClient)\n",
)
replace_once(
    cmake,
    "set_target_properties(FlaxInjector PROPERTIES\n    OUTPUT_NAME \"FlaxInjector\"",
    "set_target_properties(FlaxInjector PROPERTIES\n    OUTPUT_NAME \"FlaxClient\"",
)

injector = root / "attach/src/FlaxInjector.cpp"
replace_once(
    injector,
    "#include <chrono>\n#include <cwctype>\n#include <memory>\n#include <string>\n",
    "#include <chrono>\n#include <cstdint>\n#include <cwctype>\n#include <fstream>\n"
    "#include <iomanip>\n#include <memory>\n#include <sstream>\n#include <string>\n",
)

executable_block = """std::filesystem::path executable_path() {
    std::wstring buffer(32768, L'\\0');
    DWORD length =
        GetModuleFileNameW(nullptr, buffer.data(), static_cast<DWORD>(buffer.size()));
    if (length == 0 || length >= buffer.size()) {
        return {};
    }
    buffer.resize(length);
    return std::filesystem::path(buffer);
}
"""
embedded_block = executable_block + r'''

constexpr int embedded_dll_resource_id = 201;

std::filesystem::path embedded_runtime_directory() {
    std::wstring buffer(32768, L'\0');
    DWORD length = GetTempPathW(static_cast<DWORD>(buffer.size()), buffer.data());
    if (length == 0 || length >= buffer.size()) {
        return {};
    }
    buffer.resize(length);
    std::filesystem::path directory(buffer);
    directory /= L"FlaxClient";
    directory /= L"runtime";
    std::error_code error;
    std::filesystem::create_directories(directory, error);
    return error ? std::filesystem::path{} : directory;
}

std::uint64_t hash_bytes(const unsigned char* bytes, std::size_t size) {
    std::uint64_t hash = 1469598103934665603ULL;
    for (std::size_t index = 0; index < size; ++index) {
        hash ^= bytes[index];
        hash *= 1099511628211ULL;
    }
    return hash;
}

void cleanup_stale_embedded_dlls(
    const std::filesystem::path& directory,
    const std::filesystem::path& active_path) {
    std::error_code error;
    if (!std::filesystem::is_directory(directory, error)) {
        return;
    }
    for (const auto& entry : std::filesystem::directory_iterator(directory, error)) {
        if (error || !entry.is_regular_file()) {
            continue;
        }
        const std::filesystem::path path = entry.path();
        const std::wstring filename = path.filename().wstring();
        if (path == active_path || filename.rfind(L"FlaxClient-", 0) != 0 ||
            path.extension() != L".dll") {
            continue;
        }
        std::error_code remove_error;
        std::filesystem::remove(path, remove_error);
    }
}

std::filesystem::path materialize_embedded_dll() {
    HMODULE module = GetModuleHandleW(nullptr);
    HRSRC resource = FindResourceW(
        module,
        MAKEINTRESOURCEW(embedded_dll_resource_id),
        RT_RCDATA);
    if (resource == nullptr) {
        return {};
    }

    HGLOBAL loaded = LoadResource(module, resource);
    DWORD size = SizeofResource(module, resource);
    const auto* bytes = static_cast<const unsigned char*>(
        loaded == nullptr ? nullptr : LockResource(loaded));
    if (bytes == nullptr || size < 2 || bytes[0] != 'M' || bytes[1] != 'Z') {
        return {};
    }

    std::filesystem::path directory = embedded_runtime_directory();
    if (directory.empty()) {
        return {};
    }

    std::wstringstream filename;
    filename << L"FlaxClient-" << std::hex << std::setw(16)
             << std::setfill(L'0') << hash_bytes(bytes, size) << L".dll";
    std::filesystem::path target = directory / filename.str();
    cleanup_stale_embedded_dlls(directory, target);

    std::error_code file_error;
    if (std::filesystem::is_regular_file(target, file_error) &&
        std::filesystem::file_size(target, file_error) == size) {
        return target;
    }

    std::filesystem::path temporary = target;
    temporary += L".tmp-" + std::to_wstring(GetCurrentProcessId());
    {
        std::ofstream output(temporary, std::ios::binary | std::ios::trunc);
        if (!output) {
            return {};
        }
        output.write(
            reinterpret_cast<const char*>(bytes),
            static_cast<std::streamsize>(size));
        output.flush();
        if (!output) {
            output.close();
            std::filesystem::remove(temporary, file_error);
            return {};
        }
    }

    if (!MoveFileExW(
            temporary.c_str(),
            target.c_str(),
            MOVEFILE_REPLACE_EXISTING | MOVEFILE_WRITE_THROUGH)) {
        std::filesystem::remove(temporary, file_error);
        if (!std::filesystem::is_regular_file(target, file_error) ||
            std::filesystem::file_size(target, file_error) != size) {
            return {};
        }
    }
    return target;
}
'''
replace_once(injector, executable_block, embedded_block)
replace_once(
    injector,
    "std::filesystem::path configured_dll;\nDWORD configured_process_id = 0;\n",
    "std::filesystem::path configured_dll;\n"
    "bool configured_dll_override = false;\n"
    "bool verify_embedded_only = false;\n"
    "DWORD configured_process_id = 0;\n",
)
replace_once(
    injector,
    "return {false, L\"FlaxClient.dll was not found beside the injector\"};",
    "return {false, L\"The embedded FlaxClient runtime could not be prepared\"};",
)
replace_once(
    injector,
    "void parse_arguments() {\n    configured_dll = executable_path().parent_path() / L\"FlaxClient.dll\";\n",
    "void parse_arguments() {\n    configured_dll.clear();\n",
)
replace_once(
    injector,
    "        } else if (argument == L\"--dll\" && index + 1 < argument_count) {\n"
    "            configured_dll = std::filesystem::absolute(arguments[++index]);\n"
    "        }\n",
    "        } else if (argument == L\"--dll\" && index + 1 < argument_count) {\n"
    "            configured_dll = std::filesystem::absolute(arguments[++index]);\n"
    "            configured_dll_override = true;\n"
    "        } else if (argument == L\"--verify-embedded\") {\n"
    "            verify_embedded_only = true;\n"
    "        }\n",
)
replace_once(
    injector,
    "    parse_arguments();\n\n    const wchar_t* class_name = L\"FlaxClientInjectorWindow\";\n",
    "    parse_arguments();\n"
    "    if (!configured_dll_override) {\n"
    "        configured_dll = materialize_embedded_dll();\n"
    "    }\n"
    "    if (verify_embedded_only) {\n"
    "        std::error_code verify_error;\n"
    "        return !configured_dll.empty() &&\n"
    "                       std::filesystem::is_regular_file(configured_dll, verify_error)\n"
    "                   ? 0\n"
    "                   : 2;\n"
    "    }\n"
    "    if (configured_dll.empty()) {\n"
    "        ui_state = UiState::error;\n"
    "        status_text = L\"The embedded FlaxClient runtime could not be prepared\";\n"
    "    }\n\n"
    "    const wchar_t* class_name = L\"FlaxClientInjectorWindow\";\n",
)

workflow = root / ".github/workflows/build-lunar-injectable.yml"
workflow.write_text('''name: Build Lunar Single-EXE Package

on:
  push:
    branches:
      - chatgpt/beginner-guide-single-exe-20260803
  pull_request:
    branches:
      - chatgpt/unify-youtube-music-20260803
      - main
  workflow_dispatch:

permissions:
  contents: read

jobs:
  build:
    runs-on: windows-latest
    timeout-minutes: 90

    steps:
      - name: Check out repository
        uses: actions/checkout@v4

      - name: Set up Java 8
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '8'
          architecture: x64

      - name: Build Lunar-compatible client jar
        shell: powershell
        env:
          GRADLE_OPTS: -Dorg.gradle.jvmargs=-Xmx6g
        run: |
          & ".\\gradlew.bat" lunarJar --no-daemon --stacktrace
          if (!(Test-Path "build\\libs\\FlaxClient-Lunar.jar")) {
            throw "FlaxClient-Lunar.jar was not generated"
          }

      - name: Verify Lunar mappings and bundled media tools
        shell: powershell
        run: |
          $jar = "build\\libs\\FlaxClient-Lunar.jar"
          $bootstrap = & javap -classpath $jar -verbose me.eldodebug.soar.attach.AttachBootstrap 2>&1 | Out-String
          $hooks = & javap -classpath $jar -verbose me.eldodebug.soar.attach.LateHooks 2>&1 | Out-String
          if ($bootstrap -notmatch "net/minecraft/client/Minecraft") {
            throw "AttachBootstrap does not contain Lunar-compatible Minecraft class names"
          }
          if ($hooks -notmatch "net/minecraft/client/gui/GuiIngame") {
            throw "LateHooks does not contain Lunar-compatible Minecraft class names"
          }
          $entries = & jar tf $jar
          if ($entries -notcontains "assets/minecraft/soar/tools/windows-x64/yt-dlp.exe") {
            throw "Bundled yt-dlp resource is missing"
          }
          if ($entries -notcontains "assets/minecraft/soar/tools/windows-x64/ffmpeg.zip") {
            throw "Bundled ffmpeg resource is missing"
          }
          if ($entries -notcontains "me/eldodebug/soar/gui/modmenu/BeginnerGuideOverlay.class") {
            throw "Beginner guide class is missing"
          }

      - name: Configure Lunar single-EXE build
        shell: powershell
        run: |
          $jar = (Resolve-Path "build\\libs\\FlaxClient-Lunar.jar").Path.Replace('\\', '/')
          cmake -S attach -B attach/build-lunar -A x64 "-DFLAX_CLIENT_JAR=$jar"

      - name: Build embedded DLL and single EXE
        shell: powershell
        run: cmake --build attach/build-lunar --config Release --target FlaxInjector

      - name: Verify embedded runtime
        shell: powershell
        run: |
          $exe = "attach\\build-lunar\\bin\\Release\\FlaxClient.exe"
          $dll = "attach\\build-lunar\\bin\\Release\\FlaxClient.dll"
          if (!(Test-Path $exe)) { throw "FlaxClient.exe was not generated" }
          if (!(Test-Path $dll)) { throw "Intermediate FlaxClient.dll was not generated" }
          & $exe --verify-embedded
          if ($LASTEXITCODE -ne 0) { throw "The EXE could not verify or extract its embedded DLL" }
          if ((Get-Item $exe).Length -le (Get-Item $dll).Length) {
            throw "The final EXE does not appear to contain the DLL resource"
          }
          Get-FileHash $exe -Algorithm SHA256

      - name: Stage one-file distribution
        shell: powershell
        run: |
          New-Item -ItemType Directory -Force -Path "build\\single-exe" | Out-Null
          Copy-Item "attach\\build-lunar\\bin\\Release\\FlaxClient.exe" "build\\single-exe\\FlaxClient.exe"
          $files = @(Get-ChildItem "build\\single-exe" -File)
          if ($files.Count -ne 1 -or $files[0].Name -ne "FlaxClient.exe") {
            throw "Distribution must contain exactly one FlaxClient.exe"
          }

      - name: Upload single EXE
        uses: actions/upload-artifact@v4
        with:
          name: FlaxClient-Lunar-Single-EXE-Windows-x64
          path: build/single-exe/FlaxClient.exe
          if-no-files-found: error
          retention-days: 7
''', encoding="utf-8")
