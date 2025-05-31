import javax.swing.*;
import javax.swing.plaf.metal.MetalIconFactory;

public class Hello {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Metal图标示例");
        JButton button = new JButton("按钮");
        // 获取并设置图标
        Icon icon = MetalIconFactory.getFileChooserHomeFolderIcon();
        button.setIcon(icon);
        
        frame.add(button);
        frame.setSize(300, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
