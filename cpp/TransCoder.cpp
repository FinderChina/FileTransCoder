#include <wx/wx.h>
#include <wx/stc/stc.h>
#include <wx/dir.h>
#include <wx/file.h>
#include <wx/textdlg.h>
#include <wx/utils.h>
#include <wx/filefn.h>
#include <wx/zipstrm.h>
#include <wx/wfstream.h>
#include <wx/sstream.h>
#include <wx/textfile.h>
#include <wx/menu.h>
#include <wx/artprov.h>
#include <wx/fontmap.h>
#include <wx/filename.h>
#include <wx/stdpaths.h>
#include <wx/statline.h>
#include <wx/base64.h>
#include <wx/mstream.h>
#include <codecvt>
#include <locale>
#include <openssl/evp.h>
#include <string>
#include <memory>
#include <vector>
#include <cstdint>
#include <fstream>
#include <iomanip>
#include <sstream>
#include <windows.h>
#include <TlHelp32.h>
#include <algorithm>


// 统一换行符：将 std::string 中的 "\r\n" 替换为 "\n"
void normalizeNewlines(std::string &str) {
    size_t pos = 0;
    while ((pos = str.find("\r\n", pos)) != std::string::npos) {
        str.replace(pos, 2, "\n");
    }
}

// 统一换行符函数：将 wxScopedCharBuffer(只读) 中的 "\r\n" 替换为 "\n"
void NormalizeBufNewlines(wxScopedCharBuffer& buffer) {
    if (!buffer.data()) {
        return;
    }

    const char* src = buffer.data();
    const size_t length = buffer.length();

    // 计算新长度
    size_t newLength = 0;
    for (size_t i = 0; i < length; ) {
        if (i + 1 < length && src[i] == '\r' && src[i+1] == '\n') {
            newLength++;  // \r\n → \n
            i += 2;
        } else {
            newLength++;  // 其他字符保持不变
            i++;
        }
    }

    // 正确创建新缓冲区的方式
    char* newData = new char[newLength + 1];  // +1 用于可能的 null 终止符
    wxScopedCharBuffer newBuffer = wxScopedCharBuffer::CreateOwned(newData, newLength);

    // 执行替换
    char* dest = newBuffer.data();
    for (size_t i = 0; i < length; ) {
        if (i + 1 < length && src[i] == '\r' && src[i+1] == '\n') {
            *dest++ = '\n';
            i += 2;
        } else {
            *dest++ = src[i++];
        }
    }

    buffer = newBuffer;
}

// 获取exe路径
std::string getExeDir() {
    char path[MAX_PATH] = {0};
    GetModuleFileNameA(NULL, path, MAX_PATH);
    char* lastSlash = strrchr(path, '\\');
    if (lastSlash) *lastSlash = '\0';
    return std::string(path);
}

// 获取exe路径
std::wstring getExeDirW() {
    wchar_t path[MAX_PATH] = {0};
    GetModuleFileNameW(NULL, path, MAX_PATH);
    wchar_t* lastSlash = wcsrchr(path, L'\\');
    if (lastSlash) *lastSlash = L'\0';
    return std::wstring(path);
}

// 转换 ANSI (std::string) 到宽字符 (std::wstring)
std::wstring stringToWstring(const std::string& str) {
    int size = MultiByteToWideChar(CP_ACP, 0, str.c_str(), -1, NULL, 0);
    if (size <= 0) return L"";
    std::wstring wstr(size, 0);
    MultiByteToWideChar(CP_ACP, 0, str.c_str(), -1, &wstr[0], size);
    return wstr;
}

// 删除路径
bool deleteDirectory(const std::wstring& path) {
    // 首先在路径末尾添加双空字符
    std::wstring tempPath = path + L'\0' + L'\0';
    
    SHFILEOPSTRUCTW fileOp = {0};
    fileOp.wFunc = FO_DELETE;
    fileOp.pFrom = tempPath.c_str();
    fileOp.fFlags = FOF_NO_UI | FOF_NOCONFIRMATION | FOF_SILENT;
    
    int result = SHFileOperationW(&fileOp);
    if (result != 0) {
        std::wcerr << L"删除目录失败，错误代码: " << result << std::endl;
        //wxMessageBox(wxT("删除目录失败，可尝试重启电脑后重试。"), wxT("提示"), wxOK | wxICON_INFORMATION); 
        return false;
    }
    return true;
}

// 使用OpenSSL 3.0推荐API
std::string sha512(const std::string& input) {
    //std::wstring_convert<std::codecvt_utf8<wchar_t>> converter;
    //std::string utf8_input = converter.to_bytes(input);
    
    std::string utf8_input = input;
    normalizeNewlines(utf8_input);     //强制统一换行符
    //for (char c : utf8_input) { printf("%02x ", (unsigned char)c); }
    
    EVP_MD_CTX* ctx = EVP_MD_CTX_new();
    unsigned char digest[EVP_MAX_MD_SIZE];
    unsigned int len;
    
    EVP_DigestInit_ex(ctx, EVP_sha512(), NULL);
    EVP_DigestUpdate(ctx, utf8_input.c_str(), utf8_input.size());
    EVP_DigestFinal_ex(ctx, digest, &len);
    EVP_MD_CTX_free(ctx);

    std::stringstream ss;
    for (size_t i = 0; i < len; i++) {
        ss << std::hex << std::setw(2) << std::setfill('0') << (int)digest[i];
    }
    return ss.str();
}

wxBitmap CreateWhiteBackgroundToolBitmap(const wxArtID& id, const wxSize& size) {
    // 获取原始位图
    wxBitmap original = wxArtProvider::GetBitmap(id, wxART_TOOLBAR, size);
    
    // 创建新位图
    wxBitmap customBgBitmap(size);
    wxMemoryDC dc;
    dc.SelectObject(customBgBitmap);
    
    // 使用自定义颜色填充背景
    wxColour bgColour(240, 240, 240); // 自定义灰色
    wxBrush brush(bgColour);
    dc.SetBackground(brush);  // brush 可使用预定义颜色常量，如 *wxWHITE_BRUSH
    dc.Clear();
    
    // 绘制原始位图（保持透明部分）
    dc.DrawBitmap(original, 0, 0, true); // true表示使用掩码透明
    
    dc.SelectObject(wxNullBitmap);
    return customBgBitmap;
}

wxBitmap CreateWhiteBackgroundToolBitmapInvalid(const wxArtID& id, const wxSize& size) {
    wxBitmap original = wxArtProvider::GetBitmap(id, wxART_TOOLBAR, size);
    wxImage img = original.ConvertToImage();
    
    // 如果没有alpha通道但有掩码，转换为alpha通道
    if (!img.HasAlpha() && img.HasMask()) {
        img.InitAlpha();
    }
    
    // 创建白色背景图像
    wxImage whiteBgImg(size.GetWidth(), size.GetHeight());
    whiteBgImg.SetRGB(wxRect(size), 240, 240, 240);
    
    // 根据是否有alpha通道选择粘贴方式
    /*
    if (img.HasAlpha()) {
        // 对于有透明度的图像，使用Paste保留alpha混合
        whiteBgImg.Paste(img, 0, 0);
    } else {
        // 对于无透明度的图像，可以直接绘制
        wxBitmap tempBmp(img);
        wxMemoryDC dc;
        dc.SelectObject(wxBitmap(whiteBgImg));
        dc.DrawBitmap(tempBmp, 0, 0, true);
        dc.SelectObject(wxNullBitmap);
    }  //*/
    
    // 统一使用Paste方法
    whiteBgImg.Paste(img, 0, 0);
    
    return wxBitmap(whiteBgImg);
}

// 编码检测函数
wxString DetectFileEncoding(const wxString& filename) {
    wxFile file(filename);
    if (!file.IsOpened()) {
        return wxEmptyString;
    }

    // 读取文件前几个字节来检测BOM
    unsigned char bom[4] = {0};
    file.Read(bom, 4);
    file.Close();

    // 检查UTF-8 BOM
    if (bom[0] == 0xEF && bom[1] == 0xBB && bom[2] == 0xBF) {
        return wxT("UTF-8");
    }
    // 检查UTF-16 LE BOM
    else if (bom[0] == 0xFF && bom[1] == 0xFE) {
        return wxT("UTF-16LE");
    }
    // 检查UTF-16 BE BOM
    else if (bom[0] == 0xFE && bom[1] == 0xFF) {
        return wxT("UTF-16BE");
    }
    // 检查UTF-32 LE BOM
    else if (bom[0] == 0xFF && bom[1] == 0xFE && bom[2] == 0x00 && bom[3] == 0x00) {
        return wxT("UTF-32LE");
    }
    // 检查UTF-32 BE BOM
    else if (bom[0] == 0x00 && bom[1] == 0x00 && bom[2] == 0xFE && bom[3] == 0xFF) {
        return wxT("UTF-32BE");
    }

    // 如果没有BOM，默认使用UTF-8
    return wxT("UTF-8");
}

// Base64解码函数
std::vector<unsigned char> base64_decode(const std::string& encoded) {
    static const std::string base64_chars = 
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    
    std::vector<unsigned char> decoded;
    int val = 0, val_bits = -8;
    
    for (unsigned char c : encoded) {
        if (c == '=') break;
        const size_t index = base64_chars.find(c);
        if (index == std::string::npos) continue;
        val = (val << 6) + index;
        val_bits += 6;
        if (val_bits >= 0) {
            decoded.push_back(static_cast<unsigned char>((val >> val_bits) & 0xFF));
            val_bits -= 8;
        }
    }
    
    return decoded;
}

bool showConfirmDialog(wxWindow* parent, const wxString& message, const wxString& title = wxT("确认")) {
    wxMessageDialog dialog(parent, message, title, wxYES_NO | wxICON_QUESTION);
    int result = dialog.ShowModal();
    return (result == wxID_YES); // 返回 true 如果用户点击 "是"
}

bool runExecutable(const std::wstring& exePath, const std::wstring& arguments = L"") {
    STARTUPINFOW si = { sizeof(STARTUPINFOW) };
    PROCESS_INFORMATION pi = { 0 };

    // 构造完整命令行（exe路径 + 参数）
    std::wstring commandLine = L"\"" + exePath + L"\" " + arguments;

    // 创建进程
    BOOL success = CreateProcessW(
        NULL,                           // 不直接指定 exe 路径（用 commandLine 代替）
        &commandLine[0],                // 完整命令行（注意：需要可修改的字符串）
        NULL,                           // 进程安全属性（默认）
        NULL,                           // 线程安全属性（默认）
        FALSE,                          // 不继承句柄
        0,                              // 无特殊标志
        NULL,                           // 使用当前环境变量
        NULL,                           // 使用当前工作目录
        &si,                            // 启动信息
        &pi                             // 返回进程信息
    );

    if (!success) {
        DWORD error = GetLastError();
        _tprintf(_T("CreateProcess 失败，错误代码: %d\n"), error);
        return false;
    }

    // 关闭句柄（避免资源泄漏）
    CloseHandle(pi.hProcess);
    CloseHandle(pi.hThread);

    return true;
}

// 结束指定名称的进程
bool killProcessByName(const std::wstring& processName) {
    bool result = false;
    PROCESSENTRY32W pe32;
    pe32.dwSize = sizeof(PROCESSENTRY32W);

    // 创建进程快照
    HANDLE hProcessSnap = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    if (hProcessSnap == INVALID_HANDLE_VALUE) {
        std::wcerr << L"CreateToolhelp32Snapshot 失败" << std::endl;
        return false;
    }

    // 遍历进程列表
    if (Process32FirstW(hProcessSnap, &pe32)) {
        do {
            if (_wcsicmp(pe32.szExeFile, processName.c_str()) == 0) {
                // 找到匹配进程
                HANDLE hProcess = OpenProcess(PROCESS_TERMINATE, FALSE, pe32.th32ProcessID);
                if (hProcess != NULL) {
                    // 终止进程
                    if (TerminateProcess(hProcess, 0)) {
                        std::wcout << L"已终止进程: " << pe32.szExeFile << std::endl;
                        result = true;
                    }
                    CloseHandle(hProcess);
                }
            }
        } while (Process32NextW(hProcessSnap, &pe32));
    }

    CloseHandle(hProcessSnap);
    return result;
}

// 启动Nginx
bool startNginx(const std::wstring& nginxPath, const std::wstring& workingDir) {
    STARTUPINFOW si = { sizeof(STARTUPINFOW) };
    PROCESS_INFORMATION pi = { 0 };

    // 构造命令行
    std::wstring commandLine = L"\"" + nginxPath + L"\"";

    // 创建进程
    if (!CreateProcessW(
        NULL,
        &commandLine[0],
        NULL,
        NULL,
        FALSE,
        CREATE_NO_WINDOW,  // 不显示窗口
        NULL,
        workingDir.c_str(),  // 指定工作目录
        &si,
        &pi
    )) {
        std::wcerr << L"启动Nginx失败，错误代码: " << GetLastError() << std::endl;
        return false;
    }

    CloseHandle(pi.hProcess);
    CloseHandle(pi.hThread);
    std::wcout << L"Nginx 启动成功" << std::endl;
    return true;
}

class EncodingDialog : public wxDialog {
public:
    EncodingDialog(wxWindow* parent, const wxString& encoding)
        : wxDialog(parent, wxID_ANY, wxT("设置初始编码"), wxDefaultPosition, wxSize(800, 600)) {
        
        int openId = wxNewId();
        int encodingId = wxNewId();
        int jumpId = wxNewId();
        int hashId = wxNewId();
        int confirmId = wxNewId();
        int aboutId = wxNewId();
        
        wxBoxSizer* btnSizer = new wxBoxSizer(wxHORIZONTAL); // new wxBoxSizer(wxHORIZONTAL); new wxGridSizer(1, 5, 0); // 1行,5px间距
        wxBitmapButton* encodingBtn = new wxBitmapButton(this, encodingId, wxArtProvider::GetBitmap(wxART_TIP, wxART_BUTTON, wxSize(24, 24)), wxDefaultPosition, wxSize(32, 32), wxBORDER_NONE);
        encodingBtn->SetToolTip(wxT("设置编码"));
        encodingBtn->SetCursor(wxCursor(wxCURSOR_HAND));

        wxBitmapButton* openBtn = new wxBitmapButton(this, openId, wxArtProvider::GetBitmap(wxART_FILE_OPEN, wxART_BUTTON, wxSize(24, 24)), wxDefaultPosition, wxSize(32, 32), wxBORDER_NONE);
        openBtn->SetToolTip(wxT("打开文件"));
        openBtn->SetCursor(wxCursor(wxCURSOR_HAND));

        wxBitmapButton* jumpBtn = new wxBitmapButton(this, jumpId, wxArtProvider::GetBitmap(wxART_FIND, wxART_BUTTON, wxSize(24, 24)), wxDefaultPosition, wxSize(32, 32), wxBORDER_NONE);
        jumpBtn->SetToolTip(wxT("跳转到行"));
        jumpBtn->SetCursor(wxCursor(wxCURSOR_HAND));

        wxBitmapButton* hashBtn = new wxBitmapButton(this, hashId, wxArtProvider::GetBitmap(wxART_EDIT, wxART_BUTTON, wxSize(24, 24)), wxDefaultPosition, wxSize(32, 32), wxBORDER_NONE);
        hashBtn->SetToolTip(wxT("查看哈希"));
        hashBtn->SetCursor(wxCursor(wxCURSOR_HAND));

        wxBitmapButton* confirmBtn = new wxBitmapButton(this, confirmId, wxArtProvider::GetBitmap(wxART_GO_HOME, wxART_BUTTON, wxSize(24, 24)), wxDefaultPosition, wxSize(32, 32), wxBORDER_NONE);
        confirmBtn->SetToolTip(wxT("确认编码"));
        confirmBtn->SetCursor(wxCursor(wxCURSOR_HAND));

        wxBitmapButton* aboutBtn = new wxBitmapButton(this, aboutId, wxArtProvider::GetBitmap(wxART_INFORMATION, wxART_BUTTON, wxSize(24, 24)), wxDefaultPosition, wxSize(32, 32), wxBORDER_NONE);
        aboutBtn->SetToolTip(wxT("关于"));
        aboutBtn->SetCursor(wxCursor(wxCURSOR_HAND));
        
        btnSizer->Add(encodingBtn, 0, wxALIGN_LEFT | wxLEFT, 5);
        btnSizer->Add(openBtn, 0, wxALIGN_LEFT | wxLEFT, 5);
        btnSizer->Add(jumpBtn, 0, wxALIGN_LEFT | wxLEFT, 5);
        btnSizer->Add(hashBtn, 0, wxALIGN_LEFT | wxLEFT, 5);
        btnSizer->Add(confirmBtn, 0, wxALIGN_LEFT | wxLEFT, 5);
        //btnSizer->Add(aboutBtn, 0, wxALIGN_LEFT | wxLEFT, 5);
        
        // 添加伸展空间使按钮保持左对齐
        btnSizer->AddStretchSpacer(); 
        
        // 创建文本控件
        m_textCtrl = new wxStyledTextCtrl(this, wxID_ANY);
        m_textCtrl->SetMarginType(0, wxSTC_MARGIN_NUMBER);
        m_textCtrl->SetMarginWidth(0, 50);
        //m_textCtrl->StyleSetAlignment(wxSTC_STYLE_LINENUMBER, wxSTC_ALIGN_LEFT);  //行号左对齐
        //m_textCtrl->SetWindowStyleFlag(wxBORDER_NONE);
        m_textCtrl->SetText(encoding);
        
        // 设置字体
        wxFont font(12, wxFONTFAMILY_MODERN, wxFONTSTYLE_NORMAL, wxFONTWEIGHT_NORMAL);
        m_textCtrl->StyleSetFont(wxSTC_STYLE_DEFAULT, font);
        
        // 设置自动换行
        m_textCtrl->SetWrapMode(wxSTC_WRAP_WORD);  // 按单词换行
        // m_textCtrl->SetWrapMode(wxSTC_WRAP_CHAR); // 按字符换行
        
        // 创建模拟状态栏
        wxPanel* statusBar = new wxPanel(this);
        wxBoxSizer* statusSizer = new wxBoxSizer(wxHORIZONTAL);
        
        // 创建3个状态标签（带边框模拟分隔线）
        for (int i = 0; i < 3; i++) {
            m_statusFields[i] = new wxStaticText(
                statusBar, wxID_ANY, wxT(""), 
                wxDefaultPosition, wxSize(-1, 25),  // 高度设为50像素，宽度自适应 wxDefaultSize,
                wxST_NO_AUTORESIZE | wxALIGN_CENTER_VERTICAL // | wxSIMPLE_BORDER
            );
            statusSizer->Add(m_statusFields[i], i == 0 ? 3 : 1, wxEXPAND | wxALL, 1);
            
            // 添加细线作为视觉分隔（替代边框）
            if (i < 2) {
                wxStaticLine* vLine = new wxStaticLine(statusBar, wxID_ANY, wxDefaultPosition, wxDefaultSize, wxLI_VERTICAL);
                statusSizer->Add(vLine , 0, wxEXPAND | wxLEFT | wxRIGHT, 2);
            }
            
            // 设置背景色
            //m_statusFields[i]->SetBackgroundColour(wxColour(240, 240, 240));
        }
        
        statusBar->SetSizer(statusSizer);
        statusBar->SetBackgroundColour(wxSystemSettings::GetColour(wxSYS_COLOUR_BTNFACE));
        
        // 布局
        wxBoxSizer* mainSizer = new wxBoxSizer(wxVERTICAL);
        mainSizer->Add(btnSizer, 0, wxALIGN_LEFT | wxTOP | wxLEFT, 0);
        mainSizer->Add(m_textCtrl, 1, wxEXPAND | wxLEFT | wxRIGHT, 0);
        mainSizer->Add(statusBar, 0, wxEXPAND | wxALL | wxBOTTOM, 0); // 状态栏贴底
        SetSizer(mainSizer);
        
        // 绑定事件
        encodingBtn->Bind(wxEVT_BUTTON, &EncodingDialog::OnSetEncoding, this);
        openBtn->Bind(wxEVT_BUTTON, &EncodingDialog::OnOpenFile, this);
        jumpBtn->Bind(wxEVT_BUTTON, &EncodingDialog::OnJumpToLine, this);
        hashBtn->Bind(wxEVT_BUTTON, &EncodingDialog::OnHash, this);
        confirmBtn->Bind(wxEVT_BUTTON, &EncodingDialog::OnConfirm, this);
        aboutBtn->Bind(wxEVT_BUTTON, &EncodingDialog::OnCodeAbout, this);
        
        // 无边框控件在部分平台可能需要手动刷新,添加以下代码确保渲染正确：
        //statusBar->Bind(wxEVT_PAINT, [](wxPaintEvent& e) {
        //    e.GetEventObject()->Refresh();
        //    e.Skip();
        //});

        // 初始状态
        SetStatusText(wxT("   就绪"), 0);
        SetStatusText(wxT(""), 1);
        SetStatusText(wxT("自动检测编码"), 2);
        autoDetectionFileEncoding = true;
    }
    
    // 状态栏更新方法
    void SetStatusText(const wxString& text, int field) {
        if (field >= 0 && field < 3) {
            m_statusFields[field]->SetLabel(text);
            
            /* 长文本处理
            wxString fullText = text;
            wxString displayText = text;
            if (text.length() > 20) {
                displayText = text.Left(18) + wxT("..");
            }
            m_statusFields[field]->SetLabel(displayText);
            m_statusFields[field]->SetToolTip(fullText);
            //*/
            
            // 强制刷新显示（解决无边框时的渲染问题）
            m_statusFields[field]->Refresh();
            m_statusFields[field]->Update();
        }
    }
    
    wxString GetEncodingText() const { return m_encodingText; }

private:
    bool LoadFileContent(const wxString& path, const wxString& encoding) {
        wxFile file(path);
        if (!file.IsOpened()) {
            wxMessageBox(wxT("无法打开文件"), wxT("错误"), wxOK | wxICON_ERROR);
            return false;
        }

        wxFileOffset fileSize = file.Length();
        wxString content;

        if (encoding == wxT("UTF-8")) {
            // 对于UTF-8文件，直接读取
            wxMemoryBuffer buffer(fileSize);
            file.Read(buffer.GetData(), fileSize);
            buffer.SetDataLen(fileSize);
            content = wxString::FromUTF8((const char*)buffer.GetData(), buffer.GetDataLen());
        } else if (encoding == wxT("GB2312")) {
            // 对于GB2312文件，使用wxCSConv转换
            wxCSConv conv(wxFONTENCODING_CP936);
            char* buffer = new char[fileSize + 1];
            file.Read(buffer, fileSize);
            buffer[fileSize] = '\0';
            content = wxString(buffer, conv);
            delete[] buffer;
        } else {
            // 其他编码，尝试系统默认编码
            wxMemoryBuffer buffer(fileSize);
            file.Read(buffer.GetData(), fileSize);
            buffer.SetDataLen(fileSize);
            content = wxString((const char*)buffer.GetData(), wxConvLocal, buffer.GetDataLen());
        }

        file.Close();

        if (content.empty() && fileSize > 0) {
            wxMessageBox(wxT("文件内容读取失败，请点击设置编码按钮，尝试其它编码打开文件。"), wxT("错误"), wxOK | wxICON_ERROR);
            return false;
        }

        m_textCtrl->SetText(content);
        m_originalContent = content;
        return true;
    }

    void OnOpenFile(wxCommandEvent& event) {
        wxFileDialog openFileDialog(this, wxT("打开文件"), wxT(""), wxT(""), 
                                  wxT("文本文件 (*.txt)|*.txt|网页文件 (*.js;*.css;*.htm;*.html)|*.js;*.css;*.htm;*.html|C++文件 (*.h;*.c;*.cpp)|*.h;*.c;*.cpp|Java文件 (*.java)|*.java|所有文件 (*.*)|*.*"), 
                                  wxFD_OPEN|wxFD_FILE_MUST_EXIST);
        
        if (openFileDialog.ShowModal() == wxID_CANCEL) {
            return;
        }
        
        m_filePath = openFileDialog.GetPath();
        if(autoDetectionFileEncoding) {
            m_fileEncoding = DetectFileEncoding(m_filePath);
        }
        
        if (LoadFileContent(m_filePath, m_fileEncoding)) {
            UpdateStatusBar();
            autoDetectionFileEncoding = true;
        }
    }

    void OnSetEncoding(wxCommandEvent& event) {
        wxArrayString choices;
        choices.Add(wxT("UTF-8"));
        choices.Add(wxT("GB2312"));
        choices.Add(wxT("UTF-16LE"));
        choices.Add(wxT("UTF-16BE"));
        choices.Add(wxT("自动检测"));
        
        wxSingleChoiceDialog dialog(this, wxT("请选择文件读取编码"), wxT("设置编码"), choices);
        //dialog.SetOKCancelLabels("确定", "取消");  // 修改按钮文本 //这个方法仅对部分 wxDialog 有效
        if (dialog.ShowModal() == wxID_OK) {
            int selection = dialog.GetSelection();
            if (selection == 4) { // 自动检测
                autoDetectionFileEncoding = true;
            } else {
                autoDetectionFileEncoding = false;
                m_fileEncoding = choices[selection];
            }
            
            // 设置编码后自动弹出文件选择对话框
            wxCommandEvent dummy;
            OnOpenFile(dummy);
        }
    }

    void OnJumpToLine(wxCommandEvent& event) {
        if (m_textCtrl->GetText().IsEmpty()) {
            wxMessageBox(wxT("没有可跳转的文本。"), wxT("提示"), wxOK | wxICON_INFORMATION);
            return;
        }
        
        wxTextEntryDialog dialog(this, wxT("请输入行号:"), wxT("跳转到行"), wxT(""));
        //dialog.SetOKCancelLabels("确定", "取消");  // 修改按钮文本  //这个方法仅对部分 wxDialog 有效
        if (dialog.ShowModal() == wxID_OK) {
            long lineNumber;
            if (dialog.GetValue().ToLong(&lineNumber)) {
                lineNumber--; // 转换为0-based
                long lineCount = m_textCtrl->GetLineCount();  // textCtrl->GetNumberOfLines();
                if (lineNumber >= 0 && lineNumber < lineCount) {
                    m_textCtrl->GotoLine(lineNumber);
                    m_textCtrl->SetFocus();
                } else {
                    wxMessageBox(wxString::Format(wxT("最大行号为%d，您输入的行号超出范围。"), lineCount), wxT("错误"), wxOK | wxICON_ERROR);
                }
            } else {
                wxMessageBox(wxT("请输入有效的行号。"), wxT("错误"), wxOK | wxICON_ERROR);
            }
        }
    }

    void OnHash(wxCommandEvent& event) {
        wxString currentContent = m_textCtrl->GetText();
        if (currentContent.IsEmpty()) {
            wxMessageBox(wxT("文本内容为空。"), wxT("提示"), wxOK | wxICON_INFORMATION);
        } else {
            //std::string txt = currentContent.ToStdString(wxConvUTF8);
            std::string txt = currentContent.utf8_string();
            std::string hash = sha512(txt);
            wxMessageBox(wxT("文本内容的任意改变，将生成不同的哈希值。当前文本的SHA-512哈希值是：\n") + hash, wxT("提示"), wxOK | wxICON_INFORMATION);
        }
    }

    void OnConfirm(wxCommandEvent& event) {
        wxString currentContent = m_textCtrl->GetText();
        if (currentContent == m_originalContent) {
            wxMessageBox(wxT("文本内容未修改，请修改后再确定。"), wxT("提示"), wxOK | wxICON_INFORMATION);
        } else {
            //wxMessageBox(wxT("当前文本内容:\n\n") + currentContent, wxT("文本内容"), wxOK | wxICON_INFORMATION);
            m_encodingText = m_textCtrl->GetText();
            EndModal(wxID_OK);
        }
    }

    void UpdateStatusBar() {
        SetStatusText(wxT("   ") + m_filePath, 0);  //m_statusBar->SetStatusText("")
        SetStatusText(GetFileSizeString(m_filePath), 1);
        SetStatusText(GetFileEncoding(), 2);
    }

    wxString GetFileSizeString(const wxString& filename) {
        if (!wxFileName::FileExists(filename))
            return wxT("未知");
        
        wxULongLong size = wxFileName::GetSize(filename);
        if (size < 1024)
            return wxString::Format(wxT("%llu B"), size.GetValue());
        else if (size < 1024 * 1024)
            return wxString::Format(wxT("%.1f KB"), size.ToDouble() / 1024);
        else
            return wxString::Format(wxT("%.1f MB"), size.ToDouble() / (1024 * 1024));
    }

    wxString GetFileEncoding() {
        wxString fileEncoding = wxT("手工设置");
        if(autoDetectionFileEncoding){
            fileEncoding = wxT("自动检测");
        }
        fileEncoding = fileEncoding + wxT("编码：") + m_fileEncoding;
        return fileEncoding;
    }

    void OnCodeAbout(wxCommandEvent& event) {
        wxMessageBox(wxT("初始编码编辑器\n版本 1.0\n\n"
                    "1、太短的初始编码很容易被暴力破解。\n"
                    "2、直接使用文本文件也存在枚举风险。\n"
                    "3、使用较大的文本文件，“随机”修改文件某处的内容，会显著提高破解难度。\n"
                    "4、使用修改后的文本内容作为初始编码，请铭记修改内容。\n"),
                 wxT("关于"), 
                 wxOK|wxICON_INFORMATION, 
                 this);
    }
    
    wxStyledTextCtrl* m_textCtrl;     // 显示带行号的文本内容
    wxString m_encodingText;          // 确认后的文本内容，初始编码
    wxStaticText* m_statusFields[3];  // 3个状态栏标签
    wxString m_filePath;              // 打开的文件路径
    wxString m_fileEncoding;          // 文件编码
    wxString m_originalContent;       // 文件打开时的内容
    bool autoDetectionFileEncoding;   // 是否自动检测文件编码
};

class CycleCountDialog : public wxDialog {
public:
    CycleCountDialog(wxWindow* parent, int cycleCountDefault, bool includeFileNameDefault) 
        : wxDialog(parent, wxID_ANY, wxT("设置循环次数"), wxDefaultPosition, wxSize(300, 160)) {
        
        wxBoxSizer* sizer = new wxBoxSizer(wxVERTICAL);
        
        // 循环次数输入
        wxStaticText* label = new wxStaticText(this, wxID_ANY, wxT("加密循环次数 (1-200):"));
        m_textCtrl = new wxTextCtrl(this, wxID_ANY, std::to_string(cycleCountDefault), wxDefaultPosition, wxDefaultSize, 0, wxTextValidator(wxFILTER_NUMERIC));
        
        // 文件名参与编码复选框
        m_checkFileName = new wxCheckBox(this, wxID_ANY, wxT("文件名参与编码"));
        m_checkFileName->SetValue(includeFileNameDefault);
        
        wxButton* okBtn = new wxButton(this, wxID_OK, wxT("确定"));
        wxButton* cancelBtn = new wxButton(this, wxID_CANCEL, wxT("取消"));
        
        wxBoxSizer* btnSizer = new wxBoxSizer(wxHORIZONTAL);
        btnSizer->Add(okBtn, 0, wxALL, 5);
        btnSizer->Add(cancelBtn, 0, wxALL, 5);
        
        sizer->Add(label, 0, wxALL, 5);
        sizer->Add(m_textCtrl, 0, wxEXPAND | wxLEFT | wxRIGHT, 10);
        sizer->Add(m_checkFileName, 0, wxALL, 5); 
        sizer->Add(btnSizer, 0, wxALIGN_CENTER | wxTOP, 5);
        SetSizer(sizer);
    }
    
    int GetCycleCount() const {
        long value;
        m_textCtrl->GetValue().ToLong(&value);
        return std::max(1, std::min(200, (int)value));
    }
    
    // 获取复选框状态
    bool IsFileNameIncluded() const {
        return m_checkFileName->GetValue();
    }

private:
    wxTextCtrl* m_textCtrl;      // 循环次数输入框
    wxCheckBox* m_checkFileName; // 文件名参与编码复选框
};

class DonateDialog : public wxDialog {
public:
    DonateDialog(wxWindow* parent)
        : wxDialog(parent, wxID_ANY, wxT("捐赠【FinderDataRoom】"), wxDefaultPosition, wxSize(500, 280)) {

        // Create main sizer
        wxBoxSizer* mainSizer = new wxBoxSizer(wxVERTICAL);
        
        // Create horizontal sizer for images with 50px spacing
        wxBoxSizer* imageSizer = new wxBoxSizer(wxHORIZONTAL);
        
        // Left image panel
        wxPanel* leftPanel = new wxPanel(this);
        wxBoxSizer* leftSizer = new wxBoxSizer(wxVERTICAL);
        wxStaticBitmap* m_leftBitmap = new wxStaticBitmap(leftPanel, wxID_ANY, wxNullBitmap);
        leftSizer->Add(m_leftBitmap, 1, wxEXPAND | wxALL, 5);
        leftPanel->SetSizer(leftSizer);
        
        // Right image panel
        wxPanel* rightPanel = new wxPanel(this);
        wxBoxSizer* rightSizer = new wxBoxSizer(wxVERTICAL);
        wxStaticBitmap* m_rightBitmap = new wxStaticBitmap(rightPanel, wxID_ANY, wxNullBitmap);
        rightSizer->Add(m_rightBitmap, 1, wxEXPAND | wxALL, 5);
        rightPanel->SetSizer(rightSizer);
        
        // Add images to sizer with 50px spacing
        imageSizer->Add(leftPanel, 1, wxEXPAND);
        imageSizer->AddSpacer(50); // 50 pixels spacing between images
        imageSizer->Add(rightPanel, 1, wxEXPAND);
        
        // Add image sizer to main sizer
        mainSizer->Add(imageSizer, 1, wxEXPAND | wxALL, 10);
        
        // Add OK button
        wxButton* okBtn = new wxButton(this, wxID_OK, wxT("确定"));
        wxButton* cancelBtn = new wxButton(this, wxID_CANCEL, wxT("取消"));
        wxBoxSizer* btnSizer = new wxBoxSizer(wxHORIZONTAL);
        btnSizer->Add(okBtn, 0, wxALL, 5);
        btnSizer->Add(cancelBtn, 0, wxALL, 5);
        
        mainSizer->Add(btnSizer, 0, wxALIGN_CENTER);
        SetSizer(mainSizer);  //SetSizerAndFit(mainSizer);  // SetSizerAndFit()会根据内容自动调整窗口大小，覆盖了设置的初始尺寸
        
        const wxString& base64ImageAlipay = "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";
        const wxString& base64ImageWechat = "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";
        
        // Load and display images
        LoadAndDisplayImage(base64ImageAlipay, m_leftBitmap);
        LoadAndDisplayImage(base64ImageWechat, m_rightBitmap);
        
        // Center on parent
        CentreOnParent();
    }

private:
    void LoadAndDisplayImage(const wxString& base64Image, wxStaticBitmap* bitmapCtrl) {
        if (base64Image.IsEmpty()) {
            bitmapCtrl->SetBitmap(wxNullBitmap);
            return;
        }
        
        wxMemoryBuffer buffer = wxBase64Decode(base64Image);
        if (buffer.GetDataLen() == 0) {
            bitmapCtrl->SetBitmap(wxNullBitmap);
            return;
        }
        
        wxMemoryInputStream stream(buffer.GetData(), buffer.GetDataLen());
        wxImage image;
        
        if (image.LoadFile(stream)) {
            wxBitmap bmp(image);
            if (bmp.IsOk()) {
                bitmapCtrl->SetBitmap(bmp);
            } else {
                bitmapCtrl->SetBitmap(wxNullBitmap);
            }
        } else {
            bitmapCtrl->SetBitmap(wxNullBitmap);
        }
    }
};

class MainFrame : public wxFrame {
public:
    MainFrame() : wxFrame(nullptr, wxID_ANY, wxT("文件转码工具【XO】"), wxDefaultPosition, wxSize(1000, 800)) {
        // 设置中文编码
        //SET_LOCALE();
        SetIconFromBase64();
        
        // 文件菜单
        wxMenu* fileMenu = new wxMenu();
        fileMenu->Append(wxID_NEW, wxT("打包\tCtrl+P"));
        fileMenu->Append(wxID_OPEN, wxT("解压\tCtrl+U"));
        fileMenu->AppendSeparator();
        fileMenu->Append(wxID_EXIT, wxT("退出\tCtrl+Q"));
        
        // 配置菜单
        wxMenu* configMenu = new wxMenu();
        configMenu->Append(wxID_PREFERENCES, wxT("初始编码\tCtrl+E"));
        configMenu->Append(wxID_SETUP, wxT("循环次数\tCtrl+T"));
        
        // 服务菜单
        wxMenu* serverMenu = new wxMenu();
        serverMenu->Append(wxID_HELP_INDEX, wxT("部署\tCtrl+D"));
        //serverMenu->Append(wxID_HELP_COMMANDS, wxT("配置\tCtrl+C"));
        serverMenu->Append(wxID_HELP_SEARCH, wxT("启动\tCtrl+S"));
        //serverMenu->Append(wxID_HELP_PROCEDURES, wxT("停止\tCtrl+H"));

        // 帮助菜单
        wxMenu* helpMenu = new wxMenu();
        helpMenu->Append(wxID_ABOUT, wxT("关于\tF1"));
        helpMenu->Append(wxID_UP, wxT("捐赠\tF2"));
        
        // 创建菜单栏
        wxMenuBar* menuBar = new wxMenuBar();
        menuBar->Append(fileMenu, wxT("文件"));
        menuBar->Append(configMenu, wxT("配置"));
        menuBar->Append(serverMenu, wxT("服务"));
        menuBar->Append(helpMenu, wxT("帮助"));
        SetMenuBar(menuBar);
        
        // 创建工具栏
        wxToolBar* toolBar = CreateToolBar();
        int packId = wxNewId();
        int unPackId = wxNewId();
        int transCodeId = wxNewId();
        int transCfgId = wxNewId();
        int aboutId = wxNewId();
        
        /*
        toolBar->AddTool(packId, wxT("打包"), wxArtProvider::GetBitmap(wxART_FLOPPY, wxART_TOOLBAR), wxT("打包 (Ctrl+P)"));  //wxART_HELP_BOOK wxART_FILE_OPEN
        toolBar->AddTool(unPackId, wxT("解压"), wxArtProvider::GetBitmap(wxART_FOLDER_OPEN, wxART_TOOLBAR), wxT("解压 (Ctrl+U)"));  //wxART_FILE_SAVE wxART_FILE_SAVE_AS
        toolBar->AddTool(transCodeId, wxT("编码"), wxArtProvider::GetBitmap(wxART_LIST_VIEW, wxART_TOOLBAR), wxT("编码 (Ctrl+E)"));  //wxART_GO_TO_PARENT wxART_FIND
        toolBar->AddTool(transCfgId, wxT("次数"), wxArtProvider::GetBitmap(wxART_PLUS, wxART_TOOLBAR), wxT("次数 (Ctrl+T)"));  //wxART_TICK_MARK wxART_GO_HOME
        toolBar->AddTool(aboutId, wxT("关于"), wxArtProvider::GetBitmap(wxART_HELP, wxART_TOOLBAR), wxT("关于 (F1)"));
        //*/
        toolBar->AddTool(packId, wxT("打包"), CreateWhiteBackgroundToolBitmap(wxART_FLOPPY, wxSize(24, 24)), wxT("打包 (Ctrl+P)"));  //wxART_HELP_BOOK wxART_FILE_OPEN
        toolBar->AddTool(unPackId, wxT("解压"), CreateWhiteBackgroundToolBitmap(wxART_FOLDER_OPEN, wxSize(24, 24)), wxT("解压 (Ctrl+U)"));  //wxART_FILE_SAVE wxART_FILE_SAVE_AS
        toolBar->AddTool(transCodeId, wxT("初始编码"), CreateWhiteBackgroundToolBitmap(wxART_LIST_VIEW, wxSize(24, 24)), wxT("编码 (Ctrl+E)"));  //wxART_GO_TO_PARENT wxART_FIND
        toolBar->AddTool(transCfgId, wxT("循环次数"), CreateWhiteBackgroundToolBitmap(wxART_PLUS, wxSize(24, 24)), wxT("次数 (Ctrl+T)"));  //wxART_TICK_MARK wxART_GO_HOME
        toolBar->AddTool(aboutId, wxT("关于"), CreateWhiteBackgroundToolBitmap(wxART_HELP, wxSize(24, 24)), wxT("关于 (F1)"));
        
        //toolBar->SetBackgroundColour(*wxWHITE);
        toolBar->SetWindowStyleFlag(toolBar->GetWindowStyle() & ~wxTB_DEFAULT_STYLE | wxTB_FLAT);  //某些系统主题可能会覆盖工具栏的背景色设置，需要禁用主题
        toolBar->Realize();
    
        // 处理进度日志信息框
        wxPanel* panel = new wxPanel(this);
        m_log = new wxTextCtrl(panel, wxID_ANY, "", wxDefaultPosition, wxDefaultSize, wxTE_MULTILINE | wxTE_READONLY | wxHSCROLL);
        
        // 主界面
        wxBoxSizer* sizer = new wxBoxSizer(wxVERTICAL);
        sizer->Add(m_log, 1, wxEXPAND | wxALL, 5);
        panel->SetSizer(sizer);
        
        // 初始化变量
        m_cycleCount = 20;
        m_includeFileName = false; 
        
        // 绑定事件
        Bind(wxEVT_MENU, &MainFrame::OnPack, this, wxID_NEW);
        Bind(wxEVT_MENU, &MainFrame::OnUnpack, this, wxID_OPEN);
        Bind(wxEVT_MENU, &MainFrame::OnEncoding, this, wxID_PREFERENCES);
        Bind(wxEVT_MENU, &MainFrame::OnCycleCount, this, wxID_SETUP);
        Bind(wxEVT_MENU, &MainFrame::OnSvrDeploy, this, wxID_HELP_INDEX);
        Bind(wxEVT_MENU, &MainFrame::OnSvrConfig, this, wxID_HELP_COMMANDS);
        Bind(wxEVT_MENU, &MainFrame::OnSvrStart, this, wxID_HELP_SEARCH);
        Bind(wxEVT_MENU, &MainFrame::OnSvrStop, this, wxID_HELP_PROCEDURES);
        Bind(wxEVT_MENU, &MainFrame::OnAbout, this, wxID_ABOUT);
        Bind(wxEVT_MENU, &MainFrame::OnDonate, this, wxID_UP);
        Bind(wxEVT_MENU, &MainFrame::OnExit, this, wxID_EXIT);
        
        // 工具栏按钮事件
        Bind(wxEVT_TOOL, &MainFrame::OnPack, this, packId);
        Bind(wxEVT_TOOL, &MainFrame::OnUnpack, this, unPackId);
        Bind(wxEVT_TOOL, &MainFrame::OnEncoding, this, transCodeId);
        Bind(wxEVT_TOOL, &MainFrame::OnCycleCount, this, transCfgId);
        Bind(wxEVT_TOOL, &MainFrame::OnAbout, this, aboutId);
        
        CentreOnParent();
    }

private:
    void LogMessage(const wxString& msg) {
        wxDateTime now = wxDateTime::UNow();  // 获取当前系统时间
        wxString timestamp = now.Format("%Y-%m-%d %H:%M:%S.") + wxString::Format("%03d", now.GetMillisecond());  // 格式化时间字符串：YYYY-MM-DD HH:MM:SS.LLL
        m_log->AppendText(wxString::Format("[%s] %s\n", timestamp, msg));  // 拼接时间戳和消息
    }
    
    void OnEncoding(wxCommandEvent& event) {
        EncodingDialog dlg(this, m_encodingText);
        if (dlg.ShowModal() == wxID_OK) {
            m_encodingText = dlg.GetEncodingText();
            LogMessage(wxT("初始编码已设置，原始长度: ") + wxString::Format(wxT("%zu 字符"), m_encodingText.length()));
        }
    }
    
    void OnCycleCount(wxCommandEvent& event) {
        CycleCountDialog dlg(this, m_cycleCount, m_includeFileName);
        if (dlg.ShowModal() == wxID_OK) {
            m_cycleCount = dlg.GetCycleCount();
            m_includeFileName = dlg.IsFileNameIncluded(); // 保存复选框状态
            LogMessage(wxString::Format(wxT("循环次数: %d K, 文件名参与编码: %s"),
                      m_cycleCount,
                      m_includeFileName ? wxT("是") : wxT("否")));
        }
    }
    
    void OnSvrDeploy(wxCommandEvent& event) {
        bool isConfirmed = showConfirmDialog(this, wxT("部署时将删除之前部署的文件，确认部署？"));
        if (isConfirmed) {
            const std::wstring nginxProcessName = L"nginx.exe";
            if (!killProcessByName(nginxProcessName)) {
                LogMessage(wxT("没有找到运行的Nginx进程。"));
            }

            // 删除之前的部署目录
            std::wstring path = getExeDirW();
            path = path + L"\\svr";
            deleteDirectory(path); 
            LogMessage(wxString::Format(wxT("已删除之前的部署目录: %s"), path));

            // 创建新的部署目录
            wxMessageBox(wxT("服务文件已清理，点击确定按钮后重新部署。"), wxT("提示"), wxOK | wxICON_INFORMATION); 
            try {
                wxFFileInputStream in(path + L".dll");
                wxZipInputStream zip(in);
                long idx = 1;
                std::unique_ptr<wxZipEntry> entry(zip.GetNextEntry());
                while (entry != nullptr) {
                    wxString fullPath = path + "/" + entry->GetName();
                    wxFileName::Mkdir(wxFileName(fullPath).GetPath(), 0777, wxPATH_MKDIR_FULL);
                    
                    wxString relPath = entry->GetName();
                    relPath.Replace("\\", "/");
                    LogMessage(wxString::Format(wxT("部署文件 %d：%s"), idx++, relPath));  //entry->GetName()

                    wxFFileOutputStream out(fullPath);
                    out.Write(zip);
                    out.Close();
                    
                    entry.reset(zip.GetNextEntry());
                }
                
                LogMessage(wxT("部署完成: ") + path);
                LogMessage(wxT("默认服务根目录：") +  path + wxT("..\\..\\..\\..\\html/"));
                LogMessage(wxT("默认服务配置文件：") + path + wxT("\\conf\\nginx.conf"));
                LogMessage(wxT("请点击“启动”菜单启动服务。"));

            } catch (...) {
                LogMessage(wxT("部署失败!"));
            }
        }
    }

    void OnSvrConfig(wxCommandEvent& event) {

    }

    void OnSvrStart(wxCommandEvent& event) {
        // 1. 结束可能存在的nginx进程
        const std::wstring nginxProcessName = L"nginx.exe";
        if (!killProcessByName(nginxProcessName)) {
            LogMessage(wxT("没有找到运行的Nginx进程。"));
        }

        // 2. 启动nginx
        std::wstring path = getExeDirW();
        const std::wstring nginxPath = path + L"\\svr\\nginx.exe"; 
        const std::wstring workingDir = path + L"\\svr";  // 关键：设置工作目录
        LogMessage(wxT("启动服务: ") + wxString(nginxPath));
        if (startNginx(nginxPath, workingDir)) {
            LogMessage(wxT("启动完成，5秒后自动打开浏览器。"));
        } else {
            LogMessage(wxT("启动失败，请确认已点击部署菜单部署成功。"));
        }

        // 3. 等待5秒钟以确保Nginx启动完成
        Sleep(5000);  // 5000 毫秒 = 5 秒

        // 4. 打开浏览器访问本地服务
        bool isConfirmed = showConfirmDialog(this, wxT("打开浏览器访问本地服务？"));
        if (isConfirmed) {
            std::wstring url = L"http://localhost/";
            ShellExecuteW(
                NULL,               // 无父窗口
                L"open",           // 操作类型（"open" 表示用默认程序打开）
                url.c_str(),       // URL
                NULL,              // 无参数
                NULL,              // 使用当前工作目录
                SW_SHOWNORMAL      // 正常显示窗口
            );
        }
    }

    void OnSvrStop(wxCommandEvent& event) {

    }
    
    void OnDonate(wxCommandEvent& event) {
        DonateDialog dlg(this);
        if (dlg.ShowModal() == wxID_OK) {
            LogMessage(wxString::Format(wxT("风正时济，自当破浪扬帆；任重道远，还需策马扬鞭。")));
        }
    }
    
    // 获取sha512编码
    std::vector<unsigned char> GetSha512(const std::string& input) {
        std::vector<unsigned char> hash(EVP_MD_size(EVP_sha512()));
        unsigned int len = 0;
        
        EVP_MD_CTX* ctx = EVP_MD_CTX_new();
        if (ctx) {
            if (EVP_DigestInit_ex(ctx, EVP_sha512(), nullptr) &&
                EVP_DigestUpdate(ctx, input.c_str(), input.length()) &&
                EVP_DigestFinal_ex(ctx, hash.data(), &len)) {
                hash.resize(len); // 确保向量大小与实际哈希长度一致
            }
            EVP_MD_CTX_free(ctx);
        }
        return hash;
    }

    // 获取路径的sha512编码作为编码前缀
    std::vector<unsigned char> GetPathPreKey(wxString relPath) {
        wxScopedCharBuffer utf8 = m_encodingText.ToUTF8();
        NormalizeBufNewlines(utf8);
        std::string input = relPath.utf8_string() + std::string(utf8.data(), utf8.length());
        
        auto prefix = GetSha512(input);
        if (!prefix.empty()) {
            // 日志记录
            wxString hashStr;
            for (unsigned int j = 0; j < std::min(4u, static_cast<unsigned int>(prefix.size())); j++) {
                hashStr += wxString::Format(wxT("%02x"), static_cast<unsigned int>(prefix[j]));
            }
            LogMessage(wxString::Format(
                wxT("添加前缀哈希(输入 %zu字节) => [%s...](%zu字节)"), 
                input.length(), 
                hashStr.c_str(), 
                prefix.size()));
        }else{
            LogMessage(wxT("获取路径前缀哈希失败，返回空向量。"));
        }
        return prefix;
    }

    std::string GetPathPreKeyStr(const wxString& relPath) {
        // 1. 准备输入数据（UTF-8编码 + 统一换行符）
        wxScopedCharBuffer utf8 = m_encodingText.ToUTF8();
        NormalizeBufNewlines(utf8);
        const std::string input = relPath.utf8_string() + std::string(utf8.data(), utf8.length());

        // 2. 生成SHA512二进制哈希
        const auto binHash = GetSha512(input);
        if (binHash.empty()) {
            LogMessage(wxT("错误：生成路径前缀哈希失败（返回空字符串）"));
            return "";
        }

        // 3. 二进制转十六进制字符串
        std::string hexHash;
        hexHash.reserve(binHash.size() * 2); // 预分配空间
        static const char hexDigits[] = "0123456789abcdef";  //查表效率最高
        
        for (unsigned char byte : binHash) {
            hexHash.push_back(hexDigits[byte >> 4]);   // 高4位
            hexHash.push_back(hexDigits[byte & 0x0F]); // 低4位
        }

        // 4. 日志记录（显示前8个十六进制字符）
        LogMessage(wxString::Format(
            wxT("路径前缀哈希生成成功（输入：%zu字节）=> [%s...]（%zu字符）"),
            input.length(),
            wxString(hexHash.substr(0, 8)), // 取前4字节
            hexHash.length()
        ));

        return hexHash;
    }

    // 获取转换编码（慢）
    std::vector<unsigned char> GetXorKeyMan(wxString relPath) {
        std::vector<unsigned char> key;
        //if (m_encodingText.empty()) return key;

        int m_cycleTimes = m_cycleCount * 1000;
        const int hashSize = EVP_MD_size(EVP_sha512());
        key.resize(hashSize * m_cycleTimes);
        
        EVP_MD_CTX* ctx = EVP_MD_CTX_new();
        if (!ctx) {
            LogMessage(wxT("错误：无法创建EVP上下文"));
            return key;
        }

        // 获取编码文本字节数（转换为UTF8）
        //wxScopedCharBuffer utf8 = m_encodingText.ToUTF8();
        //std::string current(utf8.data(), utf8.length());
        std::string encodingTxtStd = m_encodingText.ToStdString(wxConvUTF8);
        normalizeNewlines(encodingTxtStd);     //强制统一换行符
        
        // 输出标 encodingTxtStd 信息
        LogMessage(wxString::Format(wxT("初始编码：%s , 有效字节数：%zu字节"), 
                  encodingTxtStd.substr(0, std::min(8, (int)encodingTxtStd.length())) + (encodingTxtStd.length() > 8 ? wxT("...") : wxT("")),
                  encodingTxtStd.length()));
        
        unsigned char* keyPos = key.data();
        for (int i = 0; i < m_cycleTimes; i++) {
            if (!EVP_DigestInit_ex(ctx, EVP_sha512(), nullptr)) {
                LogMessage(wxString::Format(wxT("第%d次循环初始化失败"), i+1));
                break;
            }

            wxString encoding = relPath + m_encodingText +  wxString::Format("%d", i);  // wxString(std::to_string(i))
            std::string encodingStd = encoding.ToStdString(wxConvUTF8);
            normalizeNewlines(encodingStd);     //强制统一换行符
            if (!EVP_DigestUpdate(ctx, encodingStd.c_str(), encodingStd.size())) {
                LogMessage(wxString::Format(wxT("第%d次循环更新失败"), i+1));
                break;
            }

            unsigned int singleLen = 0;
            if (!EVP_DigestFinal_ex(ctx, keyPos, &singleLen)) {
                LogMessage(wxString::Format(wxT("第%d次循环结束失败"), i+1));
                break;
            }
            
            // 每10000次输出日志
            if(i==0 || i == (m_cycleTimes-1) || (i+1) % 10000 ==0){
                wxString hashStr;
                for (unsigned int j = 0; j < std::min(8u, singleLen); j++) {
                    hashStr += wxString::Format(wxT("%02x"), static_cast<unsigned int>(keyPos[j]));
                }
                LogMessage(wxString::Format(
                    wxT("第%d次循环(输入 %zu字节) => 哈希[%s...](%u字节)"), 
                    i+1, 
                    encodingStd.length(), 
                    hashStr.c_str(), 
                    singleLen));
            }
            
            keyPos += singleLen;
            //current.assign(reinterpret_cast<char*>(keyPos - singleLen), singleLen);
        }

        EVP_MD_CTX_free(ctx);
        
        LogMessage(wxString::Format(wxT("密钥长度: %zu字节"), key.size()));
        return key;
    }
    
    // 获取转换编码，为避免重复创建EVP上下文未使用GetSha512方法（高效）
    std::vector<unsigned char> GetXorKey(wxString relPath) {
        std::vector<unsigned char> key;
        
        int m_cycleTimes = m_cycleCount * 1000;
        const int hashSize = EVP_MD_size(EVP_sha512());
        key.resize(hashSize * m_cycleTimes);
        
        EVP_MD_CTX* ctx = EVP_MD_CTX_new();
        if (!ctx) {
            LogMessage(wxT("错误：无法创建EVP上下文"));
            return key;
        }

        // 安全获取编码文本（转换为UTF8）
        wxScopedCharBuffer utf8 = m_encodingText.ToUTF8();
        NormalizeBufNewlines(utf8);     //强制统一换行符
        std::string current(utf8.data(), utf8.length());
        
        // 修正日志输出 - 使用明确的类型转换
        LogMessage(wxString::Format(wxT("初始编码：%s , 有效字节数：%zu字节"), 
                  current.substr(0, std::min(8, (int)current.length())) + (current.length() > 8 ? wxT("...") : wxT("")),
                  current.length() ));

        unsigned char* keyPos = key.data();
        for (int i = 0; i < m_cycleTimes; i++) {
            if (!EVP_DigestInit_ex(ctx, EVP_sha512(), nullptr)) {
                LogMessage(wxString::Format(wxT("第%d次循环初始化失败"), i+1));
                break;
            }

            std::string temp = relPath.utf8_string() + current + std::to_string(i);  // ToStdString() 使用当前locale的编码
            if (!EVP_DigestUpdate(ctx, temp.c_str(), temp.length())) {
                LogMessage(wxString::Format(wxT("第%d次循环更新失败"), i+1));
                break;
            }

            unsigned int singleLen = 0;
            if (!EVP_DigestFinal_ex(ctx, keyPos, &singleLen)) {
                LogMessage(wxString::Format(wxT("第%d次循环结束失败"), i+1));
                break;
            }

            // 安全格式化输出
            if(i==0 || i == (m_cycleTimes-1) || (i+1) % 10000 ==0){
                wxString hashStr;
                for (unsigned int j = 0; j < std::min(4u, singleLen); j++) {
                    hashStr += wxString::Format(wxT("%02x"), static_cast<unsigned int>(keyPos[j]));
                }
                LogMessage(wxString::Format(
                    wxT("第%d次循环(输入 %zu字节) => 哈希[%s...](%u字节)"), 
                    i+1, 
                    temp.length(), 
                    hashStr.c_str(), 
                    singleLen));
            }
            
            keyPos += singleLen;
            //current.assign(reinterpret_cast<char*>(keyPos - singleLen), singleLen);
        }

        EVP_MD_CTX_free(ctx);
        return key;
    }

    std::string GetXorKeyStr(wxString relPath) {
        std::string key;
        
        int m_cycleTimes = m_cycleCount * 1000;
        const int hashSize = EVP_MD_size(EVP_sha512());
        key.reserve(hashSize * m_cycleTimes * 2);  // 预分配空间提高效率
        
        EVP_MD_CTX* ctx = EVP_MD_CTX_new();
        if (!ctx) {
            LogMessage(wxT("错误：无法创建EVP上下文"));
            return key;
        }

        // 获取编码文本（UTF8）
        wxScopedCharBuffer utf8 = m_encodingText.ToUTF8();
        NormalizeBufNewlines(utf8);
        std::string current(utf8.data(), utf8.length());
        
        LogMessage(wxString::Format(wxT("初始编码：%s , 有效字节数：%zu字节"), 
                current.substr(0, std::min(8, (int)current.length())) + 
                (current.length() > 8 ? wxT("...") : wxT("")),
                current.length()));

        

        for (int i = 0; i < m_cycleTimes; i++) {
            if (!EVP_DigestInit_ex(ctx, EVP_sha512(), nullptr)) {
                LogMessage(wxString::Format(wxT("第%d次循环初始化失败"), i+1));
                break;
            }

            std::string temp = relPath.utf8_string() + current + std::to_string(i);
            if (!EVP_DigestUpdate(ctx, temp.c_str(), temp.length())) {
                LogMessage(wxString::Format(wxT("第%d次循环更新失败"), i+1));
                break;
            }

            unsigned char hash[EVP_MAX_MD_SIZE];
            unsigned int hashLen = 0;
            if (!EVP_DigestFinal_ex(ctx, hash, &hashLen)) {
                LogMessage(wxString::Format(wxT("第%d次循环结束失败"), i+1));
                break;
            }

            // 将哈希值转为十六进制字符串并追加到key
            std::string hexHash;
            hexHash.reserve(hashLen * 2);
            for (unsigned int j = 0; j < hashLen; j++) {
                char buf[3];
                snprintf(buf, sizeof(buf), "%02x", hash[j]);
                hexHash.append(buf, 2);
            }
            key += hexHash;

            // 日志记录
            if (i == 0 || i == (m_cycleTimes - 1) || (i + 1) % 10000 == 0) {
                LogMessage(wxString::Format(
                    wxT("第%d次循环(输入 %zu字符) => 哈希[%s...](%zu字符)"), 
                    i + 1, 
                    temp.length(),
                    wxString(hexHash.substr(0, 8)).c_str(),  // 显示前4个字节（8个十六进制字符）
                    hexHash.length()));
            }

            // 重置上下文以进行下一次循环
            EVP_MD_CTX_reset(ctx);
        }

        EVP_MD_CTX_free(ctx);
        return key;
    }
    
    void ProcessData(wxInputStream& in, wxOutputStream& out, const std::vector<unsigned char>& key) {
        if (key.empty()) {
            out.Write(in);
            return;
        }
        
        wxCharBuffer buffer(8192);
        size_t keyIndex = 0;
        
        while (!in.Eof()) {
            size_t read = in.Read(buffer.data(), buffer.length()).LastRead();
            if (read > 0) {
                for (size_t i = 0; i < read; i++) {
                    buffer.data()[i] ^= key[keyIndex % key.size()];
                    keyIndex++;
                }
                out.Write(buffer.data(), read);
            }
        }
    }
    
    void ProcessDataStr(wxInputStream& in, wxOutputStream& out, const std::string& key) {
        if (key.empty()) {
            out.Write(in);  // 如果 key 为空，直接复制流
            return;
        }

        wxCharBuffer buffer(8192);  // 8KB 缓冲区
        size_t keyIndex = 0;
        const size_t keySize = key.size();

        while (!in.Eof()) {
            size_t read = in.Read(buffer.data(), buffer.length()).LastRead();
            if (read > 0) {
                for (size_t i = 0; i < read; i++) {
                    // 对每个字节进行 XOR 转码
                    buffer.data()[i] ^= key[keyIndex % keySize];
                    keyIndex++;
                }
                out.Write(buffer.data(), read);  // 写入转码后的数据
            }
        }
    }

    void OnPack(wxCommandEvent& event) {
        if (m_encodingText.empty()) {
            wxMessageBox(wxT("请先点击编码按钮设置初始编码。"), wxT("提示"), wxOK | wxICON_INFORMATION);
            return;
        }
        
        wxDirDialog dirDlg(this, wxT("选择要打包的目录"), "", wxDD_DEFAULT_STYLE | wxDD_DIR_MUST_EXIST);
        if (dirDlg.ShowModal() != wxID_OK) return;
        
        wxString dirPath = dirDlg.GetPath();
        
        wxFileDialog saveDlg(this, wxT("保存ZIP文件"), "", "output.zip",  wxT("ZIP文件 (*.zip)|*.zip"), wxFD_SAVE | wxFD_OVERWRITE_PROMPT);
        if (saveDlg.ShowModal() != wxID_OK) return;
        
        wxString zipPath = saveDlg.GetPath();
        
        LogMessage(wxT("开始打包: ") + dirPath);
        /*
        auto xorKey = std::vector<unsigned char>{};
        if(!m_includeFileName){
            xorKey = GetXorKey(wxT(""));
        }   //*/
        std::string xorKey = "";
        if(!m_includeFileName){
            xorKey = GetXorKeyStr(wxT(""));  // 获取转换编码
        }
        
        try {
            wxFFileOutputStream out(zipPath);
            wxZipOutputStream zip(out);
            
            wxArrayString files;
            wxDir::GetAllFiles(dirPath, &files);
            
            long idx = 1;
            for (const auto& file : files) {
                wxString relPath = file.substr(dirPath.length() + 1);
                relPath.Replace("\\", "/");
                
                LogMessage(wxString::Format(wxT("添加文件 %d：%s"), idx++, relPath));
                
                std::string finalXorKey = "";
                if(m_includeFileName){
                    finalXorKey = GetXorKeyStr(relPath);
                }else{
                    //auto prefix = GetPathPreKey(relPath);
                    std::string prefix = GetPathPreKeyStr(relPath);
                    if (!prefix.empty()) {
                        //xorKey.insert(xorKey.begin(), prefix.begin(), prefix.end());
                        finalXorKey = prefix + xorKey;
                    }
                }
        
                zip.PutNextEntry(relPath);
                
                wxFFileInputStream in(file);
                ProcessDataStr(in, zip, finalXorKey);
            }
            
            zip.Close();
            out.Close();
            
            LogMessage(wxT("打包完成: ") + zipPath);
            wxMessageBox(wxT("打包完成"), wxT("提示"), wxOK | wxICON_INFORMATION);
        } catch (...) {
            LogMessage(wxT("打包失败!"));
            wxMessageBox(wxT("打包过程中发生错误"), wxT("错误"), wxOK | wxICON_ERROR);
        }
    }
    
    void OnUnpack(wxCommandEvent& event) {
        if (m_encodingText.empty()) {
            wxMessageBox(wxT("请先设置转码编码"), wxT("提示"), wxOK | wxICON_INFORMATION);
            return;
        }
        
        wxFileDialog openDlg(this, wxT("选择ZIP文件"), "", "", wxT("ZIP文件 (*.zip)|*.zip"), wxFD_OPEN | wxFD_FILE_MUST_EXIST);
        if (openDlg.ShowModal() != wxID_OK) return;
        
        wxString zipPath = openDlg.GetPath();
        
        wxDirDialog dirDlg(this, wxT("选择解压目录"), "", wxDD_DEFAULT_STYLE | wxDD_DIR_MUST_EXIST);
        if (dirDlg.ShowModal() != wxID_OK) return;
        
        wxString dirPath = dirDlg.GetPath();
        
        LogMessage(wxT("开始解压: ") + zipPath);
        //auto xorKey = std::vector<unsigned char>{};
        std::string xorKey = "";
        if(!m_includeFileName){
            xorKey = GetXorKeyStr(wxT(""));
        }
        
        try {
            wxFFileInputStream in(zipPath);
            wxZipInputStream zip(in);
            long idx = 1;
            std::unique_ptr<wxZipEntry> entry(zip.GetNextEntry());
            while (entry != nullptr) {
                wxString fullPath = dirPath + "/" + entry->GetName();
                wxFileName::Mkdir(wxFileName(fullPath).GetPath(), 0777, wxPATH_MKDIR_FULL);
                
                wxString relPath = entry->GetName();
                relPath.Replace("\\", "/");
                LogMessage(wxString::Format(wxT("解压文件 %d：%s"), idx++, relPath));  //entry->GetName()
                
                std::string finalXorKey = "";
                if(m_includeFileName){
                    finalXorKey = GetXorKeyStr(relPath);
                }else{
                    //auto prefix = GetPathPreKey(relPath);
                    std::string prefix = GetPathPreKeyStr(relPath);
                    if (!prefix.empty()) {
                        //xorKey.insert(xorKey.begin(), prefix.begin(), prefix.end());
                        finalXorKey = prefix + xorKey;  // 拼接前缀
                    }
                }
        
                wxFFileOutputStream out(fullPath);
                ProcessDataStr(zip, out, finalXorKey);
                out.Close();
                
                entry.reset(zip.GetNextEntry());
            }
            
            LogMessage(wxT("解压完成: ") + dirPath);
            wxMessageBox(wxT("解压完成"), wxT("提示"), wxOK | wxICON_INFORMATION);
        } catch (...) {
            LogMessage(wxT("解压失败!"));
            wxMessageBox(wxT("解压过程中发生错误"), wxT("错误"), wxOK | wxICON_ERROR);
        }
    }
    
    void OnAbout(wxCommandEvent& event) {
        wxMessageBox(wxT("文件转码工具(C)\n版本 1.0\n2025-05-31\nByXO\n\n"
                         "1、通过可配置的初始编码、循环次数，多次分别获取SHA512编码，，拼接成转码编码。\n"
                         "2、打包和解压文件时，对文件内容（每个字节）分别按转码编码进行转换，实现文件的加密和解密。\n"
                         "3、太短的初始编码很容易被暴力破解，直接使用文本文件也存在枚举风险，使用较大的文本文件，“随机”修改文件某处的内容，会显著提高破解难度。\n"
                         "4、请牢记初始编码、循环次数等转码信息，否则会导致打包后的文件无法恢复（解密）。"), 
                    wxT("关于"), wxOK | wxICON_INFORMATION);
    }
    
    void OnExit(wxCommandEvent& event) {
        Close(true);
    }
    
    void SetIconFromBase64() {
        // 解码 wxString& 格式的Base64数据 (std::string  /  wxString&)
        const wxString& base64Icon = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAABNFJREFUWEfVlmtsFFUUx/93pmVboWGrEiwFrUoLiE0bLA9FsFGw+zCl0llLFeisIHww8oiiaVCxEmiiQhsIKaK0s5KocWelCu2sQq2JQRtDg6ZiJKIhWdsQJLo8arePmWvu2tlOt7vbaawh3k8zN+ee8zv/e869l+AGD3KD48M0gMMl7qEanaT4PBvHE9o0gF0QaenS628fbZtS2fR+3Z/jBWEKwFYqvlu69Crc9iv3FFfO+LHZJ603AthLxWcoQaZfll4bK5gpAJb9sd2BNgIs2vtxeskXbRN/UBo9v+jBHIJ4ANDOgHCnJ/TjQigF1pQQgn3JyNIorBxBkM03NkrBaMBRAWyC+NEG5x9dKxZ3b2aLuSTk2V+c8bpflkqMzmyC6PLLkjeeAg6XmN/slb4bMwDLvml34HsK5IGinZ/VUuB8fM0cyiVPbPbVnzYrOQOIpUJCBWyCqOxY+3v7gtmh7eFAFBv5WS2H2KdNEE/5ZWmxWQBmV1IiWqO3IREAsQui1lQd+JlSZOvZ6wGLXKtn8ki6s9krnTALYVslZvk/lC4Y7eMC2AXx0jvPd+3LuEXdGZ297sAuuN9S5IYXxh3Abn/OYrFeDfmqfusEkBmdfaT6V667Q+PUBYmKzwgXqxBjKsAK79Ndge0cwa542UdUKHVXKb6GHezfLog9AK4B0DhCnU1eT7sRwFQNOFzibdZJ6q9HKjtDAEmPl73ueHnx09P4ZJpLiPaeInum6vNOV8V9GiXNGuEXf+Y9fD7eNo1QgGV/vDpQDYrKUbMvExdBxSd0AAv9jcOLa+iQcj9GAY+qDsz5/OiRSwnPAccKMXd6Zv+Jui0XrQAs/xw82mxyV+s540Jn2Zo5qsp/xRGUm+0Cm6tiA6GkBt1pNyvK/l7d3zAFBrM/AIpndQM+p2WEDYA+RZbCgGMZzrJ192qq2qHIUsRn5MMuiK23TlaX1G/rrOE4bhNAJ4SdE1LLZ5/cOtR6In0j37LtlY7eN3tVclmRG6aMBrHMtWFyMu1j9wBlHmMCRAK43F+veviK8tQjwQIC8iiAFEJwjFAcIjktx5lKHzyY+iWArG8uq559P/XtAOh5RfZkxwJh9mxeD8r+EwIMthOlQH9KMk4Uzb92vmBWT9rd0/qnpt1EUbx9umMQoDAsUDLJLm/tXkJB6kHQpnil+3UfxsBGBUcFMGZid7kf0igW8tByKQjr65phAASbJm+u2x8OWuquAqGvxgpsCoAdFMww1r1tdGAEoJRWpW89GHmIREscvS0Jt+B/CQDAa91S90Q8icekALsu2YLoK3NYTQx1QbgIB8dBSi2V6Vtrg/9qC8wCZKQS7J1nkUGIoBMQ4KxG8fKTp3qOGqvcCF/kqnBylByP2wWsBkJJyPfLEuvzuKNQFFNSr6NnZhqHnXkWZhtRY2dHL/Ycrh92ehatdM/jONpOCD3Z7PUsNzoecRnFezzqi4yvmuLy9VP7+wcu5qXzeGnuhLMEmGsEKFq5OoPjkroAckaRG+bFymgEgK4Ce0prQD4oLoAgCzzCqhCNCNDogCJLtbpDR9naHKpy5x6YwiPYR8MKhE9AioDik25PpOaoz/LoxTZBLGRwsZ7YtjJxPlHxLYC/FFmamCiwoXbMmP13NmNWYLxR/gZQW0I/9MHdIQAAAABJRU5ErkJggg==";
        wxMemoryBuffer buffer = wxBase64Decode(base64Icon);
        if (buffer.GetDataLen() == 0) {
            wxLogError("Failed to decode Base64 icon data");
            return;
        }
        
        // 解码 std::string 格式的Base64数据（两种方法都可以）
        //std::string base64IconStdStr = "";
        //std::vector<unsigned char> iconData = base64_decode(base64IconStdStr);
        //wxMemoryInputStream stream(iconData.data(), iconData.size());
        
        // 尝试从内存数据加载图像
        wxImage image;
        wxMemoryInputStream stream(buffer.GetData(), buffer.GetDataLen());
        if (image.LoadFile(stream, wxBITMAP_TYPE_PNG)) {
            wxIcon icon;  // wxIcon 类的 LoadFile 方法不接受 wxMemoryInputStream
            icon.CopyFromBitmap(wxBitmap(image));
            SetIcon(icon);
            return;
        } else {
            wxLogError("无法加载图标数据，请检查Base64字符串是否有效。");
            return;
        }
        
        // Windows平台使用HICON创建图标
        /*
        #ifdef __WXMSW__
        HICON hIcon = ::CreateIconFromResourceEx(
            (PBYTE)buffer.GetData(), 
            buffer.GetDataLen(), 
            TRUE, 
            0x00030000,  // 默认尺寸
            0, 0, 
            LR_DEFAULTCOLOR);
        
        if (hIcon) {
            wxIcon icon;
            // 使用推荐的SetHandle替代已弃用的SetHICON
            icon.SetHandle((WXHANDLE)hIcon);
            SetIcon(icon);
            // 注意: 不要删除hIcon，wxIcon会接管它的生命周期
            return;
        }
        #endif
    
        // 临时文件法
        wxString tempFile = wxFileName::CreateTempFileName("icon");
        if (!tempFile.empty()) {
            wxFile file(tempFile, wxFile::write);
            if (file.IsOpened()) {
                file.Write(buffer.GetData(), buffer.GetDataLen());
                file.Close();
                
                wxIcon icon;
                if (icon.LoadFile(tempFile, wxBITMAP_TYPE_ICO)) {
                    SetIcon(icon);
                    wxRemoveFile(tempFile);  // 删除临时文件
                    return;
                }
                wxRemoveFile(tempFile);  // 删除临时文件
            }
        }  
        wxLogError("Failed to create icon from Base64 data");
        //*/
    }
    
    wxTextCtrl* m_log;        // 处理进度日志信息框
    wxString m_encodingText;  // 初始编码
    int m_cycleCount;         // 循环次数
    bool m_includeFileName;   // 是否包含文件名
};

class MyApp : public wxApp {
public:
    virtual bool OnInit() {
        // 设置应用程序名称和供应商（有助于配置文件位置）
        SetAppName("FileTransCoder");
        SetVendorName("FinderDataRoom");
        
        // 初始化wxWidgets内置图像处理
        wxInitAllImageHandlers();
        
        MainFrame* frame = new MainFrame();
        frame->Show();
        return true;
    }
};

wxIMPLEMENT_APP(MyApp);


/**
请基于 wxWidgets 编写一个支持打包、解压的文件转码C++程序，实现以下功能：
1、点击编码按钮，弹出一个窗口，该窗口可以打开一个文本文件并在 wxStyledTextCtrl 中显示，可以修改，修改后确认显示的文本作为初始编码；
2、点击打包按钮，先选择需要打包的路径，选择路径后，再选择打包文件需要保存的zip文件名，然后将指定路径下的所有文件和子文件夹中的文件，打包到指定的zip文件中。打包时需要对文件每个字节按转码编码依次做异或操作。转码编码是取初始编码的sha512编码获得。
3、点击解压按钮，选选择需要解压的zip文件，再选择需要解压的路径，然后将zip文件中的所有文件解压到指定的路径。解压时，从zip文件中获取文件的每个字节按转码编码依次做异或操作。同样转码编码是取初始编码的sha512编码获得。

非常棒，功能正常，请优化：
1、界面中文都显示成了乱码，确保界面不显示乱码；
2、增加文件、配置和帮助菜单，文件菜单下主要包含打包和解压，配置菜单下主要包含编码和次数；
3、次数菜单的作用是弹出框输入循环次数，将初始编码转换成转码编码时，只取一次sha512太少，可以按循环的次数循环取值后拼接成比较长的转码编码，取sha512时，每次将循环次数拼接在初始编码后。
//*/