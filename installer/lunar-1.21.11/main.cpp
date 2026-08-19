#include <windows.h>
#include <shobjidl.h>
#include <shlwapi.h>

#include <array>
#include <filesystem>
#include <string>

#include "resource.h"

namespace {
constexpr wchar_t kOutputName[] = L"FlaxClient-Lunar-1.21.11.jar";

std::wstring getEnvironmentVariable(const wchar_t* name) {
    const DWORD required = GetEnvironmentVariableW(name, nullptr, 0);
    if (required == 0) {
        return {};
    }

    std::wstring value(required, L'\0');
    const DWORD written = GetEnvironmentVariableW(name, value.data(), required);
    if (written == 0 || written >= required) {
        return {};
    }
    value.resize(written);
    return value;
}

std::filesystem::path findInitialDirectory() {
    const std::wstring userProfile = getEnvironmentVariable(L"USERPROFILE");
    const std::wstring appData = getEnvironmentVariable(L"APPDATA");

    const std::array<std::filesystem::path, 4> candidates = {
        std::filesystem::path(userProfile) / L".lunarclient" / L"offline" / L"multiver" / L"mods",
        std::filesystem::path(userProfile) / L".lunarclient" / L"mods",
        std::filesystem::path(appData) / L".minecraft" / L"mods",
        std::filesystem::path(userProfile)
    };

    std::error_code error;
    for (const auto& candidate : candidates) {
        if (!candidate.empty() && std::filesystem::is_directory(candidate, error)) {
            return candidate;
        }
        error.clear();
    }
    return {};
}

bool selectInstallDirectory(HWND owner, std::filesystem::path& selected) {
    IFileOpenDialog* dialog = nullptr;
    HRESULT result = CoCreateInstance(CLSID_FileOpenDialog, nullptr, CLSCTX_INPROC_SERVER,
                                      IID_PPV_ARGS(&dialog));
    if (FAILED(result)) {
        return false;
    }

    DWORD options = 0;
    if (SUCCEEDED(dialog->GetOptions(&options))) {
        dialog->SetOptions(options | FOS_PICKFOLDERS | FOS_FORCEFILESYSTEM | FOS_PATHMUSTEXIST);
    }
    dialog->SetTitle(L"Lunar Client 1.21.11 の Fabric mods フォルダーを選択");
    dialog->SetOkButtonLabel(L"ここにインストール");
    dialog->SetFileName(L"mods");

    const std::filesystem::path initial = findInitialDirectory();
    IShellItem* initialItem = nullptr;
    if (!initial.empty() && SUCCEEDED(SHCreateItemFromParsingName(initial.c_str(), nullptr,
                                                                  IID_PPV_ARGS(&initialItem)))) {
        dialog->SetFolder(initialItem);
        initialItem->Release();
    }

    result = dialog->Show(owner);
    if (FAILED(result)) {
        dialog->Release();
        return false;
    }

    IShellItem* item = nullptr;
    result = dialog->GetResult(&item);
    if (FAILED(result)) {
        dialog->Release();
        return false;
    }

    PWSTR path = nullptr;
    result = item->GetDisplayName(SIGDN_FILESYSPATH, &path);
    if (SUCCEEDED(result) && path != nullptr) {
        selected = path;
        CoTaskMemFree(path);
    }

    item->Release();
    dialog->Release();
    return !selected.empty();
}

bool extractEmbeddedJar(const std::filesystem::path& destination, std::wstring& errorMessage) {
    HMODULE module = GetModuleHandleW(nullptr);
    HRSRC resource = FindResourceW(module, MAKEINTRESOURCEW(IDR_FLAX_JAR), RT_RCDATA);
    if (resource == nullptr) {
        errorMessage = L"内蔵JARリソースが見つかりません。";
        return false;
    }

    HGLOBAL loaded = LoadResource(module, resource);
    const DWORD size = SizeofResource(module, resource);
    const void* bytes = LockResource(loaded);
    if (loaded == nullptr || bytes == nullptr || size == 0) {
        errorMessage = L"内蔵JARリソースを読み込めません。";
        return false;
    }

    std::error_code fsError;
    std::filesystem::create_directories(destination, fsError);
    if (fsError) {
        errorMessage = L"選択したフォルダーを準備できません。";
        return false;
    }

    const std::filesystem::path outputPath = destination / kOutputName;
    HANDLE output = CreateFileW(outputPath.c_str(), GENERIC_WRITE, 0, nullptr, CREATE_ALWAYS,
                                FILE_ATTRIBUTE_NORMAL, nullptr);
    if (output == INVALID_HANDLE_VALUE) {
        errorMessage = L"JARを書き込めません。Lunar Clientを終了し、書き込み可能なmodsフォルダーを選択してください。";
        return false;
    }

    DWORD written = 0;
    const BOOL ok = WriteFile(output, bytes, size, &written, nullptr);
    const DWORD writeError = ok ? ERROR_SUCCESS : GetLastError();
    FlushFileBuffers(output);
    CloseHandle(output);

    if (!ok || written != size) {
        DeleteFileW(outputPath.c_str());
        errorMessage = L"JARの展開に失敗しました。Windowsエラー: " + std::to_wstring(writeError);
        return false;
    }

    return true;
}
} // namespace

int WINAPI wWinMain(HINSTANCE, HINSTANCE, PWSTR, int) {
    const HRESULT com = CoInitializeEx(nullptr, COINIT_APARTMENTTHREADED | COINIT_DISABLE_OLE1DDE);
    if (FAILED(com)) {
        MessageBoxW(nullptr, L"Windowsのフォルダー選択機能を初期化できません。", L"FlaxClient", MB_OK | MB_ICONERROR);
        return 1;
    }

    const int proceed = MessageBoxW(
        nullptr,
        L"FlaxClient 1.21.11 Fabric ModをLunar Clientへインストールします。\n\n"
        L"次の画面で、Lunar Client 1.21.11のFabric modsフォルダーを選択してください。\n"
        L"このインストーラーはプロセス注入やDLL読み込みを行いません。",
        L"FlaxClient Lunar 1.21.11 Installer",
        MB_OKCANCEL | MB_ICONINFORMATION | MB_DEFBUTTON1);

    if (proceed != IDOK) {
        CoUninitialize();
        return 0;
    }

    std::filesystem::path destination;
    if (!selectInstallDirectory(nullptr, destination)) {
        CoUninitialize();
        return 0;
    }

    std::wstring error;
    if (!extractEmbeddedJar(destination, error)) {
        MessageBoxW(nullptr, error.c_str(), L"インストール失敗", MB_OK | MB_ICONERROR);
        CoUninitialize();
        return 1;
    }

    const std::wstring installedPath = (destination / kOutputName).wstring();
    const std::wstring message = L"インストールが完了しました。\n\n" + installedPath +
                                 L"\n\nLunar Clientを再起動し、1.21.11のFabricプロファイルで起動してください。";
    MessageBoxW(nullptr, message.c_str(), L"FlaxClient", MB_OK | MB_ICONINFORMATION);

    CoUninitialize();
    return 0;
}
