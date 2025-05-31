// 1. 解决乱码问题（使用wxT宏和正确编码）

#include <wx/wx.h>
#include <wx/string.h>

// 子窗口声明
class ChildFrame;

// 主窗口类
class MainFrame : public wxFrame {
public:
    MainFrame();
    
private:
    wxTextCtrl* m_textCtrl;
    ChildFrame* m_childFrame;
    wxString m_lastInput; // 保存上次输入
    
    void OnShowChild(wxCommandEvent& event);
    void OnReceiveText(const wxString& text);
    
    friend class ChildFrame;
};

// 子窗口类
class ChildFrame : public wxFrame {
public:
    ChildFrame(MainFrame* parent, const wxString& lastInput);
    
private:
    wxTextCtrl* m_inputText;
    MainFrame* m_parent;
    bool m_isClosing;
    
    void OnConfirm(wxCommandEvent& event);
    void OnClose(wxCloseEvent& event);
    void OnKillFocus(wxFocusEvent& event);  // 新增焦点事件处理
    void OnActivate(wxActivateEvent& event); // 新增激活事件处理
};

// 主窗口实现（带编码修复）

MainFrame::MainFrame() 
    : wxFrame(nullptr, wxID_ANY, wxT("主窗口"), wxDefaultPosition, wxSize(400, 300)),
      m_childFrame(nullptr) {
    
    // 设置中文编码
    //#ifdef __WXMSW__
    //    wxLocale::AddCatalogLookupPathPrefix(wxT("."));
    //    wxLocale::AddCatalog(wxT("zh_CN"));
    //#endif
    
    wxPanel* panel = new wxPanel(this);
    wxBoxSizer* sizer = new wxBoxSizer(wxVERTICAL);
    
    m_textCtrl = new wxTextCtrl(panel, wxID_ANY, wxT(""), 
                               wxDefaultPosition, wxDefaultSize, 
                               wxTE_MULTILINE | wxTE_READONLY);
    wxButton* btn = new wxButton(panel, wxID_ANY, wxT("打开子窗口"));
    
    sizer->Add(m_textCtrl, 1, wxEXPAND | wxALL, 5);
    sizer->Add(btn, 0, wxALIGN_CENTER | wxALL, 5);
    panel->SetSizer(sizer);
    
    btn->Bind(wxEVT_BUTTON, &MainFrame::OnShowChild, this);
}

void MainFrame::OnShowChild(wxCommandEvent& event) {
    if (!m_childFrame) {
        m_childFrame = new ChildFrame(this, m_lastInput);
        m_childFrame->Bind(wxEVT_CLOSE_WINDOW, [this](wxCloseEvent& event) {
            if (m_childFrame) {
                //m_lastInput = m_childFrame->m_inputText->GetValue(); // 确保保存
                m_childFrame = nullptr;
            }
            event.Skip();
        });
        
        // 禁用主窗口
        this->Disable();
        m_childFrame->Bind(wxEVT_CLOSE_WINDOW, [this](wxCloseEvent& event) {
            this->Enable();
            event.Skip();
        });
        
        m_childFrame->Show();
        m_childFrame->SetFocus();
        
    }
}

void MainFrame::OnReceiveText(const wxString& text) {
    m_lastInput = text; // 保存最后一次输入
    m_textCtrl->SetValue(wxT("接收到子窗口文本:\n") + text);
}

// 子窗口实现（带记忆功能）

ChildFrame::ChildFrame(MainFrame* parent, const wxString& lastInput)
    : wxFrame(parent, wxID_ANY, wxT("子窗口"), wxDefaultPosition, wxSize(300, 200)),
      m_parent(parent) {
    
    wxPanel* panel = new wxPanel(this);
    wxBoxSizer* sizer = new wxBoxSizer(wxVERTICAL);
    
    m_inputText = new wxTextCtrl(panel, wxID_ANY, lastInput, // 显示上次内容
                                wxDefaultPosition, wxDefaultSize, 
                                wxTE_MULTILINE);
    wxButton* btn = new wxButton(panel, wxID_ANY, wxT("确定"));
    
    sizer->Add(m_inputText, 1, wxEXPAND | wxALL, 5);
    sizer->Add(btn, 0, wxALIGN_CENTER | wxALL, 5);
    panel->SetSizer(sizer);
    
    btn->Bind(wxEVT_BUTTON, &ChildFrame::OnConfirm, this);
    Bind(wxEVT_CLOSE_WINDOW, &ChildFrame::OnClose, this);
    
    // 绑定新事件
    Bind(wxEVT_KILL_FOCUS, &ChildFrame::OnKillFocus, this);
    Bind(wxEVT_ACTIVATE, &ChildFrame::OnActivate, this);
    Bind(wxEVT_CLOSE_WINDOW, &ChildFrame::OnClose, this);
    
    // 初始获取焦点
    Raise();
    SetFocus();
    m_inputText->SetFocus();
}

void ChildFrame::OnConfirm(wxCommandEvent& event) {
    if (m_parent) {
        m_parent->OnReceiveText(m_inputText->GetValue());
    }
    Close(true); // 关闭窗口
}

void ChildFrame::OnClose(wxCloseEvent& event) {
    m_isClosing = true;  // 关闭时设置标志位
    if (m_parent) {
        // 即使点×关闭也保存内容
        m_parent->OnReceiveText(m_inputText->GetValue()); 
    }
    Destroy(); // 确保完全销毁
    event.Skip();
}

// 失去焦点时强制取回
void ChildFrame::OnKillFocus(wxFocusEvent& event) {
    if (!m_isClosing) {
        CallAfter([this]() {
            if (!m_isClosing) {
                Raise();
                SetFocus();
                m_inputText->SetFocus();
            }
        });
    }
    event.Skip();
}

// 窗口激活状态变化
void ChildFrame::OnActivate(wxActivateEvent& event) {
    if (event.GetActive() && !m_isClosing) {
        Raise();
        SetFocus();
        m_inputText->SetFocus();
    }
    event.Skip();
}


// 应用程序类（带编码初始化）

class MyApp : public wxApp {
public:
    virtual bool OnInit() {
        // 初始化本地化（解决乱码）
        //wxLocale* locale = new wxLocale(wxLANGUAGE_CHINESE_SIMPLIFIED);
        //locale->AddCatalogLookupPathPrefix(wxT("."));
        //locale->AddCatalog(wxT("zh_CN"));
        
        MainFrame* frame = new MainFrame();
        frame->Show();
        return true;
    }
};

wxIMPLEMENT_APP(MyApp);