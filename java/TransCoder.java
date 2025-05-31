import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.metal.MetalIconFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class TransCoder extends JFrame {
    private String encodingTxt = "";           // 初始编码
    private int cycleCount = 20;               // 创建密钥循环次数
    private boolean includeFileName = false;   // 文件名是否参与编码
    private JTextArea infoArea;                // 日志信息显示区域
    private JProgressBar progressBar;          // 进度条
    private boolean codeTip = true;            // 是否在日志中显示部分编码提示
    
    public TransCoder() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("文件转码工具【XO】");
        setSize(1000, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);  // 将窗口设置为屏幕中央显示

        // 设置图标
        byte[] iconBytes = java.util.Base64.getDecoder().decode(iconBase64Str);
        Image icon = Toolkit.getDefaultToolkit().createImage(iconBytes);
        setIconImage(icon);
        
        // 文件菜单
        JMenu fileMenu = new JMenu("文件");
        JMenuItem packItem = new JMenuItem("打包");
        JMenuItem unpackItem = new JMenuItem("解压");
        JMenuItem exitMenuItem = new JMenuItem("退出");
        fileMenu.add(packItem);
        fileMenu.add(unpackItem);
        fileMenu.add(new JSeparator());
        fileMenu.add(exitMenuItem);
        
        // 配置菜单
        JMenu configMenu = new JMenu("配置");
        JMenuItem encodingItem = new JMenuItem("初始编码");
        JMenuItem settingsItem = new JMenuItem("循环次数");
        configMenu.add(encodingItem);
        configMenu.add(settingsItem);
        
        // 帮助菜单
        JMenu helpMenu = new JMenu("帮助");
        JMenuItem aboutItem = new JMenuItem("关于");
        JMenuItem donateItem = new JMenuItem("捐赠");
        helpMenu.add(aboutItem);
        helpMenu.add(donateItem);
        
        // 创建菜单栏
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        menuBar.add(configMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
        
        // 按钮区
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        // 第一行
        JPanel firstRow = new JPanel(new FlowLayout(FlowLayout.LEFT));  // FlowLayout(FlowLayout.LEFT)  GridBagLayout()
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;
        gbc.insets = new Insets(1, 10, 1, 10);

        JButton packBtn = new JButton("打包");
        packBtn.setIcon(MetalIconFactory.getTreeFloppyDriveIcon());
        firstRow.add(packBtn, gbc);
        JButton unpackBtn = new JButton("解压");
        unpackBtn.setIcon(MetalIconFactory.getFileChooserUpFolderIcon());
        firstRow.add(unpackBtn, gbc);
        JButton encodeBtn = new JButton("编码");
        encodeBtn.setIcon(MetalIconFactory.getTreeHardDriveIcon());
        firstRow.add(encodeBtn, gbc);
        JButton configBtn = new JButton("配置");
        configBtn.setIcon(MetalIconFactory.getFileChooserDetailViewIcon());
        firstRow.add(configBtn, gbc);
        JButton aboutBtn = new JButton("关于");
        aboutBtn.setIcon(MetalIconFactory.getHorizontalSliderThumbIcon());
        firstRow.add(aboutBtn, gbc);


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
        buttonPanel.add(Box.createVerticalStrut(1));
        buttonPanel.add(secondRow);

        // 列表区
        infoArea = new JTextArea();
        infoArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(infoArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 添加到主界面
        add(buttonPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        
        // 编码按钮事件
        encodeBtn.addActionListener(e -> showEncodingDialog());
        
        // 打包按钮事件
        packBtn.addActionListener(e -> {
            if (encodingTxt.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请先点击编码按钮设置初始编码。", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            JFileChooser dirChooser = new JFileChooser();
            dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            dirChooser.setDialogTitle("请选择要打包的目录");
            
            if (dirChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {

                JFileChooser saveChooser = new JFileChooser();
                saveChooser.setDialogTitle("保存ZIP文件");
                saveChooser.setFileFilter(new FileNameExtensionFilter("ZIP文件", "zip"));
                
                if (saveChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    // 统计待打包文件信息
                    File dir = dirChooser.getSelectedFile();
                    Map<String, File> fileMap = new HashMap<>();
                    countFiles(dir, dir, fileMap);

                    // 创建打包文件
                    File selectedFile = saveChooser.getSelectedFile();
                    String zipFilePath = selectedFile.getAbsolutePath();
                    if (!zipFilePath.toLowerCase().endsWith(".zip")) {
                        zipFilePath += ".zip";
                    }
                    File zipFile = new File(zipFilePath);
                    
                    new Thread(() -> {
                        try {
                            packDirectory(dir, zipFile, fileMap);
                            SwingUtilities.invokeLater(() -> {
                                logMessage("打包完成: " + zipFile.getAbsolutePath() );
                                progressBar.setVisible(false);
                            });
                        } catch (IOException ex) {
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(this, "打包失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                                progressBar.setVisible(false);
                            });
                        }
                    }).start();
                }
            }
        });
        
        // 解压按钮事件
        unpackBtn.addActionListener(e -> {
            if (encodingTxt.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请先点击编码按钮设置初始编码。", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            JFileChooser zipChooser = new JFileChooser();
            zipChooser.setDialogTitle("选择要解压的ZIP文件");
            zipChooser.setFileFilter(new FileNameExtensionFilter("ZIP文件", "zip"));
            
            if (zipChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {

                JFileChooser dirChooser = new JFileChooser();
                dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                dirChooser.setDialogTitle("选择解压目录");
                
                if (dirChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    File zipFile = zipChooser.getSelectedFile();
                    File destDir = dirChooser.getSelectedFile();
                    
                    new Thread(() -> {
                        try {
                            unpackZip(zipFile, destDir);
                            SwingUtilities.invokeLater(() -> {
                                logMessage("解压完成到: " + destDir.getAbsolutePath() );
                                progressBar.setVisible(false);
                            });
                        } catch (IOException ex) {
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(this, "解压失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                                progressBar.setVisible(false);
                            });
                        }
                    }).start();
                }
            }
        });
        
        // 配置按钮事件
        configBtn.addActionListener(e -> showSettingsDialog());
        
        // 关于按钮事件
        aboutBtn.addActionListener(e ->  JOptionPane.showMessageDialog(this, 
            "文件转码工具(J)\n" + //
            "版本 1.0\n" + //
            "2025-05-31\n" + //
            "ByXO\n\n" + //
            "1、通过可配置的初始编码、循环次数，多次分别获取SHA512编码，拼接成转码编码。\n" + //
            "2、打包和解压文件时，对文件内容（每个字节）分别按转码编码进行转换，实现文件的加密和解密。\n" + //
            "3、太短的初始编码很容易被暴力破解，直接使用文本文件也存在枚举风险，使用较大的文本文件，“随机”修改文件某处的内容，会显著提高破解难度。\n" + //
            "4、请牢记初始编码、循环次数等转码信息，否则会导致打包后的文件无法恢复（解密）。", 
        "关于", JOptionPane.INFORMATION_MESSAGE));

        // 菜单项事件
        packItem.addActionListener(e -> packBtn.doClick());
        unpackItem.addActionListener(e -> unpackBtn.doClick());
        encodingItem.addActionListener(e -> encodeBtn.doClick());
        settingsItem.addActionListener(e -> configBtn.doClick());
        aboutItem.addActionListener(e -> aboutBtn.doClick());
        donateItem.addActionListener(e -> showDonateDialog());
    }
    
    private void countFiles(File rootDir, File currentDir, Map<String, File> fileMap) {
        File[] files = currentDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    countFiles(rootDir, file, fileMap);
                } else {
                    String relativePath = rootDir.toPath().relativize(file.toPath()).toString();
                    fileMap.put(relativePath, file);
                }
            }
        }
    }
    
    private void showEncodingDialog(){
        (new LongTxtCoder()).showDialog(this, encodingTxt);
    }

    private void showDonateDialog() {
        //System.out.println(imageToBase64("E:/tmp/1.jpg"));
        //System.out.println(imageToBase64("E:/tmp/2.jpg"));
        try {
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
            //panelAli.add(textLabelAli, BorderLayout.SOUTH);

            JPanel panelWechat = new JPanel(new BorderLayout());
            panelWechat.add(labelWechat, BorderLayout.CENTER);
            //panelWechat.add(textLabelWechat, BorderLayout.SOUTH);

            JPanel panelBlank = new JPanel();
            panelBlank.setBorder(BorderFactory.createEmptyBorder(0, 50, 0, 0)); // 设置边界，创建空白效果
            
            // 创建一个主面板来放置两个组合面板，并设置中间的空白
            JPanel mainImagePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0)); // 20 像素的水平间距
            mainImagePanel.add(panelAli);
            mainImagePanel.add(panelBlank);
            mainImagePanel.add(panelWechat);

            // 弹出对话框显示图片
            JOptionPane.showMessageDialog(this, mainImagePanel, "捐赠【FinderDataRoom】", JOptionPane.PLAIN_MESSAGE);
        }catch (IOException err) {
            err.printStackTrace();
        }
    }

    private void showSettingsDialog() {
        JDialog dialog = new JDialog(this, "配置设置", true);
        dialog.setSize(280, 200);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout());
        
        // 顶部按钮面板
        JPanel topButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton confirmBtn = new JButton("确认");
        JButton cancelBtn = new JButton("取消");
        topButtonPanel.add(confirmBtn);
        topButtonPanel.add(cancelBtn);
        
        JPanel contentPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 80));
        
        JLabel iterationLabel = new JLabel("创建密钥循环次数（1~200）:");
        JSpinner iterationSpinner = new JSpinner(new SpinnerNumberModel(cycleCount, 1, 200, 1));
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) iterationSpinner.getEditor();  // 获取编辑器
        JTextField textField = editor.getTextField();                            // 获取编辑器中的文本框
        textField.setHorizontalAlignment(JTextField.LEFT);                       // 设置文本框的对齐方式为左对齐
        iterationSpinner.setPreferredSize(new Dimension(20, 25));   // 设置spinner的宽度和高度
        JCheckBox fileNameCheckBox = new JCheckBox("文件名参与编码");
        fileNameCheckBox.setSelected(includeFileName);
        
        contentPanel.add(iterationLabel);
        contentPanel.add(iterationSpinner);
        contentPanel.add(fileNameCheckBox);
        
        confirmBtn.addActionListener(e -> {
            cycleCount = (Integer) iterationSpinner.getValue();
            includeFileName = fileNameCheckBox.isSelected();
            logMessage(String.format("循环次数: %d K, 文件名参与编码: %s", cycleCount, includeFileName?"是":"否"));
            dialog.dispose();
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        panel.add(contentPanel, BorderLayout.CENTER);
        panel.add(topButtonPanel, BorderLayout.SOUTH);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private void packDirectory(File dir, File zipFile, Map<String, File> fileMap) throws IOException {
        SwingUtilities.invokeLater(() -> {
            logMessage("开始打包目录: " + dir.getAbsolutePath() + ", 文件总数: " + fileMap.size());
            progressBar.setVisible(true);
            progressBar.setValue(0);
            progressBar.setMaximum(fileMap.size());
        });
        
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            int processed = 0;

            String xorKey = null;
            if(!includeFileName){
                xorKey = generateXorKey("");
            }

            for (Map.Entry<String, File> entry : fileMap.entrySet()) {
                String relativePath = entry.getKey();
                String filePath = relativePath.replace("\\", "/"); // 替换反斜杠为正斜杠
                File file = entry.getValue();
                
                final int currentProcessed = ++processed;
                SwingUtilities.invokeLater(() -> {
                    logMessage("正在处理" + currentProcessed + "/" + fileMap.size() + ": " + filePath );
                    infoArea.setCaretPosition(infoArea.getDocument().getLength());
                    progressBar.setValue(currentProcessed);
                });
                
                ZipEntry zipEntry = new ZipEntry(filePath);
                zos.putNextEntry(zipEntry);
                
                byte[] fileContent = Files.readAllBytes(file.toPath());

                String finalXorKey = null;
                if(includeFileName){
                    String fileName = filePath;
                    finalXorKey = generateXorKey(fileName);
                }else{
                    /*
                    byte[] prefix = getPathPreKey(filePath);
                    ByteArrayOutputStream combined = new ByteArrayOutputStream();
                    combined.write(prefix);
                    combined.write(xorKey);
                    xorKey = combined.toByteArray();  //  */
                    String prefix = getPathPreKey(filePath);
                    finalXorKey = prefix + xorKey;  // 使用前缀和xorKey拼接成新的密钥
                }
                byte[] transformedContent = transformBytes(fileContent, finalXorKey);
                
                zos.write(transformedContent,0, transformedContent.length);
                zos.closeEntry();
                System.out.println("打包文件: " + filePath + ", 大小: " + transformedContent.length + " 字节");
            }
        }
    }
    
    private void unpackZip(File zipFile, File destDir) throws IOException {
        try (ZipFile zip = new ZipFile(zipFile)) {
            int totalFiles = zip.size();
            
            SwingUtilities.invokeLater(() -> {
                logMessage("开始解压文件: " + zipFile.getAbsolutePath() + ", 文件总数: " + totalFiles);
                progressBar.setVisible(true);
                progressBar.setValue(0);
                progressBar.setMaximum(totalFiles);
            });
            
            Enumeration<? extends ZipEntry> entries = zip.entries();
            int processed = 0;

            //byte[] xorKey = null;
            String xorKey = null; 
            if(!includeFileName){
                xorKey = generateXorKey("");
            }
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File entryFile = new File(destDir, entry.getName());
                
                String entryName = entry.getName();
                String fileName = entryName.replace("\\", "/"); // 替换反斜杠为正斜杠

                final int currentProcessed = ++processed;
                SwingUtilities.invokeLater(() -> {
                    logMessage("正在处理 " + currentProcessed + "/" + totalFiles + ": " + fileName );
                    infoArea.setCaretPosition(infoArea.getDocument().getLength());
                    progressBar.setValue(currentProcessed);
                });
                
                // 创建父目录
                entryFile.getParentFile().mkdirs();
                
                if (entry.isDirectory()) {
                    entryFile.mkdir();
                } else {
                    try (InputStream is = zip.getInputStream(entry)) {
                        //byte[] entryContent = is.readAllBytes();
                        //*
                        int len = (int)entry.getSize();
                        byte[] entryContent = new byte[len]; 
                        byte[] buffer = new byte[1024];
                        int length;
                        int destPos = 0;
                        while ((length = is.read(buffer)) > 0) {
                            System.arraycopy(buffer, 0, entryContent, destPos, length);
                            destPos = destPos + length;
                        } // */
                        String finalXorKey = null;
                        if(includeFileName){
                            finalXorKey = generateXorKey(fileName);
                        }else{
                            /*
                            byte[] prefix = getPathPreKey(fileName);
                            ByteArrayOutputStream combined = new ByteArrayOutputStream();
                            combined.write(prefix);
                            combined.write(xorKey);
                            xorKey = combined.toByteArray();  //  */
                            String prefix = getPathPreKey(fileName);
                            finalXorKey = prefix + xorKey;
                        }
                        byte[] transformedContent = transformBytes(entryContent, finalXorKey);
                        Files.write(entryFile.toPath(), transformedContent);
                    }
                }
            }
        }
    }
    
    private byte[] transformBytes(byte[] input, String xorKey ) {
        byte[] result = new byte[input.length];
        int len = xorKey.length();
        for (int i = 0; i < input.length; i++) {
            //result[i] = (byte) (input[i] ^ xorKey[i % len]);
            char c = xorKey.charAt(i % len);
            int ascii = (int)c;
            result[i] = (byte) (input[i] ^ ascii);
        }
        
        return result;
    }
    
    private String generateXorKey(String filePath) {
        try {
            // 处理初始编码
            String encodingTxtStd = encodingTxt.replaceAll("\r\n", "\n");  //强制统一换行符
            byte[] encodingTxtStdBytes = encodingTxtStd.getBytes(StandardCharsets.UTF_8);
            logMessage(String.format("初始编码( %d 字节): %s ...", encodingTxtStdBytes.length, codeTip?(encodingTxt.substring(0, Math.min(encodingTxtStd.length(), 8))):"" ));
            
            int cycleTimes = cycleCount * 1000;
            //ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            StringBuilder outputBuilder = new StringBuilder();
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            for (int i = 0; i < cycleTimes; i++) {
                String encoding = filePath + encodingTxtStd + String.valueOf(i);
                byte[] encodingBytes = encoding.getBytes(StandardCharsets.UTF_8);
                byte[] hash = digest.digest(encodingBytes);
                //outputStream.write(hash);
                String hashStr = byteToHex(hash);
                outputBuilder.append(hashStr);
                if (i==0 || i == (cycleTimes-1) || (i+1) % 10000 == 0) { 
                    logMessage(String.format("第 %d 次循环(输入 %d 字节) => 哈希:[%s...](%d 字节)", (i+1), encoding.length(), codeTip?(hashStr.substring(0,8)):"", hashStr.length()));
                }
            }
            //byte[] finalHash = outputStream.toByteArray();
            String finalHash = outputBuilder.toString();
            logMessage(String.format("密钥长度: %d字节", finalHash.length()));
            return finalHash;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-512算法不可用", e);
        } catch (Exception e) {
            throw new RuntimeException("保存哈希值出错", e);
        }
    }

    private String getPathPreKey(String filePath) {
        String input = filePath + encodingTxt;
        byte[] prefix = sha512(input);
        String hashStr = byteToHex(prefix);
        logMessage(String.format("添加前缀哈希(输入 %d 字节) => [%s...](%d 字节)", input.length(), codeTip?hashStr.substring(0, 8):"",  hashStr.length()));
        return hashStr;
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

    /** 取字符串的哈希值 */
    public byte[] sha512(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            String text = input.replace("\r\n", "\n");  //强制统一换行符
            byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
            // 查看字节序列
            //for (byte b : textBytes) { System.out.printf("%02x ", b); }
            byte[] hashBytes = digest.digest(textBytes);
            return hashBytes;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /** 二进制转十六进制字符串 */
    public String byteToHex(byte[] bytes){
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    /** 获取当前时间字符串 */
    private String getCurrentDateTime() {
        String format = "yyyy-MM-dd HH:mm:ss.SSS";
        String dateTime = new java.text.SimpleDateFormat(format).format(new java.util.Date());
        return dateTime;
    }

    /** 打印日志 */
    private void logMessage(String msg){
        infoArea.append("[" + getCurrentDateTime() + "] " + msg + "\n");
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TransCoder transcoder = new TransCoder();
            transcoder.setVisible(true);
        });
    }

    /***** 长文本编辑界面  *****/
    protected class LongTxtCoder {
        // 编辑器界面组件
        private JDialog editorFrame;
        private JTextArea textArea;
        private JTextArea lineNumbers;
        private JButton openFileBtn;
        private JButton confirmBtn;
        private JButton gotoLineBtn;
        private JButton viewHashBtn;
        private JFileChooser fileChooser;
        private JLabel statusLabel;
        private JScrollPane textScrollPane;
        private JScrollPane lineNumberScrollPane;
        
        // 文件信息
        private File currentFile;
        private Charset currentCharset;
        private String originalContent;
        private boolean isUpdatingLineNumbers = false;

        
        public void showDialog(Component owner, String code) {
            editorFrame = new JDialog(TransCoder.this, "设置初始编码", true);
            editorFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            editorFrame.setSize(800, 600);
            editorFrame.setLocationRelativeTo(owner);

            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.setBorder(new EmptyBorder(0, 10, 10, 10));

            // 创建按钮面板
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
            openFileBtn = new JButton("打开文件");
            openFileBtn.setIcon(MetalIconFactory.getTreeLeafIcon());
            gotoLineBtn = new JButton("跳转到行");
            gotoLineBtn.setIcon(MetalIconFactory.getInternalFrameDefaultMenuIcon());
            viewHashBtn = new JButton("查看哈希");
            viewHashBtn.setIcon(MetalIconFactory.getFileChooserListViewIcon());
            confirmBtn = new JButton("确认编码");
            confirmBtn.setIcon(MetalIconFactory.getFileChooserHomeFolderIcon());
            JButton aboutBtn = new JButton("关于");
            aboutBtn.setIcon(MetalIconFactory.getVerticalSliderThumbIcon());
            
            buttonPanel.add(openFileBtn);
            buttonPanel.add(gotoLineBtn);
            buttonPanel.add(viewHashBtn);
            buttonPanel.add(confirmBtn);
            //buttonPanel.add(aboutBtn);

            // 创建状态栏
            statusLabel = new JLabel("就绪");
            statusLabel.setBorder(BorderFactory.createLoweredBevelBorder());
            statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            
            // 创建文本区域
            textArea = new JTextArea();
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
            textArea.setText(code);
            
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
            if(!code.isEmpty()){
                updateLineNumbers(0);
            }

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
            //private final java.util.Set<String> extensions = new java.util.HashSet<>( java.util.Arrays.asList("txt", "log", "xml", "json", "properties", "js", "css", "htm", "html", "h", "c", "cpp", "java"));
            fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter("文本文件", "txt", "log", "xml", "json", "properties", "js", "css", "htm", "html", "h", "c", "cpp", "java"));

            // 添加事件监听器
            openFileBtn.addActionListener(e -> openFileAction());
            gotoLineBtn.addActionListener(e -> gotoLineAction());
            viewHashBtn.addActionListener(e -> generateSHA512());
            confirmBtn.addActionListener(e -> confirmChangesAction());
            aboutBtn.addActionListener(e -> JOptionPane.showMessageDialog(null, 
                "初始编码编辑器\n" + //
                "版本 1.0\n\n" + //
                "1、太短的初始编码很容易被暴力破解。\n" + //
                "2、直接使用文本文件也存在枚举风险。\n" + //
                "3、使用较大的文本文件，“随机”修改文件某处的内容，会显著提高破解难度。\n" + //
                "4、使用修改后的文本内容作为初始编码，请铭记修改内容。\n"  , 
                "关于", JOptionPane.INFORMATION_MESSAGE)
            );

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

        private void openFileAction() {
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
                                JOptionPane.showMessageDialog(editorFrame, "读取文件时出错: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                                statusLabel.setText("加载文件失败");
                            });
                        }
                        return null;
                    }
                }.execute();
            }
        }

        private void gotoLineAction() {
            String input = JOptionPane.showInputDialog(editorFrame, "请输入要跳转的行号:", "跳转到行", JOptionPane.PLAIN_MESSAGE);
            if (input == null || input.trim().isEmpty()) {
                return;
            }

            try {
                int lineNumber = Integer.parseInt(input.trim());
                int lineCount = textArea.getLineCount();
                
                if (lineNumber < 1 || lineNumber > lineCount) {
                    JOptionPane.showMessageDialog(editorFrame, 
                        "行号必须在1到" + lineCount + "之间。", "提示", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                // 定位到指定行
                int offset = textArea.getLineStartOffset(lineNumber - 1);
                textArea.setCaretPosition(offset);
                
                // 滚动到该行
                @SuppressWarnings("deprecation")
                java.awt.Rectangle viewRect = textArea.modelToView(offset);
                if (viewRect != null) {
                    textArea.scrollRectToVisible(viewRect);
                }
                
                // 高亮当前行
                textArea.getCaret().setSelectionVisible(true);
                textArea.select(offset, textArea.getLineEndOffset(lineNumber - 1));
                
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(editorFrame, "请输入有效的行号。", "提示", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(editorFrame, "定位行号时出错: " + ex.getMessage(),  "错误", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void generateSHA512() {
            String text = textArea.getText();
            if (text.isEmpty()) {
                JOptionPane.showMessageDialog(editorFrame, "文本区域为空。", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                String hash = byteToHex(sha512(text));
                String hashStr = hash + "[" + hash.length() + "]";
                JOptionPane.showMessageDialog(editorFrame, "文本内容的任意改变，将生成不同的哈希值。当前文本的SHA-512哈希值是:\n" + hashStr, 
                    "哈希结果", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(editorFrame, "生成SHA-512时出错: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void confirmChangesAction() {
            String currentContent = textArea.getText();
            if (currentContent==null || currentContent.equals("") || currentContent.equals(originalContent)) {
                JOptionPane.showMessageDialog(editorFrame, "内容为空或未修改，请选择文件并修改后再确认。", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            /*
            int option = JOptionPane.showConfirmDialog(editorFrame, "请牢记所选文件及其版本，以及修改的具体内容，以便恢复转码(加密)的文件。", "确认牢记编码", JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                encodingTxt = textArea.getText();
                editorFrame.dispose();
            } // */
            encodingTxt = textArea.getText();
            logMessage(String.format("初始编码已设置，长度: %d 字符", encodingTxt.length()));
            editorFrame.dispose();
        }

        private void updateFileInfo() {
            if (currentFile == null) return;
            
            try {
                BasicFileAttributes attrs = Files.readAttributes(currentFile.toPath(), BasicFileAttributes.class);
                long size = attrs.size();
                Date modifiedDate = new Date(attrs.lastModifiedTime().toMillis());
                
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String info = String.format("文件: %s | 编码: %s | 大小: %s | 修改时间: %s",
                    currentFile.getAbsolutePath(),
                    currentCharset.name(),
                    formatFileSize(size),
                    dateFormat.format(modifiedDate));
                
                statusLabel.setText(info);
                statusLabel.setToolTipText(info);
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

        private String formatFileSize(long size) {
            if (size < 1024) {
                return size + " B";
            } else if (size < 1024 * 1024) {
                return String.format("%.1f KB", size / 1024.0);
            } else if (size < 1024 * 1024 * 1024) {
                return String.format("%.1f MB", size / (1024.0 * 1024));
            } else {
                return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
            }
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

    /** 图标文件内容 */
    private static final String iconBase64Str="iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAABNFJREFUWEfVlmtsFFUUx/93pmVboWGrEiwFrUoLiE0bLA9FsFGw+zCl0llLFeisIHww8oiiaVCxEmiiQhsIKaK0s5KocWelCu2sQq2JQRtDg6ZiJKIhWdsQJLo8arePmWvu2tlOt7vbaawh3k8zN+ee8zv/e869l+AGD3KD48M0gMMl7qEanaT4PBvHE9o0gF0QaenS628fbZtS2fR+3Z/jBWEKwFYqvlu69Crc9iv3FFfO+LHZJ603AthLxWcoQaZfll4bK5gpAJb9sd2BNgIs2vtxeskXbRN/UBo9v+jBHIJ4ANDOgHCnJ/TjQigF1pQQgn3JyNIorBxBkM03NkrBaMBRAWyC+NEG5x9dKxZ3b2aLuSTk2V+c8bpflkqMzmyC6PLLkjeeAg6XmN/slb4bMwDLvml34HsK5IGinZ/VUuB8fM0cyiVPbPbVnzYrOQOIpUJCBWyCqOxY+3v7gtmh7eFAFBv5WS2H2KdNEE/5ZWmxWQBmV1IiWqO3IREAsQui1lQd+JlSZOvZ6wGLXKtn8ki6s9krnTALYVslZvk/lC4Y7eMC2AXx0jvPd+3LuEXdGZ297sAuuN9S5IYXxh3Abn/OYrFeDfmqfusEkBmdfaT6V667Q+PUBYmKzwgXqxBjKsAK79Ndge0cwa542UdUKHVXKb6GHezfLog9AK4B0DhCnU1eT7sRwFQNOFzibdZJ6q9HKjtDAEmPl73ueHnx09P4ZJpLiPaeInum6vNOV8V9GiXNGuEXf+Y9fD7eNo1QgGV/vDpQDYrKUbMvExdBxSd0AAv9jcOLa+iQcj9GAY+qDsz5/OiRSwnPAccKMXd6Zv+Jui0XrQAs/xw82mxyV+s540Jn2Zo5qsp/xRGUm+0Cm6tiA6GkBt1pNyvK/l7d3zAFBrM/AIpndQM+p2WEDYA+RZbCgGMZzrJ192qq2qHIUsRn5MMuiK23TlaX1G/rrOE4bhNAJ4SdE1LLZ5/cOtR6In0j37LtlY7eN3tVclmRG6aMBrHMtWFyMu1j9wBlHmMCRAK43F+veviK8tQjwQIC8iiAFEJwjFAcIjktx5lKHzyY+iWArG8uq559P/XtAOh5RfZkxwJh9mxeD8r+EwIMthOlQH9KMk4Uzb92vmBWT9rd0/qnpt1EUbx9umMQoDAsUDLJLm/tXkJB6kHQpnil+3UfxsBGBUcFMGZid7kf0igW8tByKQjr65phAASbJm+u2x8OWuquAqGvxgpsCoAdFMww1r1tdGAEoJRWpW89GHmIREscvS0Jt+B/CQDAa91S90Q8icekALsu2YLoK3NYTQx1QbgIB8dBSi2V6Vtrg/9qC8wCZKQS7J1nkUGIoBMQ4KxG8fKTp3qOGqvcCF/kqnBylByP2wWsBkJJyPfLEuvzuKNQFFNSr6NnZhqHnXkWZhtRY2dHL/Ycrh92ehatdM/jONpOCD3Z7PUsNzoecRnFezzqi4yvmuLy9VP7+wcu5qXzeGnuhLMEmGsEKFq5OoPjkroAckaRG+bFymgEgK4Ce0prQD4oLoAgCzzCqhCNCNDogCJLtbpDR9naHKpy5x6YwiPYR8MKhE9AioDik25PpOaoz/LoxTZBLGRwsZ7YtjJxPlHxLYC/FFmamCiwoXbMmP13NmNWYLxR/gZQW0I/9MHdIQAAAABJRU5ErkJggg==";

    /** 二维码内容 */
    String base64ImageAlipay = "/9j/4AAQSkZJRgABAQAAAQABAAD/2wCEAAoKCgoKCgsMDAsPEA4QDxYUExMUFiIYGhgaGCIzICUgICUgMy03LCksNy1RQDg4QFFeT0pPXnFlZXGPiI+7u/sBCgoKCgoKCwwMCw8QDhAPFhQTExQWIhgaGBoYIjMgJSAgJSAzLTcsKSw3LVFAODhAUV5PSk9ecWVlcY+Ij7u7+//CABEIALQAtAMBIgACEQEDEQH/xAAyAAADAAMBAQEAAAAAAAAAAAAABgcCAwgEAQUBAQEBAQEAAAAAAAAAAAAAAAAEAwUC/9oADAMBAAIQAxAAAACy+XbzudBkk2lb+x/eV3za5yU/aiYjuSPzlkJCFe3cz9In0k+orBzy1lb2SfWWMA0yCvx8jNn+NYsuaW3nPN32c/DX7Ks2C1GqktG1t1R0ehmcRP2pjmJ0WseZmNqkWMAwldXQRX3xmvjlGLJHB11elSHBqaubh33JlZFlozkQz7UwHnyOMgHH3RPpETHTm3pgbAAjti8pzr6/2/UY7/OmDtliDaqpTQezz7VQrCa/czj3+/guDYp1lSHrwimNrhz42FeAMI3ZomK9dXGcUlWuYCUr2JINSg3Jfry8Kbm10wkbtvmwq5v6TRkvPb3Nm4NKWB0aryirjmAYSmsazml6wwMFRxTjo9MxkZV5yx+yqHFj/AaL+MnI1S/AnuUiunO7KS7xptPbP3pTKi/8b9LjiAaZFX+dSvxrpKOCg07shOFFtGn9OKNPvKneCJ2W7l/s/iN0blv2ZmGFWCph0mKDklZmVfJAWIAwj1gkQNrRzmWRW0V4VPF740a97N6BKcfToNg4AmuMddBGzdtJm3KIabNzc5FiANMgsSec1buj2wkyPZ04079bgSBRbmwc47k4jgJ+ZXk36pFh5osEiNNpT0osWae4DiAaedOi5AbMIvZzczKagObKqqg4eH3pwN2OJsUWoFKw/E0UWz0JBY4zm7jY483ORYgDTIbFIT41KwfB0bTm2vuYI6q5Znk5v6aCM2LP6Rl0cgwj/SIc14WlNMbEAAAAAAAAAAAAAAAAAAAAAAH/xAAsEAABAwMEAgEFAAIDAQAAAAAGAgMEAQUUAAcSExEXFRAWMjQ1IychJDNA/9oACAEBAAEIAK1pTzqlyt2qXK3apcrdpuRHeTWrLL7Ej/xrWlPOk3C21p505Ijs1TR7TkiOzVNHnJEZiqchqRHf89DkiMxVOQ1Ijv8Ano05IjMVTkNSI7/nopcrdqlyt2qXK3aakR3/AD0akfrv6DBCKTMz3H/XA9r1hZ1tPLY2s/mkWtpP1b1pc23ONqRpG09ubWleimzWYhVEkuCptW+Rri7Lg0puXXJm7ryIsili6Nra4FL3mbvfgPaHCS4h+XpqRHf89DMZ/cjz8ntD+BDoLEo5RS493rge0ztfZX/PTtD+BD9JH67+trP5pF9NrP5pFraenOFfUaHxVgYiXNDDFvn0fZrUsMvhnYUeIZijAw9BbYEgil/bmrmDYkwNx57LRSJRrNhfGMxn9yPPyZaN24hpAzWYz+5Hn5PaiO/GTfe4WLZF4pOpcxIbtw9SdhbQ/gQ/TaH8CHW0P4EP0kfrv62teioh3xt/1wPaH7NZhiHckMbUr4QL8rQYWvkrE9bxWWybVRhu2wo8h59urRAOW0rehv0bkR3k1qyVlsm1UYbtu0P4EOgkukE9Lj3kRJcTDE0UFkizYPxjUiO/56IfjdDnmiQ3bh6k7C2nkxY9L53+uB7QvZLKLZvTtD+BD9FoottxGq7SW/xWuvXA9pra6yvUrVobHbePwrkmELlb48iXHb80BfMa0PSY4BWkO2ux5G33iHbrKPW8PiXFum1n84h1tRHfjJvvc9Gkba8fjBeyWUWzenaeTFj0vnftbXApe8x6NI214/GbTyYsel87/XA9r1LbdeuB7QkN24epOwvobFkoarEZZdjyGPHdta9FRDvjb43Y7MOMT2Gi4dgD7kJELaenOFfUaKxRgXm2pDMmWxFbUp0iIbiYLir0LktxE6vsaNCySL/HdIiSXAgz8wWEo15zfkxAbtxDn5pcSXAgwMwoLJFmwfjBAbtxDn5r0aRH49wiSXAgz8x6NIj8e4YEowtm9P03RYlLl2VbJRebwULiLeEgil/amrm+uB7TW11lepWrXdXb7y3Z4KPZdcmYYDVuv1IrsrbJfxkS9ZTdEni0P3jdeRFkUsXQUFkizYPxm68iLIpYugvCPt3Aw2JEbcnzS5zP9XcMIWEo15zfkykSilFIXcaFkkX+O6Six2UppB7mpEd/z0fSTLYitqU7Td256ruKQP06Vu7dDqGlroLFUgeRLjNS/O29UQ4cp5W19UMwSUin3+XbVzd0ZbC5dkWxEapufRb0w1EWBX47pGBKMLZvS9GkR+Pd7fueikSjWbC+MKLHZSmkHup43B/smhZJF/jumnjcH+yXhH27gYe1Ed+Mm+92nFVQ2tVHaUPG1v3cSHoF6bmvyhspdvcadS502ws7rTq2BMTjXbIcuO7KuE2xroQXm7k0u3OP7lJpcpVnxCgct9jkW9qJKeVtjVDMIovd5KMLuLSO4D+DhlxHPIMDMejSI/HuHRu3B2XShqIsCvx3TTxuD/Zp43B/slxJcCDAzKXK3aESS4EGfmarWlPOt0XY0ybY0s12xssRbS3SUZthcqK5oSHYA+iYiEUDduvr9vdmFImwQLiyHPYhA/8A4az1etU40Mbske4IkTJRE5CI34Tz9S6lPHmETsSpCGVlo5byHAzGJEbcnzS5lBZJKKQu7deNIfpY+lmM/uR5+TIiS4mGJoiJLiYYmi8I+3cDDESS4EGfmaWii23EartXAYp3UIyG4F6orldqaUVBvyauT2tt69FrJSKeQTLYqYuTFYrTvJCyTaH4KLcV2SzkmO64Nx32LXKo9HYjOtJqq5Mx7k8wmOxRFLixRBIQzy1UamjQRYFqW3pKRKNZsL4x7dG9R+PcIklwIM/M2ojvxk33uFi2SLZvSWmvwGBhfWtWn2nUp8+vaYcRbyNuf+pG9Y25jy8klJ3ySXbFvGA1DJcZ5QoVuDyJkZC9r4kTw7WPf3SplrIRcm014UYRbobXLSB6fGlR3EKr64qqsQrGoJTSBzFBqEK53ApKZBbSFr25I1Tda6aMAz7ZwOlDbjnngHlUgkz+7Uj9d/W1NWsO+tLGRJsaYnsILBFsak25lBOLNjT8DGrVG5LTkmSICcYlbmrfJhmMNTLahndJ9GXZFoikEgvhPOITKvSf+aOxbm6urj1s+VhOUWp+vrGvlkRFoxLn94mJNEOdlCRcsVzuGNI1Tda6aLiiSM4HTtKhbXz3Ntxtzzw1J8Y71KCAkzfsl2ShxtyleG6jC3pljSnz69phxK7UxKtOrbbiSFuITouEPtp2Ihlhlb7iUpLRlgbchoYbXVDiFaQ0vcz9kvLVi2BwKyxZRhcxMSaIc7KKSmQW0haQ24554FRa6O4NYqN2Jq6eE03WumluNt+Oe0qFtfPc9OK4NuL1003K8OzF1c2x8MxhIoWWRbl2DQxHGod0QztVzjwr8tYgVrKWpy3DEw+3sZpkbKpAvHnsaDg9koanLWUjccXkwOhW7E1qniu7TS1/BcEbTwl+eG734D2hYXjCWbSgkXLFc7gIFUkYz+naH8CHRgWuCuBVG7LjblLFwbcbc88NOL623F0jU9mrpKekSGMd7QoVrHm5kZPe3ts6iNGd61JU2sZEmxpieyknFmxp+BjVqjclpyTJ2pq1h31pdNq4TC0LXuv4fpY+kPKpBJn9wsLxhLNpQsJZhTSBzQ25uf8AtepY+luo2w/U8+s/ODTxuX4zRMSaIc7K2lQtr57nqR+u/raz+aRaQ24554NxH1uITWm1sRFKvIKSmQQyIDrq90LgtC0a2uaWi3kPMYEm79HnuyGW3G5DHMtFIxTSO+kWKZAlSdoULVkNJ+SVjUEpwObbjbnngKFqyGk/J0Xi0YawOkSLliudwKRaIW4WisaglOBzDyqQSZ/dpxFFtuI0LjcIajT2UCg9DF2piEVdjuNuI0LDTIvGuPQhpe4tKy5NN1ZnYyhZcVyRp2ElolKJBNMti3t3/wBy0aESyQNNzEM08bl+M1r/AGl57vVNs0KDUIVzuCG3Nz/2hAzoS53eGiLRTn80bTwl+eAgVSRjP6UbTwl+eAeVSCTP7tSP139BweyUNTlr9U2zStq4fU6tG0n6l70Vl7o8uGmPGp7NXSU8WmlRtyFRmNT2aukp7ddxDsizcBQomCzcxCKeNy/GbtD+BDoQDfubP7zIRaFaW/gHFyymlw5lgk0PYOLtK6hql+5ig1CFc7gJFyxXO4Cg1CFc7g242554akfrv62s/mkX02s/mkWtrP5pFoSJ5FhRNjNSlV2xRWKyLBv3DGuLzu1zS0W8h5igo3em5kiQndWZRxpCt21tLpYeG0rqGqX7mjdiaunhJWNQSnA5rdRth+ott7a79YvFow1gdJeLRhrA6fVNs0ZCLQrS38NpULa+e56kfrv62qUzhX1t31TbNDo7CF4d0bRtOjnDvyNLqvbH/DFIiKYUS7atZQUyB2Tb2mi8vdFXIKUxUezF0kvGItGIcZ11bbjfjmGiLRTn8xYXjCWbShkItCtLfwXtPCa8cw8qkEmf3CAb9zZ/eJiTRDnZQiLRiXP792mlr+C4fRaKLacRr1LH16lj69Sx9CosgXamIR9SoOaKFw1rFRZAu1MQgnEWSR+A8ssEWSnB5igm2LZ3AsEWSnB5lgiyU4PMsEWSnB5/QsEWSnB5+pY//wBf/8QAPBAAAAUEAQMBAwoFBAIDAAAAAAECERIDBBMUBTEyUSIQM3EGFSEjQVNkk7LDJCWSodEgQ6LCQpFAY7H/2gAIAQEACT8AF/bfmEL+2/MIX9t+YQr0qhF3QWSm+LGK9KoRdTQslN8W9l9bGZ9CypFelSl2zWSTP4P7K9KlLtmskmfwcV6dIlO01El28OK9KpFpQUSmfywr06RKdpqJLt4cV6VSLSgolM/lvZXp0iU7TUSXbw4r0qkWlBRKZ/LC/tvzCF/bfmEL+2/MIV6VSLSgolM/lvZ90v8ASLypQ1zpkUCI3kPlD/emObqVYIc4Egx4pj72gL+29ZGn3qX+kctXZDG8CHMUqSrRK4oQtByCLa1XbxgU2kD0z40yJBUvpnMV6dRtl4LI+sA1tk14ZvQ7TH4v9scdLbh74jR7oV6VSLSgolM/lhRXx2h7qCDPJn8z8QH4T9wXa6GthaBEb5HHyh/vTHOVKjdYEhQ/Cfuez7pf6R4p+zxTH2nRSLqpXKulzmlmiLO4IiqJc8agi3uSukrJap9guqtfYSszmlmiKtzanRNBIKHfIXa6qbkimpZEUYkLxd/my5IESoRZngKK+O0PdQQZ5M/mfiA5HTwZYdCnOIor47Q91BBnkz+Z+ICjUpz1WmRk7TFtSsMWPHMzROTv3jktvPin09EJD8J+57Pwn7g/Cfuez7pf6Rc0qU1UWmuI+UP96Y5ilW2EucloJokY+xVFQtaVDXOmRQN3kLWlfJrUl5FIM1w/oFGosk1EyNKTNviw5FtR/opMsV6VQi7oLJTfFjFrSvk1qS8ikGa4f0D8J+4LSlR1sTRMzebjjo6k/cka3ysLalf5smSBmvHFmeAr0qkWlBRKZ/LD+C+bWhh9c8/l/EByW3nxT6eiEhXp031mmoi6THyh/vTHNUq+1jd1oJsbj8J+57D7kmn4OOWrt5gQ+UP96Y51ayLrCChyO3nQ6+npiRi2pVU3ZoJRrNoj+Z07/wB8vrhh9Bdgr079HJe8WoyKEPEBRXfo5H3q1oN6UPEByM9pDllNKOwh/wDUKNSnPVaZGTtMUV8jv980dmDxHzMc1Sr7WN3WgmxuK9Om+s01EXSYa2ya8M3odpiivkd/vmjsweI+ZivTpvrNNRF0mPlD/emOWr/0D5Q/3pjktvPin09EJe21pVtujVeZmKNSm/SaTS/wcXNKlNVFpriOZo1iuWc1rQTQHI7ZVkLNZuk4sPtOikXS62dRmZqIiaCiFakg4KUklrJLt4ccdHWJSfqSNfeLB9tdP3roaItaVbZyvMzJsbDjtXBih1Kc5C8XYYsWOZEmcndpjktTBjh0Kc5DjtTBlh3FOcRbUr/NkyQM144szwHJamDHDoU5yFCpTk7TSaXbw447VwYodSnOQoVKcnaaTS7eHF2uvs4nmRE2N/bQqVYJqmcSMxxFShrpWRQQs3mKtxaYVIiWPukPlD/emOdWsi6wgoEXKovCnVPriNHTsH8GfHKIkFS9U8g5IrZdClVglyKYPAszpGgqvomxKCy4pViZFSLplmbn3ivTqNsvBZH1gLalf5smSBmvHFmeAr06jbLwWR9YBdxd58s/R2QiLilxuh2QMvXn8z8QH8aXJPPMUIa/hvMxeLsMWLHMiTOTu0xdroa2VoERyyMLWlW2crzMybGw5mlQ1sjRWg3yMK9KpFpQUSmfy3trUkHBSkktZJdvDjirUfJ+KVlF2qD5RuZIdnQLanVK8UglGozKIPbTyneqr6IQBbu+U1ZfTDELDUwGyepSkZCtTqwRVM4LkD09BkIKj655Rdrr7OZ5ERNjYXa6+zieZETY3FCpTk7TSaXbw44q1/rULxd/my5IESoRZngOZpUNbI0VoN8jBuJ0fc/Zlzd3vPEBa0q2zleZmTY2DcTo+5+zLm7veeIBdxd58s/R2QiKNSnPVaZGTtP2E5kkzb4EDLi12KGpI6ZZ9e8chrLtjQaEukpi2pWBoZKJKia5O/eObXVgh2RBQvF2K6K6eJKyJE38TDdlUcSuhrm3oQs3cx9chKahLOl64uZDkNkriUz+g4AtzfdSsvphiHEVKOtlaCFm82HH7efLPuOEIjjtTBlh3FObeRQqU5O00ml28OOSltw99FDYnF2uvs5nkRE2Ng3E6Pufsy5u73niAbidH3P2Zc3d7zxAcdqYMsO4pziL+2/MIcdq4MUOpTnL216SyhUI4KJQ51aDdynBI5KGshRHhivvHIbedSDX0NojktVVCUCcikLmpSXZoWZJQl3djHyeJJLKBnFY/jC5FBms6vphAV106VutCUIpk6lrUKdamds8SQtLHJvJCyV/WKB0zWbEbuTmOR1cGWHaU5xFxS43Q7IGXrz+Z+IC0pUdbK0DM3yMKFSpHaeCTNngKK+O0PdQQZ5M/mfiA46OpP3JGt8rDjo6k/cka3ysF3F3nyz9HZCI47VwYodSnOXsPuSafg45S4M6ZTaBDj4aqFJ+qI194NnOiQrU+RK/KSzUrsx+ICw1MJsgmUUpGXkXFOk7mU1El/g4taV6is+VSDNcPjEcsikq2RUiSFoN3FGpTe6S00ml/SEHJzI3uEU3+BKII2FIoseOuhLBJpSVdBMapdD8kwsG1Dqe6kvvF0uttZXmRE0GF4u/zZckCJUIszwHCUqT9JzIcdq4MUOpTnIUalOeq0yMnaYtKVbaxPMzJsbhFtd58s/W8IR/0VEm6TSZkZGzkP5knku9fZigKiL8uT7lmcMcB8oJYym2MhYauA27jOUjHKIoatJbETLlIWaK24aCMzW0GchzSnR62OkROwsjtsdySGJTmpyHGWhodomgzV8JGCKxubqkZESlSgIVqRVUG6TY2Ix/M9/v/wDDFg+EusxyiLfWKr4XLIOURc7OLwmMBx+HVy9ijW+RhwlL88xwP/MxdVLrYyv9VGONghSm8E4sNXXxMyjOWSXs+6X+kVUomdEheqrldRczRBokL06+1JzNEIsYvDvSqyWbIaMG8OKqOOPjkMlJHkyzHJauA0MUSVKQ5DazG5nEksxkDJUE1RalQq2lZLITUkaiWRi0PJ95ruv/ANsLa5Uo/tUhQy0LVHrqnVJkEgvpPqP4/wCcus/qoYP8zHI6uvjb0ynOQvVWeDFF0POb+TIWSLjZxdVwjjcUKn9Jjgf+Zjj9rYyu5mUccQhaH1v+4qIU3g3b2fdL/wDwxfHZ69SnEjQ8xUSpvBuwQpjRVH8yTyXevsxQHOzgh2KkKNQpKInif2i5qXRViWZnji0QlRkaiIzInZxf7Wclm7Elogniojb4BB8dodGLJPP8W6QFki42cvVcGgws00NbL0XOWRheqs8GKLoec38mQ4/Dq5exRrfIwQpTeCcWaLzPldl9kGHBoW3iqY4H/mYqIS/kyIIWh9b/AL+wnNKTNvgThXzZo+hBd+TL8YBB8kV+U1GZY4YhZotsMUEy5vMhyG1nS5nFmiQQZMdE2MWiLfXNBMS5ymLalc7NJbnkaAsMu3HvUaGiL47fXNBMSJvMX23lkvti0DHBoQ/mqYQpTbX7Y56TeKQ/F/tjkM23i70FTbG4skXGzi6rhHG44/a2cT+oyjjkPwn7gskXGzl6ri2OIWSm2f2xUQpvBu3sJ4pNX/onB6HzaxESCyzmK9L3S/8AzLwLNFYrw0E5rjALRyJciZGozPHDGKqETQoiNRkXUm+0Xqq5XTOZojFiF4d6VWSzZDRg3hxVRxx8chkpI8mWYqpRM6JDnmY3Y6Q+sbZeH0s8BYauviZlGcskhyGbbxd6CptjccWdvrZejrlkYJPjS43oxZMmx/iA5lf5IqI5IuR6ueKGv/mYMuVLke//AG8eD4T6zDcUXHdv+5lz/GHSAvVWeDFF0POb+TIIWh9b/v7Pul/pHimKalN4JxSqE6iJ4n9o5w6mL1smkLDWO2kxSM5yMcA0kx7zCFk5IF6dodvGKTR3hCk/WJ6k32i/xatJbJQklu44/NtYu9RobG4s02eDFF1983HKIt9bL4XLIKiFN4N2FmmzwYouvvm/s5Ha2Mr+kihBhZIuNnF1XCONxyGHVy9iCqPkYcoi31svhcsgsNXXxMyjOWSXsNpJMjP4kw5NFfbj1ZEYkY5NFfYUhRuyGgLikU0Gl5l9Di/2yqwWf0RaBBB8efGdqCKeWY4RCXNieqY47ZzpWfU0tEWGrrmxE5nKRj7usOO2s6kGfqMoxDcUXHdv+5lz/GHSA/gPm3pD62ex/iA+UhflpHKIudnF4TGASfGlxvRiyZNj/EBbUrTXxM1R55HF8q31sXRE5ZHHPSbxSHH7Wzif1GUcchz0m8UhYauviZlGcskvZ90v9Ivjt9c0ExIm8x8pC/LSOdNcEuxUR97QFki8zJWZsvsYHofNrERILLOYtaV1nSs/eM0Qeh82sREgss5hZKalWHFncZ1oV9JmloBuKLju3/cy5/jDpAfhP3BdVLXXxt9VKc3F6q42svVEI44iyRb62HoucpuL1V5nyuyGhBvBmFkh9Yv1jlEXOzi8JjAWSLjZxdVwjjccoi52cXhMYCohTeDdvZ90v9I8U/Z4pjxTFhsFdmhJm5lAFv8AzkgzdZYoQF1UtjtosWJ5yCFk5IF4dqdqaDQk0d44RCHP7aphZLbZ/bCyQ+sX6xwaFt4qmOURb62XwuWQVEckXI9XPFDX/wAzCD5H5x6uWKGv/mY5Ha2Mr+kihBhyO1sZX9JFCDD5SF+WkXqrjay9UQjjiELQ+t/39n3S/wBIqkiZ0R8pC/LSOTRcbBO5shoD7cRCmfJb7LMzLFDEONO31zMiYzW8zHH7JXMnORlGJixTXzpWr6VwaAPQ+bTIiJBZZ5ByOsdtTqsUCVNwhSX8kwvlW+ti6InLI45DNt4u9BU2xuL1VxtZeqIRxxHPRfzRFhq6+JmUZyySF1UtdfG31UpzcXqrPBii6HnN/JkOR1dfG3plOcghSm2v2/abSSaXbyTDmV/kjmV/kjmV/ki7OvsKQpzRBof6L9VDXStP0U5ymLs6+wpCnNEGgL5VDVJTESJykYvVW+rk6InPIwvVXGzi6og2Nxeqt9XJ0ROeRheqt9XJ0ROeRheqt9XJ0ROeRvbeqt9XJ0ROeRhzK/yf/l//xAAnEQACAQMCAwkAAAAAAAAAAAABAgMABBEFEhQwQSEiUFFTYHFyof/aAAgBAgEBPwDxRVZmCqMk1wN16X6KkikhbDrtPKs2C3CHyzVwZFdXIbPbt6/OcGtQkjkMRR893lRyGJ1cdKXUNhysKCp5zO+8oq/X2F//xAAlEQACAQMDAgcAAAAAAAAAAAACAwEABBEFEjEhMBVQUVNgcqH/2gAIAQMBAT8A80MwWBGc4GK8Usfe/JpL1PHco90drUYkrNseuKtQUazXEjjpv6Y44xmtIUxMOhgYyUdp6hes1lxNFpEHGCuWTFWlrFqGyDI/t8C//9k=";
    String base64ImageWechat = "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";
        
            
}

/**
请使用java编写一个支持打包、解压的文件转码程序，实现以下功能：
1、点击编码按钮，弹出一个窗口，该窗口可以打开一个文本文件并在中显示，可以修改，修改后确认显示的文本作为初始编码；
2、点击打包按钮，先选择需要打包的路径，选择路径后，再选择打包文件需要保存的zip文件名，然后将指定路径下的所有文件和子文件夹中的文件，打包到指定的zip文件中。打包时需要对文件每个字节按转码编码依次做异或操作。转码编码是取初始编码和次数的sha512编码获得。
3、点击解压按钮，选选择需要解压的zip文件，再选择需要解压的路径，然后将zip文件中的所有文件解压到指定的路径。解压时，从zip文件中获取文件的每个字节按转码编码依次做异或操作。同样转码编码是取初始编码和次数的sha512编码获得。
4、点击配置按钮，弹出界面可以设置初始编码获取sha512编码的循环次数，以及文件名称是否参与生成转码编码。
5、增加文件、配置和帮助菜单，文件菜单下主要包含打包和解压，配置菜单下主要包含编码和次数；

1、按钮放在顶部，配置画面的按钮也是放在顶部，都居左对齐；
2、打包和解压的时候，先统计文件的数量，然后在进行打包或解压，处理过程中显示每个文件的名称；
3、配置画面增加跳转到行，查看哈希等按钮，确认是，要比较文本内容和刚打开文件的内容是否一致，不一致才能确认。
4、在主界面下面增加进度条，处理时显示处理进度；编码界面打开文件时限制文本文本，并自动检测文件的编码，使用对应的编码打开文件。
5、将主界面的进度条放在按钮的下方，JTextArea的上方；在编码界面的底部加三个类似状态栏的输出，第一栏显示选择文件的全路径名，第二栏显示文件的大小，第三栏显示文件的编码。
 */