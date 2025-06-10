import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.CodeSource;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.ProtectionDomain;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.metal.MetalIconFactory;
import javax.swing.table.DefaultTableModel;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class FileManagerUI extends JFrame {
    private static final long serialVersionUID = 1L;
    private JTextField directoryTextField;
    private JProgressBar progressBar;
    private JList<String> fileList;
    private JLabel statusBar;
    private JButton packButton;
    private JButton unpackButton;
    private long startTime;
    // 存储配置信息的变量
    private boolean fileEncode = false; // 文件打包加密默认选中否
    private String fileEncoding = ""; 
    private boolean fileContentEncode = true; // 文件内容转码默认选中是
    private String contentEncoding = ""; 
    private boolean dynamicEncoding = true; // 动态生成编码默认选中是
    private String encodingType = "自动"; // 编码类型默认选中自动
    private int encodingLength = 1;
    private String lengthUnit = "M";
    private boolean unifiedSuffix = true;
    private boolean newDirOnUnpack = true;
    private String fnLoopEncoding = "";
    private String comment = "";
    private boolean fnLoopEncodeFlag = true;
    private String def_encry_pin = "密码示例-pWd_2025";
    int fileIdx;                 // 当前处理文件序号
    int fileCount;               // 需要处理文件总数
    long maxFileSize;            // 需要处理最大文件尺寸
    List<String> fileListData;   // 进度列表区显示内容清单
    Map<String, Integer> headFileNumMap;  
    JPasswordField encodingTextField = null;

    public FileManagerUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 设置图标
        byte[] iconBytes = java.util.Base64.getDecoder().decode(iconBase64Str);
        Image icon = Toolkit.getDefaultToolkit().createImage(iconBytes);
        setIconImage(icon);
        
        // 设置标题
        setTitle("文件管理工具【XO】");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(800, 500));
        
        // 创建菜单栏
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        
        // 文件菜单
        JMenu fileMenu = new JMenu("文件");
        JMenuItem directoryMenuItem = new JMenuItem("目录");
        JMenuItem packMenuItem = new JMenuItem("打包");
        JMenuItem unpackMenuItem = new JMenuItem("解压");
        JMenuItem transMenuItem = new JMenuItem("转码");
        JMenuItem configMenuItem = new JMenuItem("配置");
        JMenuItem viewCfgMenuItem = new JMenuItem("查看");
        viewCfgMenuItem.setToolTipText("查看当前配置信息");
        JMenuItem exitMenuItem = new JMenuItem("退出");
        fileMenu.add(directoryMenuItem);
        fileMenu.add(packMenuItem);
        fileMenu.add(unpackMenuItem);
        fileMenu.add(transMenuItem);
        fileMenu.add(new JSeparator());
        fileMenu.add(configMenuItem);
        fileMenu.add(viewCfgMenuItem);
        fileMenu.add(new JSeparator());
        fileMenu.add(exitMenuItem);
        menuBar.add(fileMenu);

        // 工具菜单
        JMenu toolMenu = new JMenu("工具");
        JMenuItem extractMenuItem = new JMenuItem("提取图片");
        extractMenuItem.setToolTipText("从PDF文件提取图片文件");
        JMenuItem cutpdfMenuItem = new JMenuItem("截取文档");
        cutpdfMenuItem.setToolTipText("从PDF文件截取部分页码内容");
        JMenuItem crawlingMenuItem = new JMenuItem("抓取文本");
        crawlingMenuItem.setToolTipText("从指定网址抓取文本文件");
        JMenuItem grabMenuItem = new JMenuItem("抓取图片");
        grabMenuItem.setToolTipText("从指定网址抓取图片文件");
        JMenuItem croppingMenuItem = new JMenuItem("裁剪图片");
        croppingMenuItem.setToolTipText("根据指定尺寸裁剪图片");
        JMenuItem m3u8MenuItem = new JMenuItem("M3U8");
        m3u8MenuItem.setToolTipText("根据m3u8下载ts文件");
        JMenuItem binaryMenuItem = new JMenuItem("字节查看");
        binaryMenuItem.setToolTipText("查看文件二进制格式内容");
        JMenuItem transFileMenuItem = new JMenuItem("文件转存");
        transFileMenuItem.setToolTipText("任意文件和文本文件相互转存（格式互转）");
        
        toolMenu.add(extractMenuItem);
        toolMenu.add(cutpdfMenuItem);
        toolMenu.add(crawlingMenuItem);
        toolMenu.add(grabMenuItem);
        toolMenu.add(croppingMenuItem);
        toolMenu.add(m3u8MenuItem);
        toolMenu.add(new JSeparator());
        toolMenu.add(binaryMenuItem);
        toolMenu.add(transFileMenuItem);
        menuBar.add(toolMenu);

        // 服务菜单
        JMenu svrMenu = new JMenu("服务");
        JMenuItem svrDeployMenuItem = new JMenuItem("部署");
        JMenuItem svrCfgMenuItem = new JMenuItem("配置");
        JMenuItem svrStartMenuItem = new JMenuItem("启动");
        JMenuItem svrStopMenuItem = new JMenuItem("停止");
        svrMenu.add(svrDeployMenuItem);
        svrMenu.add(svrCfgMenuItem);
        svrMenu.add(svrStartMenuItem);
        svrMenu.add(svrStopMenuItem);
        menuBar.add(svrMenu);

        // 帮助菜单
        JMenu helpMenu = new JMenu("帮助");
        JMenuItem manualMenuItem = new JMenuItem("提示");
        JMenuItem donateMenuItem = new JMenuItem("捐赠");
        JMenuItem aboutMenuItem = new JMenuItem("关于");
        helpMenu.add(manualMenuItem);
        helpMenu.add(donateMenuItem);
        helpMenu.add(aboutMenuItem);
        menuBar.add(helpMenu);

        // 按钮区
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        // 第一行
        JPanel firstRow = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;
        gbc.insets = new Insets(5, 10, 5, 10);

        JButton directorySelectButton = new JButton("目录选择");
        directorySelectButton.setToolTipText("选择需要文件打包或者存放解压文件的目录");
        directorySelectButton.setIcon(MetalIconFactory.getTreeFolderIcon());
        firstRow.add(directorySelectButton, gbc);
        gbc.weightx = 1;
        directoryTextField = new JTextField(20);
        firstRow.add(directoryTextField, gbc);
        gbc.weightx = 0;
        packButton = new JButton("打包");
        packButton.setToolTipText("将工作目录下的所有文件打包到指定压缩包");
        packButton.setIcon(MetalIconFactory.getTreeFloppyDriveIcon());
        firstRow.add(packButton, gbc);
        unpackButton = new JButton("解压");
        unpackButton.setToolTipText("将指定压缩包内的文件解压到工作目录");
        unpackButton.setIcon(MetalIconFactory.getFileChooserUpFolderIcon());
        firstRow.add(unpackButton, gbc);
        JButton configButton = new JButton("配置");
        configButton.setToolTipText("设置打包或解压时的运行参数");
        configButton.setIcon(MetalIconFactory.getTreeHardDriveIcon());
        firstRow.add(configButton, gbc);

        // 第二行
        JPanel secondRow = new JPanel(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.insets = new Insets(2, 10, 0, 10);
        progressBar = new JProgressBar(0, 100);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(progressBar.getPreferredSize().width, progressBar.getPreferredSize().height / 2));
        secondRow.add(progressBar, gbc);

        // 在第一行和第二行之间添加垂直空隙
        buttonPanel.add(Box.createVerticalStrut(5));
        buttonPanel.add(firstRow);
        buttonPanel.add(Box.createVerticalStrut(5));
        buttonPanel.add(secondRow);

        // 列表区
        fileList = new JList<>();
        JScrollPane scrollPane = new JScrollPane(fileList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 状态栏
        statusBar = new JLabel("就绪");
        statusBar.setBorder(BorderFactory.createLoweredBevelBorder());
        statusBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // 添加到主界面
        add(buttonPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        // 添加鼠标双击事件监听器
        directoryTextField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    directoryTextField.setText("D:\\FileManagerTools\\unpack");
                }
            }
        });

        // 为 fileList(JList) 添加鼠标监听器
        fileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // 检查是否为双击事件
                if (e.getClickCount() == 2) {
                    // 获取当前选中的索引
                    int selectedIndex = fileList.locationToIndex(e.getPoint());
                    if (selectedIndex != -1) {
                        // 获取对应行的内容
                        String selectedContent = fileList.getModel().getElementAt(selectedIndex);
                        // 显示选中的文件信息
                        //JOptionPane.showMessageDialog(null, selectedContent);
                        directoryTextField.setText(selectedContent);
                    }
                }
            }
        });

        // 为打包按钮添加事件监听器
        packButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //performOperation("正在打包...", "打包");
                packOperation();
            }
        });

        // 为解压按钮添加事件监听器
        unpackButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //performOperation("正在解压...", "解压");
                unPackOperation();
            }
        });

        // 为文件菜单中的打包菜单项添加事件监听器
        packMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //performOperation("正在打包...", "打包");
                packOperation();
            }
        });

        // 为文件菜单中的解压菜单项添加事件监听器
        unpackMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //performOperation("正在解压...", "解压");
                unPackOperation();
            }
        });

        // 为文件菜单中的转码菜单项添加事件监听器
        transMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 在新的线程中执行长时间任务
                new Thread(() -> {
                    //performOperation("正在转码...", "转码");
                    transOperation();
                }).start();
            }
        });

        // 为文件菜单中的目录菜单项添加事件监听器
        directoryMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showDirectoryChooser();
            }
        });

        // 为目录选择按钮添加事件监听器
        directorySelectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showDirectoryChooser();
            }
        });

        // 为配置按钮添加事件监听器
        configButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showConfigDialog();
            }
        });

        // 为文件菜单中的配置菜单项添加事件监听器
        configMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showConfigDialog();
            }
        });
        
        // 为文件菜单中的查看菜单项添加事件监听器
        viewCfgMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int idx = 1;
                fileListData = new ArrayList<>();
                fileListData.add((idx++) + " 文件内容转码: " + fileContentEncode);
                fileListData.add((idx++) + " 动态生成编码: " + dynamicEncoding);
                fileListData.add((idx++) + " 编码生成方式: " + encodingType);
                fileListData.add((idx++) + " 完整编码长度: " + encodingLength );
                fileListData.add((idx++) + " 编码长度单位: " + lengthUnit );
                fileListData.add((idx++) + " 统一文件后缀: " + unifiedSuffix );
                fileListData.add((idx++) + " 解压非空目录: " + newDirOnUnpack );
                fileListData.add((idx++) + " 名称循环编码: " + fnLoopEncodeFlag );
                fileList.setListData(fileListData.toArray(new String[0]));
                // 确保最后一行显示
                fileList.ensureIndexIsVisible(fileListData.size() - 1);
                revalidate();
                repaint();
            }
        });

        // 为文件菜单中的退出菜单项添加事件监听器
        exitMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exitProgram();
            }
        });
        
        // 为工具菜单中的透视菜单项添加事件监听器
        binaryMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showOpenDialog(FileManagerUI.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    FileBinaryViewer fbv = new FileBinaryViewer();
                    fbv.selectedFileBinaryViewer = fileChooser.getSelectedFile();;
                    fbv.fileOffsetBinaryViewer = 0;
                    fbv.displayBinaryContentDialog();
                }
            }
        });
        
        // 为工具菜单中的提取图片菜单项添加事件监听器
        extractMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                extractOperation();
            }
        });

        // 为工具菜单中的截取文档菜单项添加事件监听器
        cutpdfMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cutpdfOperation();
            }
        });

        // 为工具菜单中的抓取菜单项添加事件监听器
        grabMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(() -> {
                    grabOperation();
                }).start();
            }
        });

        // 为工具菜单中的抓取文本菜单项添加事件监听器
        crawlingMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                (new WebScraper()).displayWebScraperDialog();
            }
        });

        // 为工具菜单中的裁剪图片菜单项添加事件监听器
        croppingMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(() -> {
                    croppingOperation();
                }).start();
            }
        });
        
        // 为工具菜单中的M3u8菜单项添加事件监听器
        m3u8MenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                m3u8Operation();
            }
        });
        
        // 为工具菜单中的转存菜单项添加事件监听器
        transFileMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                (new FileTxtInterTrans()).displayTransFileDialog();
            }
        });

        // 为服务菜单中的部署菜单项添加事件监听器
        svrDeployMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                (new NginxSvr()).deploySvr();
            }
         });

        // 为服务菜单中的配置菜单项添加事件监听器
        svrCfgMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                (new NginxSvr()).showConfigDialog(FileManagerUI.this);
            }
         });

        // 为服务菜单中的启动菜单项添加事件监听器
        svrStartMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    (new NginxSvr()).start();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
         });

        // 为服务菜单中的停止菜单项添加事件监听器
        svrStopMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    (new NginxSvr()).stop();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
         });

        // 为帮助菜单中的提示菜单项添加事件监听器
        manualMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null, "building... ", "提示", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // 为帮助菜单中的捐赠菜单项添加事件监听器
        donateMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //System.out.println(imageToBase64("E:/tmp/1.jpg"));
                //System.out.println(imageToBase64("E:/tmp/2.jpg"));
                try {
                    String base64ImageAlipay = "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";
                    String base64ImageWechat = "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";
                    
                    // 将 Base64 字符串解码为 BufferedImage 对象
                    BufferedImage imageAli = decodeBase64ToImage(base64ImageAlipay);
                    BufferedImage imageWechat = decodeBase64ToImage(base64ImageWechat);

                    // 缩放图片
                    Image scaledImageAli = imageAli.getScaledInstance(180, 180, Image.SCALE_SMOOTH);
                    Image scaledImageWechat = imageWechat.getScaledInstance(180, 180, Image.SCALE_SMOOTH);

                    // 创建 JLabel 来显示图片
                    JLabel labelAli = new JLabel(new ImageIcon(scaledImageAli));
                    JLabel labelWechat = new JLabel(new ImageIcon(scaledImageWechat));

                    // 创建图片下方的文字说明标签
                    JLabel textLabelAli = new JLabel("支付宝");
                    textLabelAli.setHorizontalAlignment(SwingConstants.CENTER);
                    JLabel textLabelWechat = new JLabel("微信");
                    textLabelWechat.setHorizontalAlignment(SwingConstants.CENTER);

                    // 创建面板来组合图片和对应的文字说明
                    JPanel panelAli = new JPanel(new BorderLayout());
                    panelAli.add(labelAli, BorderLayout.CENTER);
                    panelAli.add(textLabelAli, BorderLayout.SOUTH);

                    JPanel panelWechat = new JPanel(new BorderLayout());
                    panelWechat.add(labelWechat, BorderLayout.CENTER);
                    panelWechat.add(textLabelWechat, BorderLayout.SOUTH);

                    JPanel panelBlank = new JPanel();
                    panelBlank.setBorder(BorderFactory.createEmptyBorder(0, 50, 0, 0)); // 设置边界，创建空白效果
                    
                    // 创建一个主面板来放置两个组合面板，并设置中间的空白
                    JPanel mainImagePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0)); // 20 像素的水平间距
                    mainImagePanel.add(panelAli);
                    mainImagePanel.add(panelBlank);
                    mainImagePanel.add(panelWechat);

                    // 弹出对话框显示图片
                    JOptionPane.showMessageDialog(null, mainImagePanel, "捐赠二维码", JOptionPane.PLAIN_MESSAGE);
                }catch (IOException err) {
                    err.printStackTrace();
                }
            }
        });

        // 为帮助菜单中的关于菜单项添加事件监听器
        aboutMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAboutDialog();
            }
        });

        // 窗口大小调整监听器
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                int newWidth = getWidth();
                int newHeight = getHeight();
                scrollPane.setPreferredSize(new Dimension(newWidth - 20, newHeight - buttonPanel.getPreferredSize().height - statusBar.getPreferredSize().height - 20));
                directoryTextField.setPreferredSize(new Dimension((int) ((newWidth - 20) * 0.6), directoryTextField.getPreferredSize().height));
                progressBar.setPreferredSize(new Dimension(newWidth - 20, progressBar.getPreferredSize().height));
                revalidate();
                repaint();
            }
        });

        // 将窗口设置为屏幕中央显示
        setLocationRelativeTo(null);

        pack();
        setVisible(true);
    }

        /** 将 Base64 字符串解码为 BufferedImage 对象 */
    private BufferedImage decodeBase64ToImage(String base64) throws IOException {
        // 移除可能存在的 Base64 数据前缀（例如 "data:image/jpeg;base64,"）
        if (base64.startsWith("data:")) {
            int commaIndex = base64.indexOf(',');
            if (commaIndex != -1) {
                base64 = base64.substring(commaIndex + 1);
            }
        }
        // 解码 Base64 字符串为字节数组
        byte[] imageBytes = Base64.getDecoder().decode(base64);
        // 将字节数组转换为 ByteArrayInputStream
        ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
        // 从输入流中读取图片数据并返回 BufferedImage 对象
        return ImageIO.read(bis);
    }
    
    /** 将图片转换为 Base64 字符串 */
    public String imageToBase64(String imagePath) {
        File file = new File(imagePath);
        try (FileInputStream fis = new FileInputStream(file)) {
            // 创建一个与文件大小相同的字节数组
            byte[] imageBytes = new byte[(int) file.length()];
            // 将文件内容读取到字节数组中
            fis.read(imageBytes);
            // 使用 Base64 编码器将字节数组编码为 Base64 字符串
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void extractOperation() {
        String dir = directoryTextField.getText();
        if ("".equals(dir)) {
            JOptionPane.showMessageDialog(this, "请先选择提取图片的保存目录。", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String pureDir = getPureDir(dir);
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择提取的 pdf 文件");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".pdf");
            }

            @Override
            public String getDescription() {
                return "PDF 文件 (*.pdf)";
            }
        });
        int result = fileChooser.showOpenDialog(FileManagerUI.this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File pdf = fileChooser.getSelectedFile();
            String fn = pdf.getName();
            fn = fn.substring(0, fn.length()-4);
            //* // 使用PDFBox提取图片
            try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.Loader.loadPDF(pdf)) {
                int idx=1;
                for (org.apache.pdfbox.pdmodel.PDPage page : document.getDocumentCatalog().getPages()) {
                    org.apache.pdfbox.pdmodel.PDResources resources = page.getResources();
                    for (org.apache.pdfbox.cos.COSName name : resources.getXObjectNames()) {
                        org.apache.pdfbox.pdmodel.graphics.PDXObject obj = resources.getXObject(name);
                        if(obj instanceof org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject) {
                            org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject img = (org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject)obj;
                            java.awt.image.BufferedImage bim = img.getImage();
                            int type = bim.getType();   // 1:BufferedImage.TYPE_INT_RGB  4:BufferedImage.TYPE_INT_BGR
                            System.out.println("Image " + idx + " Type: " + type);
                            String index = idx<100?String.format("%03d", idx):idx+"";
                            if(type==2) {
                                File filePng = new File(pureDir + "/" + fn + "-ei-" + index + ".png");
                                javax.imageio.ImageIO.write(bim, "PNG", filePng);
                            }else {
                                File fileJpg = new File(pureDir + "/" + fn + "-ei-" + index + ".jpg");
                                javax.imageio.ImageIO.write(bim, "JPEG", fileJpg);
                            }
                            idx++;
                        }
                    }
                }
                JOptionPane.showMessageDialog(this, "提取图片数量：" + idx, "提示", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                e.printStackTrace();
            }  // */
        }
    }

    // 解析页码范围字符串
    private static List<Integer> parsePageRange(String rangeStr) {
        List<Integer> pages = new ArrayList<>();
        String[] parts = rangeStr.split(",");

        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;

            if (part.contains("-")) {
                // 处理范围(如1-3)
                String[] range = part.split("-");
                if (range.length != 2) {
                    throw new IllegalArgumentException("无效的范围格式: '" + part + "'. 正确格式如 '1-3'");
                }
                try {
                    int start = Integer.parseInt(range[0].trim());
                    int end = Integer.parseInt(range[1].trim());
                    if (start > end) {
                        throw new IllegalArgumentException("起始页码不能大于结束页码: '" + part + "'");
                    }
                    for (int i = start; i <= end; i++) {
                        if (!pages.contains(i)) {
                            pages.add(i);
                        }
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("无效的页码: '" + part + "'. 必须为数字");
                }
            } else {
                // 处理单个页码
                try {
                    int page = Integer.parseInt(part);
                    if (!pages.contains(page)) {
                        pages.add(page);
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("无效的页码: '" + part + "'. 必须为数字");
                }
            }
        }

        // 对页码进行排序
        pages.sort(Integer::compareTo);
        return pages;
    }

    public void cutpdfOperation() {
        String pages = directoryTextField.getText();
        if ("".equals(pages)) {
            JOptionPane.showMessageDialog(this, "请先输入需要保存的页码范围(如: 1-3,5,7-9)。", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择截取的 pdf 文件");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".pdf");
            }

            @Override
            public String getDescription() {
                return "PDF 文件 (*.pdf)";
            }
        });
        int result = fileChooser.showOpenDialog(FileManagerUI.this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File pdf = fileChooser.getSelectedFile();

            String outputFilePath = pdf.getParent() + "/extracted_pages.pdf"; // 输出文件路径
            String extractResult = extractPages(pdf.getAbsolutePath(), outputFilePath, pages);
            JOptionPane.showMessageDialog(this, "PDF文档截取完成：" + extractResult, "提示", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // 提取PDF页面
    public String extractPages(String inputFilePath, String outputFilePath,  String pageRange) {
        // 解析页码范围
        List<Integer> pagesToExtract;
        try {
            pagesToExtract = parsePageRange(pageRange);
            if (pagesToExtract.isEmpty()) {
                return "错误: 无效的页码范围 - 未指定任何有效页码。" + pageRange;
            }
        } catch (IllegalArgumentException e) {
            return "错误: " + e.getMessage();
        }

        try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(new File(inputFilePath))) {
            int totalPages = document.getNumberOfPages();
            
            // 验证页码是否有效
            for (int page : pagesToExtract) {
                if (page < 1) {
                    return "错误: 页码不能小于1";
                }
                if (page > totalPages) {
                    return "错误: 页码 " + page + " 超出文档范围(文档共 " + totalPages + " 页)";
                }
            }

            // 提取指定页面
            Splitter splitter = new Splitter();
            List<PDDocument> splitDocuments = new ArrayList<>();

            for (int page : pagesToExtract) {
                splitter.setStartPage(page);
                splitter.setEndPage(page);
                splitter.setSplitAtPage(1);
                
                List<PDDocument> singlePageDoc = splitter.split(document);
                if (!singlePageDoc.isEmpty()) {
                    splitDocuments.add(singlePageDoc.get(0));
                }
            }

            if (splitDocuments.isEmpty()) {
                return "错误: 未能提取到任何页面, 请检查页码范围是否正确。";
            }

            // 合并提取的页面
            PDFMergerUtility merger = new PDFMergerUtility();
            for (PDDocument splitDoc : splitDocuments) {
                // Save the PDDocument to a temporary file
                File tempFile = File.createTempFile("tempDoc", ".pdf");
                splitDoc.save(tempFile);
                splitDoc.close(); // Close the document after saving
                merger.addSource(tempFile.getAbsolutePath());
            }
            merger.setDestinationFileName(outputFilePath);
            merger.mergeDocuments(null);

            // 关闭分割后的文档
            for (PDDocument splitDoc : splitDocuments) {
                splitDoc.close();
            }
            
            return "成功: 提取的页面已保存到 " + outputFilePath + "。";
        } catch (IOException e) {
            return "错误: 处理PDF文件时出错 - " + e.getMessage();
        }
    }

    public void croppingOperation() {
        String path = directoryTextField.getText();
        if ("".equals(path)) {
            JOptionPane.showMessageDialog(null, "请先选择需要裁剪图片的所在目录。", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        fileListData = new ArrayList<String>();
        
        String[] supportedFormats = ImageIO.getReaderFileSuffixes();
        showProgress("支持的图片格式: " + Arrays.toString(supportedFormats));
        try {
            String cutSize = "";
            if(path.indexOf("?")>0) {
                cutSize = path.substring(path.indexOf("?")+1);
                path = path.substring(0,path.indexOf("?"));
            }
            File dir = new File(path);
            String[] files = dir.list();
            int idx=1, cutIdx = 0, fileCount = 0;
            
            if(files!=null && files.length>0) {
                (new File(path + "/split/")).mkdirs();
                for(String file:files) {
                    boolean cuted = false;
                    File input = new File(path+ "/" + file); // 可以是jpg, png, bmp等
                    if(input.isFile()) {
                        fileCount++;
                        String formatName = file.substring(file.lastIndexOf(".") + 1);
                        if(Arrays.asList(supportedFormats).contains(formatName)) {
                            BufferedImage img = ImageIO.read(input);
                            int width = img.getWidth();
                            int height = img.getHeight();
                            int cut = width/2;  //380
                            BufferedImage area = img.getSubimage(cut, 0, width - cut, height);
                            if(!"".equals(cutSize)) {
                                String[] cs = cutSize.split(",");
                                if(cs.length<4) {
                                    cut = Integer.parseInt(cs[0]);  
                                    if(cut<0) { 
                                        area = img.getSubimage(width + cut, 0, -1*cut, height);     //保留右侧指定宽度
                                    } else {     
                                        area = img.getSubimage(0, 0, cut, height);                //保留左侧侧指定宽度
                                    }
                                }else {
                                    area = img.getSubimage(Integer.parseInt(cs[0]), Integer.parseInt(cs[1]), Integer.parseInt(cs[2]) , Integer.parseInt(cs[3]));
                                }
                            }

                            // ImageIO.write(area, formatName, new File(path + "/split/" + file));   // 直接保存会损失图片质量
                            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
                            if (!writers.hasNext()) {
                                throw new IllegalStateException("没有找到JPEG编码器");
                            }

                            ImageWriter writer = writers.next();
                            try {
                                // 设置JPEG编码参数
                                JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
                                jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                                jpegParams.setCompressionQuality(0.95f); // 设置质量 (0.0-1.0)
                                jpegParams.setOptimizeHuffmanTables(true); // 启用霍夫曼表优化
                                jpegParams.setProgressiveMode(ImageWriteParam.MODE_DEFAULT); // 渐进式JPEG
                                IIOMetadata metadata = null;
                                metadata = writer.getDefaultImageMetadata(new ImageTypeSpecifier(area), jpegParams); // 设置DPI信息（如果原图有）
                                // 创建输出流
                                try (ImageOutputStream out = ImageIO.createImageOutputStream(new File(path + "/split/" + file))) {
                                    writer.setOutput(out);
                                    writer.write(null, new IIOImage(area, null, metadata), jpegParams);
                                }
                            } finally {
                                writer.dispose();
                            }

                            cuted = true;
                            cutIdx++;
                        }
                    }
                    showProgress((idx++) + (cuted?" 完成 ":" 忽略 ") + " : " + path+ "/" + file);
                }
            }
            showProgress("split finished : " + cutIdx + "/" + fileCount);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void grabOperation() {
        String dir = directoryTextField.getText();
        if ("".equals(dir)) {
            JOptionPane.showMessageDialog(this, "请先选择抓取图片的保存目录。", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // 获取网址或源码： String url = JOptionPane.showInputDialog(this, "请输入需要抓取图片的网址：", "https://www.txcb.com/yszp/page/2");
        String inputTxt = "";
        JTextArea textArea = new JTextArea(10, 80);
        JScrollPane scrollPane = new JScrollPane(textArea);
        
        // 显示带文本域的对话框
        int result = JOptionPane.showConfirmDialog(
            this, 
            scrollPane, 
            "请输入需要抓取图片的网址 或 直接输入该网址的网页源代码", 
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        if (result == JOptionPane.OK_OPTION) {
            inputTxt = textArea.getText();
            inputTxt = inputTxt.trim();
        }
        
        // 输入内容为空直接返回
        if(inputTxt==null || "".equals(inputTxt)) {
            statusBar.setText("输入内容为空。");
            return;
        }
        
        // 获取网页内容
        try {
            String pureDir = getPureDir(dir);
            if(inputTxt.startsWith("http")) {
                String[] urls = inputTxt.split("\\r?\\n|\\r");  // 兼容所有换行符的分割方式
                for(String url:urls) {
                    String htmlContent = getJsoupHtml(url);  // getJsoupHtml   getHtml
                    System.out.println("\n\n网页内容: \n" + htmlContent);
        statusBar.setText(url);
                    getHtmlImg(url, htmlContent, pureDir);
                }
            } else if(inputTxt.startsWith("<"))  {
                System.out.println("\n\n网页内容: \n" + inputTxt);
                getHtmlImg("", inputTxt, pureDir);
            } else {
                String[] urls = inputTxt.split("\\r?\\n|\\r");  // 兼容所有换行符的分割方式
                int idx=1;
                fileListData = new ArrayList<>();
                for(String url:urls) {
                    String[] nu = url.split("=");
                    boolean downException = getNetImg(nu[1], pureDir, nu[0]);
                    showProgress((idx++) + (downException?" 失败 ":" 成功 ") + " : " + url);
                }
            }
            showProgress("Download complete: " + pureDir);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, e.getMessage(), "异常", JOptionPane.ERROR_MESSAGE);
        }
    }

    public String getHtml(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        // 自动处理重定向
        conn.setInstanceFollowRedirects(true);

        // 获取响应编码（从Content-Type中提取）
        String contentType = conn.getContentType();
        String charset = "UTF-8"; // 默认编码
        if (contentType != null && contentType.contains("charset=")) {
            charset = contentType.split("charset=")[1].trim();
        }

        // 处理压缩（如gzip）
        InputStream inputStream = conn.getInputStream();
        if ("gzip".equalsIgnoreCase(conn.getContentEncoding())) {
            inputStream = new GZIPInputStream(inputStream);
        }

        // 读取内容
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, charset))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
            return response.toString();
        } finally {
            conn.disconnect();
        }
    }

    public String getJsoupHtml(String urlStr) throws IOException {
        Document doc = Jsoup.connect(urlStr).get();
        String html = doc.html(); // 自动处理编码
        return html;
    }
        
    public void getHtmlImg(String url, String htmlContent, String pureDir) {
        String imageUrlSrc = "";
        String imageDataSrc = "";
        try {
            // 确定协议
            String protocol = "";
            if(!url.equals("")) {
                URL urlPage = new URL(url);
                protocol = urlPage.getProtocol();
            }else {
                // 正则表达式匹配HTML注释（直接输入网页源代码时，可以将网页protocol放在第一行的注释中）
                Pattern pattern = Pattern.compile("<!--(.*?)-->", Pattern.DOTALL);
                Matcher matcher = pattern.matcher(htmlContent);
                
                // 查找所有匹配的注释
                if (matcher.find()) {
                    protocol = matcher.group(1).trim();
                    System.out.println("找到第一个注释: " + comment);
                }
            }

            // 解析HTML内容
            Document doc = Jsoup.parse(htmlContent);
            Elements imgTags = doc.select("img");
            System.out.println("\n\n图片对象: " + imgTags.size());

            List<String> imageUrls = new ArrayList<>();
            for (Element imgTag : imgTags) {
                imageUrlSrc = imgTag.attr("src");
                imageDataSrc = imgTag.attr("data-src");
                System.out.println("img src: " + imageUrlSrc);
                System.out.println("img data-src: " + imageDataSrc);
                System.out.println();

                if(imageUrlSrc.startsWith("//") && !protocol.equals("")){
                    imageUrlSrc = protocol + ":" + imageUrlSrc;
                }

                if(imageDataSrc.startsWith("//") && !protocol.equals("")){
                    imageDataSrc = protocol + ":" + imageDataSrc;
                }

                if(!"".equals(imageUrlSrc)) {
                    imageUrls.add(imageUrlSrc);
                }

                if(!"".equals(imageDataSrc)) {
                    imageUrls.add(imageDataSrc);
                }
            }
            
            // 下载图片
            int imageCount = imageUrls.size();
            if(imageCount>0) {
            System.out.println("\n\n图片地址: " + imageCount);

            // 下载图片
            int idx = 1;
            fileListData = new ArrayList<>();
            boolean downException = false;
            for (int i=0; i<imageCount; i++) {
                imageUrlSrc = imageUrls.get(i);
                System.out.println(String.format("正在处理(%d/%d): %s", i + 1, imageCount, imageUrlSrc));

                    // 下载
                    downException = getNetImg(imageUrlSrc, pureDir, "");
                    
                    // 提示
                    showProgress((idx++) + (downException?" 失败 ":" 成功 ") + " : " + imageUrlSrc);
                }
                statusBar.setText(url + " → " + pureDir + " (" + imageUrls.size() + " images)");
            }else {
                //JOptionPane.showMessageDialog(this, "未找到图片链接。", "提示", JOptionPane.INFORMATION_MESSAGE);
                statusBar.setText(url + " → 未找到图片链接。");
            }

        } catch (IOException e) {
            System.out.println("当前图片URL: " + imageUrlSrc);
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, e.getMessage(), "异常", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public boolean getNetImg(String imageUrlSrc, String pureDir, String name) throws IOException {
        boolean downException = false;
        URL urlImg = new URL(imageUrlSrc);
        String fileName = name;
        if("".equals(fileName)) {
            fileName = urlImg.getFile().substring(urlImg.getFile().lastIndexOf("/") + 1);
                // 定义正则表达式，匹配非法文件名字符（在Windows中）
                String invalidCharsRegex = "[\\\\/:*?\"<>|\\r\\n]+";
                // 替换非法字符为空字符串
                fileName = fileName.replaceAll(invalidCharsRegex, "");
                fileName = fileName.replaceAll("@.*", ""); // 删除@及其后所有字符
        }
                File saveFile = new File(pureDir + "/" + fileName);
    
                try (InputStream inImg = urlImg.openStream();
                     FileOutputStream out = new FileOutputStream(saveFile)) {
    
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inImg.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    downException = true;
                }
        return downException;
    }

    public void m3u8Operation() {
        String dir = directoryTextField.getText();
        if ("".equals(dir)) {
            JOptionPane.showMessageDialog(this, "请先选择ts文件的保存目录。", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // 输入网址
        String dufaultUrl = "https://www.xxx.com/fileh.php?id=2589&server=1&hash=fda88359ff68bed36152&expire=1739892065&file=/480p.m3u8";
        String url = JOptionPane.showInputDialog(this, "请输入m3u8文件的网址：", dufaultUrl);
        if(url==null || "".equals(url)) {
            return;
        }
        String dateTime = getCurrentDateTime();
        statusBar.setText(url + " (Start at: " + dateTime + ")");
        
        // 下载文件
        try {
            (new M3u8Downloader()).m3u8Down(url, dir);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, e.getMessage(), "异常", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void packOperation() {
        // 校验打包目录不能为空
        if ("".equals(directoryTextField.getText())) {
            JOptionPane.showMessageDialog(this, "请先选择打包目录。", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("D:/FileManagerTools")); 
        fileChooser.setDialogTitle("选择保存的 zip 文件");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".zip");
            }

            @Override
            public String getDescription() {
                return "ZIP 文件 (*.zip)";
            }
        });
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            // 在新的线程中执行长时间任务
            new Thread(() -> {
                String zipFilePath = fileChooser.getSelectedFile().getAbsolutePath();
                if (!zipFilePath.toLowerCase().endsWith(".zip")) {
                    zipFilePath += ".zip";
                }
                pack(directoryTextField.getText(), zipFilePath);
            }).start();
        }
    }

    public void unPackOperation() {
        // 校验解压目录不能为空
        if ("".equals(directoryTextField.getText())) {
            JOptionPane.showMessageDialog(this, "请先选择解压目录。", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("D:/FileManagerTools")); 
        fileChooser.setDialogTitle("选择解压的 zip 文件");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".zip");
            }

            @Override
            public String getDescription() {
                return "ZIP 文件 (*.zip)";
            }
        });
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            // 在新的线程中执行长时间任务
            new Thread(() -> {
                String zipFilePath = fileChooser.getSelectedFile().getAbsolutePath();
                unpack(zipFilePath, directoryTextField.getText());
            }).start();
        }
    }

    public void transOperation() {
        // 校验转码目录不能为空
        if ("".equals(directoryTextField.getText())) {
            JOptionPane.showMessageDialog(this, "请先选择转码目录。", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String fileDir = directoryTextField.getText();
        File[] files = new File(fileDir).listFiles();
        if(files == null || files.length == 0) {
            JOptionPane.showMessageDialog(this, "目录下没有文件。", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int idx = 0;
        fileListData = new ArrayList<>();
        progressBar.setVisible(true);

        contentEncoding = contentEncoding.equals("")?def_encry_pin:contentEncoding;
        int contentEncodLen = contentEncoding.length();
        showProgress((idx++) + "/" + files.length + " : " + (contentEncodLen>50?contentEncoding.substring(0,50)+"..." + contentEncodLen:contentEncoding));
        StringBuffer buf = new StringBuffer();
        for(int i=0; i<=1000; i++){
            String key = contentEncoding + i;
            String e = encrypBySha512(key);   // encrypBySha512  encrypByMd5
            buf.append(e).append(e);
        }
        String transCode = buf.toString();

        (new File(fileDir + "/trans/")).mkdirs();
        for(File file:files){
            if(file.isFile()){
                String fileName = file.getName();
                showProgress((idx++) + "/" + files.length + " : " + fileDir+ "/" + fileName);
                transFile(fileDir + "/" + fileName, fileDir + "/trans/" + fileName, transCode);
            }
        }
        progressBar.setVisible(false);
    }

    /** 单个文件转码 */
    private void transFile(String inputPath, String outputPath, String transCode) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(inputPath);
            FileChannel inChannel = fis.getChannel();
            fos = new FileOutputStream(outputPath);
            FileChannel outChannel = fos.getChannel();

            long fileSize = inChannel.size();
            ByteBuffer buffer = ByteBuffer.allocateDirect(8 * 1024 * 1024); // 8MB
            long bytesProcessed = 0;
            int bytesRead;

            int encryLen = transCode.length();
            while ((bytesRead = inChannel.read(buffer)) != -1) {
                buffer.flip();

                // 处理当前缓冲区的字节
                for (int i = 0; i < bytesRead; i++) {
                    buffer.put(i, (byte) ~buffer.get(i));
                    char c = transCode.charAt(i % encryLen);
                    int ascii = (int)c;
                    buffer.put(i, (byte) (buffer.get(i) ^ ascii));
                }

                outChannel.write(buffer);
                buffer.clear();

                // 更新进度
                bytesProcessed += bytesRead;
                long progress = bytesProcessed * 100 / fileSize;
                progressBar.setValue((int) progress);
                printProgress(bytesProcessed, fileSize);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if (fos != null) fos.close();
                if (fis != null) fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

        /**
     * 打印处理进度
     */
    private static void printProgress(long processed, long total) {
        if (total > 0) {
            double percent = (double) processed / total * 100;
            System.out.printf("Progress: %s/%s (%.2f%%)%n", formatFileSize(processed), formatFileSize(total), percent);
        }
    }

    /**
     * 格式化文件大小显示
     */
    private static String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }

    public void performOperation(final String operationInProgress, final String operationName) {
        startTime = System.currentTimeMillis();
        statusBar.setText(operationInProgress);
        progressBar.setVisible(true);
        revalidate();
        repaint();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    List<String> fileListData = new ArrayList<>();
                    Random random = new Random();
                    for (int i = 0; i <= 100; i++) {
                        final int progress = i;
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setValue(progress);
                                // 随机添加信息到列表框
                                fileListData.add("File " + random.nextInt(1000));
                                fileList.setListData(fileListData.toArray(new String[0]));
                                // 确保最后一行显示
                                fileList.ensureIndexIsVisible(fileListData.size() - 1);
                            }
                        });
                        Thread.sleep(50);
                    }
                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;
                    double seconds = TimeUnit.MILLISECONDS.toSeconds(duration);
                    statusBar.setText("就绪 耗时" + seconds + "秒");
                    progressBar.setVisible(false);
                    revalidate();
                    repaint();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }).start();
    }

    private void showConfigDialog() {
        final JDialog configDialog = new JDialog(this, "配置", true); // 创建 JDialog 并设置为模态
        configDialog.setLayout(new GridLayout(5, 1));
        configDialog.setResizable(false);
        configDialog.setSize(400, 300);

        // 第一行
        JPanel headRowConfig = new JPanel();
        headRowConfig.setLayout(new FlowLayout(FlowLayout.LEFT));
        JRadioButton fileEncodeYes = new JRadioButton("是");
        JRadioButton fileEncodeNo = new JRadioButton("否");
        final JPasswordField encodingTxtField = new JPasswordField(18);
        ButtonGroup fileEncodeGroup = new ButtonGroup();
        fileEncodeGroup.add(fileEncodeYes);
        fileEncodeGroup.add(fileEncodeNo);
        headRowConfig.add(new JLabel("文件打包加密"));
        headRowConfig.add(fileEncodeYes);
        headRowConfig.add(fileEncodeNo);
        headRowConfig.add(encodingTxtField);
        fileEncodeYes.setSelected(fileEncode);
        fileEncodeNo.setSelected(!fileEncode);
        encodingTxtField.setText(fileEncoding);
        encodingTxtField.setVisible(fileEncode);
        fileEncodeYes.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                encodingTxtField.setVisible(true);
                configDialog.revalidate();
                configDialog.repaint();
            }
        });
        fileEncodeNo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                encodingTxtField.setVisible(false);
                configDialog.revalidate();
                configDialog.repaint();
            }
        });
        // 添加鼠标双击事件监听器
        encodingTxtField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    encodingTxtField.setText("Secure-Password-159");
                }
            }
        });

        // 第二行
        JPanel firstRowConfig = new JPanel();
        firstRowConfig.setLayout(new FlowLayout(FlowLayout.LEFT));
        JRadioButton fileContentEncodeYes = new JRadioButton("是");
        JRadioButton fileContentEncodeNo = new JRadioButton("否");
        encodingTextField = new JPasswordField(18);
        ButtonGroup fileContentEncodeGroup = new ButtonGroup();
        fileContentEncodeGroup.add(fileContentEncodeYes);
        fileContentEncodeGroup.add(fileContentEncodeNo);
        firstRowConfig.add(new JLabel("文件内容转码"));
        firstRowConfig.add(fileContentEncodeYes);
        firstRowConfig.add(fileContentEncodeNo);
        firstRowConfig.add(encodingTextField);
        JButton longTxtButton = new JButton("…");
        longTxtButton.setPreferredSize(new Dimension(25, 20)); // 设置宽度和高度
        longTxtButton.setToolTipText("修改长文本文件中个别内容作为编码");
        firstRowConfig.add(longTxtButton);
        fileContentEncodeYes.setSelected(fileContentEncode);
        fileContentEncodeNo.setSelected(!fileContentEncode);
        encodingTextField.setText(contentEncoding);
        encodingTextField.setVisible(fileContentEncode);
        longTxtButton.addActionListener(new ActionListener() {
            @SuppressWarnings("deprecation")
            @Override
            public void actionPerformed(ActionEvent e) {
                (new LongTxtCoder()).showDialog(encodingTextField.getText());
            }
        });
        fileContentEncodeYes.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                encodingTextField.setVisible(true);
                configDialog.revalidate();
                configDialog.repaint();
                longTxtButton.setVisible(true);
            }
        });
        fileContentEncodeNo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                encodingTextField.setVisible(false);
                configDialog.revalidate();
                configDialog.repaint();
                longTxtButton.setVisible(false);
            }
        });
        // 添加鼠标双击事件监听器
        encodingTextField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    encodingTextField.setText(def_encry_pin);
                }
            }
        });

        // 第三行
        JPanel secondRowConfig = new JPanel();
        secondRowConfig.setLayout(new FlowLayout(FlowLayout.LEFT));
        JRadioButton dynamicEncodingYes = new JRadioButton("是");
        JRadioButton dynamicEncodingNo = new JRadioButton("否");
        JComboBox<String> encodingTypeComboBox = new JComboBox<>(new String[]{"自动", "限高", "手工"});
        final JTextField encodingLengthTextField = new JTextField(6);
        final JComboBox<String> lengthUnitComboBox = new JComboBox<>(new String[]{"M", "十M", "百M", "百K"});
        ButtonGroup dynamicEncodingGroup = new ButtonGroup();
        dynamicEncodingGroup.add(dynamicEncodingYes);
        dynamicEncodingGroup.add(dynamicEncodingNo);
        secondRowConfig.add(new JLabel("动态生成编码"));
        secondRowConfig.add(dynamicEncodingYes);
        secondRowConfig.add(dynamicEncodingNo);
        secondRowConfig.add(encodingTypeComboBox);
        secondRowConfig.add(encodingLengthTextField);
        secondRowConfig.add(lengthUnitComboBox);
        dynamicEncodingYes.setSelected(dynamicEncoding);
        dynamicEncodingNo.setSelected(!dynamicEncoding);
        encodingTypeComboBox.setVisible(dynamicEncoding);
        encodingTypeComboBox.setSelectedItem(encodingType);
        encodingLengthTextField.setText(encodingLength > 0? String.valueOf(encodingLength) : "");
        encodingLengthTextField.setVisible(!"自动".equals(encodingType) && dynamicEncoding);
        lengthUnitComboBox.setSelectedItem(lengthUnit);
        lengthUnitComboBox.setVisible(!"自动".equals(encodingType) && dynamicEncoding);
        encodingTypeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedType = (String) encodingTypeComboBox.getSelectedItem();
                if (selectedType.equals("自动")) {
                    encodingLengthTextField.setVisible(false);
                    lengthUnitComboBox.setVisible(false);
                } else {
                    encodingLengthTextField.setVisible(true);
                    lengthUnitComboBox.setVisible(true);
                }
                configDialog.revalidate();
                configDialog.repaint();
            }
        });
        dynamicEncodingYes.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                encodingTypeComboBox.setVisible(true);
                configDialog.revalidate();
                configDialog.repaint();
            }
        });
        dynamicEncodingNo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                encodingTypeComboBox.setVisible(false);
                encodingLengthTextField.setVisible(false);
                lengthUnitComboBox.setVisible(false);
                configDialog.revalidate();
                configDialog.repaint();
            }
        });
        encodingLengthTextField.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int value = Integer.parseInt(encodingLengthTextField.getText());
                if (e.getWheelRotation() < 0 && value < 999) {
                    value++;
                } else if (e.getWheelRotation() > 0 && value > 1) {
                    value--;
                }
                encodingLengthTextField.setText(String.valueOf(value));
            }
        });

        // 第四行
        JPanel thirdRowConfig = new JPanel();
        thirdRowConfig.setLayout(new FlowLayout(FlowLayout.LEFT));
        JCheckBox unifiedSuffixCheckBox = new JCheckBox("统一后缀");
        JCheckBox newDirOnUnpackCheckBox = new JCheckBox("解压到空目录");   // 解压目录不为空时新建目录
        JCheckBox packFileNameCheckBox = new JCheckBox("文件名循环编码");   // 将文件名循环作为编码内容
        thirdRowConfig.add(unifiedSuffixCheckBox);
        thirdRowConfig.add(newDirOnUnpackCheckBox);
        thirdRowConfig.add(packFileNameCheckBox);
        unifiedSuffixCheckBox.setSelected(unifiedSuffix);
        newDirOnUnpackCheckBox.setSelected(newDirOnUnpack);
        packFileNameCheckBox.setSelected(fnLoopEncodeFlag);

        // 第五行
        JPanel fourthRowConfig = new JPanel();
        fourthRowConfig.setLayout(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("确定");
        JButton closeButton = new JButton("关闭");
        okButton.addActionListener(new ActionListener() {
            @SuppressWarnings("deprecation")
            @Override
            public void actionPerformed(ActionEvent e) {
                // 校验文件打包加密
                if (fileEncodeYes.isSelected() && encodingTxtField.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(configDialog, "文件打包加密选中是时，打包密码不能为空。", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // 校验文件内容转码
                if (fileContentEncodeYes.isSelected() && encodingTextField.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(configDialog, "文件内容转码选中是时，转码编码不能为空。", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // 校验编码长度
                if ((encodingTypeComboBox.getSelectedItem().equals("限高") || encodingTypeComboBox.getSelectedItem().equals("手工")) 
                        && (encodingLengthTextField.getText().trim().isEmpty() ||!isNumeric(encodingLengthTextField.getText()))) {
                    JOptionPane.showMessageDialog(configDialog, "编码生成类型是限高或手工时，编码长度不能为空且只能是数字。", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                saveConfig(
                        fileEncodeYes.isSelected(),
                        encodingTxtField.getText(),
                        fileContentEncodeYes.isSelected(),
                        encodingTextField.getText(),
                        dynamicEncodingYes.isSelected(),
                        (String) encodingTypeComboBox.getSelectedItem(),
                        Integer.parseInt(encodingLengthTextField.getText()),
                        (String) lengthUnitComboBox.getSelectedItem(),
                        unifiedSuffixCheckBox.isSelected(),
                        newDirOnUnpackCheckBox.isSelected(),
                        packFileNameCheckBox.isSelected()
                );
                configDialog.dispose();
            }
        });
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                configDialog.dispose();
            }
        });
        fourthRowConfig.add(okButton);
        fourthRowConfig.add(closeButton);

        // 添加到配置对话框
        configDialog.add(headRowConfig);
        configDialog.add(firstRowConfig);
        configDialog.add(secondRowConfig);
        configDialog.add(thirdRowConfig);
        configDialog.add(fourthRowConfig);

        configDialog.setLocationRelativeTo(this);
        configDialog.setVisible(true);
    }

    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void showDirectoryChooser() {
        String dir = directoryTextField.getText();
        if(!dir.isEmpty()){
            File file = new File(dir);
            file.mkdirs();
        }
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            directoryTextField.setText(selectedFile.getAbsolutePath());
        }
    }

    private void exitProgram() {
        System.exit(0);
    }

    private void showAboutDialog() {
        JOptionPane.showMessageDialog(this, "文件打包解压工具\n版本：1.0_20250610\n作者：estc@sina.com", "关于", JOptionPane.INFORMATION_MESSAGE);
    }

    private void saveConfig(boolean fileEncode, String fileEncoding, boolean fileContentEncode, String contentEncoding, boolean dynamicEncoding, String encodingType, int encodingLength, String lengthUnit, boolean unifiedSuffix, boolean newDirOnUnpack, boolean fixEncodeFlag) {
        this.fileEncode = fileEncode;
        this.fileEncoding = fileEncoding;
        this.fileContentEncode = fileContentEncode;
        this.contentEncoding = contentEncoding;
        this.dynamicEncoding = dynamicEncoding;
        this.encodingType = encodingType;
        this.encodingLength = encodingLength;
        this.lengthUnit = lengthUnit;
        this.unifiedSuffix = unifiedSuffix;
        this.newDirOnUnpack = newDirOnUnpack;
        this.fnLoopEncodeFlag = fixEncodeFlag;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new FileManagerUI();
            }
        });
    }

    
    /** 打包zip文件 */
    public void pack(String dir, String zipFile) {
        try {
            startTime = System.currentTimeMillis();
            initParamVal();
            
            File dirFile = new File(dir);
            countFile(dirFile);
            System.out.println("The biggest image size is: " + maxFileSize);

            ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(zipFile));
            File[] children = dirFile.listFiles();
            if (children != null && children.length > 0) {
                for (File child : children) {
                     packFile(child, child.getName(), zip);
                }
            }
            if(!"".equals(comment)) {
                if(comment.length()>18) {
                    comment = comment.substring(0,18);
                }
                zip.setComment(comment);
            }
            zip.close();

            /*
            for (Map.Entry<String, Integer> entry : headFileNumMap.entrySet()) {
                System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
            } //*/
            System.out.println("ZIP file created successfully!");
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            double seconds = TimeUnit.MILLISECONDS.toSeconds(duration);  // MICROSECONDS  MILLISECONDS
            showProgress(true, "　　　　打包文件：" + zipFile + "　　　　耗时：" + seconds + "秒");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /** 统计指定目录的文件数量和最大文件尺寸 */
    private void countFile(File dir){
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null && children.length > 0) {
                for (File child : children) {
                    countFile(child);
                }
            }
        }else {
            fileCount++;
            long fileLen = dir.length();
            if(fileLen > maxFileSize) {
                maxFileSize = fileLen;
            }
        }
    }
    
    /** 获取设置的编码长度 */
    private int getSetSize() {
        int setSize = encodingLength;
        if("M".equals(lengthUnit)) {
            setSize = setSize * 1024 * 1024;
        }else if("十M".equals(lengthUnit)) {
            setSize = setSize * 1024 * 1024 * 10;
        }else if("百M".equals(lengthUnit)) {
            setSize = setSize * 1024 * 1024 * 100;
        }else if("百K".equals(lengthUnit)) {
            setSize = setSize * 1024 * 100;
        }
        return setSize;
    }
    
    /** 获取文件内容转码编码 */
    private String getContentEncoding(String fn){
        StringBuffer bufLog = new StringBuffer(100);
        String encoding = "";
        if(dynamicEncoding) {
            int maxSize = Long.valueOf(maxFileSize).intValue();;
            if("手工".equals(encodingType)) {
                maxSize = getSetSize();
            }else if("限高".equals(encodingType)) {
                int setSize = getSetSize();
                if(maxSize > setSize) {
                    maxSize = setSize;
                }
            }
            bufLog.append("\tmaxSize: ").append(maxSize);
            
            if(fnLoopEncodeFlag) {
                int times = maxSize/(128 + fn.length());
                StringBuffer buf = new StringBuffer(maxSize);
                for(int i=0; i<=times; i++){
                    if(i%10000==0){
                        System.err.println(Math.round(i/times*100) + "%");
                    }
                    String key = contentEncoding + i;
                    String e = encrypBySha512(key);   // encrypBySha512  encrypByMd5
                    String puppet = getFnPuppet(fn);
                    buf.append(e).append(puppet);
                    
                    if(i==0) {
                        comment = e;  //"1:" + "".equals(comment)
                    }
                }
                encoding = buf.toString();
                bufLog.append("\tFileNameLoop\tTimes: ").append(times);
            }else {
                if("".equals(fnLoopEncoding)) {
                    int times = maxSize/128;
                    StringBuffer buf = new StringBuffer(maxSize);
                    for(int i=0; i<=times; i++){
                        if(i%10000==0){
                            System.err.println(Math.round(i/times*100) + "%");
                        }
                        String key = contentEncoding + i;
                        String e = encrypBySha512(key);   // encrypBySha512  encrypByMd5
                        buf.append(e);
                        
                        if(i==0) {
                            comment = e;  //"2:" + "".equals(comment)
                        }
                    }
                    fnLoopEncoding = buf.toString();
                    bufLog.append("\tFileNameNotLoop\tTimes: ").append(times);
                }
                encoding = getFnPuppet(fn) + fnLoopEncoding;  // fnLoopEncoding + fn;
            }
        }else {
            encoding = getFnPuppet(fn) + contentEncoding;  // contentEncoding + fn
        }

        System.out.println("File:\t" + fn + bufLog.toString());
        return encoding;
    }
    
    /** 文件名的替代者 */
    private String getFnPuppet(String fn) {
        String puppet = encrypBySha512(fn);
        if(puppet.length()>fn.length()) {
            puppet = puppet.substring(0, fn.length());
        }
        return puppet;
    }
    
    /** MD5摘要 */
    public String encrypByMd5(String context) {
        try {
            // 获取一个MD5消息摘要实例
            MessageDigest md = MessageDigest.getInstance("MD5");

            // 更新消息摘要，将输入的文本内容转换为字节数组并进行处理
            md.update(context.getBytes());

            // 计算消息摘要，得到MD5散列值
            byte[] encryContext = md.digest();

            int i;
            StringBuffer buf = new StringBuffer("");
            for (int offset = 0; offset < encryContext.length; offset++) {
                // 将字节值转换为无符号整数
                i = encryContext[offset];
                if (i < 0) i += 256;  // 处理负值
                if (i < 16) buf.append("0");  // 补充前导0，以保证每个字节都被表示为两位十六进制数
                buf.append(Integer.toHexString(i));  // 将字节值转换为十六进制字符串并追加到结果字符串
            }

            // 返回MD5散列值的十六进制表示
            return buf.toString();
        } catch (NoSuchAlgorithmException e) {
            // 处理NoSuchAlgorithmException异常，通常是因为指定的MD5算法不可用
            e.printStackTrace();
            return  null;
        }
    }
    
    /** Sha512摘要 */
    public String encrypBySha512(String context) {
        try {
            // 创建SHA-512实例
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            
            // 执行加密
            byte[] hashBytes = digest.digest(context.getBytes("UTF-8"));
            
            // 将字节转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            // 打印输出加密后的字符串
            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return  null;
        }
    }
    
    /** 根据文件头统计文件数量 */
    private void countHeadNum(String key, int fileIdx, String fileName) {
        if(headFileNumMap.get(key)==null) {
            headFileNumMap.put(key, 1);
        }else {
            headFileNumMap.put(key, headFileNumMap.get(key) + 1);
        }
    }
    
    /** 打包zip文件 */
    private void packFile(File dir, String name, ZipOutputStream zip) throws IOException {
        if (dir.isDirectory()) {
            // 创建目录条目
            zip.putNextEntry(new ZipEntry(name + "/"));
            zip.closeEntry();

            File[] children = dir.listFiles();
            if (children != null && children.length > 0) {
                for (File child : children) {
                    packFile(child, name + "/" + child.getName(), zip);
                }
            }
        } else {
            // 添加文件条目
            fileIdx++;
            String dataName = name;
            if(unifiedSuffix) {
                dataName = name.split("[.]")[0]+".dat";
            }
            showProgress(false, dataName);

            int len = (int) dir.length();
            if(unifiedSuffix) {
                len = len - 2;
            }
            byte[] fileContent = new byte[len];  // Integer.MAX_VALUE 2147483647;
            byte[] buffer = new byte[1024];
            int length;
            int idx=0;
            int destPos = 0;
            FileInputStream fis = new FileInputStream(dir);
            while ((length = fis.read(buffer)) > 0) {
                if(idx==0) {
                    String head = Integer.toHexString(buffer[0] & 0xFF).toUpperCase() + Integer.toHexString(buffer[1] & 0xFF).toUpperCase();
                    System.out.println(head + "\t" + dataName + "\t" + len);
                    countHeadNum(head, fileIdx, name);
                    
                    if(unifiedSuffix) {
                        System.arraycopy(buffer, 2, fileContent, destPos, length-2);
                        destPos = destPos + length - 2;
                    }else {
                        System.arraycopy(buffer, 0, fileContent, destPos, length);
                        destPos = destPos + length;
                    }
                }else {
                    System.arraycopy(buffer, 0, fileContent, destPos, length);
                    destPos = destPos + length;
                }
                idx++;
            }
            fis.close();
            
            if(fileContentEncode) {
                String encryPin = getContentEncoding(dataName);
                int encryLen = encryPin.length();
                for(int i=0; i<len; i++){
                    char c = encryPin.charAt(i % encryLen);
                    int ascii = (int)c;
                    fileContent[i] = (byte) (fileContent[i] ^ ascii);
                }
            }
            
            zip.putNextEntry(new ZipEntry(dataName));
            zip.write(fileContent, 0, len);
            zip.closeEntry();
        }
    }
    
    /** 显示进度信息 */
    private void showProgress(boolean done, String msg) {
        if(done) {
            progressBar.setVisible(false);
            statusBar.setText("就绪" + msg);
        }else {
            progressBar.setVisible(true);
            int progress = fileIdx * 100 / fileCount;
            progressBar.setValue(progress);
            statusBar.setText("正在处理：" + msg + " ("  + progress + "%)");
            // 随机添加信息到列表框
            fileListData.add(fileIdx + ": " + msg);
            fileList.setListData(fileListData.toArray(new String[0]));
            // 确保最后一行显示
            fileList.ensureIndexIsVisible(fileListData.size() - 1);
        }
        revalidate();
        repaint();
    }
    
    /** 显示进度信息 */
    private void showProgress( String msg) {
        fileListData.add(msg);
        fileList.setListData(fileListData.toArray(new String[0]));
        // 确保最后一行显示
        fileList.ensureIndexIsVisible(fileListData.size() - 1);
        revalidate();
        repaint();
        
    }
    
    /** 根据当前时间获取文件夹名称 */
    private String getTime() {
        // 获取当前时间的 Instant 对象
        Instant now = Instant.now();
        // 创建 2000 年 0 时的 Instant 对象
        Instant year2000 = Instant.parse("2000-01-01T00:00:00Z");
        // 计算当前时间与 2000 年 0 时的时间差
        Duration duration = Duration.between(year2000, now);
        long seconds = duration.getSeconds();
        // 将秒数转换为 36 进制
        String base36Str = Long.toString(seconds, 36).toUpperCase();
        // 补零直到长度达到 8 位
        while (base36Str.length() < 7) {
            base36Str = "0" + base36Str;
        }
        if (base36Str.length() < 8) {
            base36Str = "F" + base36Str;
        }
        // 每 4 位用 - 连接
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < base36Str.length(); i += 4) {
            if (i > 0) {
                result.append("-");
            }
            result.append(base36Str.substring(i, Math.min(i + 4, base36Str.length())));
        }
        System.out.println(result.toString());
        return result.toString();
    }
    
    /** 获取空白的解压zip文件目录 */
    private String getPureDir(String dir) {
        if(newDirOnUnpack) {
            File d = new File(dir);
            String[] l = d.list();
            if(l!=null && l.length>0) {
                String subDir = getTime();
                File mk = new File(dir + "/" + subDir);
                mk.mkdirs();
                return dir + "/" + subDir;
            }else {
                d.mkdirs();
            }
        }
        return dir;
    }
    
    /** 初始化打回/解压相关变量 */
    private void initParamVal() {
        fileIdx = 0;
        fileCount = 0;
        maxFileSize = 0;

        headFileNumMap = new HashMap<String, Integer>();
        fileListData = new ArrayList<>();
        fnLoopEncoding = "";
        comment = "";
    }
    
    /** 解压zip文件 */
    public void unpack(String zipFile, String dir){
        try {
            startTime = System.currentTimeMillis();
            String pureDir = getPureDir(dir);
            initParamVal();
            
            ZipFile zip = new ZipFile(zipFile);
            // 获取ZIP文件中的所有条目
            Enumeration<? extends ZipEntry> countEntries = zip.entries();
            while (countEntries.hasMoreElements()) {
                ZipEntry countEntry = (ZipEntry) countEntries.nextElement();
                if (!countEntry.isDirectory()) {
                    fileCount++;
                    long fileLen = countEntry.getSize();
                    System.out.println(countEntry.getName() + ":\t" + fileLen);
                    if(fileLen > maxFileSize) {
                        maxFileSize = fileLen;
                    }
                }
            }
            
            Enumeration<? extends ZipEntry> entries = zip.entries();  // 重新获取条目，因为之前的遍历已经到达末尾
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                String entryName = entry.getName();
                if (entry.isDirectory()) {
                    File entryFile = new File(pureDir, entryName);
                    entryFile.mkdirs();
                } else {
                    fileIdx++;
                    String fileName = entryName;
                    showProgress(false, fileName);
                    
                    int len = (int)entry.getSize();
                    if(unifiedSuffix) {
                        fileName = fileName.split("[.]")[0]+".jpg";
                    }
                    File entryFile = new File(pureDir, fileName);
                    entryFile.getParentFile().mkdirs();
                    
                    byte[] fileContent = new byte[len]; 
                    try {
                        byte[] buffer = new byte[1024];
                        int length;
                        int destPos = 0;
                        InputStream in = zip.getInputStream(entry);
                        while ((length = in.read(buffer)) > 0) {
                            System.arraycopy(buffer, 0, fileContent, destPos, length);
                            destPos = destPos + length;
                        }
                        
                        if(fileContentEncode) {
                            String encryPin = getContentEncoding(entryName);
                            int encryLen = encryPin.length();
                            for(int i=0; i<len; i++){
                                char c = encryPin.charAt(i % encryLen);
                                int ascii = (int)c;
                                fileContent[i] = (byte) (fileContent[i] ^ ascii);
                            }
                        }
                        
                        FileOutputStream fos = new FileOutputStream(entryFile);
                        if(unifiedSuffix) {
                            byte[] fileWrite = new byte[len+2]; 
                            fileWrite[0] = (byte)255;  //0xFF;
                            fileWrite[1] = (byte)216;  //0xD8;
                            System.arraycopy(fileContent, 0, fileWrite, 2, len);
                            fos.write(fileWrite, 0, len+2);
                        }else {
                            fos.write(fileContent, 0, len);
                        }
                        fos.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }

            }
            zip.close();
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            double seconds = TimeUnit.MILLISECONDS.toSeconds(duration);  // MICROSECONDS  MILLISECONDS
            showProgress(true, "　　　　解压路径：" + pureDir + "　　　　耗时：" + seconds + "秒");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    
    public int[] getKeys(String key) {
        int[] keys = new int[key.length()];
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            keys[i] = (int)c; //Integer.parseInt(key.substring(i, i + 1));
        }
        return keys;
    }

    /** 列表转字符串 */
    public String getContent(List<String> lst){
        StringBuffer buf = new StringBuffer();
        int row = lst.size();
        for(int i=0;i<row;i++) {
            buf.append(lst.get(i)).append("\n");
        }
        return buf.toString();
    }

    /** 保存文本文件内容 */
    public void txtFileSave(File file, String content) {
        try {
            java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter(file, false));  //false表示覆盖
            writer.write(content);
            writer.flush();
            writer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** 读取文本文件内容 */
    public String txtFileLoad(File file) {
        String content = "";
        try {
            java.io.FileReader reader = new java.io.FileReader(file);
            java.io.BufferedReader bufferedReader = new java.io.BufferedReader(reader);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (!"".equals(line.trim())) {
                    content += line;
                }
            }
            bufferedReader.close();
            reader.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            content = ex.getMessage();
        }
        return content;
    }

    /** 获取当前时间字符串 */
    private String getCurrentDateTime() {
        String format = "yyyy-MM-dd HH:mm:ss.SSS";
        String dateTime = new java.text.SimpleDateFormat(format).format(new java.util.Date());
        return dateTime;
    }

    /** 显示日志信息 */
    private void log(String info, boolean view) {
        if(view) {
            String dateTime = getCurrentDateTime();
            fileListData.add(dateTime + " " + info);
            fileList.setListData(fileListData.toArray(new String[0]));
            // 确保最后一行显示
            fileList.ensureIndexIsVisible(fileListData.size() - 1);
            revalidate();
            repaint();
        }else{
            System.out.println(info);
        }
    }



    /** 图标文件内容 */
    private static final String iconBase64Str="iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAABNFJREFUWEfVlmtsFFUUx/93pmVboWGrEiwFrUoLiE0bLA9FsFGw+zCl0llLFeisIHww8oiiaVCxEmiiQhsIKaK0s5KocWelCu2sQq2JQRtDg6ZiJKIhWdsQJLo8arePmWvu2tlOt7vbaawh3k8zN+ee8zv/e869l+AGD3KD48M0gMMl7qEanaT4PBvHE9o0gF0QaenS628fbZtS2fR+3Z/jBWEKwFYqvlu69Crc9iv3FFfO+LHZJ603AthLxWcoQaZfll4bK5gpAJb9sd2BNgIs2vtxeskXbRN/UBo9v+jBHIJ4ANDOgHCnJ/TjQigF1pQQgn3JyNIorBxBkM03NkrBaMBRAWyC+NEG5x9dKxZ3b2aLuSTk2V+c8bpflkqMzmyC6PLLkjeeAg6XmN/slb4bMwDLvml34HsK5IGinZ/VUuB8fM0cyiVPbPbVnzYrOQOIpUJCBWyCqOxY+3v7gtmh7eFAFBv5WS2H2KdNEE/5ZWmxWQBmV1IiWqO3IREAsQui1lQd+JlSZOvZ6wGLXKtn8ki6s9krnTALYVslZvk/lC4Y7eMC2AXx0jvPd+3LuEXdGZ297sAuuN9S5IYXxh3Abn/OYrFeDfmqfusEkBmdfaT6V667Q+PUBYmKzwgXqxBjKsAK79Ndge0cwa542UdUKHVXKb6GHezfLog9AK4B0DhCnU1eT7sRwFQNOFzibdZJ6q9HKjtDAEmPl73ueHnx09P4ZJpLiPaeInum6vNOV8V9GiXNGuEXf+Y9fD7eNo1QgGV/vDpQDYrKUbMvExdBxSd0AAv9jcOLa+iQcj9GAY+qDsz5/OiRSwnPAccKMXd6Zv+Jui0XrQAs/xw82mxyV+s540Jn2Zo5qsp/xRGUm+0Cm6tiA6GkBt1pNyvK/l7d3zAFBrM/AIpndQM+p2WEDYA+RZbCgGMZzrJ192qq2qHIUsRn5MMuiK23TlaX1G/rrOE4bhNAJ4SdE1LLZ5/cOtR6In0j37LtlY7eN3tVclmRG6aMBrHMtWFyMu1j9wBlHmMCRAK43F+veviK8tQjwQIC8iiAFEJwjFAcIjktx5lKHzyY+iWArG8uq559P/XtAOh5RfZkxwJh9mxeD8r+EwIMthOlQH9KMk4Uzb92vmBWT9rd0/qnpt1EUbx9umMQoDAsUDLJLm/tXkJB6kHQpnil+3UfxsBGBUcFMGZid7kf0igW8tByKQjr65phAASbJm+u2x8OWuquAqGvxgpsCoAdFMww1r1tdGAEoJRWpW89GHmIREscvS0Jt+B/CQDAa91S90Q8icekALsu2YLoK3NYTQx1QbgIB8dBSi2V6Vtrg/9qC8wCZKQS7J1nkUGIoBMQ4KxG8fKTp3qOGqvcCF/kqnBylByP2wWsBkJJyPfLEuvzuKNQFFNSr6NnZhqHnXkWZhtRY2dHL/Ycrh92ehatdM/jONpOCD3Z7PUsNzoecRnFezzqi4yvmuLy9VP7+wcu5qXzeGnuhLMEmGsEKFq5OoPjkroAckaRG+bFymgEgK4Ce0prQD4oLoAgCzzCqhCNCNDogCJLtbpDR9naHKpy5x6YwiPYR8MKhE9AioDik25PpOaoz/LoxTZBLGRwsZ7YtjJxPlHxLYC/FFmamCiwoXbMmP13NmNWYLxR/gZQW0I/9MHdIQAAAABJRU5ErkJggg==";



    



    

    
    /** Ts文件配置信息 */
    protected class Ts {
        public String basePath;  // 基准地址
        public String url;       // 完整URL
        public boolean encry;    // 是否加密
        public String key;       // 加密密钥
        public float second;     // Ts文件时长
        public boolean finished; // 是否下载完成
        
        public Ts(String basePath, String url, boolean encry, String key, float second) {
            super();
            this.basePath = basePath;
            this.url = url;
            this.encry = encry;
            this.key = key;
            this.second = second;
            this.finished = false;
        }
    }



    /***** M3U8文件下载工具 *****/
    protected class M3u8Downloader {
        /** 下载指定m3u8的URL文件中的ts文件 */
        public void m3u8Down(String m3u8Url, String dir) {
            // 在新的线程中执行长时间任务
            new Thread(() -> {
                try {
                    fileListData = new ArrayList<>();
                    List<Ts> ts = new ArrayList<Ts>();
                    m3u8Parse(m3u8Url, ts, 0);
                    tsDownload(ts, dir);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
        
        /** 解析m3u8文件内容 */
        public void m3u8Parse(String m3u8Url, List<Ts> ts, float totalSeconds) throws Exception {
            List<String> lines = new ArrayList<String>();
            downLoadTxt(m3u8Url, lines, "", 10);  // String m3u8Txt = 
            boolean encry = false;            // 是否加密
            String key = "";                  // 加密密钥
            String basePath = getBasePath(m3u8Url);
            
            int size = lines.size();
            float seconds = 0;
            for(int i=0; i<size; i++) {
                String line = lines.get(i);
                if (line.startsWith("#")) {
                    if(line.contains("#EXT-X-KEY")) {
                        encry = true;
                    }
                    if (line.startsWith("#EXTINF:")) {
                        line = line.substring(8);
                        if (line.indexOf(",") > 0) {
                            line = line.substring(0, line.indexOf(","));
                        }
                        seconds = Float.parseFloat(line);
                        totalSeconds = totalSeconds + seconds;
                    }
                    continue;
                }
                
                // m3u8中包含新的m3u8
                String lowerLine = line.toLowerCase();
                if (line.endsWith("m3u8")) {
                    m3u8Parse(lowerLine.startsWith("http")?line:(basePath + line), ts, totalSeconds);
                    continue;
                }
                
                // m3u8中的ts地址及对应的时长
                String tsUrl = lowerLine.startsWith("http")?line:(basePath + line);
                ts.add(new Ts(basePath, tsUrl, encry, key, seconds));
                log("ts: " + tsUrl + ", " + seconds + "/" + totalSeconds + " s", true);
                
                seconds = 0;
            }
        }
        
        /** m3u8配置信息中是否配置为加密 */
        protected boolean getM3u8Encry(String m3u8Txt) {
            return m3u8Txt.contains("#EXT-X-KEY");
        }
        
        /** m3u8配置信息配置的密钥 */
        protected String getM3u8Key(String m3u8Txt) {
            return "";
        }
        
        /** 获取url链接对象 */
        private HttpURLConnection getHttpConnection(String url) throws Exception {
            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
            httpURLConnection.setConnectTimeout(3000);
            httpURLConnection.setReadTimeout(3000);
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setDoInput(true);
            //for (Map.Entry<String, Object> entry : requestHeaderMap.entrySet())
            //    httpURLConnection.addRequestProperty(entry.getKey(), entry.getValue().toString());
            return httpURLConnection;
        }
        
        /** 下载文本文件 */
        public String downLoadTxt(String url, List<String> lines, String progress, int retryCount) {
            int count = 1;
            String content = null;
            HttpURLConnection httpURLConnection = null;
            
            while (count <= retryCount) {
                StringBuilder contentBuf = new StringBuilder();
                try {
                    httpURLConnection = getHttpConnection(url);
                    String line;
                    InputStream inputStream = httpURLConnection.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    while ((line = bufferedReader.readLine()) != null) {
                        lines.add(line);
                        contentBuf.append(line).append("\n");
                    }
                    bufferedReader.close();
                    inputStream.close();
                    
                    content = contentBuf.toString();
                    log(content, false);
                    log("TxtFile download finished: " + "(" + progress + lines.size() + " rows) " + url , true);
                    break;
                } catch (Exception e) {
                    log("第" + count + "次打开链接： " + url + ", 异常信息： " + e.getMessage(), true);
                    count++;
                } finally {
                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                }
            }
            if (count > retryCount) {
                log("下载文本文件失败， 尝试打开链接" + (count-1) + "次失败, URL： " + url , true);
            }
            return content;
        }
        
        /** 下载流文件 */
        public void downLoadByte(String url, String fileName, String progress, int retryCount) {
            HttpURLConnection httpURLConnection = null;
            OutputStream fileOutputStream = null;
            byte[] bytes = new byte[40960];;

            //重试次数判断
            int count = 1;
            while (count <= retryCount) {
                File file = new File(fileName);
                if (file.exists()) {
                    file.delete();
                }
                
                try {
                    httpURLConnection = getHttpConnection(url);
                    InputStream inputStream = httpURLConnection.getInputStream();
                    fileOutputStream = new FileOutputStream(fileName);
                    int len;
                    int totalLen = 0;
                    while ((len = inputStream.read(bytes)) != -1) {
                        fileOutputStream.write(bytes, 0, len);
                        totalLen = totalLen + len;
                    }
                    fileOutputStream.flush();
                    inputStream.close();

                    log("File download finished: " + "(" + progress + totalLen + ") " + url  , true);
                    break;
                } catch (Exception e) {
                    log("第" + count + "次打开链接: " + url + ", 异常信息：" + e.getMessage(), true);
                    count++;
                } finally {
                    try {
                        if (fileOutputStream != null)
                            fileOutputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                }
            }
            if (count > retryCount) {
                log("下载流文件失败， 尝试打开链接" + (count-1) + "次失败, URL： " + url , true);
            }
        }
        
        public String getBasePath(String url) {
            String basePath="";
            HttpURLConnection httpURLConnection=null;
            try {
                httpURLConnection = getHttpConnection(url);
                if (httpURLConnection.getResponseCode() == 200) {
                    String realUrl = httpURLConnection.getURL().toString();
                    if(!url.toLowerCase().equals(realUrl.toLowerCase())) {
                        log("实际地址与初始访问地址不一致，需要修改ts文件地址： " + realUrl , true);  //如果有变化后面ts文件的地址要做对应修改
                        basePath = realUrl.substring(0, realUrl.lastIndexOf("/") + 1);
                    }else {
                        basePath = url.substring(0, url.lastIndexOf("/") + 1);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
            }
            return basePath;
        }
        
        /** 下载所有ts文件 */
        public void tsDownload(List<Ts> ts, String dir) {
            int size = ts.size();
            String saveDir = "";
            if(size > 0){
                saveDir = getPureDir(dir);
                StringBuffer buf = new StringBuffer();
                String newline = System.getProperty("line.separator");
                startTime = System.currentTimeMillis();

                //线程池
                final ExecutorService fixedThreadPool = Executors.newFixedThreadPool(5);
                
                for(int i=0; i<size; i++) {
                    Ts tsObj = ts.get(i);
                    fixedThreadPool.execute(getTsThread(tsObj, saveDir, i, size));

                    String name = String.format("%04d", i) + ".ts";
                    buf.append(i>0?newline:"").append("file '").append(name).append("'");
                }

                // 保存文件列表
                String filelist = saveDir + "/filelist.txt";
                txtFileSave(new File(filelist), buf.toString());

                // 启动关闭序列，不再接受新任务，但会继续执行完所有已经提交的任务
                fixedThreadPool.shutdown();

                // 在新的线程中检查是否都下载完成
                new Thread(() -> {
                    while (true) {
                        boolean finished = true;
                        for (int i = 0; i < size; i++) {
                            if (!ts.get(i).finished) {
                                finished = false;
                                break;
                            }
                        }
                        if (finished) {
                            log("所有ts文件下载完成", true);

                            // ffmpeg -i input.mp4 -vf "fps=1" output_%05d.jpg  // 将视频文件转为图片
                            // -f concat指定了输入格式为concat，-safe 0是为了允许文件名中包含特殊字符，-c copy表示复制流而不重新编码
                            // ffmpeg -i "concat:video1.ts|video2.ts|video3.ts" -c copy output.mp4  // 需要下载ffmpeg
                            log("合并文件命令：ffmpeg -f concat -safe 0 -i filelist.txt -c copy output.mp4  （需要先下载ffmpeg并配置path后，再执行该命令）", true);

                            long endTime = System.currentTimeMillis();
                            long duration = endTime - startTime;
                            double seconds = TimeUnit.MILLISECONDS.toSeconds(duration);
                            statusBar.setText("就绪 下载耗时" + seconds + "秒");
                            break;
                        }
                    }
                }).start();

            }else{
                log("没有ts文件需要下载", true);
            }
        }
        
        /** 下载ts文件线程 */
        private Thread getTsThread(Ts ts, String saveDir, int i, int size) {
            return new Thread(() -> {
                String name = String.format("%04d", i) + ".ts";
                String fileName = saveDir + "/O" + name;
                String tsflName = saveDir + "/X" + name;
                String progress = i + "/" + size + ":";

                // 下载ts文件
                downLoadByte(ts.url, fileName, progress, 10);
                
                // 解密ts文件（解密方法待完善）
                InputStream is = null;
                FileOutputStream os = null;
                
                if(ts.encry) {
                    try {
                        is = new FileInputStream(fileName);
                        int available = is.available();
                        byte[] bytes = new byte[available];
                        is.read(bytes);
                        
                        File file = new File(tsflName);
                        os = new FileOutputStream(file);
                        byte[] decrypt = decryptTs(bytes, available, ts.key);
                        os.write(decrypt);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (is != null)
                                is.close();
                            if (os != null)
                                os.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                // 设置下载完成标志
                ts.finished = true;
            });
        }
        
        /** 解密ts文件 */
        private byte[] decryptTs(byte[] sSrc, int length, String sKey) throws Exception {
            return sSrc;
        }
    }


    /***** 文件二进制内容查看工具 *****/
    protected class FileBinaryViewer {
        private File selectedFileBinaryViewer;
        private long fileOffsetBinaryViewer = 0;
        private int fileReadBufBinaryViewer = 1024*10;
        private DefaultListModel<String> hexModelBinaryViewer;
        private DefaultListModel<String> binaryModelBinaryViewer;
        
        public void displayBinaryContentDialog() {
            JDialog binaryViewerDialog = new JDialog(FileManagerUI.this, selectedFileBinaryViewer.getPath(), true);
            binaryViewerDialog.setLayout(new BorderLayout());
            binaryViewerDialog.setSize(1000, 600);
            binaryViewerDialog.setLocationRelativeTo(null); //该方法也可以达到窗口居中的效果

            // 创建数据模型
            binaryModelBinaryViewer = new DefaultListModel<>();
            hexModelBinaryViewer = new DefaultListModel<>();
            JList<String> binaryList = new JList<>(binaryModelBinaryViewer);
            JList<String> hexList = new JList<>(hexModelBinaryViewer);

            // 添加列表选择监听器
            binaryList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    int index = binaryList.getSelectedIndex();
                    hexList.setSelectedIndex(index);
                }
            });
            hexList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    int index = hexList.getSelectedIndex();
                    binaryList.setSelectedIndex(index);
                }
            });

            JScrollPane binaryScrollPane = new JScrollPane(binaryList);
            JScrollPane hexScrollPane = new JScrollPane(hexList);
            binaryScrollPane.getVerticalScrollBar().setModel(hexScrollPane.getVerticalScrollBar().getModel());
            hexScrollPane.getVerticalScrollBar().setModel(binaryScrollPane.getVerticalScrollBar().getModel());

            // 使用 JSplitPane 实现可拖动分隔
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, binaryScrollPane, hexScrollPane);
            splitPane.setResizeWeight(0.7);  // 设置左右两部分的初始比例，可调整

            JButton prevButton = new JButton("<<");
            prevButton.addActionListener(new PrevButtonListener());
            JButton nextButton = new JButton("显示更多");
            nextButton.addActionListener(new NextButtonListener());
            JPanel buttonPanel = new JPanel();
            //buttonPanel.add(prevButton);
            buttonPanel.add(nextButton);

            binaryViewerDialog.add(splitPane, BorderLayout.CENTER);
            binaryViewerDialog.add(buttonPanel, BorderLayout.SOUTH);

            displayFileBinaryContent();

            binaryViewerDialog.setVisible(true);
        }

        private void displayFileBinaryContent() {
            try (FileInputStream fis = new FileInputStream(selectedFileBinaryViewer)) {
                fis.skip(fileOffsetBinaryViewer);
                byte[] buffer = new byte[fileReadBufBinaryViewer];
                int bytesRead = fis.read(buffer);
                if (bytesRead > 0) {
                    StringBuilder binaryBuilder = new StringBuilder();
                    StringBuilder hexBuilder = new StringBuilder();
                    int binaryCount = 0;
                    int startPosition = (int) fileOffsetBinaryViewer;
                    binaryBuilder.append(String.format("%8d: ", startPosition));
                    hexBuilder.append(String.format("%8d: ", startPosition));
                    for (int i = 0; i < bytesRead; i++) {
                        String binary = String.format("%8s", Integer.toBinaryString(buffer[i] & 0xFF)).replace(' ', '0');
                        String hex = String.format("%02X", buffer[i] & 0xFF);
                        binaryBuilder.append(binary).append(" ");
                        hexBuilder.append(hex).append(" ");
                        binaryCount += 8;
                        if (binaryCount >= 80) {
                            binaryModelBinaryViewer.addElement(binaryBuilder.toString());
                            hexModelBinaryViewer.addElement(hexBuilder.toString());
                            binaryBuilder = new StringBuilder();
                            hexBuilder = new StringBuilder();
                            startPosition += 10;
                            binaryBuilder.append(String.format("%8d: ", startPosition));
                            hexBuilder.append(String.format("%8d: ", startPosition));
                            binaryCount = 0;
                        }
                    }
                    // 添加最后未填满 80 位的部分
                    if (binaryBuilder.length() > 0) {
                        binaryModelBinaryViewer.addElement(binaryBuilder.toString());
                        hexModelBinaryViewer.addElement(hexBuilder.toString());
                    }
                }else {
                    JOptionPane.showMessageDialog(null, "文件内容已经全部显示。", "提示", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(FileManagerUI.this, "读取文件时出错: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
            
            revalidate();
            repaint();
        }
        
        private class PrevButtonListener implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (fileOffsetBinaryViewer >= fileReadBufBinaryViewer) {
                    fileOffsetBinaryViewer -= fileReadBufBinaryViewer;
                    displayFileBinaryContent();
                }
            }
        }

        private class NextButtonListener implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                fileOffsetBinaryViewer += fileReadBufBinaryViewer;
                displayFileBinaryContent();
            }
        }
    }

    /***** 文件格式转换工具 *****/
    protected class FileTxtInterTrans {
        private JTextArea txtAreaTransFile = new JTextArea();
        private JComboBox<String> cboEncryTransFile = new JComboBox<String>();
        private JTextField txtEncryEncryTransFile = new JTextField(20);
        private boolean encryTxtTransFile = false;
        private String encryAlgorithmTransFile = "";
        
        public void displayTransFileDialog() {
            JDialog transFileDialog = new JDialog(FileManagerUI.this, "File ↔ Txt", true);
            transFileDialog.setLayout(new BorderLayout());
            
            int width=1000;
            int height=600;
            java.awt.Point p = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
            transFileDialog.setBounds(p.x - width / 2, p.y - height / 2, width, height);
            // transFileDialog.setLocationRelativeTo(null); //该方法也可以达到窗口居中的效果
            
            // 按钮面板
            JPanel pnlFileTxt = new JPanel();
            transFileDialog.getContentPane().add(pnlFileTxt, BorderLayout.NORTH);

            // 转文本按钮及其监听事件
            JButton btnToTxt = new JButton(" → ");
            btnToTxt.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        // 显示打开的文件对话框
                        JFileChooser jfc = new JFileChooser();

                        // 使用文件类获取选择器选择的文件
                        jfc.setDialogTitle("请选择任意文件...");
                        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);

                        int returnValue = jfc.showOpenDialog(transFileDialog);
                        if (returnValue == JFileChooser.APPROVE_OPTION) {
                            File file = jfc.getSelectedFile();

                            // 将文件内容转为文本
                            String content = "";
                            try {
                                FileInputStream inputFile = new FileInputStream(file);
                                byte[] buffer = new byte[(int) file.length()];
                                inputFile.read(buffer);
                                inputFile.close();
                                content = new String(java.util.Base64.getEncoder().encode(buffer));
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                content = ex.getMessage();
                            }
                            String msg = "文件大小：" + file.length() + "，文本长度：" + content.length();
                            
                            // 加密文本
                            if (encryTxtTransFile) {
                                String key = txtEncryEncryTransFile.getText();
                                if (key == null || key.equals("")) {
                                    key = def_encry_pin;
                                }
                                key = encrypBySha512(key);
                                if("AES".equals(encryAlgorithmTransFile)) {
                                    content = AESEncode(key, content); 
                                }else {
                                    content = encryptKaiser(key, content); 
                                }
                                
                                msg = msg + "，加密后长度：" + content.length();
                            }
                            
                            // 显示文本
                            txtAreaTransFile.setText(content);

                            // 提示转换结果信息
                            JOptionPane.showMessageDialog(transFileDialog, msg, "信息", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } catch (Exception err) {
                        err.printStackTrace();
                        JOptionPane.showMessageDialog(transFileDialog, err.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            btnToTxt.setToolTipText("将文件转码成文本显示");
            pnlFileTxt.add(btnToTxt);

            JButton btnTxt = new JButton(" TXT ");
            btnTxt.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        // 显示打开的文件对话框
                        JFileChooser jfc = new JFileChooser();

                        // 使用文件类获取选择器选择的文件
                        jfc.setFileFilter(new javax.swing.filechooser.FileFilter() {
                            @Override
                            public boolean accept(File f) {
                                return f.isDirectory() || f.getName().toLowerCase().endsWith(".txt");
                            }

                            @Override
                            public String getDescription() {
                                return "文本文件 (*.txt)";
                            }
                        });
                        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                        int returnValue;
                        
                        // 文本框中内容空白时打开文件，否则保存文件
                        String transContent = txtAreaTransFile.getText();
                        if (transContent == null || transContent.equals("")) {
                            jfc.setDialogTitle("请选择需要打开的文本文件");
                            returnValue = jfc.showOpenDialog(transFileDialog);
                            if (returnValue == JFileChooser.APPROVE_OPTION) {
                                File file = jfc.getSelectedFile();
        
                                // 读取文本文件内容
                                String content = txtFileLoad(file);
                                
                                // 显示文本
                                txtAreaTransFile.setText(content);
        
                                // 提示转换结果信息
                                String msg = "文件大小：" + file.length() + "，文本长度：" + content.length();
                                JOptionPane.showMessageDialog(transFileDialog, msg, "信息", JOptionPane.INFORMATION_MESSAGE);
                            }
                        }else {
                            jfc.setDialogTitle("请选择需要保存的文本文件");
                            returnValue = jfc.showSaveDialog(transFileDialog);
                            if (returnValue == JFileChooser.APPROVE_OPTION) {
                                File file = jfc.getSelectedFile();
        
                                // 保存文本内容到文件
                                txtFileSave(file, transContent);
        
                                // 提示转换结果信息
                                String msg = "文件已保存，文本长度：" + transContent.length();
                                JOptionPane.showMessageDialog(transFileDialog, msg, "信息", JOptionPane.INFORMATION_MESSAGE);
                            }
                        }
                    } catch (Exception err) {
                        err.printStackTrace();
                        JOptionPane.showMessageDialog(transFileDialog, err.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            btnTxt.setToolTipText("打开或保存文本文件");
            pnlFileTxt.add(btnTxt);
            
            // 转文件按钮及其监听事件
            JButton btnToFile = new JButton(" → ");
            btnToFile.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {

                        // 将文本内容转为文件并保存
                        String content = txtAreaTransFile.getText();
                        if (content == null || content.equals("")) {
                            JOptionPane.showMessageDialog(transFileDialog, "请先打开文本文件或粘贴文本内容。", "提示", JOptionPane.WARNING_MESSAGE);
                        } else {

                            // 解密
                            if (encryTxtTransFile) {
                                String key = txtEncryEncryTransFile.getText();
                                if (key == null || key.equals("")) {
                                    key = def_encry_pin;
                                }
                                key = encrypBySha512(key);
                                if("AES".equals(encryAlgorithmTransFile)) {
                                    content = AESDncode(key, content); 
                                }else {
                                    content = decryptKaiser(key, content); 
                                }
                            }
                            
                            // 显示打开的文件对话框
                            JFileChooser jfc = new JFileChooser();

                            // 使用文件类获取选择器选择的文件
                            jfc.setDialogTitle("请设置文件名...");
                            jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);

                            int returnValue = jfc.showSaveDialog(transFileDialog);
                            if (returnValue == JFileChooser.APPROVE_OPTION) {
                                File file = jfc.getSelectedFile();
                                String filePath = file.getAbsolutePath();
        
                                byte[] buffer = java.util.Base64.getDecoder().decode(content);
                                FileOutputStream out = new FileOutputStream(filePath);
                                out.write(buffer);
                                out.close();

                                // 提示保存结果信息
                                JOptionPane.showMessageDialog(transFileDialog, "文件已保存：" + filePath, "信息", JOptionPane.INFORMATION_MESSAGE);
                            }
                        }
                    } catch (Exception err) {
                        err.printStackTrace();
                        JOptionPane.showMessageDialog(transFileDialog, err.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            btnToFile.setToolTipText("将文本转码成文件保存");
            pnlFileTxt.add(btnToFile);
            
            // 加密选项及输入框
            JCheckBox chkEncry = new JCheckBox("加密");
            chkEncry.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    // 获取事件源
                    JCheckBox check = (JCheckBox) e.getSource();
                    if (check.isSelected()) {
                        encryTxtTransFile = true;
                        txtEncryEncryTransFile.setVisible(true);
                        cboEncryTransFile.setVisible(true);
                    } else {
                        encryTxtTransFile = false;
                        txtEncryEncryTransFile.setVisible(false);
                        cboEncryTransFile.setVisible(false);
                    }
                }
            });

            // 加密类型下拉选择
            if(cboEncryTransFile.getItemCount()<1) {
                cboEncryTransFile.addItem("AES");
                cboEncryTransFile.addItem("Kaiser");
            }
            cboEncryTransFile.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent arg0) {
                    if (ItemEvent.SELECTED == arg0.getStateChange()) {
                        encryAlgorithmTransFile = arg0.getItem().toString();
                    }
                }
            });
            
            pnlFileTxt.add(chkEncry);
            pnlFileTxt.add(cboEncryTransFile);
            pnlFileTxt.add(txtEncryEncryTransFile);
            pnlFileTxt.add(new javax.swing.JLabel(" "));
            cboEncryTransFile.setVisible(false);
            txtEncryEncryTransFile.setVisible(false);

            // 可滚动面板
            JScrollPane scrollPane = new JScrollPane();
            scrollPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 2, 2, 2));
            transFileDialog.getContentPane().add(scrollPane, BorderLayout.CENTER);
            
            txtAreaTransFile.setLineWrap(true); // 激活自动换行功能
            txtAreaTransFile.setWrapStyleWord(true); // 激活断行不断字功能
            // textArea.setMargin(new java.awt.Insets(0, 2, 2, 2));
            scrollPane.setViewportView(txtAreaTransFile);

            // 这个最好放在最后，否则会出现视图问题。
            transFileDialog.setVisible(true);
        }
        

        public final String algorithm = "AES";
        // AES/CBC/NOPaddin
        // AES 默认模式
        // 使用CBC模式, 在初始化Cipher对象时, 需要增加参数, 初始化向量IV : IvParameterSpec iv = new
        // IvParameterSpec(key.getBytes());
        // NOPadding: 使用NOPadding模式时, 原文长度必须是8byte的整数倍
        public final String transformation = "AES/CBC/NOPadding";



        /*
        * 加密 1.构造密钥生成器 2.根据ecnodeRules规则初始化密钥生成器 3.产生密钥 4.创建和初始化密码器 5.内容加密 6.返回字符串
        */
        public String AESEncode(String encodeRules, String content) {
            try {
                // 1.构造密钥生成器，指定为AES算法,不区分大小写
                KeyGenerator keygen = KeyGenerator.getInstance("AES");
                // 2.根据ecnodeRules规则初始化密钥生成器
                // 生成一个128位的随机源,根据传入的字节数组
                keygen.init(128, new SecureRandom(encodeRules.getBytes()));
                // 3.产生原始对称密钥
                SecretKey original_key = keygen.generateKey();
                // 4.获得原始对称密钥的字节数组
                byte[] raw = original_key.getEncoded();
                // 5.根据字节数组生成AES密钥
                SecretKey key = new SecretKeySpec(raw, "AES");
                // 6.根据指定算法AES自成密码器
                Cipher cipher = Cipher.getInstance("AES");
                // 7.初始化密码器，第一个参数为加密(Encrypt_mode)或者解密解密(Decrypt_mode)操作，第二个参数为使用的KEY
                cipher.init(Cipher.ENCRYPT_MODE, key);
                // 8.获取加密内容的字节数组(这里要设置为utf-8)不然内容中如果有中文和英文混合中文就会解密为乱码
                byte[] byte_encode = content.getBytes("utf-8");
                // 9.根据密码器的初始化方式--加密：将数据加密
                byte[] byte_AES = cipher.doFinal(byte_encode);
                // 10.将加密后的数据转换为字符串
                String AES_encode = parseByte2HexStr(byte_AES);
                // 11.将字符串返回
                return AES_encode;
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            // 如果有错就返加nulll
            return null;
        }

        /*
        * 解密 解密过程： 1.同加密1-4步 2.将加密后的字符串反纺成byte[]数组 3.将加密内容解密
        */
        public String AESDncode(String encodeRules, String content) {
            try {
                // 1.构造密钥生成器，指定为AES算法,不区分大小写
                KeyGenerator keygen = KeyGenerator.getInstance("AES");
                // 2.根据ecnodeRules规则初始化密钥生成器
                // 生成一个128位的随机源,根据传入的字节数组
                keygen.init(128, new SecureRandom(encodeRules.getBytes()));
                // 3.产生原始对称密钥
                SecretKey original_key = keygen.generateKey();
                // 4.获得原始对称密钥的字节数组
                byte[] raw = original_key.getEncoded();
                // 5.根据字节数组生成AES密钥
                SecretKey key = new SecretKeySpec(raw, "AES");
                // 6.根据指定算法AES自成密码器
                Cipher cipher = Cipher.getInstance("AES");
                // 7.初始化密码器，第一个参数为加密(Encrypt_mode)或者解密(Decrypt_mode)操作，第二个参数为使用的KEY
                cipher.init(Cipher.DECRYPT_MODE, key);
                // 8.将加密并编码后的内容解码成字节数组
                byte[] byte_content = parseHexStr2Byte(content);
                /*
                * 解密
                */
                byte[] byte_decode = cipher.doFinal(byte_content);
                String AES_decode = new String(byte_decode, "utf-8");
                return AES_decode;
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            }

            // 如果有错就返加nulll
            return null;
        }

        /**
         * 将二进制转换成16进制
         * 
         * @param buf
         * @return
         */
        private String parseByte2HexStr(byte buf[]) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < buf.length; i++) {
                String hex = Integer.toHexString(buf[i] & 0xFF);
                if (hex.length() == 1) {
                    hex = '0' + hex;
                }
                sb.append(hex.toUpperCase());
            }
            return sb.toString();
        }

        /**
         * 将16进制转换为二进制
         * 
         * @param hexStr
         * @return
         */
        private byte[] parseHexStr2Byte(String hexStr) {
            if (hexStr.length() < 1)
                return null;
            byte[] result = new byte[hexStr.length() / 2];
            for (int i = 0; i < hexStr.length() / 2; i++) {
                int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
                int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
                result[i] = (byte) (high * 16 + low);
            }
            return result;
        }

        /**
         * 使用凯撒加密方式加密数据
         *
         * @param orignal :原文
         * @param key     :密钥
         * @return :加密后的数据
         */
        public String encryptKaiser(String key, String orignal) {
            int[] keys = getKeys(key);
            int l = keys.length;
            int i = 0;
            // 将字符串转为字符数组
            char[] chars = orignal.toCharArray();
            StringBuilder sb = new StringBuilder();
            // 遍历数组
            for (char aChar : chars) {
                // 获取字符的ASCII编码
                int asciiCode = aChar;
                // 偏移数据
                asciiCode += keys[i++];
                i = i % l;
                // 将偏移后的数据转为字符
                char result = (char) asciiCode;
                // 拼接数据
                sb.append(result);
            }

            return sb.toString();
        }

        /**
         * 使用凯撒加密方式解密数据
         *
         * @param encryptedData :密文
         * @param key           :密钥
         * @return : 源数据
         */
        public String decryptKaiser(String key, String encryptedData) {
            int[] keys = getKeys(key);
            int l = keys.length;
            int i = 0;
            // 将字符串转为字符数组
            char[] chars = encryptedData.toCharArray();
            StringBuilder sb = new StringBuilder();
            // 遍历数组
            for (char aChar : chars) {
                // 获取字符的ASCII编码
                int asciiCode = aChar;
                // 偏移数据
                asciiCode -= keys[i++];
                i = i % l;
                // 将偏移后的数据转为字符
                char result = (char) asciiCode;
                // 拼接数据
                sb.append(result);
            }

            return sb.toString();
        }
    }

    /***** 网络服务配置  *****/
    protected static class SvrConfig {
        private String name;
        private String path;
        private boolean showFileList;
        private boolean videoPlay;
        
        public SvrConfig(String name, String path, boolean showFileList, boolean videoPlay) {
            this.name = name;
            this.path = path;
            this.showFileList = showFileList;
            this.videoPlay = videoPlay;
        }
        
        public String getName() { return name; }
        public String getPath() { return path; }
        public boolean isShowFileList() { return showFileList; }
        public boolean isVideoPlay() { return videoPlay; }
    }


    /***** 网络服务工具  *****/
    protected class NginxSvr {

        public void deploySvr(){
            if(getJarPath()) {
                String zipPath = jarPath + "\\svr.jar";
                String svrPath = jarPath + "\\..\\svr";
                if(!unzipFile(zipPath, svrPath)){
                    JOptionPane.showMessageDialog(null, "部署失败: \n" + zipPath + "\n" + svrPath, "错误", JOptionPane.ERROR_MESSAGE);
                }else{
                    JOptionPane.showMessageDialog(null, "部署成功: \n" + zipPath + "\n" + svrPath, "信息", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }

        public void start() throws Exception  {
            if(getJarPath()) {
                String exePath = jarPath + "\\..\\svr\\nginx.exe";
                int option = JOptionPane.showConfirmDialog(null, "确认启动网络服务？\n" + exePath, "确认", JOptionPane.YES_NO_OPTION);
                    if (option == JOptionPane.YES_OPTION) {
                    killProcess("nginx.exe"); // 先杀掉可能存在的nginx进程
                    boolean success = executeProgram(exePath); // 启动nginx服务
                    if(success){
                        option = JOptionPane.showConfirmDialog(null, "是否打开浏览器访问？", "确认", JOptionPane.YES_NO_OPTION);
                        if(option == JOptionPane.YES_OPTION) {
                            openUrl("http://localhost");
                        }
                    }
                }
            }
        }

        public void stop() throws Exception   {
            int option = JOptionPane.showConfirmDialog(null, "确认停止网络服务？", "确认", JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                killProcess("nginx.exe"); 
                JOptionPane.showMessageDialog(null, "已停止。" , "信息", JOptionPane.INFORMATION_MESSAGE);
            }
        }

        private boolean getJarPath() {
            try {
                ProtectionDomain protectionDomain = NginxSvr.class.getProtectionDomain();
                CodeSource codeSource = protectionDomain.getCodeSource();
                URL jarUrl = codeSource.getLocation();
                File jarFile = new File(jarUrl.toURI());
                jarPath = jarFile.getParentFile().getAbsolutePath(); // 返回 JAR 所在目录

                // 检查 lib/svr.jar 是否存在
                String zipPath = (jarPath==null?"":jarPath) + "\\svr.jar";
                File zip = new File(zipPath);
                while (!zip.exists()) {
                    // 创建服务目录选择器
                    JFileChooser chooser = new JFileChooser();
                    chooser.setDialogTitle("请确认服务文件“svr.jar”所在路径：");
                    if(jarPath!=null){
                        File defaultDir = new File(jarPath);
                        chooser.setCurrentDirectory(defaultDir);
                    }
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    
                    // 显示对话框
                    int result = chooser.showDialog(null, "确认");
                    
                    if (result == JFileChooser.APPROVE_OPTION) {
                        File selectedDir = chooser.getSelectedFile();
                        jarPath = selectedDir.getAbsolutePath();
                        zipPath = (jarPath==null?"":jarPath) + "\\svr.jar";
                        zip = new File(zipPath);
                    } else {
                        JOptionPane.showMessageDialog(null, "未确认服务目录，取消后续处理。" , "信息", JOptionPane.INFORMATION_MESSAGE);
                        return false;
                    }
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        public void showConfigDialog(JFrame parent) {
            if(getJarPath()) {
                String cfgFile = jarPath + "\\..\\svr.cfg";
                String tmlFile = jarPath + "\\..\\svr\\conf\\cfg.tpl";
                String outFile = jarPath + "\\..\\svr\\conf\\nginx.conf";
                loadConfigurations(cfgFile);
            
                JDialog dialog = new JDialog(parent, "服务访问名称配置管理", true);
                dialog.setSize(800, 600);
                dialog.setLayout(new BorderLayout());
                
                // 表格模型
                DefaultTableModel model = new DefaultTableModel(new Object[]{"访问名称", "目录路径", "显示文件列表", "视频播放"}, 0) {
                    @Override
                    public Class<?> getColumnClass(int columnIndex) {
                        return columnIndex == 2 || columnIndex == 3 ? Boolean.class : String.class;
                    }
                    
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return true;
                    }
                };
                
                // 填充表格数据
                for (SvrConfig config : configurations) {
                    model.addRow(new Object[]{config.getName(), config.getPath(), config.isShowFileList(), config.isVideoPlay()});
                }
                
                JTable table = new JTable(model);
                table.setRowHeight(30);
                table.setFont(new Font("微软雅黑", Font.PLAIN, 14));
                
                // 设置列宽比例为2:4:1:1
                int totalWidth = table.getWidth();  //此时组件尚未被添加到可见容器中，或者尚未完成布局计算，getWidth()返回0
                totalWidth = Math.max(totalWidth, 800); // 确保总宽度至少为800
                table.getColumnModel().getColumn(0).setPreferredWidth((int)(totalWidth * 0.25)); // 访问名称 25%
                table.getColumnModel().getColumn(1).setPreferredWidth((int)(totalWidth * 0.50)); // 目录路径 50%
                table.getColumnModel().getColumn(2).setPreferredWidth((int)(totalWidth * 0.125)); // 显示文件列表 12.5%
                table.getColumnModel().getColumn(3).setPreferredWidth((int)(totalWidth * 0.125)); // 视频播放 12.5%
                
                // 为目录路径列设置自定义编辑器
                table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(new JTextField()) {
                    private JTextField textField;
                    
                    @Override
                    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                        JPanel panel = new JPanel(new BorderLayout());
                        textField = new JTextField(value != null ? value.toString() : "");
                        textField.setFont(new Font("微软雅黑", Font.PLAIN, 14));
                        
                        JButton browseButton = new JButton("...");
                        browseButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
                        
                        browseButton.addActionListener(e -> {
                            JFileChooser chooser = new JFileChooser();
                            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                            if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                                String selectedPath = chooser.getSelectedFile().getAbsolutePath();
                                textField.setText(selectedPath);
                                table.setValueAt(selectedPath, row, column);
                            }
                        });
                        
                        panel.add(textField, BorderLayout.CENTER);
                        panel.add(browseButton, BorderLayout.EAST);
                        
                        return panel;
                    }
                    
                    @Override
                    public Object getCellEditorValue() {
                        return textField != null ? textField.getText() : "";
                    }
                });
                
                // 为布尔值列设置复选框编辑器
                table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(new JCheckBox()) {
                    @Override
                    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                        JCheckBox checkBox = (JCheckBox) super.getTableCellEditorComponent(table, value, isSelected, row, column);
                        checkBox.setHorizontalAlignment(SwingConstants.CENTER);
                        return checkBox;
                    }
                });
                
                table.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(new JCheckBox()) {
                    @Override
                    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                        JCheckBox checkBox = (JCheckBox) super.getTableCellEditorComponent(table, value, isSelected, row, column);
                        checkBox.setHorizontalAlignment(SwingConstants.CENTER);
                        return checkBox;
                    }
                });
                
                JScrollPane scrollPane = new JScrollPane(table);
                
                // 按钮面板
                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                
                JButton addButton = new JButton("添加");
                JButton deleteButton = new JButton("删除");
                JButton saveButton = new JButton("保存");  // 改为保存按钮
                JButton applyButton = new JButton("应用");
                JButton openButton = new JButton("打开");
                JButton closeButton = new JButton("关闭");
                
                Font buttonFont = new Font("微软雅黑", Font.PLAIN, 14);
                addButton.setFont(buttonFont);
                deleteButton.setFont(buttonFont);
                saveButton.setFont(buttonFont);
                applyButton.setFont(buttonFont);
                openButton.setFont(buttonFont);
                closeButton.setFont(buttonFont);
                
                buttonPanel.add(addButton);
                buttonPanel.add(deleteButton);
                buttonPanel.add(saveButton);
                buttonPanel.add(applyButton);
                buttonPanel.add(openButton);
                buttonPanel.add(closeButton);
                
                // 添加事件监听器
                addButton.addActionListener(e -> {
                    model.addRow(new Object[]{"path" + (table.getRowCount()+1), "", false, false});
                    table.editCellAt(model.getRowCount() - 1, 0);
                    table.getSelectionModel().setSelectionInterval(model.getRowCount() - 1, model.getRowCount() - 1);
                });
                
                deleteButton.addActionListener(e -> {
                    int selectedRow = table.getSelectedRow();
                    if (selectedRow == -1) {
                        JOptionPane.showMessageDialog(dialog, "请选择要删除的行", "提示", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    
                    int confirm = JOptionPane.showConfirmDialog(dialog, "确定要删除此配置吗?", "确认", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        model.removeRow(selectedRow);
                    }
                });
                
                saveButton.addActionListener(e -> {
                    saveConfigurations(model, cfgFile);  // 保存到指定配置文件
                    JOptionPane.showMessageDialog(null, "保存配置完成：\n" + cfgFile, "提示", JOptionPane.INFORMATION_MESSAGE);
                });
                
                applyButton.addActionListener(e -> {
                    saveConfigurations(model, cfgFile);  // 应用前先保存
                    applyConfigurations(tmlFile, outFile);  // 应用配置
                    JOptionPane.showMessageDialog(null, "应用配置完成: \n" + tmlFile + "\n" + outFile, "提示", JOptionPane.INFORMATION_MESSAGE);
                });

                openButton.addActionListener(e -> {
                    int selectedRow = table.getSelectedRow();
                    if (selectedRow == -1) {
                        JOptionPane.showMessageDialog(dialog, "请选择要打开的行", "提示", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    String name = (String) model.getValueAt(selectedRow, 0);
                    try {
                        openUrl("http://localhost/" + name + "/");
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });
                
                closeButton.addActionListener(e -> dialog.dispose());
                
                dialog.add(scrollPane, BorderLayout.CENTER);
                dialog.add(buttonPanel, BorderLayout.SOUTH);
                dialog.setLocationRelativeTo(parent);
                dialog.setVisible(true);
            }
        }

        private void applyConfigurations(String tmlFile, String outFile) {
            try {
                // 读取模板文件
                String templateContent = new String(Files.readAllBytes(Paths.get(tmlFile)),StandardCharsets.UTF_8);
                
                // 为配置生成内容并替换模板
                String configContent = "";
                for (SvrConfig config : configurations) {
                    configContent += generateConfigContent(config);
                }
                templateContent = templateContent.replace("########", configContent);
                
                // 保存结果到outFile文件
                Files.write(Paths.get(outFile), templateContent.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "应用配置失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }

        private String generateConfigContent(SvrConfig config) {
            return String.format(
                "        location /%s/ {\r\n" + 
                "            alias   %s/;\r\n" + 
                "            index  index.html index.htm;\r\n" + 
                "            autoindex %s;\r\n" + 
                "            autoindex_exact_size off;\r\n" + 
                "            autoindex_localtime on;\r\n" + 
                "%s" +  // 这里放置视频播放配置
                "        }\r\n\r\n",
                config.getName(),
                config.getPath().replace("\\", "/"), // 替换路径中的反斜杠为正斜杠
                config.isShowFileList() ? "on" : "off",
                config.isVideoPlay() ? "            add_header Accept-Ranges bytes; # 支持范围请求\r\n" : ""
            );
        }

        private void saveConfigurations(DefaultTableModel model, String cfgFile) {
            // 先清空现有配置
            configurations.clear();
            
            // 从表格模型中读取所有配置
            for (int i = 0; i < model.getRowCount(); i++) {
                String name = (String) model.getValueAt(i, 0);
                String path = (String) model.getValueAt(i, 1);
                boolean showList = (Boolean) model.getValueAt(i, 2);
                boolean videoPlay = (Boolean) model.getValueAt(i, 3);
                
                configurations.add(new SvrConfig(name, path, showList, videoPlay));
            }
            
            // 保存到Properties文件
            Properties props = new Properties();
            props.setProperty("count", String.valueOf(configurations.size()));
            
            for (int i = 0; i < configurations.size(); i++) {
                SvrConfig config = configurations.get(i);
                props.setProperty("config." + (i+1) + ".name", config.getName());
                props.setProperty("config." + (i+1) + ".path", config.getPath());
                props.setProperty("config." + (i+1) + ".showList", String.valueOf(config.isShowFileList()));
                props.setProperty("config." + (i+1) + ".videoPlay", String.valueOf(config.isVideoPlay()));
            }
            
            try (OutputStream output = new FileOutputStream(cfgFile)) {
                props.store(output, "Configuration Properties");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "保存配置失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    
        private void loadConfigurations(String cfgFile) {
            configurations.clear();
            Properties props = new Properties();
            
            try (InputStream input = new FileInputStream(cfgFile)) {
                props.load(input);
                
                int count = Integer.parseInt(props.getProperty("count", "0"));
                for (int i = 1; i <= count; i++) {
                    String name = props.getProperty("config." + i + ".name");
                    String path = props.getProperty("config." + i + ".path");
                    boolean showList = Boolean.parseBoolean(props.getProperty("config." + i + ".showList"));
                    boolean videoPlay = Boolean.parseBoolean(props.getProperty("config." + i + ".videoPlay"));
                    
                    if (name != null && path != null) {
                        configurations.add(new SvrConfig(name, path, showList, videoPlay));
                    }
                }
            } catch (IOException e) {
                // 如果文件不存在，忽略错误
            }
        }

        private boolean unzipFile(String zipFilePath, String destDirectory) {
            File destDir = new File(destDirectory);
            if (!destDir.exists()) {
                destDir.mkdirs();
            }
            
            try (ZipFile zipFile = new ZipFile(zipFilePath)) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    File entryDestination = new File(destDir, entry.getName());
                    
                    if (entry.isDirectory()) {
                        entryDestination.mkdirs();
                    } else {
                        entryDestination.getParentFile().mkdirs();
                        
                        try (InputStream in = zipFile.getInputStream(entry);
                             OutputStream out = new FileOutputStream(entryDestination)) {
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = in.read(buffer)) > 0) {
                                out.write(buffer, 0, length);
                            }
                        }
                    }
                }
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        private void killProcess(String processName) throws IOException, InterruptedException {
            if (processName == null || processName.trim().isEmpty()) return;
            
            ProcessBuilder builder = new ProcessBuilder("taskkill", "/F", "/IM", processName);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            process.waitFor();
        }

        // 使用了多线程来异步读取进程的输出流和错误流，避免阻塞
        private void printProcessOutput(Process process) throws IOException {
            ExecutorService executor = Executors.newFixedThreadPool(2);
        
            // 读取输出流
            executor.submit(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[Nginx] " + line);
                    }
                } catch (IOException e) {
                    System.err.println("输出流读取错误: " + e.getMessage());
                }
            });
            
            // 读取错误流
            executor.submit(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.err.println("[Nginx Error] " + line);
                    }
                } catch (IOException e) {
                    System.err.println("错误流读取错误: " + e.getMessage());
                }
            });
            
            executor.shutdown();
        }
    
        private boolean executeProgram(String exePath) throws Exception {
            // 检查文件是否存在
            File exeFile = new File(exePath);
            if (!exeFile.exists()) {
                JOptionPane.showMessageDialog(null, "文件不存在: " + exePath, "错误", JOptionPane.ERROR_MESSAGE);
            }
            
            // 获取所在目录
            String exeDir = exeFile.getParent();
            
            // 使用ProcessBuilder启动
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("cmd.exe", "/c", "start", "nginx.exe");
            processBuilder.directory(new File(exeDir));
            
            // 合并错误流和输出流
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            // 读取输出
            //printProcessOutput(process);
            
            // 等待启动完成
            int exitCode = process.waitFor();
            System.out.println("Nginx启动完成，退出码: " + exitCode);
            if (exitCode == 0) {
                return true;
            } else {
                JOptionPane.showMessageDialog(null, "Nginx启动失败，退出码: " + exitCode, "错误", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }

        private void openUrl(String url) throws IOException{
            ProcessBuilder pb = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url);
            pb.start();
        }

        private List<SvrConfig> configurations = new ArrayList<>();
        private String jarPath = "";  // 用于存储当前JAR文件的路径
    }

    /***** 网页数据抓取工具  *****/
    protected class WebScraper {
        WebDriver driver;  // https://www.selenium.dev/downloads/  https://github.com/SeleniumHQ/selenium/releases
        List<String> webScraperContent;
        Object[][] webScraperCfgData;
        DefaultTableModel webScraperCfgTabModel;
        public void displayWebScraperDialog() {
            JDialog webScraperDialog = new JDialog(FileManagerUI.this, "Web Scraper", true);
            webScraperDialog.setLayout(new BorderLayout());
            webScraperDialog.setLayout(new java.awt.BorderLayout());  // FlowLayout  BorderLayout
            webScraperDialog.setMinimumSize(new Dimension(1000, 600));
            webScraperDialog.setLocationRelativeTo(null);  // 居中显示

            // 按钮区
            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new java.awt.GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0;
            gbc.insets = new Insets(2, 5, 2, 5);
            
            // 创建一个JTextField来输入网址
            JTextField urlField = new JTextField(60);
            buttonPanel.add(new JLabel("网址"));
            buttonPanel.add(urlField, gbc);

            // 创建一个JButton来打开网页
            JButton openButton = new JButton("打 开");
            buttonPanel.add(openButton, gbc);

            // 创建一个JButton来抓取内容
            JButton scrapeCfgButton = new JButton("配 置");
            buttonPanel.add(scrapeCfgButton, gbc);
            
            // 处理选项
            JComboBox<String> comboBox = new JComboBox<>(new String[]{" ", "UUID", "MBTI", "ZSGK"});
            buttonPanel.add(new JLabel("  处理类型"));
            buttonPanel.add(comboBox, gbc);
            comboBox.setSelectedItem("UUID");
            
            JButton goButton = new JButton("GO");
            buttonPanel.add(goButton, gbc);
            webScraperDialog.add(buttonPanel, java.awt.BorderLayout.NORTH);
            
            // 创建一个JTextArea来显示抓取的内容
            JTextArea resultArea = new JTextArea();
            JScrollPane scrollArea = new JScrollPane(resultArea);
            webScraperDialog.add(scrollArea, java.awt.BorderLayout.CENTER);
            // 设置 JTextArea , 如果内容过多（如 10,000+ 行），考虑使用 JList 替代 JTextArea。
            resultArea.setLineWrap(false);  // 自动换行
            resultArea.setWrapStyleWord(false); // 按单词换行
            
            // 创建右键菜单
            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem exportItem = new JMenuItem("导出");
            //popupMenu.add(copyItem);
            popupMenu.add(exportItem);
            
            // 添加鼠标双击事件监听器
            resultArea.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        if(webScraperDialog.getWidth()>1000) {
                            webScraperDialog.setSize(new Dimension(1000, 600));
                        }else {
                            java.awt.GraphicsConfiguration gc = java.awt.GraphicsEnvironment
                                    .getLocalGraphicsEnvironment()
                                    .getDefaultScreenDevice()
                                    .getDefaultConfiguration();

                            java.awt.Rectangle bounds = gc.getBounds(); // 整个屏幕的尺寸
                            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc); // 系统边距（如任务栏）

                            int availableWidth = bounds.width - insets.left - insets.right;
                            int availableHeight = bounds.height - insets.top - insets.bottom;
                                
                            webScraperDialog.setSize(new Dimension(availableWidth, availableHeight));
                        }
                        webScraperDialog.setLocationRelativeTo(null); 
                    }
                }
            });
            
            // 网址双击事件（文件名转换小功能）
            urlField.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        String content = resultArea.getText();
                        String transformed = getTransformedCode(content);
                        resultArea.setText(transformed);
                    }
                }
            });
            
            // 设置导出菜单项的点击事件
            exportItem.addActionListener(e -> {
                exportContent();
            });

            // 绑定右键菜单到 resultArea
            resultArea.setComponentPopupMenu(popupMenu);
            
            // 设置打开按钮的监听器
            openButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // 驱动路径 // https://googlechromelabs.github.io/chrome-for-testing/
                    String chromeWebDriverDir = "C:/pro/chromedriver-win64/135.0.7049.95/chromedriver.exe";
                    // 页面地址
                    String url = urlField.getText();
                    try {
                        openChrome(chromeWebDriverDir, url);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            
            // 处理类型选择项改变事件
            comboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String type = (String) comboBox.getSelectedItem();
                    if (type.equals("UUID")) {
                        urlField.setText("");
                    } else if (type.equals("MBTI")) {
                        urlField.setText("https://mbti9.hzshuf.cn/");
                    }else if (type.equals("ZSGK")) {
                        urlField.setText("https://mnzy.gaokao.cn/");  //掌上高考
                    }
                }
            });

            // 设置抓取按钮的监听器
            scrapeCfgButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showScrapeCfgDialog();
                }
            });
            
            // 设置go按钮的监听器
            goButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (driver == null && !"UUID".equals(comboBox.getSelectedItem())) {
                        JOptionPane.showMessageDialog(null, "请先打开网页！", "警告", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    new Thread(() -> {
                        // 在EDT中清空内容
                        SwingUtilities.invokeLater(() -> {
                            resultArea.setText("");
                        });
                        webScraperContent = new ArrayList<String>();
                        
                        String type = (String) comboBox.getSelectedItem();
                        if("UUID".equals(type)) {
                            for(int i=0;i<10;i++) {
                                try {
                                    String text = scrapeWebTxt(true);
                                    addWebTxt(resultArea, text);
                                    text = scrapeWebTxt(true);
                                    addWebTxt(resultArea, text.replaceAll("-","") + "...");
                                    text = scrapeWebTxt(true) + scrapeWebTxt(true) + scrapeWebTxt(true);
                                    addWebTxt(resultArea, text.replaceAll("-","") + "...");
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }else if("MBTI".equals(type)) {
                            for(int i=0;i<200;i++) {
                                try {
                                    String text = scrapeWebTxt(false);
                                    addWebTxt(resultArea, text);
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }else if("ZSGK".equals(type)) {
                            String table = scrapeWebTab();
                            // 将生成的HTML表格显示在JTextArea中
                            resultArea.setText(table);
                        }else {
                            JOptionPane.showMessageDialog(null, "请先选择处理类型！", "提示", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }).start();
                }
            });
            
            // 这个最好放在最后，否则会出现视图问题。
            webScraperDialog.setVisible(true);
        }

        // 文件名转码和还原
        public String getTransformedCode(String content) {
            String[] codes = content.split("\\r?\\n|\\r");  // 兼容所有换行符的分割方式
            String nl = System.getProperty("line.separator");
            StringBuilder ret = new StringBuilder();
            ret.append(codes[0].equals("1")?"2":"1");
            boolean transformed = codes[0].equals("1")?false:true;
            for(int x=1; x<codes.length; x++) {
                ret.append(nl);
                String code = codes[x];
                String[] parts = code.split("-", 2);
                if (parts.length != 2) {
                    ret.append(code);
                    continue;
                }
                
                String prefix = parts[0];
                String numberStr = parts[1];
                Random random = new Random(System.currentTimeMillis() + System.nanoTime() + Runtime.getRuntime().freeMemory());
                
                String format = "";
                if(numberStr.indexOf(".")>0) {
                    format = numberStr.substring(numberStr.indexOf("."));
                    numberStr = numberStr.substring(0,numberStr.indexOf("."));
                }
                
                if(transformed) {
                    // 还原前缀部分：每隔一个字符取原始字符
                    for (int i = 1; i < prefix.length(); i += 2) {
                        ret.append(prefix.charAt(i));
                    }
                    ret.append("-");
                    
                    // 还原数字部分
                    if (!numberStr.isEmpty()) {
                        char baseChar = numberStr.charAt(0);
                        for (int i = 1; i < numberStr.length(); i++) {
                            char currentChar = numberStr.charAt(i);
                            int num = (currentChar - baseChar - 1) % 26;
                            ret.append(num<0?(num+26):num);
                        }
                    }
                } else {
                    prefix = prefix.toUpperCase();
                    // 处理前缀部分：每个字符前添加随机大写字母
                    for (char c : prefix.toCharArray()) {
                        char r = (char) (random.nextInt(26) + 'A');
                        ret.append(r).append(c);
                    }
                    ret.append("-");
                    
                    // 处理数字部分
                    if (!numberStr.isEmpty()) {
                        // 随机选择A-F作为基准字母
                        char baseChar = (char) ('A' + random.nextInt(18)); // A-F
                        ret.append(baseChar); // 第一位是基准字母
                        for (int i = 0; i < numberStr.length(); i++) {
                            char digit = numberStr.charAt(i);
                            int num = digit - '0';
                            char transformedChar = (char) (baseChar + 1 + num); // 基准字母后第num个字母
                            ret.append(transformedChar > 'Z'?(char) (transformedChar - 26):transformedChar);
                        }
                    }
                }
                ret.append(format);
            }
            return ret.toString();
        }
        
        public String scrapeWebTab() {
            // 创建HTML表格头部
            String table = "<table border='1'><tr><th>内容</th></tr>";

            // 获取class名为“h-60vh pb-10px max-h-650px min-h-250px scrollbar_Ps_vK”的div下的所有子div
            WebElement root = driver.findElement(By.cssSelector(".h-60vh.pb-10px.max-h-650px.min-h-250px.scrollbar_Ps_vK > div"));
            List<WebElement> elements = root.findElements(By.tagName("div"));

            // 遍历子div，获取内容并添加到HTML表格中
            for (WebElement element : elements) {
                table += "<tr><td>" + element.getText() + "</td></tr>";
            } 

            // 添加表格底部
            table += "</table>";

            return table;
        }
        
        public String scrapeWebTxt(boolean test) throws Exception {
            String text = "";
            if(test) {
                text = UUID.randomUUID().toString();
                TimeUnit.MILLISECONDS.sleep(20);
            }else {
                int idx = webScraperContent.size()+1;
                String index = idx<100?String.format("%03d", idx):idx+"";
                WebElement q = driver.findElement(By.cssSelector(".team_t"));
                String question = q.getText();
                question = question.replaceAll(",", "，");
                List<WebElement> a = driver.findElements(By.cssSelector(".team_item"));
                String answer = "";
                for(int i=0;i<a.size();i++) {
                    String b = a.get(i).getText();
                    b = b.replaceAll(",", "，");
                    answer = answer + "," + b; 
                }
                text = "No" + index + "," + question + answer;
                a.get(0).click();
                TimeUnit.MILLISECONDS.sleep(1000);
            }
            return text;
        }
        
        public void addWebTxt(JTextArea resultArea, String text) {
            webScraperContent.add(text);
            
            // 在EDT中更新UI
            SwingUtilities.invokeLater(() -> {
                resultArea.append(webScraperContent.size() + ". " + text + "\n");
                // 动态调整 JTextArea 高度（基于行数）
                resultArea.setRows(webScraperContent.size());
                // 确保滚动到底部
                resultArea.setCaretPosition(resultArea.getDocument().getLength());
            });
        }
        
        public void exportContent() {
            if (webScraperContent == null || webScraperContent.isEmpty()) {
                JOptionPane.showMessageDialog(null, "没有内容可导出！", "警告", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String fileName = "D://web_" + (new java.text.SimpleDateFormat("yyyyMMddHHmmssSSS").format(new java.util.Date())) + ".csv";
            txtFileSave(new File(fileName), getContent(webScraperContent));
            JOptionPane.showMessageDialog(null, "导出成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
        }
        
        /** 获取Driver对象 */
        public WebDriver openChrome(String webDriverDir, String url) throws Exception {

            // 加载驱动
            // https://registry.npmmirror.com/binary.html?path=chromedriver/
            System.setProperty("webdriver.chrome.driver", webDriverDir);

            // 设置参数
            ChromeOptions chromeOptions = new ChromeOptions();
            chromeOptions.addArguments("no-sandbox");//设置无沙盒访问
            chromeOptions.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"}); //不显示“正受到自动测试软件控制”
            //chromeOptions.addArguments("--disable-dev-shm-usage");//这将强制Chrome使用该/tmp目录。尽管这将减慢执行速度，因为将使用磁盘而不是内存（目的:在docker容器中不加会导致失败）
            //chromeOptions.addArguments("--headless");

            // 创建chrome驱动对象并等待一会
            driver = new ChromeDriver(chromeOptions);

            // 最大化窗口
            driver.manage().window().maximize();
            // 打开url
            driver.get(url);
            
            String windowHandle = driver.getWindowHandle();
            System.out.println("WindowHandle:" + windowHandle);
            
            return driver;
        }

        /** 显示网页抓取配置界面*/
        private void showScrapeCfgDialog() {
            // 创建对话框
            JDialog dialog = new JDialog(FileManagerUI.this, "Web Scraper Config", true);
            dialog.setSize(1200, 800);
            dialog.setLayout(new BorderLayout());
            dialog.setLocationRelativeTo(null);

            // 上部分面板, 是否循环复选框、循环次数文本框
            JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JCheckBox loopCheckBox = new JCheckBox("是否循环");
            topPanel.add(loopCheckBox);
            topPanel.add(new JLabel("循环次数:"));
            JTextField loopCountField = new JTextField(5);
            loopCountField.setText("1");
            topPanel.add(loopCountField);
            
            // 添加弹簧使确定按钮居右
            topPanel.add(Box.createHorizontalGlue());
            JButton okButton = new JButton("确定");
            topPanel.add(okButton);
            dialog.add(topPanel, BorderLayout.NORTH);
            
            // 绑定确定按钮事件
            okButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // 获取输入值
                    boolean isLoop = loopCheckBox.isSelected();
                    int loopCount = Integer.parseInt(loopCountField.getText());
                    System.out.println("是否循环: " + isLoop + ", 循环次数: " + loopCount);
                    
                    int rows = webScraperCfgTabModel.getRowCount();
                    webScraperCfgData = new Object[rows][];
                    
                    // 获取表格数据
                    for (int i = 0; i < rows; i++) {
                        String locateType = (String) webScraperCfgTabModel.getValueAt(i, 0);
                        String attribute = (String) webScraperCfgTabModel.getValueAt(i, 1);
                        String action = (String) webScraperCfgTabModel.getValueAt(i, 2);
                        String remark = (String) webScraperCfgTabModel.getValueAt(i, 3);
                        System.out.println(locateType + " | " + attribute + " | " + action + " | " + remark);
                        webScraperCfgData[i] = new Object[] {locateType, attribute, action, remark};
                    }
                    
                    dialog.dispose();
                }
            });

            // 中间部分面板 - 表格
            String[] columnNames = {"定位类型", "属性值", "操作类型", "说明"};
            String[] locateType = new String[]{"byName", "byClass", "byId"};
            String[] actionType = new String[]{"取值", "点击"};
            String[] defaultVal = new String[]{"byName", "", "取值", ""};
            Object[][] data = { defaultVal };
            if(webScraperCfgData != null) {
                data = webScraperCfgData;
            }

            // 自定义表格模型 ， DefaultTableModel只负责数据和结构，与列宽无关。
            webScraperCfgTabModel = new DefaultTableModel(data, columnNames);
            JTable table = new JTable(webScraperCfgTabModel);

            // 设置第一列(定位类型)为下拉框
            JComboBox<String> locateTypeCombo = new JComboBox<>(locateType);
            table.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(locateTypeCombo));
            
            // 设置第三列(操作类型)为下拉框
            JComboBox<String> actionTypeCombo = new JComboBox<>(actionType);
            table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(actionTypeCombo));

            // 设置表格属性
            table.setRowHeight(25);
            table.getTableHeader().setReorderingAllowed(false);
            
            // 添加表格到滚动面板, 创建包含表格和按钮的面板
            JScrollPane scrollPane = new JScrollPane(table);
            JPanel centerPanel = new JPanel(new BorderLayout());
            centerPanel.add(scrollPane, BorderLayout.CENTER);
            
            // 设置表格列宽比例（总宽度800像素）
            int[] columnWidths = {10, 50, 10, 30}; // 比例值
            for (int i = 0; i < columnWidths.length; i++) {
                int width = (int)(1200 * columnWidths[i] / 100.0);
                table.getColumnModel().getColumn(i).setPreferredWidth(width); 
            }
            table.revalidate();
            table.repaint();
            
            // 底部按钮面板
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton addButton = new JButton("添加行");
            buttonPanel.add(addButton);
            JButton deleteButton = new JButton("删除行");
            buttonPanel.add(deleteButton);
            JButton clearButton = new JButton("清空表格");
            buttonPanel.add(clearButton);
            centerPanel.add(buttonPanel, BorderLayout.SOUTH);
            dialog.add(centerPanel, BorderLayout.CENTER);
            
            // 绑定添加行按钮事件
            addButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    webScraperCfgTabModel.addRow(defaultVal);
                    // 滚动到最后一行
                    table.scrollRectToVisible(table.getCellRect(table.getRowCount()-1, 0, true));
                }
            });
             
            // 绑定删除行按钮事件
            deleteButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int selectedRow = table.getSelectedRow();
                    if (selectedRow >= 0) {
                        webScraperCfgTabModel.removeRow(selectedRow);
                    } else {
                        JOptionPane.showMessageDialog(dialog, "请先选择要删除的行", "提示", JOptionPane.WARNING_MESSAGE);
                    }
                }
            });

            // 绑定清空表格按钮事件
            clearButton.addActionListener(e -> {
                int option = JOptionPane.showConfirmDialog(dialog, "确定要清空表格吗？", "确认", JOptionPane.YES_NO_OPTION);
                if(option == JOptionPane.YES_OPTION) {
                    webScraperCfgTabModel.setRowCount(0);
                }
            });

            // 显示配置面板
            dialog.setVisible(true);
        }
        
    }

    /***** 长文本编辑界面  *****/
    protected class LongTxtCoder {
        // 编辑器界面组件
        private JDialog editorFrame;
        private JTextArea textArea;
        private JTextArea lineNumbers;
        private JButton openButton;
        private JButton confirmButton;
        private JButton gotoButton;
        private JButton sha512Button;
        private JFileChooser fileChooser;
        private JLabel statusLabel;
        private JScrollPane textScrollPane;
        private JScrollPane lineNumberScrollPane;
        
        // 文件信息
        private File currentFile;
        private Charset currentCharset;
        private String originalContent;
        private boolean isUpdatingLineNumbers = false;

        
        public void showDialog(String code) {
            //editorFrame = new JFrame("长文本编码");
            editorFrame = new JDialog(FileManagerUI.this, "设置初始编码", true);
            editorFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            editorFrame.setSize(850, 600);
            editorFrame.setLocationRelativeTo(null);

            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

            // 创建按钮面板
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
            openButton = new JButton("打开文件");
            openButton.setToolTipText("使用文本文件的内容作为文件转码的编码种子。");
            gotoButton = new JButton("跳转到行");
            sha512Button = new JButton("查看哈希值");
            sha512Button.setToolTipText("查看内容生成的SHA512的哈希值，文本内容稍加改变，将生成不同的哈希值。");
            confirmButton = new JButton("确认编码");
            confirmButton.setToolTipText("长文本编码可增加文件被暴力破解的计算量。");
            
            buttonPanel.add(openButton);
            buttonPanel.add(gotoButton);
            buttonPanel.add(sha512Button);
            buttonPanel.add(confirmButton);

            // 创建状态栏
            statusLabel = new JLabel("就绪");
            statusLabel.setBorder(BorderFactory.createLoweredBevelBorder());
            statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            
            // 创建文本区域
            textArea = new JTextArea();
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
            textArea.setText(code.trim());
            
            // 创建行号区域 - 放在右侧，左对齐
            lineNumbers = new JTextArea("1");
            lineNumbers.setBackground(new Color(240, 240, 240));
            lineNumbers.setEditable(false);
            lineNumbers.setFont(new Font("Monospaced", Font.PLAIN, 14));
            lineNumbers.setMargin(new Insets(0, 5, 0, 5));
            
            // 创建滚动面板
            textScrollPane = new JScrollPane(textArea);
            lineNumberScrollPane = new JScrollPane(lineNumbers);
            
            // 配置行号滚动面板
            lineNumberScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
            lineNumberScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            lineNumberScrollPane.setBorder(null);
            lineNumberScrollPane.setPreferredSize(new Dimension(50, textArea.getHeight()));
            
            // 保存当前滚动位置
            final int[] lastScrollPosition = {0};
            
            // 同步滚动 - 只同步文本区域到行号区域
            textScrollPane.getViewport().addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    if (!isUpdatingLineNumbers) {
                        JViewport source = (JViewport) e.getSource();
                        Point p = source.getViewPosition();
                        lineNumberScrollPane.getViewport().setViewPosition(p);
                        lastScrollPosition[0] = p.y;
                    }
                }
            });
            
            // 创建文本显示面板 - 行号放在右侧
            JPanel textPanel = new JPanel(new BorderLayout());
            textPanel.add(textScrollPane, BorderLayout.CENTER);
            textPanel.add(lineNumberScrollPane, BorderLayout.EAST);

            // 将组件添加到主面板
            mainPanel.add(buttonPanel, BorderLayout.NORTH);
            mainPanel.add(textPanel, BorderLayout.CENTER);
            mainPanel.add(statusLabel, BorderLayout.SOUTH);

            editorFrame.add(mainPanel);

            // 初始化文件选择器
            fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith(".txt");
                }

                @Override
                public String getDescription() {
                    return "文本文件 (*.txt)";
                }
            });

            // 添加事件监听器
            openButton.addActionListener(this::openFileAction);
            gotoButton.addActionListener(this::gotoLineAction);
            sha512Button.addActionListener(this::generateSHA512);
            confirmButton.addActionListener(this::confirmChangesAction);

            // 文档监听器
            textArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e) { 
                    SwingUtilities.invokeLater(() -> {
                        updateLineNumbers(lastScrollPosition[0]);
                    });
                }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { 
                    SwingUtilities.invokeLater(() -> {
                        updateLineNumbers(lastScrollPosition[0]);
                    });
                }
                public void changedUpdate(javax.swing.event.DocumentEvent e) {
                    SwingUtilities.invokeLater(() -> {
                        updateLineNumbers(lastScrollPosition[0]);
                    });
                }
            });
            
            editorFrame.setVisible(true);
        }

        private void updateLineNumbers(int lastScrollPosition) {
            isUpdatingLineNumbers = true;
            
            try {
                String text = textArea.getText();
                int lineCount = text.isEmpty() ? 1 : textArea.getLineCount();
                
                StringBuilder numbers = new StringBuilder();
                for (int i = 1; i <= lineCount; i++) {
                    numbers.append(i).append("\n");
                }
                
                // 保存当前行号区域的滚动位置
                //int oldScrollPosition = lineNumberScrollPane.getViewport().getViewPosition().y;
                
                lineNumbers.setText(numbers.toString());
                
                // 恢复滚动位置
                SwingUtilities.invokeLater(() -> {
                    lineNumberScrollPane.getViewport().setViewPosition(
                        new Point(0, Math.min(lastScrollPosition, lineNumbers.getHeight()))
                    );
                    
                    // 调整行号区域宽度
                    int maxDigits = String.valueOf(lineCount).length();
                    int width = maxDigits * 10 + 15;
                    lineNumberScrollPane.setPreferredSize(new Dimension(width, textArea.getHeight()));
                    
                    lineNumberScrollPane.revalidate();
                    lineNumberScrollPane.repaint();
                });
            } finally {
                isUpdatingLineNumbers = false;
            }
        }

        private void openFileAction(ActionEvent e) {
            int returnValue = fileChooser.showOpenDialog(editorFrame);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                currentFile = fileChooser.getSelectedFile();
                statusLabel.setText("正在加载文件...");
                
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        try {
                            currentCharset = detectCharset(currentFile);
                            try (BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(new FileInputStream(currentFile), currentCharset))) {
                                StringBuilder content = new StringBuilder();
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    content.append(line).append("\n");
                                }
                                originalContent = content.toString();
                                
                                SwingUtilities.invokeLater(() -> {
                                    textArea.setText(originalContent);
                                    updateFileInfo();
                                    updateLineNumbers(0);
                                });
                            }
                        } catch (Exception ex) {
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(editorFrame, 
                                    "读取文件时出错: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                                statusLabel.setText("加载文件失败");
                            });
                        }
                        return null;
                    }
                }.execute();
            }
        }

        @SuppressWarnings("deprecation")
        private void gotoLineAction(ActionEvent e) {
            String input = JOptionPane.showInputDialog(editorFrame, "请输入要跳转的行号:", "跳转到行", JOptionPane.PLAIN_MESSAGE);
            if (input == null || input.trim().isEmpty()) {
                return;
            }

            try {
                int lineNumber = Integer.parseInt(input.trim());
                int lineCount = textArea.getLineCount();
                
                if (lineNumber < 1 || lineNumber > lineCount) {
                    JOptionPane.showMessageDialog(editorFrame, 
                        "行号必须在1到" + lineCount + "之间", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // 定位到指定行
                int offset = textArea.getLineStartOffset(lineNumber - 1);
                textArea.setCaretPosition(offset);
                
                // 滚动到该行
                java.awt.Rectangle viewRect = textArea.modelToView(offset);
                if (viewRect != null) {
                    textArea.scrollRectToVisible(viewRect);
                }
                
                // 高亮当前行
                textArea.getCaret().setSelectionVisible(true);
                textArea.select(offset, textArea.getLineEndOffset(lineNumber - 1));
                
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(editorFrame, "请输入有效的行号", "错误", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(editorFrame, "定位行号时出错: " + ex.getMessage(), 
                    "错误", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void generateSHA512(ActionEvent e) {
            String text = textArea.getText();
            if (text.isEmpty()) {
                JOptionPane.showMessageDialog(editorFrame, "文本区域为空", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-512");
                byte[] hashBytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
                
                StringBuilder hexString = new StringBuilder();
                for (byte b : hashBytes) {
                    hexString.append(String.format("%02x", b));
                }
                
                JOptionPane.showMessageDialog(editorFrame, "SHA-512哈希值:\n" + hexString.toString(), 
                    "SHA-512结果", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(editorFrame, "生成SHA-512时出错: " + ex.getMessage(), 
                    "错误", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void confirmChangesAction(ActionEvent e) {
            String currentContent = textArea.getText();
            if (currentContent==null || currentContent.equals("") || currentContent.equals(originalContent)) {
                JOptionPane.showMessageDialog(editorFrame, "内容为空或未修改，请选择文件并修改后再确认。", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int option = JOptionPane.showConfirmDialog(editorFrame, 
                "请牢记所选文件及其版本，以及修改的具体内容，以便恢复转码(加密)的文件。", "确认牢记编码", JOptionPane.YES_NO_OPTION);
            
            if (option == JOptionPane.YES_OPTION) {
                //mainTextField.setText(currentContent);
                //contentEncoding = currentContent;
                encodingTextField.setText(currentContent);
                editorFrame.dispose();
            }
        }

        private void updateFileInfo() {
            if (currentFile == null) return;
            
            try {
                BasicFileAttributes attrs = Files.readAttributes(currentFile.toPath(), BasicFileAttributes.class);
                long size = attrs.size();
                Date modifiedDate = new Date(attrs.lastModifiedTime().toMillis());
                
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String info = String.format("文件: %s | 编码: %s | 大小: %d 字节 | 修改时间: %s",
                    currentFile.getAbsolutePath(),
                    currentCharset.name(),
                    size,
                    dateFormat.format(modifiedDate));
                
                statusLabel.setText(info);
            } catch (IOException e) {
                statusLabel.setText("无法获取文件信息");
            }
        }

        private Charset detectCharset(File file) throws IOException {
            String[] encodings = {"UTF-8", "GBK", "GB2312", "ISO-8859-1", "UTF-16", "Windows-1252"};
            for (String encoding : encodings) {
                try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), encoding)) {
                    char[] buffer = new char[1024];
                    int read = reader.read(buffer);
                    if (read > 0) {
                        boolean valid = true;
                        for (int i = 0; i < read; i++) {
                            if (buffer[i] == '\ufffd') {
                                valid = false;
                                break;
                            }
                        }
                        if (valid) {
                            return Charset.forName(encoding);
                        }
                    }
                } catch (Exception e) {
                    // 继续尝试下一个编码
                }
            }
            return StandardCharsets.UTF_8;
        }

        public Charset detectCharsetByte(File file) throws IOException {
            // 简单的编码检测方法，实际项目中可以使用juniversalchardet等库
            try (InputStream inputStream = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int read = inputStream.read(buffer);
                
                // 检查BOM标记
                if (read >= 3 && buffer[0] == (byte)0xEF && buffer[1] == (byte)0xBB && buffer[2] == (byte)0xBF) {
                    return StandardCharsets.UTF_8;
                }
                if (read >= 2 && buffer[0] == (byte)0xFE && buffer[1] == (byte)0xFF) {
                    return StandardCharsets.UTF_16BE;
                }
                if (read >= 2 && buffer[0] == (byte)0xFF && buffer[1] == (byte)0xFE) {
                    return StandardCharsets.UTF_16LE;
                }
                
                // 尝试解码为UTF-8
                try {
                    String test = new String(buffer, 0, read, StandardCharsets.UTF_8);
                    if (test.indexOf('\uFFFD') == -1) { // 没有替换字符
                        return StandardCharsets.UTF_8;
                    }
                } catch (Exception ignored) {}
                
                // 尝试解码为GBK（常见的中文编码）
                try {
                    String test = new String(buffer, 0, read, Charset.forName("GBK"));
                    if (test.indexOf('\uFFFD') == -1) {
                        return Charset.forName("GBK");
                    }
                } catch (Exception ignored) {}
                
                // 默认返回系统默认编码
                return Charset.defaultCharset();
            }
        }
    }
}
