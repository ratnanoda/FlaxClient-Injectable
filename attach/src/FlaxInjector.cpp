#include <Windows.h>
#include <Windowsx.h>
#include <Shellapi.h>
#include <TlHelp32.h>

#include <filesystem>
#include <iostream>
#include <iterator>
#include <algorithm>
#include <chrono>
#include <cwctype>
#include <memory>
#include <string>
#include <thread>
#include <vector>

namespace {

struct WindowSearch {
    DWORD process_id;
    bool minecraft_window;
};

BOOL CALLBACK find_minecraft_window(HWND window, LPARAM parameter) {
    auto* search = reinterpret_cast<WindowSearch*>(parameter);
    DWORD process_id = 0;
    GetWindowThreadProcessId(window, &process_id);
    if (process_id != search->process_id || !IsWindowVisible(window)) {
        return TRUE;
    }

    wchar_t title[512]{};
    GetWindowTextW(window, title, static_cast<int>(std::size(title)));
    std::wstring value(title);
    if (value.find(L"Minecraft") != std::wstring::npos ||
        value.find(L"Lunar Client") != std::wstring::npos) {
        search->minecraft_window = true;
        return FALSE;
    }
    return TRUE;
}

bool has_minecraft_window(DWORD process_id) {
    WindowSearch search{process_id, false};
    EnumWindows(find_minecraft_window, reinterpret_cast<LPARAM>(&search));
    return search.minecraft_window;
}

std::filesystem::path executable_path() {
    std::wstring buffer(32768, L'\0');
    DWORD length =
        GetModuleFileNameW(nullptr, buffer.data(), static_cast<DWORD>(buffer.size()));
    if (length == 0 || length >= buffer.size()) {
        return {};
    }
    buffer.resize(length);
    return std::filesystem::path(buffer);
}

uintptr_t remote_module_base(DWORD process_id, const wchar_t* module_name) {
    HANDLE snapshot = CreateToolhelp32Snapshot(
        TH32CS_SNAPMODULE | TH32CS_SNAPMODULE32,
        process_id);
    if (snapshot == INVALID_HANDLE_VALUE) {
        return 0;
    }

    MODULEENTRY32W module{};
    module.dwSize = sizeof(module);
    uintptr_t result = 0;
    if (Module32FirstW(snapshot, &module)) {
        do {
            if (_wcsicmp(module.szModule, module_name) == 0) {
                result = reinterpret_cast<uintptr_t>(module.modBaseAddr);
                break;
            }
        } while (Module32NextW(snapshot, &module));
    }
    CloseHandle(snapshot);
    return result;
}

bool is_java_process(const PROCESSENTRY32W& process) {
    return _wcsicmp(process.szExeFile, L"javaw.exe") == 0 ||
           _wcsicmp(process.szExeFile, L"java.exe") == 0;
}

std::filesystem::path process_executable_path(DWORD process_id) {
    HANDLE process = OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION, FALSE, process_id);
    if (process == nullptr) {
        return {};
    }
    std::wstring buffer(32768, L'\0');
    DWORD length = static_cast<DWORD>(buffer.size());
    if (!QueryFullProcessImageNameW(process, 0, buffer.data(), &length)) {
        CloseHandle(process);
        return {};
    }
    CloseHandle(process);
    buffer.resize(length);
    return std::filesystem::path(buffer);
}

bool is_badlion_process(DWORD process_id) {
    std::wstring path = process_executable_path(process_id).wstring();
    std::transform(
        path.begin(),
        path.end(),
        path.begin(),
        [](wchar_t character) { return std::towlower(character); });
    return path.find(L"badlion") != std::wstring::npos;
}

bool is_supported_minecraft_process(DWORD process_id) {
    return remote_module_base(process_id, L"jvm.dll") != 0 &&
           has_minecraft_window(process_id) &&
           !is_badlion_process(process_id);
}

std::vector<DWORD> find_supported_minecraft_processes() {
    std::vector<DWORD> candidates;
    HANDLE snapshot = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    if (snapshot == INVALID_HANDLE_VALUE) {
        return candidates;
    }

    PROCESSENTRY32W process{};
    process.dwSize = sizeof(process);
    if (Process32FirstW(snapshot, &process)) {
        do {
            if (is_java_process(process) &&
                is_supported_minecraft_process(process.th32ProcessID)) {
                candidates.push_back(process.th32ProcessID);
            }
        } while (Process32NextW(snapshot, &process));
    }
    CloseHandle(snapshot);
    return candidates;
}

bool same_machine_type(HANDLE target) {
    using IsWow64Process2Fn = BOOL(WINAPI*)(HANDLE, USHORT*, USHORT*);
    auto is_wow64_process2 = reinterpret_cast<IsWow64Process2Fn>(
        GetProcAddress(GetModuleHandleW(L"kernel32.dll"), "IsWow64Process2"));
    if (is_wow64_process2 == nullptr) {
        BOOL self_wow64 = FALSE;
        BOOL target_wow64 = FALSE;
        return IsWow64Process(GetCurrentProcess(), &self_wow64) &&
               IsWow64Process(target, &target_wow64) &&
               self_wow64 == target_wow64;
    }

    USHORT self_process = 0;
    USHORT self_native = 0;
    USHORT target_process = 0;
    USHORT target_native = 0;
    return is_wow64_process2(GetCurrentProcess(), &self_process, &self_native) &&
           is_wow64_process2(target, &target_process, &target_native) &&
           self_process == target_process &&
           self_native == target_native;
}

bool inject(DWORD process_id, const std::filesystem::path& dll_path) {
    HANDLE process = OpenProcess(
        PROCESS_CREATE_THREAD | PROCESS_QUERY_INFORMATION |
            PROCESS_VM_OPERATION | PROCESS_VM_WRITE | PROCESS_VM_READ,
        FALSE,
        process_id);
    if (process == nullptr) {
        std::wcerr << L"Could not open PID " << process_id
                   << L" (Windows error " << GetLastError() << L").\n";
        return false;
    }

    if (!same_machine_type(process)) {
        std::wcerr << L"Injector, DLL, and Minecraft must all be x64.\n";
        CloseHandle(process);
        return false;
    }

    const std::wstring path = dll_path.wstring();
    const SIZE_T byte_count = (path.size() + 1) * sizeof(wchar_t);
    void* remote_path = VirtualAllocEx(
        process,
        nullptr,
        byte_count,
        MEM_COMMIT | MEM_RESERVE,
        PAGE_READWRITE);
    if (remote_path == nullptr ||
        !WriteProcessMemory(process, remote_path, path.c_str(), byte_count, nullptr)) {
        std::wcerr << L"Could not write the DLL path into Minecraft (Windows error "
                   << GetLastError() << L").\n";
        if (remote_path != nullptr) {
            VirtualFreeEx(process, remote_path, 0, MEM_RELEASE);
        }
        CloseHandle(process);
        return false;
    }

    HMODULE local_kernel32 = GetModuleHandleW(L"kernel32.dll");
    uintptr_t remote_kernel32 = remote_module_base(process_id, L"kernel32.dll");
    auto local_load_library = reinterpret_cast<uintptr_t>(
        GetProcAddress(local_kernel32, "LoadLibraryW"));
    if (local_kernel32 == nullptr || remote_kernel32 == 0 ||
        local_load_library == 0) {
        std::wcerr << L"Could not resolve LoadLibraryW.\n";
        VirtualFreeEx(process, remote_path, 0, MEM_RELEASE);
        CloseHandle(process);
        return false;
    }

    uintptr_t load_library_offset =
        local_load_library - reinterpret_cast<uintptr_t>(local_kernel32);
    auto remote_load_library = reinterpret_cast<LPTHREAD_START_ROUTINE>(
        remote_kernel32 + load_library_offset);
    HANDLE thread = CreateRemoteThread(
        process,
        nullptr,
        0,
        remote_load_library,
        remote_path,
        0,
        nullptr);
    if (thread == nullptr) {
        std::wcerr << L"Could not create the Minecraft loader thread (Windows error "
                   << GetLastError() << L").\n";
        VirtualFreeEx(process, remote_path, 0, MEM_RELEASE);
        CloseHandle(process);
        return false;
    }

    DWORD wait_result = WaitForSingleObject(thread, 15000);
    DWORD load_result = 0;
    if (wait_result == WAIT_OBJECT_0) {
        GetExitCodeThread(thread, &load_result);
    }

    CloseHandle(thread);
    VirtualFreeEx(process, remote_path, 0, MEM_RELEASE);
    CloseHandle(process);

    if (wait_result != WAIT_OBJECT_0 || load_result == 0) {
        std::wcerr << L"Minecraft did not load FlaxClient.dll.\n";
        return false;
    }
    return true;
}

enum class UiState {
    idle,
    injecting,
    success,
    error
};

struct InjectionResult {
    bool success;
    std::wstring message;
};

constexpr UINT result_message = WM_APP + 1;
constexpr UINT animation_timer = 1;
constexpr int base_window_width = 460;
constexpr int base_window_height = 290;

HWND main_window = nullptr;
UiState ui_state = UiState::idle;
UINT ui_dpi = 96;
std::filesystem::path configured_dll;
DWORD configured_process_id = 0;
std::wstring status_text = L"Ready to attach to Minecraft 1.8.9";
int animation_frame = 0;
std::chrono::steady_clock::time_point close_at;

int scale_ui(int value) {
    return MulDiv(value, static_cast<int>(ui_dpi), 96);
}

int window_width() {
    return scale_ui(base_window_width);
}

int window_height() {
    return scale_ui(base_window_height);
}

RECT inject_button_rect() {
    return RECT{
        scale_ui(90), scale_ui(166), scale_ui(370), scale_ui(218)};
}

RECT close_button_rect() {
    return RECT{
        window_width() - scale_ui(42), 0, window_width(), scale_ui(38)};
}

void fill_rect(HDC dc, const RECT& rect, COLORREF color) {
    HBRUSH brush = CreateSolidBrush(color);
    FillRect(dc, &rect, brush);
    DeleteObject(brush);
}

void draw_centered_text(
    HDC dc,
    const std::wstring& value,
    RECT rect,
    HFONT font,
    COLORREF color) {
    HFONT old_font = static_cast<HFONT>(SelectObject(dc, font));
    SetBkMode(dc, TRANSPARENT);
    SetTextColor(dc, color);
    DrawTextW(
        dc,
        value.c_str(),
        static_cast<int>(value.size()),
        &rect,
        DT_CENTER | DT_VCENTER | DT_SINGLELINE | DT_END_ELLIPSIS);
    SelectObject(dc, old_font);
}

void paint_window(HWND window) {
    PAINTSTRUCT paint{};
    HDC window_dc = BeginPaint(window, &paint);
    RECT client{};
    GetClientRect(window, &client);
    HDC dc = CreateCompatibleDC(window_dc);
    HBITMAP bitmap = CreateCompatibleBitmap(
        window_dc, client.right - client.left, client.bottom - client.top);
    HBITMAP old_bitmap = static_cast<HBITMAP>(SelectObject(dc, bitmap));

    fill_rect(dc, client, RGB(15, 18, 28));
    RECT title_bar{0, 0, client.right, scale_ui(38)};
    fill_rect(dc, title_bar, RGB(20, 23, 35));
    RECT glow{0, scale_ui(38), client.right, scale_ui(42)};
    fill_rect(dc, glow, RGB(89, 97, 255));

    HFONT title_font = CreateFontW(
        scale_ui(42), 0, 0, 0, FW_BOLD, FALSE, FALSE, FALSE, DEFAULT_CHARSET,
        OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY,
        DEFAULT_PITCH | FF_DONTCARE, L"Segoe UI");
    HFONT body_font = CreateFontW(
        scale_ui(18), 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, DEFAULT_CHARSET,
        OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY,
        DEFAULT_PITCH | FF_DONTCARE, L"Segoe UI");
    HFONT button_font = CreateFontW(
        scale_ui(20), 0, 0, 0, FW_SEMIBOLD, FALSE, FALSE, FALSE, DEFAULT_CHARSET,
        OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY,
        DEFAULT_PITCH | FF_DONTCARE, L"Segoe UI");
    HFONT caption_font = CreateFontW(
        scale_ui(15), 0, 0, 0, FW_SEMIBOLD, FALSE, FALSE, FALSE, DEFAULT_CHARSET,
        OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY,
        DEFAULT_PITCH | FF_DONTCARE, L"Segoe UI");

    RECT caption{scale_ui(14), 0, scale_ui(210), scale_ui(38)};
    HFONT old_caption_font = static_cast<HFONT>(SelectObject(dc, caption_font));
    SetBkMode(dc, TRANSPARENT);
    SetTextColor(dc, RGB(205, 211, 232));
    DrawTextW(dc, L"FlaxClient Injector", -1, &caption,
              DT_LEFT | DT_VCENTER | DT_SINGLELINE);
    SelectObject(dc, old_caption_font);

    RECT close = close_button_rect();
    fill_rect(dc, close, RGB(20, 23, 35));
    draw_centered_text(dc, L"\u00D7", close, button_font, RGB(205, 211, 232));

    RECT title{
        scale_ui(20), scale_ui(53), client.right - scale_ui(20), scale_ui(104)};
    draw_centered_text(dc, L"FlaxClient", title, title_font, RGB(244, 246, 255));

    RECT subtitle{
        scale_ui(25), scale_ui(111), client.right - scale_ui(25), scale_ui(146)};
    draw_centered_text(dc, status_text, subtitle, body_font, RGB(156, 164, 190));

    RECT button = inject_button_rect();
    COLORREF button_color = RGB(88, 80, 236);
    std::wstring button_text = L"Inject";
    if (ui_state == UiState::injecting) {
        button_color = RGB(67, 69, 105);
        button_text = L"Injecting";
        button_text.append(static_cast<size_t>((animation_frame / 18) % 4), L'.');
    } else if (ui_state == UiState::success) {
        button_color = RGB(37, 178, 112);
        button_text = L"Completed";
    } else if (ui_state == UiState::error) {
        button_color = RGB(207, 67, 83);
        button_text = L"Try Again";
    }
    fill_rect(dc, button, button_color);

    if (ui_state == UiState::injecting) {
        const int width = button.right - button.left;
        const int segment = scale_ui(62);
        int x = button.left
            + (animation_frame * scale_ui(5)) % (width + segment) - segment;
        RECT shimmer{
            std::max(button.left, static_cast<LONG>(x)),
            button.top,
            std::min(button.right, static_cast<LONG>(x + segment)),
            button.bottom};
        if (shimmer.right > shimmer.left) {
            fill_rect(dc, shimmer, RGB(104, 108, 177));
        }
    }
    draw_centered_text(dc, button_text, button, button_font, RGB(255, 255, 255));

    RECT footer{
        scale_ui(20), scale_ui(236), client.right - scale_ui(20), scale_ui(264)};
    std::wstring footer_text =
        ui_state == UiState::success
            ? L"This window will close automatically in 5 seconds"
            : L"Official / Lunar Client 1.8.9 (x64)";
    draw_centered_text(dc, footer_text, footer, body_font, RGB(105, 113, 142));

    DeleteObject(title_font);
    DeleteObject(body_font);
    DeleteObject(button_font);
    DeleteObject(caption_font);
    BitBlt(
        window_dc,
        0,
        0,
        client.right,
        client.bottom,
        dc,
        0,
        0,
        SRCCOPY);
    SelectObject(dc, old_bitmap);
    DeleteObject(bitmap);
    DeleteDC(dc);
    EndPaint(window, &paint);
}

InjectionResult perform_injection() {
    std::error_code path_error;
    std::filesystem::path dll_path =
        std::filesystem::weakly_canonical(configured_dll, path_error);
    if (path_error || !std::filesystem::is_regular_file(dll_path)) {
        return {false, L"FlaxClient.dll was not found beside the injector"};
    }

    DWORD process_id = configured_process_id;
    if (process_id == 0) {
        std::vector<DWORD> candidates = find_supported_minecraft_processes();
        if (candidates.empty()) {
            return {
                false,
                L"Start Minecraft 1.8.9 and wait for the main menu first"};
        }
        if (candidates.size() > 1) {
            return {false, L"More than one supported Minecraft process was found"};
        }
        process_id = candidates.front();
    }

    if (remote_module_base(process_id, L"jvm.dll") == 0) {
        return {false, L"The selected process is not a supported Java JVM"};
    }
    if (is_badlion_process(process_id)) {
        return {false, L"Badlion is not supported by this build"};
    }
    if (remote_module_base(process_id, L"FlaxClient.dll") != 0) {
        return {true, L"FlaxClient is already loaded"};
    }
    if (!inject(process_id, dll_path)) {
        return {false, L"Minecraft could not load FlaxClient.dll"};
    }
    return {true, L"Injection completed successfully"};
}

void start_injection(HWND window) {
    if (ui_state == UiState::injecting || ui_state == UiState::success) {
        return;
    }
    ui_state = UiState::injecting;
    status_text = L"Attaching FlaxClient to the running game";
    animation_frame = 0;
    InvalidateRect(window, nullptr, FALSE);

    std::thread([window]() {
        auto* result = new InjectionResult(perform_injection());
        PostMessageW(
            window,
            result_message,
            result->success ? 1 : 0,
            reinterpret_cast<LPARAM>(result));
    }).detach();
}

LRESULT CALLBACK window_procedure(
    HWND window,
    UINT message,
    WPARAM w_param,
    LPARAM l_param) {
    switch (message) {
        case WM_PAINT:
            paint_window(window);
            return 0;
        case WM_ERASEBKGND:
            return 1;
        case WM_LBUTTONUP: {
            POINT point{
                static_cast<LONG>(GET_X_LPARAM(l_param)),
                static_cast<LONG>(GET_Y_LPARAM(l_param))};
            RECT button = inject_button_rect();
            RECT close = close_button_rect();
            if (PtInRect(&close, point)) {
                DestroyWindow(window);
                return 0;
            }
            if (PtInRect(&button, point)) {
                start_injection(window);
            }
            return 0;
        }
        case WM_SETCURSOR: {
            POINT point{};
            GetCursorPos(&point);
            ScreenToClient(window, &point);
            RECT button = inject_button_rect();
            RECT close = close_button_rect();
            if (PtInRect(&close, point) ||
                (PtInRect(&button, point) && ui_state != UiState::injecting &&
                 ui_state != UiState::success)) {
                SetCursor(LoadCursorW(nullptr, IDC_HAND));
                return TRUE;
            }
            break;
        }
        case WM_NCHITTEST: {
            LRESULT hit = DefWindowProcW(window, message, w_param, l_param);
            if (hit != HTCLIENT) {
                return hit;
            }
            POINT point{
                static_cast<LONG>(GET_X_LPARAM(l_param)),
                static_cast<LONG>(GET_Y_LPARAM(l_param))};
            ScreenToClient(window, &point);
            RECT close = close_button_rect();
            if (point.y >= 0 && point.y < scale_ui(38) &&
                !PtInRect(&close, point)) {
                return HTCAPTION;
            }
            return HTCLIENT;
        }
        case WM_DPICHANGED: {
            ui_dpi = HIWORD(w_param);
            const RECT* suggested = reinterpret_cast<const RECT*>(l_param);
            SetWindowPos(
                window,
                nullptr,
                suggested->left,
                suggested->top,
                suggested->right - suggested->left,
                suggested->bottom - suggested->top,
                SWP_NOACTIVATE | SWP_NOZORDER);
            InvalidateRect(window, nullptr, FALSE);
            return 0;
        }
        case WM_TIMER:
            if (ui_state == UiState::injecting) {
                ++animation_frame;
                InvalidateRect(window, nullptr, FALSE);
            }
            if (ui_state == UiState::success &&
                std::chrono::steady_clock::now() >= close_at) {
                DestroyWindow(window);
                return 0;
            }
            return 0;
        case result_message: {
            std::unique_ptr<InjectionResult> result(
                reinterpret_cast<InjectionResult*>(l_param));
            if (result->success) {
                ui_state = UiState::success;
                status_text = L"FlaxClient is ready";
                close_at =
                    std::chrono::steady_clock::now() + std::chrono::seconds(5);
            } else {
                ui_state = UiState::error;
                status_text = result->message;
            }
            InvalidateRect(window, nullptr, FALSE);
            return 0;
        }
        case WM_DESTROY:
            KillTimer(window, animation_timer);
            PostQuitMessage(0);
            return 0;
        default:
            break;
    }
    return DefWindowProcW(window, message, w_param, l_param);
}

void parse_arguments() {
    configured_dll = executable_path().parent_path() / L"FlaxClient.dll";
    int argument_count = 0;
    LPWSTR* arguments = CommandLineToArgvW(GetCommandLineW(), &argument_count);
    if (arguments == nullptr) {
        return;
    }
    for (int index = 1; index < argument_count; ++index) {
        std::wstring argument = arguments[index];
        if (argument == L"--pid" && index + 1 < argument_count) {
            try {
                configured_process_id = std::stoul(arguments[++index]);
            } catch (...) {
                configured_process_id = 0;
            }
        } else if (argument == L"--dll" && index + 1 < argument_count) {
            configured_dll = std::filesystem::absolute(arguments[++index]);
        }
    }
    LocalFree(arguments);
}

}  // namespace

int WINAPI wWinMain(HINSTANCE instance, HINSTANCE, PWSTR, int show_command) {
    using SetDpiAwarenessContextFn = BOOL(WINAPI*)(HANDLE);
    auto set_dpi_awareness = reinterpret_cast<SetDpiAwarenessContextFn>(
        GetProcAddress(GetModuleHandleW(L"user32.dll"),
                       "SetProcessDpiAwarenessContext"));
    if (set_dpi_awareness != nullptr) {
        // DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2
        set_dpi_awareness(reinterpret_cast<HANDLE>(-4));
    } else {
        SetProcessDPIAware();
    }

    using GetDpiForSystemFn = UINT(WINAPI*)();
    auto get_dpi_for_system = reinterpret_cast<GetDpiForSystemFn>(
        GetProcAddress(GetModuleHandleW(L"user32.dll"), "GetDpiForSystem"));
    if (get_dpi_for_system != nullptr) {
        ui_dpi = get_dpi_for_system();
    }

    parse_arguments();

    const wchar_t* class_name = L"FlaxClientInjectorWindow";
    WNDCLASSEXW window_class{};
    window_class.cbSize = sizeof(window_class);
    window_class.style = CS_OWNDC;
    window_class.lpfnWndProc = window_procedure;
    window_class.hInstance = instance;
    window_class.hCursor = LoadCursorW(nullptr, IDC_ARROW);
    window_class.hIcon = LoadIconW(nullptr, IDI_APPLICATION);
    window_class.hbrBackground = nullptr;
    window_class.lpszClassName = class_name;
    if (!RegisterClassExW(&window_class)) {
        return 1;
    }

    int x = (GetSystemMetrics(SM_CXSCREEN) - window_width()) / 2;
    int y = (GetSystemMetrics(SM_CYSCREEN) - window_height()) / 2;

    main_window = CreateWindowExW(
        WS_EX_APPWINDOW,
        class_name,
        L"FlaxClient Injector",
        WS_POPUP,
        x,
        y,
        window_width(),
        window_height(),
        nullptr,
        nullptr,
        instance,
        nullptr);
    if (main_window == nullptr) {
        return 1;
    }

    SetTimer(main_window, animation_timer, 30, nullptr);
    ShowWindow(main_window, show_command);
    UpdateWindow(main_window);

    MSG message{};
    while (GetMessageW(&message, nullptr, 0, 0) > 0) {
        TranslateMessage(&message);
        DispatchMessageW(&message);
    }
    return static_cast<int>(message.wParam);
}
