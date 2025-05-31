#include <windows.h>
 
LRESULT CALLBACK WindowProcedure(HWND, UINT, WPARAM, LPARAM);
 
char szClassName[ ] = "myWindowClass";
 
int WINAPI WinMain(HINSTANCE hThisInstance, HINSTANCE hPrevInstance, LPSTR lpszArgument, int nCmdShow) {
    HWND hwnd;               // 窗口句柄
    MSG messages;            // 消息
    WNDCLASSEX wincl;        // 窗口类结构体
 
    // 窗口类结构体初始化
    wincl.hInstance = hThisInstance;
    wincl.lpszClassName = szClassName;
    wincl.lpfnWndProc = WindowProcedure;      // 指向窗口过程函数
    wincl.style = CS_DBLCLKS;                 // 双击风格
    wincl.cbSize = sizeof(WNDCLASSEX);
 
    // 使用默认图标和鼠标指针
    wincl.hIcon = LoadIcon(NULL, IDI_APPLICATION);
    wincl.hIconSm = LoadIcon(NULL, IDI_APPLICATION);
    wincl.hCursor = LoadCursor(NULL, IDC_ARROW);
    wincl.lpszMenuName = NULL;                 // 没有菜单
    wincl.cbClsExtra = 0;                      // 没有额外窗口数据
    wincl.cbWndExtra = 0;                      // 没有额外窗口数据
    wincl.hbrBackground = (HBRUSH) COLOR_BACKGROUND; // 背景色为系统颜色
 
    if (!RegisterClassEx(&wincl)) {
        return 0;
    }
 
    // 创建窗口
    hwnd = CreateWindowEx(
        0,                   // 扩展风格
        szClassName,         // 类名
        "工具",      // 窗口标题
        WS_OVERLAPPEDWINDOW, // 默认窗口风格
        CW_USEDEFAULT,       // X位置
        CW_USEDEFAULT,       // Y位置
        544,                 // 宽度
        375,                // 高度
        HWND_DESKTOP,        // 父窗口句柄（桌面）
        NULL,                // 没有菜单句柄
        hThisInstance,       // 应用实例句柄
        NULL                // 没有窗口创建数据
    );
 
    ShowWindow(hwnd, nCmdShow); // 显示窗口
 
    // 消息循环
    while (GetMessage(&messages, NULL, 0, 0)) {
        TranslateMessage(&messages); // 将虚拟键码转换为字符码等。
        DispatchMessage(&messages);  // 将消息派发到窗口过程函数。
    }
 
    return messages.wParam; // 返回消息循环的最后结果。
}
 
LRESULT CALLBACK WindowProcedure(HWND hwnd, UINT message, WPARAM wParam, LPARAM lParam) {
    switch (message) {                  // 处理消息类型。
        case WM_DESTROY:
            PostQuitMessage(0);       // 发送一个WM_QUIT消息并退出。
            break;
        default:                      // 处理所有其他消息。
            return DefWindowProc(hwnd, message, wParam, lParam); // 使用默认过程。
    }
    return 0; // 如果消息已被处理，返回0。否则返回1。
}