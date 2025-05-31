#include <wx/wx.h>
#include <wx/stc/stc.h>
#include <wx/dir.h>
#include <wx/filefn.h>
#include <wx/zipstrm.h>
#include <wx/wfstream.h>
#include <wx/sstream.h>
#include <wx/textfile.h>
#include <wx/menu.h>
#include <wx/artprov.h>
#include <openssl/evp.h>
#include <memory>

// 设置中文编码宏
/*
#ifdef __WXMSW__
    #include <wx/msw/private.h>
    #define SET_LOCALE() wxLocale::AddCatalogLookupPathPrefix("."); \
                        wxLocale::AddCatalog("zh_CN")
#else
    #define SET_LOCALE() setlocale(LC_ALL, "zh_CN.UTF-8"); \
                        wxLocale::AddCatalogLookupPathPrefix("/usr/share/locale"); \
                        wxLocale::AddCatalog("wxstd")
#endif
//*/

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

class EncodingDialog : public wxDialog {
public:
    EncodingDialog(wxWindow* parent, const wxString& title)
        : wxDialog(parent, wxID_ANY, title, wxDefaultPosition, wxSize(600, 400)) {
        
        wxBoxSizer* sizer = new wxBoxSizer(wxVERTICAL);
        
        m_stc = new wxStyledTextCtrl(this, wxID_ANY);
        m_stc->SetMarginWidth(0, 40);
        m_stc->StyleSetFont(wxSTC_STYLE_DEFAULT, wxFont(10, wxFONTFAMILY_MODERN, wxFONTSTYLE_NORMAL, wxFONTWEIGHT_NORMAL));
        
        wxBoxSizer* btnSizer = new wxBoxSizer(wxHORIZONTAL);
        wxButton* openBtn = new wxButton(this, wxID_ANY, wxT("打开文件"));
        wxButton* saveBtn = new wxButton(this, wxID_ANY, wxT("确认编码"));
        
        btnSizer->Add(openBtn, 0, wxALL, 5);
        btnSizer->Add(saveBtn, 0, wxALL, 5);
        
        sizer->Add(m_stc, 1, wxEXPAND | wxALL, 5);
        sizer->Add(btnSizer, 0, wxALIGN_CENTER | wxBOTTOM, 10);
        
        SetSizer(sizer);
        
        openBtn->Bind(wxEVT_BUTTON, &EncodingDialog::OnOpenFile, this);
        saveBtn->Bind(wxEVT_BUTTON, &EncodingDialog::OnSaveEncoding, this);
    }
    
    wxString GetEncodingText() const { return m_encodingText; }

private:
    void OnOpenFile(wxCommandEvent& event) {
        wxFileDialog dlg(this, wxT("打开文本文件"), "", "", 
                        wxT("文本文件 (*.txt)|*.txt"), wxFD_OPEN | wxFD_FILE_MUST_EXIST);
        
        if (dlg.ShowModal() == wxID_OK) {
            wxTextFile file;
            if (file.Open(dlg.GetPath())) {
                m_stc->SetText(file.GetFirstLine());
                while (!file.Eof()) {
                    m_stc->AppendText("\n" + file.GetNextLine());
                }
                file.Close();
            }
        }
    }
    
    void OnSaveEncoding(wxCommandEvent& event) {
        m_encodingText = m_stc->GetText();
        EndModal(wxID_OK);
    }
    
    wxStyledTextCtrl* m_stc;
    wxString m_encodingText;
};

class CycleCountDialog : public wxDialog {
public:
    CycleCountDialog(wxWindow* parent, int cycleCountDefault, bool includeFileNameDefault) 
        : wxDialog(parent, wxID_ANY, wxT("设置循环次数"), wxDefaultPosition, wxSize(300, 180)) {
        
        wxBoxSizer* sizer = new wxBoxSizer(wxVERTICAL);
        
        // 循环次数输入
        wxStaticText* label = new wxStaticText(this, wxID_ANY, wxT("加密循环次数 (1-200):"));
        m_textCtrl = new wxTextCtrl(this, wxID_ANY, std::to_string(cycleCountDefault), wxDefaultPosition, wxDefaultSize, 0, wxTextValidator(wxFILTER_NUMERIC));
        
        // 新增复选框
        m_checkFileName = new wxCheckBox(this, wxID_ANY, wxT("文件名参与编码"));
        m_checkFileName->SetValue(includeFileNameDefault); // 默认勾选
        
        wxButton* okBtn = new wxButton(this, wxID_OK, wxT("确定"));
        wxButton* cancelBtn = new wxButton(this, wxID_CANCEL, wxT("取消"));
        
        wxBoxSizer* btnSizer = new wxBoxSizer(wxHORIZONTAL);
        btnSizer->Add(okBtn, 0, wxALL, 5);
        btnSizer->Add(cancelBtn, 0, wxALL, 5);
        
        sizer->Add(label, 0, wxALL, 5);
        sizer->Add(m_textCtrl, 0, wxEXPAND | wxLEFT | wxRIGHT, 10);
        sizer->Add(m_checkFileName, 0, wxALL, 5); // 添加复选框
        sizer->Add(btnSizer, 0, wxALIGN_CENTER | wxTOP, 5);
        
        SetSizer(sizer);
    }
    
    int GetCycleCount() const {
        long value;
        m_textCtrl->GetValue().ToLong(&value);
        return std::max(1, std::min(200, (int)value));
    }
    
    // 新增方法：获取复选框状态
    bool IsFileNameIncluded() const {
        return m_checkFileName->GetValue();
    }

private:
    wxTextCtrl* m_textCtrl;
    wxCheckBox* m_checkFileName; // 新增复选框成员
};

class MainFrame : public wxFrame {
public:
    MainFrame() : wxFrame(nullptr, wxID_ANY, wxT("文件转码工具"), wxDefaultPosition, wxSize(800, 600)) {
        // 设置中文编码
        //SET_LOCALE();
        
        // 创建菜单栏
        wxMenuBar* menuBar = new wxMenuBar();
        
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
        
        // 帮助菜单
        wxMenu* helpMenu = new wxMenu();
        helpMenu->Append(wxID_ABOUT, wxT("关于\tF1"));
        
        menuBar->Append(fileMenu, wxT("文件"));
        menuBar->Append(configMenu, wxT("配置"));
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
    
        // 主界面
        wxPanel* panel = new wxPanel(this);
        wxBoxSizer* sizer = new wxBoxSizer(wxVERTICAL);
        
        m_log = new wxTextCtrl(panel, wxID_ANY, "", wxDefaultPosition, wxDefaultSize, 
                             wxTE_MULTILINE | wxTE_READONLY | wxHSCROLL);
        
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
        Bind(wxEVT_MENU, &MainFrame::OnAbout, this, wxID_ABOUT);
        Bind(wxEVT_MENU, &MainFrame::OnExit, this, wxID_EXIT);
        
        // 工具栏按钮事件
        Bind(wxEVT_TOOL, &MainFrame::OnPack, this, packId);
        Bind(wxEVT_TOOL, &MainFrame::OnUnpack, this, unPackId);
        Bind(wxEVT_TOOL, &MainFrame::OnEncoding, this, transCodeId);
        Bind(wxEVT_TOOL, &MainFrame::OnCycleCount, this, transCfgId);
        Bind(wxEVT_TOOL, &MainFrame::OnAbout, this, aboutId);
    }

private:
    void LogMessage(const wxString& msg) {
        //m_log->AppendText(msg + "\n");
        
        // 获取当前系统时间
        wxDateTime now = wxDateTime::UNow();
        
        // 格式化时间字符串：YYYY-MM-DD HH:MM:SS.LLL
        wxString timestamp = now.Format("%Y-%m-%d %H:%M:%S.") + 
                             wxString::Format("%03d", now.GetMillisecond());
        
        // 拼接时间戳和消息
        m_log->AppendText(wxString::Format("[%s] %s\n", timestamp, msg));
    }
    
    void OnEncoding(wxCommandEvent& event) {
        EncodingDialog dlg(this, wxT("设置转码编码"));
        if (dlg.ShowModal() == wxID_OK) {
            m_encodingText = dlg.GetEncodingText();
            LogMessage(wxT("编码已设置，长度: ") + wxString::Format("%zu", m_encodingText.length()));
        }
    }
    
    void OnCycleCount(wxCommandEvent& event) {
        CycleCountDialog dlg(this, m_cycleCount, m_includeFileName);
        if (dlg.ShowModal() == wxID_OK) {
            m_cycleCount = dlg.GetCycleCount();
            m_includeFileName = dlg.IsFileNameIncluded(); // 保存复选框状态
            LogMessage(wxString::Format(wxT("循环次数: %d, 文件名参与编码: %s"),
                      m_cycleCount,
                      m_includeFileName ? wxT("是") : wxT("否")));
        }
    }
    
    std::vector<unsigned char> GetXorKeyOk() {
        std::vector<unsigned char> key(EVP_MAX_MD_SIZE * m_cycleCount); // 预分配足够空间
        unsigned int keyLen = 0;
        
        if (!m_encodingText.empty()) {
            EVP_MD_CTX* ctx = EVP_MD_CTX_new();
            std::string current = m_encodingText.ToStdString();
            
            for (int i = 0; i < m_cycleCount; i++) {
                EVP_DigestInit_ex(ctx, EVP_sha512(), nullptr);
                std::string temp = current + std::to_string(i);
                EVP_DigestUpdate(ctx, temp.c_str(), temp.length());
                
                //EVP_DigestFinal_ex(ctx, key.data(), &keyLen);
                //current.assign(key.begin(), key.begin() + keyLen);
                
                //*
                // 临时缓冲区存储单次哈希结果
                unsigned char singleHash[EVP_MAX_MD_SIZE];
                unsigned int singleLen = 0;
                EVP_DigestFinal_ex(ctx, singleHash, &singleLen);
                
                // 将本次哈希结果拼接到总密钥
                memcpy(key.data() + i * EVP_MAX_MD_SIZE, singleHash, singleLen);
                
                // 准备下一次迭代
                //current.assign(singleHash, singleHash + singleLen);
                
                if(i==0 || i == (m_cycleCount-1)){
                    // 将哈希结果转为十六进制字符串用于显示
                    wxString hashStr;
                    for (unsigned int j = 0; j < singleLen; j++) {
                        hashStr += wxString::Format("%02x", singleHash[j]);
                    }
                    LogMessage(wxString::Format("第%d次循环哈希 (输入: '%s'):\n%s", i + 1, temp, hashStr.Left(16) + (hashStr.length() > 16 ? "..." : "")));
                }  //*/
            }
            
            EVP_MD_CTX_free(ctx);
            key.resize(keyLen * m_cycleCount); // 拼接多次哈希结果
            LogMessage(wxT("编码生成完成"));
        }
        
        return key;
    }
    
    std::vector<unsigned char> GetXorKey(wxString relPath) {
        std::vector<unsigned char> key;
        if (m_encodingText.empty()) return key;

        const int hashSize = EVP_MD_size(EVP_sha512());
        key.resize(hashSize * m_cycleCount);
        
        EVP_MD_CTX* ctx = EVP_MD_CTX_new();
        if (!ctx) {
            LogMessage(wxT("错误：无法创建EVP上下文"));
            return key;
        }

        // 安全获取编码文本（转换为UTF8）
        wxScopedCharBuffer utf8 = m_encodingText.ToUTF8();
        std::string current(utf8.data(), utf8.length());
        
        // 修正日志输出 - 使用明确的类型转换
        LogMessage(wxString::Format(wxT("初始编码(%zu字节)：%.32s%s"), 
                  current.length(), 
                  current.c_str(),
                  current.substr(0, std::min(16, (int)current.length())) + (current.length() > 16 ? wxT("...") : wxT(""))));

        unsigned char* keyPos = key.data();
        for (int i = 0; i < m_cycleCount; i++) {
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
            if(i==0 || i == (m_cycleCount-1) || (i+1) % 10000 ==0){
                wxString hashStr;
                for (unsigned int j = 0; j < std::min(8u, singleLen); j++) {
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
    
    void OnPack(wxCommandEvent& event) {
        if (m_encodingText.empty()) {
            wxMessageBox(wxT("请先设置转码编码"), wxT("提示"), wxOK | wxICON_INFORMATION);
            return;
        }
        
        wxDirDialog dirDlg(this, wxT("选择要打包的目录"), "", wxDD_DEFAULT_STYLE | wxDD_DIR_MUST_EXIST);
        if (dirDlg.ShowModal() != wxID_OK) return;
        
        wxString dirPath = dirDlg.GetPath();
        
        wxFileDialog saveDlg(this, wxT("保存ZIP文件"), "", "output.zip", 
                           wxT("ZIP文件 (*.zip)|*.zip"), wxFD_SAVE | wxFD_OVERWRITE_PROMPT);
        if (saveDlg.ShowModal() != wxID_OK) return;
        
        wxString zipPath = saveDlg.GetPath();
        
        auto xorKey = GetXorKey(wxT(""));
        LogMessage(wxT("开始打包: ") + dirPath);
        LogMessage(wxString::Format(wxT("密钥长度: %zu字节"), xorKey.size()));
        
        try {
            wxFFileOutputStream out(zipPath);
            wxZipOutputStream zip(out);
            
            wxArrayString files;
            wxDir::GetAllFiles(dirPath, &files);
            
            for (const auto& file : files) {
                wxString relPath = file.substr(dirPath.length() + 1);
                relPath.Replace("\\", "/");
                
                LogMessage(wxT("添加文件: ") + relPath);
                
                if(m_includeFileName){
                    xorKey = GetXorKey(relPath);
                    LogMessage(wxString::Format(wxT("密钥长度: %zu字节"), xorKey.size()));
                }
        
                zip.PutNextEntry(relPath);
                
                wxFFileInputStream in(file);
                ProcessData(in, zip, xorKey);
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
        
        wxFileDialog openDlg(this, wxT("选择ZIP文件"), "", "", 
                           wxT("ZIP文件 (*.zip)|*.zip"), wxFD_OPEN | wxFD_FILE_MUST_EXIST);
        if (openDlg.ShowModal() != wxID_OK) return;
        
        wxString zipPath = openDlg.GetPath();
        
        wxDirDialog dirDlg(this, wxT("选择解压目录"), "", wxDD_DEFAULT_STYLE | wxDD_DIR_MUST_EXIST);
        if (dirDlg.ShowModal() != wxID_OK) return;
        
        wxString dirPath = dirDlg.GetPath();
        
        auto xorKey = GetXorKey(wxT(""));
        LogMessage(wxT("开始解压: ") + zipPath);
        LogMessage(wxString::Format(wxT("密钥长度: %zu字节"), xorKey.size()));
        
        try {
            wxFFileInputStream in(zipPath);
            wxZipInputStream zip(in);
            
            std::unique_ptr<wxZipEntry> entry(zip.GetNextEntry());
            while (entry != nullptr) {
                wxString fullPath = dirPath + "/" + entry->GetName();
                wxFileName::Mkdir(wxFileName(fullPath).GetPath(), 0777, wxPATH_MKDIR_FULL);
                
                wxString relPath = entry->GetName();
                relPath.Replace("\\", "/");
                LogMessage(wxT("解压文件: ") + relPath);  //entry->GetName()
                
                if(m_includeFileName){
                    xorKey = GetXorKey(relPath);
                    LogMessage(wxString::Format(wxT("密钥长度: %zu字节"), xorKey.size()));
                }
        
                wxFFileOutputStream out(fullPath);
                ProcessData(zip, out, xorKey);
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
        wxMessageBox(wxT("文件转码工具\n版本 1.0\n2025-05-11\nBy东海青蛙\n\n1、通过初始编码，循环多次分别获取SHA512编码，拼接成转码编码。\n2、打包和解压文件时，对文件内容（每个字节）分别按转码编码进行转换，实现文件的加密和解密。\n3、请牢记初始编码、循环次数等信息，任何原因导致文件无法恢复（解密）或其他任何损失的，与本工具无关。"), 
                    wxT("关于"), wxOK | wxICON_INFORMATION);
    }
    
    void OnExit(wxCommandEvent& event) {
        Close(true);
    }
    
    wxTextCtrl* m_log;
    wxString m_encodingText;
    int m_cycleCount;
    bool m_includeFileName; // 是否包含文件名
};

class MyApp : public wxApp {
public:
    virtual bool OnInit() {
        // 设置应用程序名称和供应商（有助于配置文件位置）
        SetAppName("FileTransCoder");
        SetVendorName("MyCompany");
        
        // 初始化wxWidgets内置图像处理
        wxInitAllImageHandlers();
        
        MainFrame* frame = new MainFrame();
        frame->Show();
        return true;
    }
};

wxIMPLEMENT_APP(MyApp);