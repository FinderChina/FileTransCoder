# 1 MinGw
## （1）官网
https://www.mingw-w64.org/
## （2）下载地址
https://github.com/niXman/mingw-builds-binaries/releases
https://sourceforge.net/projects/mingw-w64/files/mingw-w64/mingw-w64-release/
https://sourceforge.net/projects/mingw-w64/files/Toolchains%20targetting%20Win64/Personal%20Builds/mingw-builds/8.1.0/threads-posix/seh/
## （3）安装MinGw x86_64-8.1.0-release-posix-seh-rt_v6-rev0 
PATH路径示例：$MinGw-8.1.0_HOME$/bin

# 2 OpenSSL
## （1）官网地址
https://openssl-library.org/source/
## （2）预编译二进制文件下载地址（Windows）
https://slproweb.com/products/Win32OpenSSL.html
https://slproweb.com/download/Win64OpenSSL-3_5_0.exe
## （3）安装Win64OpenSSL-3_5_0
包含目录路径示例：$OpenSSL-Win64_HOME$/include/  (项目属性 → C/C++ → 常规 → 附加包含目录)
库目录路径示例：$OpenSSL-Win64_HOME$/lib/VC/x64/MT/  (项目属性 → 链接器 → 常规 → 附加库目录)
PATH路径示例：$OpenSSL-Win64_HOME$  (需要将 libcrypto-3-x64.dll 和 libssl-3-x64.dll 添加到 PATH 路径中)

‌MT目录‌
    包含‌静态链接运行时库‌的OpenSSL库文件（如libsslMT.lib、libcryptoMT.lib），编译时将C运行时库（CRT）完全嵌入二进制文件。
‌特点‌：
    生成的可执行文件体积较大，但运行时无需依赖外部DLL（如MSVCRxxx.dll）16。
    适用于需要独立部署且避免外部依赖的场景（如嵌入式系统或封闭环境）46。
‌MD目录‌
     包含‌动态链接运行时库‌的OpenSSL库文件（如libsslMD.lib、libcryptoMD.dll），依赖操作系统的C运行时库DLL。
‌特点‌：
    生成的可执行文件体积更小，但需目标机器安装对应版本的Visual C++ Redistributable16。
    适用于标准Windows开发环境，便于多模块共享运行时库，避免内存管理冲突。

# 3 wxWidgets-3.2.8
## （1）官网
https://wxwidgets.org/index.html
## （2）下载地址（）
Header Files
https://github.com/wxWidgets/wxWidgets/releases/download/v3.2.8/wxWidgets-3.2.8-headers.7z
Development Files: 64-Bit (x86_64)
https://github.com/wxWidgets/wxWidgets/releases/download/v3.2.8/wxMSW-3.2.8_gcc810_x64_Dev.7z
Release DLLs: 64-Bit (x86_64)
https://github.com/wxWidgets/wxWidgets/releases/download/v3.2.8/wxMSW-3.2.8_gcc810_x64_ReleaseDLL.7z
## （3）解压
包含目录路径示例：$wxWidgets-3.2.8_HOME$/include/  $wxWidgets-3.2.8-Dev_HOME$/lib/gcc810_x64_dll/mswu/
库目录路径示例：$wxWidgets-3.2.8-Dev_HOME$/lib/gcc810_x64_dll/
PATH路径示例：$wxWidgets-3.2.8-Release_HOME$/lib/gcc810_x64_dll/

# 4 VSCode
## （1）安装VSCode及插件: C/C++; C/C++ Extension Pack; C/C++ Runner;  
## （2）配置 tasks.json 和 launch.json
## （3）定义快捷键运行程序
ctrl+shift+P打开全局终端，输入open keyboard shutcuts.打开键盘快捷键设置
选择打开 keybindings.json 文件，之后会弹出两栏，左边是系统设置，右边是用户自定义设置，编辑右边，添加如下配置
{
    "key": "ctrl+shift+r",
    "command": "workbench.action.tasks.runTask",
    "args": "Run Active C++ File" // 对应 tasks.json 文件中定义的运行任务的 label
}

# 5 命令行直接编译命令
"D:/Program Files/mingw64-8/bin/g++.exe" sha512.cpp -o sha512 -L"C:/Program Files/OpenSSL-Win64/lib/VC/x64/MT/" -I"C:/Program Files/OpenSSL-Win64/include/" -llibssl -llibcrypto -static

g++ sha512.cpp -o sha512 -I"C:/Program Files/OpenSSL-Win64/include/" -L"C:/Program Files/OpenSSL-Win64/lib/VC/x64/MT/" -llibssl -llibcrypto -static

"D:/Program Files/mingw64-8/bin/g++.exe" -fdiagnostics-color=always -g Txt.cpp -o Txt -s -ID:/C/wxWidgets-3.2.8/include/ -ID:/C/wxWidgets-3.2.8/dev/lib/gcc810_x64_dll/mswu/ -mwindows -LD:/C/wxWidgets-3.2.8/dev/lib/gcc810_x64_dll/ -lwxbase32u -lwxmsw32u_core -lwxmsw32u_stc -static 

"D:/Program Files/mingw64-8/bin/g++.exe" -fdiagnostics-color=always -g TransCoder.cpp -o TransCoder -s -I"C:/Program Files/OpenSSL-Win64/include/" -ID:/wxWidgets/wxWidgets-3.2.8/include/ -ID:/wxWidgets/wxWidgets-3.2.8/dev/lib/gcc810_x64_dll/mswu/ -mwindows -L"C:/Program Files/OpenSSL-Win64/lib/VC/x64/MT/" -LD:/wxWidgets/wxWidgets-3.2.8/dev/lib/gcc810_x64_dll/ -llibssl -llibcrypto -lwxbase32u -lwxmsw32u_core -lwxmsw32u_stc -static 

"D:/Program Files/mingw64-8/bin/g++.exe" -fdiagnostics-color=always -g artprov.cpp -o artprov -s -ID:/wxWidgets/wxWidgets-3.2.8/include/ -ID:/wxWidgets/wxWidgets-3.2.8/dev/lib/gcc810_x64_dll/mswu/ -mwindows -LD:/wxWidgets/wxWidgets-3.2.8/dev/lib/gcc810_x64_dll/ -lwxbase32u -lwxmsw32u_core -static 

"D:/Program Files/mingw64-8/bin/g++.exe" -fdiagnostics-color=always -g FrameMsg.cpp -o FrameMsg -s -ID:/wxWidgets/wxWidgets-3.2.8/include/ -ID:/wxWidgets/wxWidgets-3.2.8/dev/lib/gcc810_x64_dll/mswu/ -mwindows -LD:/wxWidgets/wxWidgets-3.2.8/dev/lib/gcc810_x64_dll/ -lwxbase32u -lwxmsw32u_core -static 

"D:/Program Files/mingw64-8/bin/g++.exe" -fdiagnostics-color=always -g ImgDialog.cpp -o ImgDialog -s -ID:/wxWidgets/wxWidgets-3.2.8/include/ -ID:/wxWidgets/wxWidgets-3.2.8/dev/lib/gcc810_x64_dll/mswu/ -mwindows -LD:/wxWidgets/wxWidgets-3.2.8/dev/lib/gcc810_x64_dll/ -lwxbase32u -lwxmsw32u_core -static 

# 参考网页
## VSCode帮助
https://code.visualstudio.com/docs/debugtest/debugging
https://code.visualstudio.com/docs/debugtest/tasks
https://code.visualstudio.com/docs/cpp/config-mingw

## VScode：配置一键编译运行(Windows+MinGW)
https://www.cnblogs.com/yongdaimi/p/14475971.html
## VSCODE 一键编译运行
https://blog.csdn.net/qq_30690821/article/details/84502287
## VsCode配置C++Windows开发编译环境 【通过MinGW-w64实现】
https://zhuanlan.zhihu.com/p/2702808666
## 搭建windows下基于VSCode的C++编译和调试环境
https://blog.csdn.net/qijitao/article/details/130083052
## VSCode下配置C++编译debug环境
https://blog.csdn.net/m0_46069225/article/details/147717610
## Windows环境下OpenSSL安装与集成指南
https://blog.csdn.net/arbboter/article/details/146522943

# MSYS2-MinGw
## （1）MSYS2安装
官网：https://www.msys2.org/
下载：https://github.com/msys2/msys2-installer/releases/download/2025-02-21/msys2-x86_64-20250221.exe （ Installing MSYS2 requires 64 bit Windows 10 or newer.）
## （2）环境更新
打开 MSYS2 MINGW64 终端
rem 更新系统及软件包列表
pacman -Syu
rem 如果提示关闭终端，重新打开 MSYS2 并再次运行更新
pacman -Su

rem 安装 MinGW-w64 工具链（64位 版本），包含 gcc、g++、make 等工具
pacman -S mingw-w64-x86_64-toolchain

rem 手工设置: path=$MSYS2_HOME$\mingw64\bin，查看 g++ 版本
g++ --version

rem 安装 libzip（64位 版本）
pacman -S mingw-w64-x86_64-libzip

rem 安装 OpenSSL（64位 版本）
pacman -S mingw-w64-x86_64-openssl

rem 查看 OpenSSL 版本
openssl version

rem 安装 wxWidgets 和 Scintilla 库（STC 依赖）
pacman -S mingw-w64-x86_64-wxWidgets3.2
pacman -S mingw-w64-x86_64-scintilla

rem 验证库是否包含所需符号
nm /mingw64/lib/libwxscintilla.a | grep wxStyledTextCtrl
wx-config --version
wx-config --cxxflags  # 查看头文件路径
wx-config --libs      # 查看链接库

rem 使用 cygpath 转换路径
pwd
cygpath -w /home/user_name

## （3）编译命令
gcc -o myprogram myprogram.c -lzip -lssl -lcrypto

rem MSYS2 的 wxWidgets 包可能未将 STC 设为默认组件，导致 wx-config 输出不完整。
rem 即 wx-config 不提供 STC 库，需要手动添加补全 STC 链接： -lwx_mswu_stc-3.2

g++ Txt.cpp -o Txt \
    $(wx-config --cxxflags --libs) \
    -lwx_mswu_stc-3.2 \
    -lwxscintilla-3.2
    
g++ Txt.cpp -o Txt \
    $(wx-config --static --cxxflags --libs) \  # 强制静态链接
    -lwx_mswu_stc-3.2 \
    /mingw64/lib/libwxscintilla-3.2.a
    
g++ TransCoder.cpp -o TransCoder $(wx-config --cxxflags --libs) -lwx_mswu_stc-3.2 -lwxscintilla-3.2 -lcrypto 
g++ TransCoder.cpp -o TransCoder $(wx-config --static --cxxflags --libs) -lwx_mswu_stc-3.2 -lwxscintilla-3.2 -lcrypto 