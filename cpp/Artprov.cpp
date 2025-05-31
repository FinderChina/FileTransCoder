#include <wx/wx.h>
#include <wx/artprov.h>
#include <wx/listctrl.h>

class ArtBrowserApp : public wxApp
{
public:
    virtual bool OnInit();
};

class ArtBrowserFrame : public wxFrame
{
public:
    ArtBrowserFrame(const wxString& title);

private:
    void PopulateArtList();
    wxListView* m_listView;
};

wxIMPLEMENT_APP(ArtBrowserApp);

bool ArtBrowserApp::OnInit()
{
    ArtBrowserFrame *frame = new ArtBrowserFrame("wxWidgets Art Provider Browser");
    frame->Show(true);
    return true;
}

ArtBrowserFrame::ArtBrowserFrame(const wxString& title)
    : wxFrame(NULL, wxID_ANY, title, wxDefaultPosition, wxSize(600, 400))
{
    // 创建控件
    m_listView = new wxListView(this, wxID_ANY, 
                               wxDefaultPosition, wxDefaultSize,
                               wxLC_ICON | wxLC_AUTOARRANGE);
    
    // 填充图标列表
    PopulateArtList();
    
    // 设置窗口布局
    wxBoxSizer* sizer = new wxBoxSizer(wxVERTICAL);
    sizer->Add(m_listView, 1, wxEXPAND | wxALL, 5);
    SetSizer(sizer);
    
    // 设置窗口图标
    SetIcon(wxArtProvider::GetIcon(wxART_INFORMATION));
}

void ArtBrowserFrame::PopulateArtList()
{
    // 获取所有标准图标ID
    const wxArtID artIDs[] = {
        wxART_ADD_BOOKMARK,
        wxART_DEL_BOOKMARK,
        wxART_HELP_SIDE_PANEL,
        wxART_HELP_SETTINGS,
        wxART_HELP_BOOK,
        wxART_HELP_FOLDER,
        wxART_HELP_PAGE,
        wxART_GO_BACK,
        wxART_GO_FORWARD,
        wxART_GO_UP,
        wxART_GO_DOWN,
        wxART_GO_TO_PARENT,
        wxART_GO_HOME,
        wxART_GOTO_FIRST,
        wxART_GOTO_LAST,
        wxART_FILE_OPEN,
        wxART_FILE_SAVE,
        wxART_FILE_SAVE_AS,
        wxART_PRINT,
        wxART_HELP,
        wxART_TIP,
        wxART_REPORT_VIEW,
        wxART_LIST_VIEW,
        wxART_NEW_DIR,
        wxART_FOLDER,
        wxART_FOLDER_OPEN,
        wxART_GO_DIR_UP,
        wxART_EXECUTABLE_FILE,
        wxART_NORMAL_FILE,
        wxART_TICK_MARK,
        wxART_CROSS_MARK,
        wxART_ERROR,
        wxART_QUESTION,
        wxART_WARNING,
        wxART_INFORMATION,
        wxART_MISSING_IMAGE,
        wxART_COPY,
        wxART_CUT,
        wxART_PASTE,
        wxART_DELETE,
        wxART_NEW,
        wxART_UNDO,
        wxART_REDO,
        wxART_PLUS,
        wxART_MINUS,
        wxART_CLOSE,
        wxART_QUIT,
        wxART_FIND,
        wxART_FIND_AND_REPLACE,
        wxART_HARDDISK,
        wxART_FLOPPY,
        wxART_CDROM,
        wxART_REMOVABLE
        ,wxART_EDIT
        ,wxART_WX_LOGO
        ,wxART_REFRESH
        ,wxART_STOP
    };
    
    // 创建图像列表
    wxImageList* imageList = new wxImageList(32, 32);
    m_listView->AssignImageList(imageList, wxIMAGE_LIST_NORMAL);
    
    // 添加每个图标到列表
    for (size_t i = 0; i < WXSIZEOF(artIDs); i++)
    {
        wxBitmap bmp = wxArtProvider::GetBitmap(artIDs[i], wxART_OTHER, wxSize(32, 32));
        imageList->Add(bmp);
        m_listView->InsertItem(i, artIDs[i], i);
    }
    
    // 自动调整列宽
    m_listView->SetColumnWidth(0, wxLIST_AUTOSIZE);
}