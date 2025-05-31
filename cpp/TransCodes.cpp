#include <windows.h>
#include <commctrl.h>  // 包含通用控件定义
#include <shlwapi.h>
#include <string>
#include <vector>
#include <ctime>
#include <iomanip>
#include <sstream>

#pragma comment(lib, "shlwapi.lib")
#pragma comment(lib, "comctl32.lib")

// 定义常量
#define IDM_DIR_SELECT 1001
#define IDM_CONFIG 1002
#define IDM_VIEW_PARAMS 1003
#define IDM_EXIT 1004
#define IDM_PACK 2001
#define IDM_UNPACK 2002
#define IDM_TRANSCODE 2003
#define IDM_TIPS 3001
#define IDM_DONATE 3002
#define IDM_ABOUT 3003

// 全局变量
HWND g_hMainWnd, g_hConfigWnd;
HWND g_hDirEdit, g_hProgress, g_hListBox, g_hStatusBar;
HWND g_hConfigEdit, g_hConfigStatusBar;
bool g_bShowProgress = false;
std::wstring g_currentFilePath;

// 前向声明
LRESULT CALLBACK ConfigWndProc(HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam);

// 工具函数
std::wstring GetFileSizeString(DWORD fileSize) {
    const double KB = 1024.0;
    const double MB = KB * 1024.0;
    const double GB = MB * 1024.0;

    if (fileSize < KB) {
        return std::to_wstring(fileSize) + L" B";
    }
    else if (fileSize < MB) {
        return std::to_wstring(fileSize / KB) + L" KB";
    }
    else if (fileSize < GB) {
        return std::to_wstring(fileSize / MB) + L" MB";
    }
    else {
        return std::to_wstring(fileSize / GB) + L" GB";
    }
}

std::wstring TimeToString(FILETIME ft) {
    SYSTEMTIME st;
    FileTimeToSystemTime(&ft, &st);

    std::wstringstream ss;
    ss << st.wYear << L"-"
        << std::setw(2) << std::setfill(L'0') << st.wMonth << L"-"
        << std::setw(2) << std::setfill(L'0') << st.wDay << L" "
        << std::setw(2) << std::setfill(L'0') << st.wHour << L":"
        << std::setw(2) << std::setfill(L'0') << st.wMinute << L":"
        << std::setw(2) << std::setfill(L'0') << st.wSecond;

    return ss.str();
}

void UpdateStatusBar(HWND hStatusBar, const std::wstring& text) {
    SendMessage(hStatusBar, SB_SETTEXTW, 0, (LPARAM)text.c_str());
}

void ShowConfigWindow() {
    if (g_hConfigWnd) {
        SetForegroundWindow(g_hConfigWnd);
        return;
    }

    WNDCLASSEX wcex = { sizeof(WNDCLASSEX) };
    wcex.style = CS_HREDRAW | CS_VREDRAW;
    wcex.lpfnWndProc = ConfigWndProc;
    wcex.hInstance = GetModuleHandle(NULL);
    wcex.hCursor = LoadCursor(NULL, IDC_ARROW);
    wcex.hbrBackground = (HBRUSH)(COLOR_WINDOW + 1);
    wcex.lpszClassName = reinterpret_cast<LPCSTR>(L"ConfigWindowClass");

    RegisterClassEx(&wcex);

    g_hConfigWnd = CreateWindowEx(
        0, reinterpret_cast<LPCSTR>(L"ConfigWindowClass"), reinterpret_cast<LPCSTR>(L"参数配置"),
        WS_OVERLAPPEDWINDOW,
        CW_USEDEFAULT, CW_USEDEFAULT, 800, 600,
        g_hMainWnd, NULL, GetModuleHandle(NULL), NULL);

    if (!g_hConfigWnd) return;

    // 创建按钮行
    HWND hBtnOpen = CreateWindow(
        reinterpret_cast<LPCSTR>(L"BUTTON"), reinterpret_cast<LPCSTR>(L"打开文件"),
        WS_VISIBLE | WS_CHILD | BS_PUSHBUTTON,
        10, 10, 100, 30, g_hConfigWnd, (HMENU)100, GetModuleHandle(NULL), NULL);

    HWND hBtnGoto = CreateWindow(
        reinterpret_cast<LPCSTR>(L"BUTTON"), reinterpret_cast<LPCSTR>(L"跳转到行"),
        WS_VISIBLE | WS_CHILD | BS_PUSHBUTTON,
        120, 10, 100, 30, g_hConfigWnd, (HMENU)101, GetModuleHandle(NULL), NULL);

    HWND hBtnHash = CreateWindow(
        reinterpret_cast<LPCSTR>(L"BUTTON"), reinterpret_cast<LPCSTR>(L"查看哈希"),
        WS_VISIBLE | WS_CHILD | BS_PUSHBUTTON,
        230, 10, 100, 30, g_hConfigWnd, (HMENU)102, GetModuleHandle(NULL), NULL);

    HWND hBtnEncoding = CreateWindow(
        reinterpret_cast<LPCSTR>(L"BUTTON"), reinterpret_cast<LPCSTR>(L"确认编码"),
        WS_VISIBLE | WS_CHILD | BS_PUSHBUTTON,
        340, 10, 100, 30, g_hConfigWnd, (HMENU)103, GetModuleHandle(NULL), NULL);

    // 创建文本编辑区域
    g_hConfigEdit = CreateWindowEx(
        WS_EX_CLIENTEDGE, reinterpret_cast<LPCSTR>(L"EDIT"), NULL,
        WS_VISIBLE | WS_CHILD | ES_MULTILINE | ES_AUTOVSCROLL | ES_AUTOHSCROLL | WS_VSCROLL | WS_HSCROLL,
        10, 50, 760, 480, g_hConfigWnd, NULL, GetModuleHandle(NULL), NULL);

    // 创建状态栏
    g_hConfigStatusBar = CreateWindowEx(
        0, reinterpret_cast<LPCSTR>(STATUSCLASSNAMEW), NULL,
        WS_CHILD | WS_VISIBLE | SBARS_SIZEGRIP,
        0, 0, 0, 0, g_hConfigWnd, NULL, GetModuleHandle(NULL), NULL);

    ShowWindow(g_hConfigWnd, SW_SHOW);
    UpdateWindow(g_hConfigWnd);
}

void OpenFileInConfigWindow() {
    OPENFILENAMEW ofn;
    wchar_t szFile[MAX_PATH] = { 0 };

    ZeroMemory(&ofn, sizeof(ofn));
    ofn.lStructSize = sizeof(ofn);
    ofn.hwndOwner = g_hConfigWnd;
    ofn.lpstrFile = szFile;
    ofn.nMaxFile = sizeof(szFile) / sizeof(szFile[0]);
    ofn.lpstrFilter = L"文本文件 (*.txt)\0*.txt\0所有文件 (*.*)\0*.*\0";
    ofn.nFilterIndex = 1;
    ofn.Flags = OFN_PATHMUSTEXIST | OFN_FILEMUSTEXIST;

    if (GetOpenFileNameW(&ofn)) {
        g_currentFilePath = ofn.lpstrFile;

        HANDLE hFile = CreateFileW(ofn.lpstrFile, GENERIC_READ, FILE_SHARE_READ, NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
        if (hFile == INVALID_HANDLE_VALUE) return;

        DWORD fileSize = GetFileSize(hFile, NULL);
        std::vector<char> buffer(fileSize + 1);
        DWORD bytesRead;
        ReadFile(hFile, buffer.data(), fileSize, &bytesRead, NULL);
        CloseHandle(hFile);

        // 获取文件信息
        WIN32_FILE_ATTRIBUTE_DATA fileInfo;
        GetFileAttributesExW(ofn.lpstrFile, GetFileExInfoStandard, &fileInfo);

        // 设置编辑框内容
        int len = MultiByteToWideChar(CP_ACP, 0, buffer.data(), -1, NULL, 0);
        std::vector<wchar_t> wbuffer(len);
        MultiByteToWideChar(CP_ACP, 0, buffer.data(), -1, wbuffer.data(), len);
        SetWindowTextW(g_hConfigEdit, wbuffer.data());

        // 更新状态栏
        std::wstring statusText = L"路径: " + g_currentFilePath +
            L" | 大小: " + GetFileSizeString(fileSize) +
            L" | 修改时间: " + TimeToString(fileInfo.ftLastWriteTime) +
            L" | 编码: ANSI";
        UpdateStatusBar(g_hConfigStatusBar, statusText);
    }
}

LRESULT CALLBACK ConfigWndProc(HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam) {
    switch (message) {
    case WM_COMMAND:
        switch (LOWORD(wParam)) {
        case 100: // 打开文件
            OpenFileInConfigWindow();
            break;
        case 101: // 跳转到行
            MessageBoxW(hWnd, L"跳转到行功能待实现", L"提示", MB_OK);
            break;
        case 102: // 查看哈希
            MessageBoxW(hWnd, L"查看哈希功能待实现", L"提示", MB_OK);
            break;
        case 103: // 确认编码
            MessageBoxW(hWnd, L"确认编码功能待实现", L"提示", MB_OK);
            break;
        }
        break;
    case WM_SIZE: {
        RECT rc;
        GetClientRect(hWnd, &rc);

        // 调整状态栏大小
        SendMessageW(g_hConfigStatusBar, WM_SIZE, 0, 0);

        // 调整编辑框大小
        RECT sbRect;
        GetWindowRect(g_hConfigStatusBar, &sbRect);
        int sbHeight = sbRect.bottom - sbRect.top;

        SetWindowPos(g_hConfigEdit, NULL, 10, 50, rc.right - 20, rc.bottom - 60 - sbHeight, SWP_NOZORDER);
        break;
    }
    case WM_CLOSE:
        DestroyWindow(hWnd);
        g_hConfigWnd = NULL;
        break;
    default:
        return DefWindowProcW(hWnd, message, wParam, lParam);
    }
    return 0;
}

LRESULT CALLBACK WndProc(HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam) {
    switch (message) {
    case WM_CREATE: {
        // 初始化通用控件
        INITCOMMONCONTROLSEX icex;
        icex.dwSize = sizeof(INITCOMMONCONTROLSEX);
        icex.dwICC = ICC_PROGRESS_CLASS | ICC_LISTVIEW_CLASSES | ICC_BAR_CLASSES;
        InitCommonControlsEx(&icex);

        // 创建菜单栏
        HMENU hMenuBar = CreateMenu();
        HMENU hFileMenu = CreatePopupMenu();
        HMENU hToolsMenu = CreatePopupMenu();
        HMENU hHelpMenu = CreatePopupMenu();

        AppendMenuW(hFileMenu, MF_STRING, IDM_DIR_SELECT, L"目录选择");
        AppendMenuW(hFileMenu, MF_STRING, IDM_CONFIG, L"配置选项");
        AppendMenuW(hFileMenu, MF_STRING, IDM_VIEW_PARAMS, L"查看参数");
        AppendMenuW(hFileMenu, MF_SEPARATOR, 0, NULL);
        AppendMenuW(hFileMenu, MF_STRING, IDM_EXIT, L"退出");

        AppendMenuW(hToolsMenu, MF_STRING, IDM_PACK, L"打包");
        AppendMenuW(hToolsMenu, MF_STRING, IDM_UNPACK, L"解压");
        AppendMenuW(hToolsMenu, MF_STRING, IDM_TRANSCODE, L"转码");

        AppendMenuW(hHelpMenu, MF_STRING, IDM_TIPS, L"提示");
        AppendMenuW(hHelpMenu, MF_STRING, IDM_DONATE, L"捐赠");
        AppendMenuW(hHelpMenu, MF_STRING, IDM_ABOUT, L"关于");

        AppendMenuW(hMenuBar, MF_POPUP, (UINT_PTR)hFileMenu, L"文件");
        AppendMenuW(hMenuBar, MF_POPUP, (UINT_PTR)hToolsMenu, L"工具");
        AppendMenuW(hMenuBar, MF_POPUP, (UINT_PTR)hHelpMenu, L"帮助");

        SetMenu(hWnd, hMenuBar);

        // 创建按钮行
        HWND hBtnDir = CreateWindowW(
            L"BUTTON", L"目录选择",
            WS_VISIBLE | WS_CHILD | BS_PUSHBUTTON,
            10, 10, 80, 30, hWnd, (HMENU)1, GetModuleHandle(NULL), NULL);

        g_hDirEdit = CreateWindowExW(
            WS_EX_CLIENTEDGE, L"EDIT", NULL,
            WS_VISIBLE | WS_CHILD | ES_AUTOHSCROLL,
            100, 10, 300, 30, hWnd, NULL, GetModuleHandle(NULL), NULL);

        HWND hBtnPack = CreateWindowW(
            L"BUTTON", L"打包",
            WS_VISIBLE | WS_CHILD | BS_PUSHBUTTON,
            410, 10, 60, 30, hWnd, (HMENU)2, GetModuleHandle(NULL), NULL);

        HWND hBtnUnpack = CreateWindowW(
            L"BUTTON", L"解压",
            WS_VISIBLE | WS_CHILD | BS_PUSHBUTTON,
            480, 10, 60, 30, hWnd, (HMENU)3, GetModuleHandle(NULL), NULL);

        HWND hBtnTranscode = CreateWindowW(
            L"BUTTON", L"转码",
            WS_VISIBLE | WS_CHILD | BS_PUSHBUTTON,
            550, 10, 60, 30, hWnd, (HMENU)4, GetModuleHandle(NULL), NULL);

        HWND hBtnConfig = CreateWindowW(
            L"BUTTON", L"配置",
            WS_VISIBLE | WS_CHILD | BS_PUSHBUTTON,
            620, 10, 60, 30, hWnd, (HMENU)5, GetModuleHandle(NULL), NULL);

        // 创建进度条
        g_hProgress = CreateWindowExW(
            0, PROGRESS_CLASSW, NULL,
            WS_VISIBLE | WS_CHILD | PBS_SMOOTH,
            10, 50, 670, 20, hWnd, NULL, GetModuleHandle(NULL), NULL);
        SendMessageW(g_hProgress, PBM_SETRANGE, 0, MAKELPARAM(0, 100));
        ShowWindow(g_hProgress, g_bShowProgress ? SW_SHOW : SW_HIDE);

        // 创建列表项
        g_hListBox = CreateWindowExW(
            WS_EX_CLIENTEDGE, L"LISTBOX", NULL,
            WS_VISIBLE | WS_CHILD | WS_VSCROLL | LBS_NOTIFY | LBS_HASSTRINGS,
            10, 80, 670, 400, hWnd, NULL, GetModuleHandle(NULL), NULL);

        // 创建状态栏
        g_hStatusBar = CreateWindowExW(
            0, STATUSCLASSNAMEW, NULL,
            WS_CHILD | WS_VISIBLE | SBARS_SIZEGRIP,
            0, 0, 0, 0, hWnd, NULL, GetModuleHandle(NULL), NULL);

        // 添加一些示例列表项
        for (int i = 1; i <= 20; i++) {
            std::wstring item = L"列表项 " + std::to_wstring(i);
            SendMessageW(g_hListBox, LB_ADDSTRING, 0, (LPARAM)item.c_str());
        }

        UpdateStatusBar(g_hStatusBar, L"就绪");
        break;
    }
    case WM_SIZE: {
        RECT rc;
        GetClientRect(hWnd, &rc);

        // 调整状态栏大小
        SendMessageW(g_hStatusBar, WM_SIZE, 0, 0);

        // 调整列表项大小
        RECT sbRect;
        GetWindowRect(g_hStatusBar, &sbRect);
        int sbHeight = sbRect.bottom - sbRect.top;

        int buttonRowHeight = 50;
        int progressHeight = g_bShowProgress ? 30 : 0;

        SetWindowPos(g_hListBox, NULL, 10, 80, rc.right - 20,
            rc.bottom - 90 - sbHeight - progressHeight, SWP_NOZORDER);

        // 调整进度条位置
        if (g_bShowProgress) {
            SetWindowPos(g_hProgress, NULL, 10, rc.bottom - sbHeight - 30,
                rc.right - 20, 20, SWP_NOZORDER);
        }
        break;
    }
    case WM_COMMAND:
        switch (LOWORD(wParam)) {
        case 1: // 目录选择按钮
            MessageBoxW(hWnd, L"目录选择功能待实现", L"提示", MB_OK);
            break;
        case 2: // 打包按钮
            g_bShowProgress = true;
            ShowWindow(g_hProgress, SW_SHOW);
            SendMessageW(g_hProgress, PBM_SETPOS, 0, 0);
            SetTimer(hWnd, 1, 100, NULL); // 模拟进度更新
            UpdateStatusBar(g_hStatusBar, L"正在打包...");
            break;
        case 3: // 解压按钮
            g_bShowProgress = true;
            ShowWindow(g_hProgress, SW_SHOW);
            SendMessageW(g_hProgress, PBM_SETPOS, 0, 0);
            SetTimer(hWnd, 2, 100, NULL); // 模拟进度更新
            UpdateStatusBar(g_hStatusBar, L"正在解压...");
            break;
        case 4: // 转码按钮
            g_bShowProgress = true;
            ShowWindow(g_hProgress, SW_SHOW);
            SendMessageW(g_hProgress, PBM_SETPOS, 0, 0);
            SetTimer(hWnd, 3, 100, NULL); // 模拟进度更新
            UpdateStatusBar(g_hStatusBar, L"正在转码...");
            break;
        case 5: // 配置按钮
            ShowConfigWindow();
            break;
        case IDM_CONFIG: // 配置选项菜单
        case IDM_VIEW_PARAMS: // 查看参数菜单
            ShowConfigWindow();
            break;
        case IDM_EXIT: // 退出菜单
            PostQuitMessage(0);
            break;
        }
        break;
    case WM_TIMER:
        if (wParam == 1 || wParam == 2 || wParam == 3) {
            int pos = SendMessageW(g_hProgress, PBM_GETPOS, 0, 0);
            if (pos < 100) {
                SendMessageW(g_hProgress, PBM_SETPOS, pos + 2, 0);
            }
            else {
                KillTimer(hWnd, wParam);
                g_bShowProgress = false;
                ShowWindow(g_hProgress, SW_HIDE);
                UpdateStatusBar(g_hStatusBar, L"操作完成");
                RECT rc;
                GetClientRect(hWnd, &rc);
                InvalidateRect(hWnd, &rc, TRUE);
            }
        }
        break;
    case WM_CLOSE:
        PostQuitMessage(0);
        break;
    default:
        return DefWindowProcW(hWnd, message, wParam, lParam);
    }
    return 0;
}

int WINAPI wWinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, PWSTR pCmdLine, int nCmdShow) {
    WNDCLASSEXW wcex = { sizeof(WNDCLASSEXW) };
    wcex.style = CS_HREDRAW | CS_VREDRAW;
    wcex.lpfnWndProc = WndProc;
    wcex.hInstance = hInstance;
    wcex.hCursor = LoadCursor(NULL, IDC_ARROW);
    wcex.hbrBackground = (HBRUSH)(COLOR_WINDOW + 1);
    wcex.lpszClassName = L"MainWindowClass";

    if (!RegisterClassExW(&wcex)) {
        MessageBoxW(NULL, L"窗口类注册失败!", L"错误", MB_ICONERROR);
        return 1;
    }

    g_hMainWnd = CreateWindowW(
        L"MainWindowClass", L"应用程序",
        WS_OVERLAPPEDWINDOW,
        CW_USEDEFAULT, CW_USEDEFAULT, 800, 600,
        NULL, NULL, hInstance, NULL);

    if (!g_hMainWnd) {
        MessageBoxW(NULL, L"窗口创建失败!", L"错误", MB_ICONERROR);
        return 1;
    }

    ShowWindow(g_hMainWnd, nCmdShow);
    UpdateWindow(g_hMainWnd);

    MSG msg;
    while (GetMessageW(&msg, NULL, 0, 0)) {
        TranslateMessage(&msg);
        DispatchMessageW(&msg);
    }

    return (int)msg.wParam;
}