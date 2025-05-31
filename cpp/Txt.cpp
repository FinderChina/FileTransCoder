#include <wx/wx.h>
#include <wx/stc/stc.h>
#include <wx/file.h>
#include <wx/textdlg.h>
#include <wx/utils.h>
#include <wx/artprov.h>
#include <wx/fontmap.h>
#include <wx/filename.h>
#include <wx/stdpaths.h>
#include <codecvt>
#include <locale>
#include <string>

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

class MyApp : public wxApp {
public:
    virtual bool OnInit();
};

class MainFrame : public wxFrame {
public:
    MainFrame(const wxString& title);
    
private:
    wxStyledTextCtrl* m_textCtrl;
    wxStatusBar* m_statusBar;
    wxString m_filePath;
    wxString m_fileEncoding;
    wxString m_originalContent;
    bool autoDetectionFileEncoding;
    wxString GetFileSizeString(const wxString& filename);
    wxString GetFileEncoding();
    
    void OnOpen(wxCommandEvent& event);
    void OnSetEncoding(wxCommandEvent& event);
    void OnJumpToLine(wxCommandEvent& event);
    void OnConfirm(wxCommandEvent& event);
    void OnAbout(wxCommandEvent& event);
    void UpdateStatusBar();
    bool LoadFileContent(const wxString& path, const wxString& encoding);
};

bool MyApp::OnInit() {
    // 初始化图像处理器（使用自定义图片作为图标是需要调用）
    //wxInitAllImageHandlers();  // 或 wxImage::AddHandler(new wxPNGHandler);
    
    // 设置中文标题等
    /*
    wxLocale* locale = new wxLocale(wxLANGUAGE_CHINESE_SIMPLIFIED);
    locale->AddCatalogLookupPathPrefix("."); // 可选：指定翻译文件路径, 需要确保系统或项目中存在对应的 .mo 翻译文件（如 wxstd.mo）, 如果未找到翻译文件，按钮可能仍显示英文。
    locale->AddCatalog("wxstd");  // 加载 wxWidgets 标准翻译   //*/
    
    MainFrame* frame = new MainFrame(wxT("文本查看器"));
    frame->Show(true);
    return true;
}

MainFrame::MainFrame(const wxString& title) 
    : wxFrame(NULL, wxID_ANY, title, wxDefaultPosition, wxSize(800, 600)) {
    
    autoDetectionFileEncoding = true;
    
    // 创建菜单栏
    wxMenu* fileMenu = new wxMenu;
    fileMenu->Append(wxID_OPEN, wxT("打开(&L)\tCtrl+L"), wxT("打开文本文件"));
    //fileMenu->Append(wxID_SAVE, wxT("保存(&E)\tCtrl+E"), wxT("保存文本内容"));
    fileMenu->AppendSeparator();
    fileMenu->Append(wxID_EXIT, wxT("退出(&Q)\tCtrl+Q"), wxT("关闭本程序"));
    
    wxMenu* editMenu = new wxMenu;
    editMenu->Append(wxID_ANY, wxT("设置编码(&S)\tCtrl+S"), wxT("设置指定的文件编码后打开文本文件"));
    editMenu->Append(wxID_ANY, wxT("跳转到行(&G)\tCtrl+G"), wxT("跳转到指定行"));
    editMenu->Append(wxID_ANY, wxT("确定编码(&O)\tCtrl+O"), wxT("确定文本内容作为初始编码"));
    
    wxMenu* menuHelp = new wxMenu;
    menuHelp->Append(wxID_ABOUT, wxT("关于(&A)\tF1"), wxT("关于本程序"));
    
    wxMenuBar* menuBar = new wxMenuBar;
    menuBar->Append(fileMenu, wxT("文件(&F)"));
    menuBar->Append(editMenu, wxT("编辑(&E)"));
    menuBar->Append(menuHelp, wxT("帮助(&H)"));
    SetMenuBar(menuBar);
    
    // 创建工具栏
    wxToolBar* toolBar = CreateToolBar();
    int openId = wxNewId();
    int saveId = wxNewId();
    int encodingId = wxNewId();
    int jumpId = wxNewId();
    int confirmId = wxNewId();
    
    // 获取可执行文件的路径
    /*
    wxString exePath = wxStandardPaths::Get().GetExecutablePath();
    wxString exeDir = wxPathOnly(exePath);
    wxString imgConfirm = exeDir + wxFileName::GetPathSeparator() + "check.png";
    if (wxFileExists(imgConfirm)) {
        wxImage imageConfirm(imgConfirm, wxBITMAP_TYPE_PNG);
        imageConfirm.Rescale(16, 16, wxIMAGE_QUALITY_HIGH);
        wxBitmap bitmapConfirm(imageConfirm);
    }  //*/
    
    toolBar->AddTool(encodingId, wxT("编码"), wxArtProvider::GetBitmap(wxART_TIP, wxART_TOOLBAR), wxT("设置指定的文件编码后打开文本文件 (Ctrl+S)"));  //wxART_HELP_BOOK
    toolBar->AddTool(openId, wxT("打开"), wxArtProvider::GetBitmap(wxART_FILE_OPEN, wxART_TOOLBAR), wxT("打开文本文件 (Ctrl+L)"));
    //toolBar->AddTool(saveId, wxT("保存"), wxArtProvider::GetBitmap(wxART_FILE_SAVE, wxART_TOOLBAR));
    //toolBar->AddSeparator();
    toolBar->AddTool(jumpId, wxT("跳转"), wxArtProvider::GetBitmap(wxART_FIND, wxART_TOOLBAR), wxT("跳转到指定行 (Ctrl+G)"));  //wxART_GO_TO_PARENT
    toolBar->AddTool(confirmId, wxT("确定"), wxArtProvider::GetBitmap(wxART_GO_HOME, wxART_TOOLBAR), wxT("确定文本内容作为初始编码 (Ctrl+O)")); //wxART_TICK_MARK  
    toolBar->Realize();
    
    // 创建文本控件
    m_textCtrl = new wxStyledTextCtrl(this, wxID_ANY);
    m_textCtrl->SetMarginType(0, wxSTC_MARGIN_NUMBER);
    m_textCtrl->SetMarginWidth(0, 50);
    
    // 设置字体
    wxFont font(12, wxFONTFAMILY_MODERN, wxFONTSTYLE_NORMAL, wxFONTWEIGHT_NORMAL);
    m_textCtrl->StyleSetFont(wxSTC_STYLE_DEFAULT, font);
    
    // 设置自动换行
    m_textCtrl->SetWrapMode(wxSTC_WRAP_WORD);  // 按单词换行
    // m_textCtrl->SetWrapMode(wxSTC_WRAP_CHAR); // 按字符换行
    
    // 创建状态栏
    m_statusBar = CreateStatusBar(3);
    int widths[] = {-1, 150, 200};
    m_statusBar->SetStatusWidths(3, widths);
    m_statusBar->SetStatusText(wxT("就绪"), 0);
    
    // 绑定事件
    Bind(wxEVT_MENU, &MainFrame::OnOpen, this, wxID_OPEN);
    Bind(wxEVT_MENU, [this](wxCommandEvent&) { Close(true); }, wxID_EXIT);
    Bind(wxEVT_MENU, &MainFrame::OnAbout, this, wxID_ABOUT);
    Bind(wxEVT_MENU, &MainFrame::OnSetEncoding, this, editMenu->FindItem(wxT("设置编码(&S)")));
    Bind(wxEVT_MENU, &MainFrame::OnJumpToLine, this, editMenu->FindItem(wxT("跳转到行(&G)")));
    Bind(wxEVT_MENU, &MainFrame::OnConfirm, this, editMenu->FindItem(wxT("确定编码(&O)")));
    
    // 工具栏按钮事件
    Bind(wxEVT_TOOL, &MainFrame::OnOpen, this, openId);
    Bind(wxEVT_TOOL, &MainFrame::OnSetEncoding, this, encodingId);
    Bind(wxEVT_TOOL, &MainFrame::OnJumpToLine, this, jumpId);
    Bind(wxEVT_TOOL, &MainFrame::OnConfirm, this, confirmId);
    
    // 添加快捷键
    wxAcceleratorEntry entries[2];
    entries[0].Set(wxACCEL_CTRL, (int)'O', wxID_OPEN);
    entries[1].Set(wxACCEL_CTRL, (int)'Q', wxID_EXIT);
    wxAcceleratorTable accel(2, entries);
    SetAcceleratorTable(accel);
}

bool MainFrame::LoadFileContent(const wxString& path, const wxString& encoding) {
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
    }
    else if (encoding == wxT("GB2312")) {
        // 对于GB2312文件，使用wxCSConv转换
        wxCSConv conv(wxFONTENCODING_CP936);
        char* buffer = new char[fileSize + 1];
        file.Read(buffer, fileSize);
        buffer[fileSize] = '\0';
        content = wxString(buffer, conv);
        delete[] buffer;
    }
    else {
        // 其他编码，尝试系统默认编码
        wxMemoryBuffer buffer(fileSize);
        file.Read(buffer.GetData(), fileSize);
        buffer.SetDataLen(fileSize);
        content = wxString((const char*)buffer.GetData(), wxConvLocal, buffer.GetDataLen());
    }

    file.Close();

    if (content.empty() && fileSize > 0) {
        wxMessageBox(wxString::Format(wxT("使用编码%s打开文件失败，请点击编码按钮选择其它编码。"), encoding), wxT("错误"), wxOK | wxICON_ERROR);
        return false;
    }

    m_textCtrl->SetText(content);
    m_originalContent = content;
    return true;
}

void MainFrame::OnOpen(wxCommandEvent& event) {
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

void MainFrame::OnSetEncoding(wxCommandEvent& event) {
    /*
    if (m_filePath.empty()) {
        wxMessageBox(wxT("请先打开文件"), wxT("提示"), wxOK | wxICON_INFORMATION);
        return;
    } //*/

    wxArrayString choices;
    choices.Add(wxT("UTF-8"));
    choices.Add(wxT("GB2312"));
    choices.Add(wxT("UTF-16LE"));
    choices.Add(wxT("UTF-16BE"));
    choices.Add(wxT("自动检测"));
    
    wxSingleChoiceDialog dialog(this, wxT("请选择文件编码"), wxT("设置编码"), choices);
    //dialog.SetOKCancelLabels("确定", "取消");  // 修改按钮文本 //这个方法仅对部分 wxDialog 有效
    if (dialog.ShowModal() == wxID_OK) {
        int selection = dialog.GetSelection();
        if (selection == 4) { // 自动检测
            autoDetectionFileEncoding = true;
        } else {
            autoDetectionFileEncoding = false;
            m_fileEncoding = choices[selection];
        }
        
        // 使用新编码重新加载文件
        /*
        if (LoadFileContent(m_filePath, m_fileEncoding)) {
            UpdateStatusBar();
        }  //*/
        wxCommandEvent dummy;
        OnOpen(dummy);
    }
}

void MainFrame::OnJumpToLine(wxCommandEvent& event) {
    if (m_textCtrl->GetText().IsEmpty()) {
        wxMessageBox(wxT("没有可跳转的文本"), wxT("提示"), wxOK | wxICON_INFORMATION);
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
                wxMessageBox(wxString::Format(wxT("行号超出范围，最大行号为%d"), lineCount), wxT("错误"), wxOK | wxICON_ERROR);
            }
        } else {
            wxMessageBox(wxT("请输入有效的行号"), wxT("错误"), wxOK | wxICON_ERROR);
        }
    }
}

void MainFrame::OnConfirm(wxCommandEvent& event) {
    wxString currentContent = m_textCtrl->GetText();
    if (currentContent == m_originalContent) {
        wxMessageBox(wxT("文本内容未修改，请修改后再确定。"), wxT("提示"), wxOK | wxICON_INFORMATION);
    } else {
        wxMessageBox(wxT("当前文本内容:\n\n") + currentContent, wxT("文本内容"), wxOK | wxICON_INFORMATION);
    }
}

void MainFrame::UpdateStatusBar() {
    /*
    wxFile file(m_filePath);
    wxFileOffset fileSize = file.IsOpened() ? file.Length() : 0;
    file.Close();  //*/
    
    m_statusBar->SetStatusText(m_filePath, 0);
    //m_statusBar->SetStatusText(wxString::Format(wxT("%lld 字节"), fileSize), 1);
    m_statusBar->SetStatusText(GetFileSizeString(m_filePath), 1);
    m_statusBar->SetStatusText(GetFileEncoding(), 2);
}

wxString MainFrame::GetFileSizeString(const wxString& filename) {
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

wxString MainFrame::GetFileEncoding() {
    wxString fileEncoding = wxT("手工设置");
    if(autoDetectionFileEncoding){
        fileEncoding = wxT("自动检测");
    }
    fileEncoding = fileEncoding + wxT("编码：") + m_fileEncoding;
    return fileEncoding;
}

void MainFrame::OnAbout(wxCommandEvent& event) {
    wxMessageBox(wxT("文本查看器\n"
                "版本 1.0\n\n"
                "使用修改后的文本内容作为初始编码，请铭记修改内容。"),
             wxT("关于"), 
             wxOK|wxICON_INFORMATION, 
             this);
}

wxIMPLEMENT_APP(MyApp);

/**

g++ -fdiagnostics-color=always -g txt.cpp -o txt -s -ID:/C/wxWidgets-3.2.8/include/ -ID:/C/wxWidgets-3.2.8/dev/lib/gcc810_x64_dll/mswu/ -mwindows -LD:/C/wxWidgets-3.2.8/dev/lib/gcc810_x64_dll/ -lwxbase32u -lwxmsw32u_core -lwxmsw32u_stc -static 
pause

请基于 wxWidgets 编写C++程序，实现以下功能：
1、设置按钮：可以弹出界面指定打开文件的编码，常用编码包括UTF-8、GB2312等，界面中文不能显示乱码；
2、打开按钮：打开文件时，如果指定了编码，按指定编码打开，否则自动检测文件的编码，根据检测的编码打开文件；
3、使用wxStyledTextCtrl显示文件内容时，左侧使用另一个wxStyledTextCtrl显示行号；
4、显示文本内容的wxStyledTextCtrl禁止文本换行，显示行号的wxStyledTextCtrl禁止显示滚动条，两者使用同样的样式字体，确保每行的高度一致；
5、文本内容上下滚动，文本内容变化时容，拖到滚动条，点击滚动条等出现文本滚动时，左侧的行号跟随滚动，即左侧行号的滚动条偏移位置和文本内容的滚动条偏移位置保持一致；
6、状态栏第一栏显示打开文件的完整路径，第二栏显示文件大小，第三栏显示文件的编码；
7、跳转按钮：弹出行号输入框，根据输入的行号，跳转到显示文本的对应的行数；
8、确定按钮：点击确定时，比较文本内容和刚打开时的文本是否一致，如果一致的提示文本需要修改后才能确定，如果不一致则用消息框显示文本内容。



请基于 wxWidgets 编写C++程序，实现以下功能：
1、设置按钮：可以弹出界面指定打开文件的编码，常用编码包括UTF-8、GB2312等，界面中文不能显示乱码；
2、打开按钮：打开文件时，如果指定了编码，按指定编码打开，否则自动检测文件的编码，根据检测的编码打开文件；
3、使用wxStyledTextCtrl显示文件内容时，同时在左侧通过wxSTC_MARGIN_NUMBER显示行号；
4、状态栏第一栏显示打开文件的完整路径，第二栏显示文件大小，第三栏显示文件的编码；
5、跳转按钮：弹出行号输入框，根据输入的行号，跳转到显示文本的对应的行数；
6、确定按钮：点击确定时，比较文本内容和刚打开时的文本是否一致，如果一致的提示文本需要修改后才能确定，如果不一致则用消息框显示文本内容。
//*/