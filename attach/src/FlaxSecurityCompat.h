#pragma once

#include <Windows.h>

// Store extracted runtime files under the user's persistent application-data
// directory rather than a temporary directory. This keeps the runtime path
// stable, avoids random-looking files in %TEMP%, and makes cleanup/auditing
// straightforward for users and security software.
inline DWORD WINAPI FlaxGetRuntimeBasePathW(DWORD capacity, LPWSTR buffer) {
    if (buffer == nullptr || capacity == 0) {
        return 0;
    }

    DWORD length = GetEnvironmentVariableW(L"LOCALAPPDATA", buffer, capacity);
    if (length == 0 || length >= capacity) {
        // Keep the original Windows fallback for unusual environments where
        // LOCALAPPDATA is unavailable.
        return GetTempPathW(capacity, buffer);
    }

    if (buffer[length - 1] != L'\\') {
        if (length + 1 >= capacity) {
            SetLastError(ERROR_INSUFFICIENT_BUFFER);
            return 0;
        }
        buffer[length++] = L'\\';
        buffer[length] = L'\0';
    }
    return length;
}

// The existing sources use GetTempPathW for their private runtime directory.
// Force-include this header so both the injector and loaded runtime use the
// stable LocalAppData location without changing their public behavior.
#define GetTempPathW FlaxGetRuntimeBasePathW
