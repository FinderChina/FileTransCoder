#include <wx/wx.h>
#include <wx/file.h>
#include <wx/textctrl.h>
#include <wx/filedlg.h>
#include <wx/artprov.h>
#include <wx/fontmap.h>
#include <wx/filename.h>
#include <wx/numdlg.h>
#include <wx/choicdlg.h>
#include <wx/dcclient.h>
#include <wx/scrolbar.h>

// 自定义ID定义
enum {
    ID_SET_ENCODING = wxID_HIGHEST + 1,
    ID_GO_TO_LINE
};

// 自定义带行号的文本控件
class LineNumberTextCtrl : public wxPanel {
public:
    LineNumberTextCtrl(wxWindow* parent, wxWindowID id = wxID_ANY)
        : wxPanel(parent, id), textCtrl(nullptr), lineNumberWidth(50) {
        
        // 创建文本控件
        textCtrl = new wxTextCtrl(this, wxID_ANY, "",
                                wxDefaultPosition, wxDefaultSize,
                                wxTE_MULTILINE | wxTE_RICH2 | wxTE_NOHIDESEL);
        
        // 设置相同的字体和行高
        wxFont font(12, wxFONTFAMILY_MODERN, wxFONTSTYLE_NORMAL, wxFONTWEIGHT_NORMAL);
        textCtrl->SetFont(font);
        SetFont(font);
        
        // 绑定所有可能影响滚动位置的事件
        textCtrl->Bind(wxEVT_KEY_DOWN, &LineNumberTextCtrl::OnScrollEvent, this);
        textCtrl->Bind(wxEVT_LEFT_DOWN, &LineNumberTextCtrl::OnScrollEvent, this);
        textCtrl->Bind(wxEVT_MOUSEWHEEL, &LineNumberTextCtrl::OnScrollEvent, this);
        textCtrl->Bind(wxEVT_TEXT, &LineNumberTextCtrl::OnScrollEvent, this);
        textCtrl->Bind(wxEVT_SCROLLWIN_THUMBTRACK, &LineNumberTextCtrl::OnScrollEvent, this);
        textCtrl->Bind(wxEVT_SCROLLWIN_THUMBRELEASE, &LineNumberTextCtrl::OnScrollEvent, this);
        textCtrl->Bind(wxEVT_SCROLLWIN_LINEUP, &LineNumberTextCtrl::OnScrollEvent, this);
        textCtrl->Bind(wxEVT_SCROLLWIN_LINEDOWN, &LineNumberTextCtrl::OnScrollEvent, this);
        textCtrl->Bind(wxEVT_SCROLLWIN_PAGEUP, &LineNumberTextCtrl::OnScrollEvent, this);
        textCtrl->Bind(wxEVT_SCROLLWIN_PAGEDOWN, &LineNumberTextCtrl::OnScrollEvent, this);
        Bind(wxEVT_PAINT, &LineNumberTextCtrl::OnPaint, this);
        Bind(wxEVT_SIZE, &LineNumberTextCtrl::OnSize, this);
        
        // 初始布局
        UpdateTextCtrlPosition();
    }
    
    wxTextCtrl* GetTextCtrl() { return textCtrl; }
    
    void SetLineNumberWidth(int width) {
        lineNumberWidth = width;
        UpdateTextCtrlPosition();
        Refresh();
    }
    
private:
    wxTextCtrl* textCtrl;
    int lineNumberWidth;
    
    void UpdateTextCtrlPosition() {
        if (textCtrl) {
            wxSize size = GetClientSize();
            textCtrl->SetSize(lineNumberWidth, 0, 
                             size.x - lineNumberWidth, size.y);
        }
    }
    
    void OnPaint(wxPaintEvent& event) {
        wxPaintDC dc(this);
        PrepareDC(dc);
        
        // 设置行号区域背景
        dc.SetPen(*wxTRANSPARENT_PEN);
        dc.SetBrush(wxBrush(wxColour(240, 240, 240)));
        dc.DrawRectangle(0, 0, lineNumberWidth, GetClientSize().y);
        
        // 使用与文本控件相同的字体
        dc.SetFont(GetFont());
        dc.SetTextForeground(wxColour(100, 100, 100));
        
        // 获取文本行高和滚动位置
        int lineHeight = textCtrl->GetCharHeight() + 2; // 增加行间距
        int scrollPos = textCtrl->GetScrollPos(wxVERTICAL);
        wxSize clientSize = GetClientSize();
        
        // 计算可见行范围
        int firstVisibleLine = scrollPos;
        int linesVisible = clientSize.y / lineHeight + 10;
        
        // 绘制行号
        for (int i = 0; i < linesVisible; i++) {
            int lineNum = firstVisibleLine + i + 1;
            if (lineNum > textCtrl->GetNumberOfLines()) break;
            
            wxString lineText = wxString::Format("%d", lineNum);
            int textWidth, textHeight;
            dc.GetTextExtent(lineText, &textWidth, &textHeight);
            
            // 精确计算每行位置，确保与文本对齐
            int yPos = i * lineHeight - (textCtrl->GetScrollPos(wxVERTICAL) % lineHeight);
            dc.DrawText(lineText, lineNumberWidth - textWidth - 8, yPos + (lineHeight - textHeight)/2);
        }
    }
    
    void OnScrollEvent(wxEvent& event) {
        Refresh();
        event.Skip();
    }
    
    void OnSize(wxSizeEvent& event) {
        UpdateTextCtrlPosition();
        Refresh();
        event.Skip();
    }
    
    void OnTextChanged(wxCommandEvent& event) {
        Refresh();
        event.Skip();
    }
    
    void OnKeyDown(wxKeyEvent& event) {
        Refresh();
        event.Skip();
    }
    
    void OnMouseDown(wxMouseEvent& event) {
        Refresh();
        event.Skip();
    }
    
    void OnMouseWheel(wxMouseEvent& event) {
        Refresh();
        event.Skip();
    }
};

class MyApp : public wxApp {
public:
    virtual bool OnInit();
};

class MyFrame : public wxFrame {
public:
    MyFrame(const wxString& title);

private:
    void OnOpen(wxCommandEvent& event);
    void OnSetEncoding(wxCommandEvent& event);
    void OnExit(wxCommandEvent& event);
    void OnAbout(wxCommandEvent& event);
    void OnShowContent(wxCommandEvent& event);
    void OnGoToLine(wxCommandEvent& event);
    wxFontEncoding DetectFileEncoding(const wxString& filename);
    bool TryLoadFile(const wxString& filename, wxFontEncoding encoding);
    wxString FormatFileSize(wxFileOffset size);
    void GoToLine(int lineNumber);
    wxFontEncoding GetEncodingFromString(const wxString& encodingStr);
    
    LineNumberTextCtrl* lineNumberCtrl;
    wxStatusBar* statusBar;
    wxFontEncoding selectedEncoding;
    bool useAutoDetection;
    wxString currentFilePath;
};

wxIMPLEMENT_APP(MyApp);

bool MyApp::OnInit() {
    wxLocale* locale = new wxLocale(wxLANGUAGE_CHINESE_SIMPLIFIED);
    locale->AddCatalogLookupPathPrefix(".");
    locale->AddCatalog("wxstd");
    
    MyFrame* frame = new MyFrame(wxT("文本查看器"));
    frame->Show(true);
    return true;
}

MyFrame::MyFrame(const wxString& title)
    : wxFrame(NULL, wxID_ANY, title, wxDefaultPosition, wxSize(800, 600)),
      selectedEncoding(wxFONTENCODING_SYSTEM),
      useAutoDetection(true) {
    
    // 创建菜单栏
    wxMenu* menuFile = new wxMenu;
    menuFile->Append(wxID_OPEN, wxT("打开(&O)\tCtrl+O"), wxT("打开文本文件"));
    menuFile->AppendSeparator();
    menuFile->Append(ID_SET_ENCODING, wxT("设置编码(&E)"), wxT("设置文件编码"));
    menuFile->AppendSeparator();
    menuFile->Append(wxID_EXIT, wxT("退出(&Q)\tCtrl+Q"), wxT("退出程序"));
    
    wxMenu* menuEdit = new wxMenu;
    menuEdit->Append(ID_GO_TO_LINE, wxT("跳转到行(&G)\tCtrl+G"), wxT("跳转到指定行号"));
    
    wxMenu* menuHelp = new wxMenu;
    menuHelp->Append(wxID_ABOUT, wxT("关于(&A)\tF1"), wxT("关于本程序"));
    
    wxMenuBar* menuBar = new wxMenuBar;
    menuBar->Append(menuFile, wxT("文件(&F)"));
    menuBar->Append(menuEdit, wxT("编辑(&E)"));
    menuBar->Append(menuHelp, wxT("帮助(&H)"));
    SetMenuBar(menuBar);
    
    // 创建工具栏
    wxToolBar* toolBar = CreateToolBar();
    toolBar->AddTool(wxID_OPEN, wxT("打开"), 
                    wxArtProvider::GetBitmap(wxART_FILE_OPEN, wxART_TOOLBAR),
                    wxT("打开文本文件 (Ctrl+O)"));
    
    toolBar->AddTool(ID_SET_ENCODING, wxT("设置编码"), 
                   wxArtProvider::GetBitmap(wxART_FIND, wxART_TOOLBAR),
                   wxT("设置文件编码"));
    
    toolBar->AddTool(wxID_OK, wxT("确定"), 
                   wxArtProvider::GetBitmap(wxART_TICK_MARK, wxART_TOOLBAR),
                   wxT("显示当前文本内容"));
    
    toolBar->AddTool(ID_GO_TO_LINE, wxT("跳转"), 
                    wxArtProvider::GetBitmap(wxART_GOTO_FIRST, wxART_TOOLBAR),
                    wxT("跳转到指定行号 (Ctrl+G)"));
    
    toolBar->Realize();
    
    // 创建带行号的文本控件
    lineNumberCtrl = new LineNumberTextCtrl(this);
    
    // 创建状态栏
    statusBar = CreateStatusBar(3);
    int widths[] = {-1, 150, 200};
    statusBar->SetStatusWidths(3, widths);
    statusBar->SetStatusText(wxT("就绪"), 0);
    statusBar->SetStatusText(wxT(""), 1);
    statusBar->SetStatusText(wxT("编码: 自动检测"), 2);
    
    // 绑定事件
    Bind(wxEVT_MENU, &MyFrame::OnOpen, this, wxID_OPEN);
    Bind(wxEVT_MENU, &MyFrame::OnSetEncoding, this, ID_SET_ENCODING);
    Bind(wxEVT_MENU, &MyFrame::OnExit, this, wxID_EXIT);
    Bind(wxEVT_MENU, &MyFrame::OnAbout, this, wxID_ABOUT);
    Bind(wxEVT_MENU, &MyFrame::OnShowContent, this, wxID_OK);
    Bind(wxEVT_MENU, &MyFrame::OnGoToLine, this, ID_GO_TO_LINE);
    
    // 添加快捷键
    wxAcceleratorEntry entries[3];
    entries[0].Set(wxACCEL_CTRL, (int)'O', wxID_OPEN);
    entries[1].Set(wxACCEL_CTRL, (int)'G', ID_GO_TO_LINE);
    entries[2].Set(wxACCEL_CTRL, (int)'Q', wxID_EXIT);
    wxAcceleratorTable accel(3, entries);
    SetAcceleratorTable(accel);
}

void MyFrame::OnSetEncoding(wxCommandEvent& event) {
    wxArrayString choices;
    choices.Add(wxT("自动检测"));
    choices.Add(wxT("UTF-8"));
    choices.Add(wxT("UTF-8 with BOM"));
    choices.Add(wxT("UTF-16LE"));
    choices.Add(wxT("UTF-16BE"));
    choices.Add(wxT("GB2312/GBK"));
    choices.Add(wxT("系统默认编码"));
    
    wxString title = wxT("设置文件编码");
    wxString message = wxT("请选择文件编码方式:");
    
    int selection = wxGetSingleChoiceIndex(message, title, choices, this);
    
    if (selection != -1) {
        if (selection == 0) {
            useAutoDetection = true;
            statusBar->SetStatusText(wxT("编码: 自动检测"), 2);
        } else {
            useAutoDetection = false;
            wxString selected = choices[selection];
            selectedEncoding = GetEncodingFromString(selected);
            statusBar->SetStatusText(wxString::Format(wxT("编码: %s"), selected), 2);
        }
    }
}

wxFontEncoding MyFrame::GetEncodingFromString(const wxString& encodingStr) {
    if (encodingStr == wxT("UTF-8")) return wxFONTENCODING_UTF8;
    if (encodingStr == wxT("UTF-8 with BOM")) return wxFONTENCODING_UTF8;
    if (encodingStr == wxT("UTF-16LE")) return wxFONTENCODING_UTF16LE;
    if (encodingStr == wxT("UTF-16BE")) return wxFONTENCODING_UTF16BE;
    if (encodingStr == wxT("GB2312/GBK")) return wxFONTENCODING_CP936;
    return wxFONTENCODING_SYSTEM;
}

wxFontEncoding MyFrame::DetectFileEncoding(const wxString& filename) {
    wxFile file(filename);
    if (!file.IsOpened()) {
        return wxFONTENCODING_SYSTEM;
    }

    // 检查BOM标记
    unsigned char bom[4] = {0};
    if (file.Read(bom, 3) == 3) {
        if (bom[0] == 0xEF && bom[1] == 0xBB && bom[2] == 0xBF) {
            return wxFONTENCODING_UTF8;
        }
    }
    
    file.Seek(0);
    if (file.Read(bom, 2) == 2) {
        if (bom[0] == 0xFF && bom[1] == 0xFE) {
            return wxFONTENCODING_UTF16LE;
        } else if (bom[0] == 0xFE && bom[1] == 0xFF) {
            return wxFONTENCODING_UTF16BE;
        }
    }

    // 尝试UTF-8无BOM
    file.Seek(0);
    wxString testContent;
    if (file.ReadAll(&testContent, wxCSConv(wxFONTENCODING_UTF8))) {
        bool validUTF8 = true;
        for (size_t i = 0; i < testContent.length(); i++) {
            if (testContent[i] < 0) {
                validUTF8 = false;
                break;
            }
        }
        if (validUTF8) return wxFONTENCODING_UTF8;
    }
    
    // 尝试GB2312/GBK (CP936)
    file.Seek(0);
    if (file.ReadAll(&testContent, wxCSConv(wxFONTENCODING_CP936))) {
        bool validGB = true;
        for (size_t i = 0; i < testContent.length(); i++) {
            unsigned char c = (unsigned char)testContent[i];
            if (c >= 0x81 && c <= 0xFE) {
                if (++i >= testContent.length()) {
                    validGB = false;
                    break;
                }
                unsigned char c2 = (unsigned char)testContent[i];
                if (c2 < 0x40 || c2 == 0x7F || c2 > 0xFE) {
                    validGB = false;
                    break;
                }
            } else if (c < 32 && c != '\n' && c != '\r' && c != '\t') {
                validGB = false;
                break;
            }
        }
        if (validGB) return wxFONTENCODING_CP936;
    }
    
    return wxFONTENCODING_SYSTEM;
}

bool MyFrame::TryLoadFile(const wxString& filename, wxFontEncoding encoding) {
    wxFile file(filename);
    if (!file.IsOpened()) {
        return false;
    }

    wxString content;
    wxCSConv conv(encoding);
    if (!conv.IsOk()) {
        conv = wxCSConv(wxFONTENCODING_SYSTEM);
    }

    if (!file.ReadAll(&content, conv)) {
        return false;
    }

    lineNumberCtrl->GetTextCtrl()->SetValue(content);
    currentFilePath = filename;
    
    // 更新状态栏
    statusBar->SetStatusText(filename, 0); // 第一格显示完整路径
    statusBar->SetStatusText(wxString::Format(wxT("大小: %s"), FormatFileSize(file.Length())), 1); // 第二格显示大小
    statusBar->SetStatusText(wxString::Format("编码: %s", encoding), 2); // 文件编码
    
    return true;
}

wxString MyFrame::FormatFileSize(wxFileOffset size) {
    const wxString units[] = {wxT("B"), wxT("KB"), wxT("MB"), wxT("GB")};
    double s = (double)size;
    int i = 0;
    
    while (s >= 1024 && i < 3) {
        s /= 1024;
        i++;
    }
    
    return wxString::Format(wxT("%.2f %s"), s, units[i]);
}

void MyFrame::OnOpen(wxCommandEvent& event) {
    wxFileDialog openFileDialog(this, wxT("打开文件"), wxT(""), wxT(""),
                              wxT("文本文件 (*.txt)|*.txt|网页文件 (*.html)|*.js;*.css;*.htm;*.html|C++文件 (*.cpp)|*.h;*.c;*.cpp|Java文件 (*.java)|*.java|所有文件 (*.*)|*.*"), 
                              wxFD_OPEN|wxFD_FILE_MUST_EXIST);
    
    if (openFileDialog.ShowModal() != wxID_OK) {
        return;
    }

    wxString filename = openFileDialog.GetPath();
    wxFontEncoding encoding;
    
    if (useAutoDetection) {
        encoding = DetectFileEncoding(filename);
        wxString encName;
        switch(encoding) {
            case wxFONTENCODING_UTF8: encName = wxT("UTF-8"); break;
            case wxFONTENCODING_UTF16LE: encName = wxT("UTF-16LE"); break;
            case wxFONTENCODING_UTF16BE: encName = wxT("UTF-16BE"); break;
            case wxFONTENCODING_CP936: encName = wxT("GB2312/GBK"); break;
            default: encName = wxT("系统编码"); break;
        }
        statusBar->SetStatusText(wxString::Format(wxT("编码: %s (自动检测)"), encName), 2);
    } else {
        encoding = selectedEncoding;
    }
    
    if (!TryLoadFile(filename, encoding)) {
        // 如果指定编码失败，尝试自动检测
        if (!useAutoDetection) {
            wxFontEncoding autoEncoding = DetectFileEncoding(filename);
            if (TryLoadFile(filename, autoEncoding)) {
                wxString encName;
                switch(autoEncoding) {
                    case wxFONTENCODING_UTF8: encName = wxT("UTF-8"); break;
                    case wxFONTENCODING_UTF16LE: encName = wxT("UTF-16LE"); break;
                    case wxFONTENCODING_UTF16BE: encName = wxT("UTF-16BE"); break;
                    case wxFONTENCODING_CP936: encName = wxT("GB2312/GBK"); break;
                    default: encName = wxT("系统编码"); break;
                }
                statusBar->SetStatusText(wxString::Format(wxT("编码: %s (自动检测)"), encName), 2);
                wxMessageBox(wxT("使用指定编码打开失败，已自动检测编码"), 
                            wxT("提示"), wxOK|wxICON_INFORMATION, this);
                return;
            }
        }
        
        // 如果自动检测也失败，尝试系统默认编码
        if (TryLoadFile(filename, wxFONTENCODING_SYSTEM)) {
            statusBar->SetStatusText(wxT("编码: 系统默认"), 2);
            wxMessageBox(wxT("使用系统默认编码打开文件"), 
                        wxT("提示"), wxOK|wxICON_INFORMATION, this);
            return;
        }
        
        wxMessageBox(wxT("无法打开文件！"), 
                    wxT("错误"), wxOK|wxICON_ERROR, this);
    }
}

void MyFrame::GoToLine(int lineNumber) {
    wxTextCtrl* textCtrl = lineNumberCtrl->GetTextCtrl();
    if (lineNumber <= 0) {
        wxMessageBox(wxT("行号必须大于0"), wxT("错误"), wxOK|wxICON_ERROR, this);
        return;
    }

    int lineCount = textCtrl->GetNumberOfLines();
    if (lineNumber > lineCount) {
        wxMessageBox(wxString::Format(wxT("行号超出范围，最大行号为%d"), lineCount), 
                    wxT("错误"), wxOK|wxICON_ERROR, this);
        return;
    }

    // 计算要跳转的位置
    long pos = 0;
    for (int i = 0; i < lineNumber - 1; i++) {
        pos += textCtrl->GetLineLength(i) + 1; // +1 for newline
    }

    // 设置光标位置并滚动到可见
    textCtrl->SetInsertionPoint(pos);
    textCtrl->ShowPosition(pos);
    textCtrl->SetFocus();
    
    // 强制刷新行号显示
    lineNumberCtrl->Refresh();
    
    // 在状态栏显示当前行号
    statusBar->SetStatusText(wxString::Format(wxT("当前行: %d/%d"), lineNumber, lineCount), 1);
}

void MyFrame::OnGoToLine(wxCommandEvent& event) {
    wxTextCtrl* textCtrl = lineNumberCtrl->GetTextCtrl();
    int lineCount = textCtrl->GetNumberOfLines();
    int lineNumber = wxGetNumberFromUser(
        wxString::Format(wxT("输入要跳转的行号 (1-%d):"), lineCount),
        wxT("行号:"), wxT("跳转到行"), 
        1, 1, lineCount, this);
    
    if (lineNumber != -1) { // 用户没有取消
        GoToLine(lineNumber);
    }
}

void MyFrame::OnShowContent(wxCommandEvent& event) {
    wxString content = lineNumberCtrl->GetTextCtrl()->GetValue();
    if (content.IsEmpty()) {
        wxMessageBox(wxT("文本内容为空！"), wxT("提示"), wxOK|wxICON_INFORMATION, this);
    } else {
        // 只显示前200个字符，避免消息框太大
        wxString displayContent = content.Length() > 200 ? 
            content.Left(200) + wxT("\n\n...（内容已截断）") : content;
        wxMessageBox(wxString::Format(wxT("当前文本内容：\n\n%s"), displayContent), 
                    wxT("文本内容"), wxOK|wxICON_INFORMATION, this);
    }
}

void MyFrame::OnExit(wxCommandEvent& event) {
    Close(true);
}

void MyFrame::OnAbout(wxCommandEvent& event) {
    wxMessageBox(wxT("文本查看器\n")
               wxT("版本 9.0\n\n")
               wxT("功能特性:\n")
               wxT("- 左侧显示行号\n")
               wxT("- 支持多种编码\n")
               wxT("- 行号跳转功能"),
               wxT("关于"), wxOK|wxICON_INFORMATION, this);
}