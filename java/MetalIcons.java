import javax.swing.*;
import javax.swing.plaf.metal.MetalIconFactory;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MetalIcons {
    public static void main(String[] args) {
        // 设置窗口
        JFrame frame = new JFrame("MetalIconFactory 图标浏览器");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        
        // 使用网格布局
        JPanel panel = new JPanel(new GridLayout(0, 5, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 获取所有 MetalIconFactory 的图标方法
        List<IconInfo> icons = getAllMetalIcons();
        
        // 为每个图标创建按钮
        for (IconInfo iconInfo : icons) {
            JButton button = new JButton(iconInfo.icon);
            button.setToolTipText(iconInfo.methodName);  // 悬停提示方法名
            
            // 点击时显示方法名
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    JOptionPane.showMessageDialog(frame, 
                            "方法名: " + iconInfo.methodName, 
                            "图标信息", 
                            JOptionPane.INFORMATION_MESSAGE);
                }
            });
            
            panel.add(button);
        }
        
        // 添加滚动条
        JScrollPane scrollPane = new JScrollPane(panel);
        frame.add(scrollPane);
        
        frame.setVisible(true);
    }
    
    // 获取 MetalIconFactory 中的所有图标
    private static List<IconInfo> getAllMetalIcons() {
        List<IconInfo> icons = new ArrayList<>();
        Method[] methods = MetalIconFactory.class.getMethods();
        
        for (Method method : methods) {
            // 只获取返回 Icon 的无参方法
            if (method.getParameterCount() == 0 && 
                Icon.class.isAssignableFrom(method.getReturnType())) {
                try {
                    Icon icon = (Icon) method.invoke(null);
                    if (icon != null) {
                        icons.add(new IconInfo(method.getName(), icon));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        return icons;
    }
    
    // 存储图标信息
    private static class IconInfo {
        String methodName;
        Icon icon;
        
        public IconInfo(String methodName, Icon icon) {
            this.methodName = methodName;
            this.icon = icon;
        }
    }
}


/**
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;

public class MetalIcons {
    private static JTextArea resultArea;
    private static JPanel iconPanel;
    private static JTextField methodField;
    private static JComboBox<String> methodComboBox;

    // 原始方法清单
    private static final String[] ORIGINAL_METHODS = {
        "fileChooserDetailViewIcon", "fileChooserHomeFolderIcon", 
        "fileChooserListViewIcon", "fileChooserNewFolderIcon", 
        "fileChooserUpFolderIcon", "internalFrameAltMaximizeIcon", 
        "internalFrameCloseIcon", "internalFrameDefaultMenuIcon", 
        "internalFrameMaximizeIcon", "internalFrameMinimizeIcon", 
        "radioButtonIcon", "treeComputerIcon", 
        "treeFloppyDriveIcon", "treeHardDriveIcon", 
        "menuArrowIcon", "menuItemArrowIcon", 
        "checkBoxMenuItemIcon", "radioButtonMenuItemIcon", 
        "checkBoxIcon", "oceanHorizontalSliderThumb", 
        "oceanVerticalSliderThumb"
    };

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("MetalIconFactory图标执行器");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 创建输入面板
        JPanel inputPanel = new JPanel(new FlowLayout());
        
        JLabel methodLabel = new JLabel("方法名称:");
        methodField = new JTextField(20);
        
        // 生成下拉框选项（首字母大写并添加get前缀）
        String[] displayMethods = new String[ORIGINAL_METHODS.length];
        for (int i = 0; i < ORIGINAL_METHODS.length; i++) {
            displayMethods[i] = "get" + capitalizeFirstLetter(ORIGINAL_METHODS[i]);
        }
        
        methodComboBox = new JComboBox<>(displayMethods);
        methodComboBox.setPreferredSize(new Dimension(200, 25));
        
        // 当下拉框选择变化时，更新文本框
        methodComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedMethod = (String) methodComboBox.getSelectedItem();
                if (selectedMethod != null) {
                    // 转换回原始方法名格式
                    //String originalMethod = selectedMethod.substring(3); // 去掉"get"
                    //originalMethod = originalMethod.substring(0, 1).toLowerCase() + originalMethod.substring(1);
                    methodField.setText(selectedMethod);
                }
            }
        });
        
        JButton executeButton = new JButton("执行方法");

        inputPanel.add(methodLabel);
        inputPanel.add(methodField);
        inputPanel.add(new JLabel("或选择:"));
        inputPanel.add(methodComboBox);
        inputPanel.add(executeButton);

        // 创建图标显示面板
        iconPanel = new JPanel(new BorderLayout());
        iconPanel.setBorder(BorderFactory.createTitledBorder("图标预览"));

        // 创建结果文本区域
        resultArea = new JTextArea(10, 60);
        resultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("执行结果"));

        // 添加组件到主面板
        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(iconPanel, BorderLayout.CENTER);
        mainPanel.add(scrollPane, BorderLayout.SOUTH);

        // 添加按钮事件监听器
        executeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String methodName = methodField.getText().trim();
                if (!methodName.isEmpty()) {
                    executeMethod(methodName);
                } else {
                    resultArea.setText("请输入方法名称或从下拉框中选择！");
                }
            }
        });

        frame.add(mainPanel);
        frame.setVisible(true);
    }

    private static void executeMethod(String methodName) {
        resultArea.setText("正在执行方法: " + methodName + "\n");

        try {
            Class<?> metalIconFactoryClass = javax.swing.plaf.metal.MetalIconFactory.class;
            
            // 获取所有方法
            Method[] methods = metalIconFactoryClass.getMethods();
            boolean methodFound = false;
            
            // 查找匹配的方法
            for (Method method : methods) {
                if (method.getName().equals(methodName) && 
                    Icon.class.isAssignableFrom(method.getReturnType()) &&
                    method.getParameterCount() == 0) {
                    
                    // 执行方法
                    Icon icon = (Icon) method.invoke(null);
                    
                    // 显示图标
                    displayIcon(icon, methodName);
                    
                    resultArea.append("方法执行成功！\n");
                    resultArea.append("图标类型: " + icon.getClass().getName() + "\n");
                    resultArea.append("图标尺寸: " + icon.getIconWidth() + "x" + icon.getIconHeight() + " 像素");
                    
                    methodFound = true;
                    break;
                }
            }
            
            if (!methodFound) {
                resultArea.append("未找到无参数且返回Icon类型的方法: " + methodName);
                clearIconDisplay();
            }
            
        } catch (Exception ex) {
            resultArea.append("执行方法时出错: " + ex.getMessage());
            clearIconDisplay();
            ex.printStackTrace();
        }
    }

    private static void displayIcon(Icon icon, String methodName) {
        iconPanel.removeAll();
        
        if (icon != null) {
            JLabel iconLabel = new JLabel(icon);
            JLabel nameLabel = new JLabel("方法: " + methodName, JLabel.CENTER);
            nameLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(iconLabel, BorderLayout.CENTER);
            panel.add(nameLabel, BorderLayout.SOUTH);
            
            iconPanel.add(panel, BorderLayout.CENTER);
        } else {
            JLabel noIconLabel = new JLabel("无法生成图标", JLabel.CENTER);
            iconPanel.add(noIconLabel, BorderLayout.CENTER);
        }
        
        iconPanel.revalidate();
        iconPanel.repaint();
    }

    private static void clearIconDisplay() {
        iconPanel.removeAll();
        iconPanel.revalidate();
        iconPanel.repaint();
    }

    private static String capitalizeFirstLetter(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}    
// */