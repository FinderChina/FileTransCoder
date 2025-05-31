#include <wx/wx.h>
#include <wx/file.h>
#include <wx/filedlg.h>
#include <wx/artprov.h>
#include <wx/fontmap.h>
#include <wx/filename.h>
#include <wx/numdlg.h>
#include <wx/choicdlg.h>
#include <wx/dcclient.h>
#include <wx/encinfo.h>
#include <wx/stc/stc.h>

class MyApp : public wxApp
{
public:
    virtual bool OnInit();
};

class MainFrame : public wxFrame
{
public:
    MainFrame(const wxString& title);
    
private:
    wxStyledTextCtrl* m_textCtrl;
    wxStyledTextCtrl* m_lineNumberCtrl;
    wxStatusBar* m_statusBar;
    wxString m_filePath;
    wxString m_fileEncoding;
    wxString m_originalContent;
    bool m_isFileOpened;
    
    void OnOpenFile(wxCommandEvent& event);
    void OnSetEncoding(wxCommandEvent& event);
    void OnJumpToLine(wxCommandEvent& event);
    void OnConfirm(wxCommandEvent& event);
    void OnTextUpdateUI(wxStyledTextEvent& event);
    void OnTextModified(wxStyledTextEvent& event);
    void OnTextSize(wxSizeEvent& event);
    void UpdateLineNumbers();
    void UpdateStatusBar();
    void UpdateScrollPosition();
    bool DetectFileEncoding(const wxString& filename, wxString& encoding);
    wxString GetFileSizeString(const wxString& filename);
    wxFontEncoding GetEncodingFromString(const wxString& encoding);
    void ApplyEditorSettings();
    
    wxDECLARE_EVENT_TABLE();
};

wxBEGIN_EVENT_TABLE(MainFrame, wxFrame)
    EVT_MENU(wxID_OPEN, MainFrame::OnOpenFile)
    EVT_MENU(wxID_PREFERENCES, MainFrame::OnSetEncoding)
    EVT_MENU(wxID_JUMP_TO, MainFrame::OnJumpToLine)
    EVT_MENU(wxID_OK, MainFrame::OnConfirm)
    EVT_SIZE(MainFrame::OnTextSize)
wxEND_EVENT_TABLE()

bool MyApp::OnInit()
{
    wxLocale* locale = new wxLocale(wxLANGUAGE_CHINESE_SIMPLIFIED);
    locale->AddCatalogLookupPathPrefix(".");
    locale->AddCatalog("wxstd");
    
    MainFrame *frame = new MainFrame(wxT("文本编辑器"));
    frame->Show(true);
    return true;
}

MainFrame::MainFrame(const wxString& title)
    : wxFrame(NULL, wxID_ANY, title, wxDefaultPosition, wxSize(800, 600)),
      m_isFileOpened(false)
{
    // 创建菜单栏
    wxMenu *menuFile = new wxMenu;
    menuFile->Append(wxID_OPEN, wxT("打开(&O)\tCtrl+O"));
    menuFile->Append(wxID_PREFERENCES, wxT("设置编码(&E)"));
    menuFile->Append(wxID_JUMP_TO, wxT("跳转到行(&J)"));
    menuFile->AppendSeparator();
    menuFile->Append(wxID_OK, wxT("确定(&C)"));
    menuFile->Append(wxID_EXIT, wxT("退出(&Q)"));
    
    wxMenuBar *menuBar = new wxMenuBar;
    menuBar->Append(menuFile, wxT("文件(&F)"));
    SetMenuBar(menuBar);
    
    // 创建工具栏
    wxToolBar *toolBar = CreateToolBar();
    toolBar->AddTool(wxID_OPEN, wxT("打开"), wxArtProvider::GetBitmap(wxART_FILE_OPEN, wxART_TOOLBAR));
    toolBar->AddTool(wxID_PREFERENCES, wxT("编码"), wxArtProvider::GetBitmap(wxART_LIST_VIEW, wxART_TOOLBAR));
    toolBar->AddTool(wxID_JUMP_TO, wxT("跳转"), wxArtProvider::GetBitmap(wxART_GO_FORWARD, wxART_TOOLBAR));
    toolBar->AddTool(wxID_OK, wxT("确定"), wxArtProvider::GetBitmap(wxART_TICK_MARK, wxART_TOOLBAR));
    toolBar->Realize();
    
    // 创建面板和控件
    wxPanel *panel = new wxPanel(this);
    wxBoxSizer *sizer = new wxBoxSizer(wxHORIZONTAL);
    
    // 行号控件
    m_lineNumberCtrl = new wxStyledTextCtrl(panel, wxID_ANY, wxDefaultPosition, wxDefaultSize, wxBORDER_NONE);
    m_lineNumberCtrl->SetMarginType(0, wxSTC_MARGIN_NUMBER);
    m_lineNumberCtrl->SetMarginWidth(0, 50);
    m_lineNumberCtrl->StyleSetBackground(wxSTC_STYLE_DEFAULT, wxColour(240, 240, 240));
    m_lineNumberCtrl->SetEditable(false);
    
    // 文本控件
    m_textCtrl = new wxStyledTextCtrl(panel, wxID_ANY);
    ApplyEditorSettings();
    
    // 设置相同的字体
    wxFont font(12, wxFONTFAMILY_MODERN, wxFONTSTYLE_NORMAL, wxFONTWEIGHT_NORMAL);
    m_textCtrl->StyleSetFont(wxSTC_STYLE_DEFAULT, font);
    m_lineNumberCtrl->StyleSetFont(wxSTC_STYLE_DEFAULT, font);
    
    sizer->Add(m_lineNumberCtrl, 0, wxEXPAND | wxALL, 0);
    sizer->Add(m_textCtrl, 1, wxEXPAND | wxALL, 0);
    panel->SetSizer(sizer);
    
    // 创建状态栏
    m_statusBar = CreateStatusBar(3);
    int widths[] = {-1, 150, 200};
    m_statusBar->SetStatusWidths(3, widths);
    m_statusBar->SetStatusText(wxT("就绪"), 0);
    
    // 绑定事件
    m_textCtrl->Bind(wxEVT_STC_UPDATEUI, &MainFrame::OnTextUpdateUI, this);
    m_textCtrl->Bind(wxEVT_STC_MODIFIED, &MainFrame::OnTextModified, this);
}

void MainFrame::ApplyEditorSettings()
{
    m_textCtrl->SetWrapMode(wxSTC_WRAP_NONE);
    m_textCtrl->SetIndentationGuides(wxSTC_IV_LOOKFORWARD);
    m_textCtrl->SetMarginWidth(1, 0); // 隐藏折叠边距
    m_textCtrl->SetEOLMode(wxSTC_EOL_LF);
    m_textCtrl->SetViewEOL(false);
    m_textCtrl->SetViewWhiteSpace(false);
    m_textCtrl->SetCaretLineVisible(true);
    m_textCtrl->SetCaretLineBackground(wxColour(240, 240, 255));
}

void MainFrame::OnOpenFile(wxCommandEvent& event)
{
    wxFileDialog openFileDialog(this, wxT("打开文件"), wxT(""), wxT(""),
                           wxT("All files (*.*)|*.*"), wxFD_OPEN|wxFD_FILE_MUST_EXIST);
    
    if (openFileDialog.ShowModal() == wxID_CANCEL)
        return;
    
    wxString filename = openFileDialog.GetPath();
    m_filePath = filename;
    
    // 检测文件编码
    if (!DetectFileEncoding(filename, m_fileEncoding)) {
        m_fileEncoding = wxT("UTF-8"); // 默认编码
    }
    
    // 读取文件内容
    wxFile file(filename);
    if (!file.IsOpened()) {
        wxLogError(wxT("无法打开文件: %s"), filename);
        return;
    }
    
    size_t len = file.Length();
    char* buffer = new char[len];
    file.Read(buffer, len);
    file.Close();
    
    // 处理BOM头
    size_t startPos = 0;
    if (len >= 3 && (unsigned char)buffer[0] == 0xEF && 
                   (unsigned char)buffer[1] == 0xBB && 
                   (unsigned char)buffer[2] == 0xBF) {
        startPos = 3; // 跳过UTF-8 BOM
    }
    
    // 设置文本内容
    if (m_fileEncoding == wxT("UTF-8")) {
        m_textCtrl->SetTextRaw(buffer + startPos, len - startPos);
    } else {
        wxCSConv conv(m_fileEncoding);
        if (conv.IsOk()) {
            wxString content(buffer + startPos, conv, len - startPos);
            m_textCtrl->SetText(content);
        } else {
            m_textCtrl->SetTextRaw(buffer + startPos, len - startPos);
        }
    }
    
    delete[] buffer;
    m_originalContent = m_textCtrl->GetText();
    m_isFileOpened = true;
    
    UpdateLineNumbers();
    UpdateStatusBar();
}

bool MainFrame::DetectFileEncoding(const wxString& filename, wxString& encoding)
{
    // 尝试自动检测编码
    wxEncodingDetector detector;
    if (detector.DetectFileEncoding(filename)) {
        encoding = detector.GetEncodingName();
        return true;
    }
    
    // 检查BOM头
    wxFile file(filename);
    if (file.IsOpened()) {
        char bom[3];
        if (file.Read(bom, 3) == 3) {
            if ((unsigned char)bom[0] == 0xEF && 
                (unsigned char)bom[1] == 0xBB && 
                (unsigned char)bom[2] == 0xBF) {
                encoding = wxT("UTF-8");
                file.Close();
                return true;
            }
        }
        file.Close();
    }
    
    return false;
}

void MainFrame::OnSetEncoding(wxCommandEvent& event)
{
    wxArrayString choices;
    choices.Add(wxT("UTF-8"));
    choices.Add(wxT("GB2312"));
    choices.Add(wxT("BIG5"));
    choices.Add(wxT("自动检测"));
    
    wxSingleChoiceDialog dialog(this, wxT("请选择文件编码:"), wxT("设置编码"), choices);
    if (dialog.ShowModal() == wxID_OK) {
        int selection = dialog.GetSelection();
        if (selection == 3) { // 自动检测
            m_fileEncoding = wxT("");
        } else {
            m_fileEncoding = choices[selection];
        }
        
        if (m_isFileOpened) {
            wxCommandEvent dummy;
            OnOpenFile(dummy); // 重新加载文件
        }
    }
}

void MainFrame::OnJumpToLine(wxCommandEvent& event)
{
    if (!m_isFileOpened) {
        wxMessageBox(wxT("请先打开文件"), wxT("提示"), wxOK | wxICON_INFORMATION);
        return;
    }
    
    int lineCount = m_textCtrl->GetLineCount();
    long lineNum = wxGetNumberFromUser(
        wxT("请输入要跳转的行号 (1-") + wxString::Format(wxT("%d"), lineCount) + wxT("):"), 
        wxT(""), wxT("跳转到行"), 1, 1, lineCount, this);
    
    if (lineNum != -1) {
        int pos = m_textCtrl->PositionFromLine(lineNum - 1);
        m_textCtrl->GotoPos(pos);
        m_textCtrl->EnsureVisible(lineNum - 1);
    }
}

void MainFrame::OnConfirm(wxCommandEvent& event)
{
    if (!m_isFileOpened) {
        wxMessageBox(wxT("请先打开文件"), wxT("提示"), wxOK | wxICON_INFORMATION);
        return;
    }
    
    wxString currentContent = m_textCtrl->GetText();
    if (currentContent == m_originalContent) {
        wxMessageBox(wxT("文本内容未修改"), wxT("提示"), wxOK | wxICON_INFORMATION);
    } else {
        wxMessageBox(wxT("文本内容已修改"), wxT("提示"), wxOK | wxICON_INFORMATION);
    }
}

void MainFrame::OnTextUpdateUI(wxStyledTextEvent& event)
{
    UpdateScrollPosition();
    event.Skip();
}

void MainFrame::OnTextModified(wxStyledTextEvent& event)
{
    UpdateLineNumbers();
    event.Skip();
}

void MainFrame::OnTextSize(wxSizeEvent& event)
{
    UpdateLineNumbers();
    event.Skip();
}

void MainFrame::UpdateLineNumbers()
{
    if (!m_isFileOpened) return;
    
    int lineCount = m_textCtrl->GetLineCount();
    wxString numbers;
    for (int i = 1; i <= lineCount; i++) {
        numbers << wxString::Format(wxT("%d\n"), i);
    }
    
    m_lineNumberCtrl->SetText(numbers);
    
    // 同步滚动位置
    int firstVisibleLine = m_textCtrl->GetFirstVisibleLine();
    m_lineNumberCtrl->SetFirstVisibleLine(firstVisibleLine);
}

void MainFrame::UpdateStatusBar()
{
    if (m_isFileOpened) {
        m_statusBar->SetStatusText(m_filePath, 0);
        m_statusBar->SetStatusText(GetFileSizeString(m_filePath), 1);
        m_statusBar->SetStatusText(m_fileEncoding.IsEmpty() ? wxT("自动检测") : m_fileEncoding, 2);
    }
}

void MainFrame::UpdateScrollPosition()
{
    int firstVisibleLine = m_textCtrl->GetFirstVisibleLine();
    int visibleLines = m_textCtrl->LinesOnScreen();
    wxString status = wxString::Format(wxT("行 %d-%d"), 
              firstVisibleLine + 1, firstVisibleLine + visibleLines);
    m_statusBar->SetStatusText(status, 0);
}

wxString MainFrame::GetFileSizeString(const wxString& filename)
{
    if (!wxFileName::FileExists(filename))
        return wxT("未知");
    
    wxULongLong size = wxFileName::GetSize(filename);
    if (size < 1024)
        return wxString::Format(wxT("%llu B"), size);
    else if (size < 1024 * 1024)
        return wxString::Format(wxT("%.1f KB"), size.ToDouble() / 1024);
    else
        return wxString::Format(wxT("%.1f MB"), size.ToDouble() / (1024 * 1024));
}

wxIMPLEMENT_APP(MyApp);