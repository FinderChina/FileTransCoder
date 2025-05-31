import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class FileRenamer {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        // 获取用户输入
        System.out.print("请输入要重命名文件的目录路径: ");
        String directoryPath = scanner.nextLine().trim();
        
        System.out.print("请输入新文件名列表(用逗号分隔): ");
        String newNamesInput = scanner.nextLine().trim();
        scanner.close();

        // 处理新文件名列表
        if(newNamesInput.isEmpty()){
           newNamesInput = "2.施工方案与技术措施,3.质量保证措施,4.安全施工保障措施,5.绿色施工方案,6.工程进度计划与保证措施,8.现场管理机构设置与人员配置"; 
        }
        List<String> newNames = Arrays.asList(newNamesInput.split(","));
        newNames.replaceAll(String::trim);
        newNames.removeIf(String::isEmpty);
        
        if (newNames.isEmpty()) {
            System.out.println("错误：未提供有效的新文件名");
            return;
        }
        
        // 获取目录中的文件
        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            System.out.println("错误：目录不存在或不是有效目录");
            return;
        }
        
        File[] files = directory.listFiles(File::isFile);
        if (files == null || files.length == 0) {
            System.out.println("目录中没有文件可重命名");
            return;
        }
        
        // 检查文件数量是否匹配
        if (files.length > newNames.size()) {
            System.out.println("警告：新文件名数量(" + newNames.size() + 
                             ")少于目录中文件数量(" + files.length + ")，部分文件不会被重命名");
        }
        
        // 执行重命名
        int renamedCount = 0;
        for (int i = 0; i < Math.min(files.length, newNames.size()); i++) {
            File originalFile = files[i];
            String newName = newNames.get(i);
            
            // 获取文件扩展名
            String extension = "";
            String originalName = originalFile.getName();
            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = originalName.substring(dotIndex);
            }
            
            // 构建新文件名（保留原扩展名）
            String newFileName = newName + extension;
            File newFile = new File(directory, newFileName);
            
            // 检查新文件名是否已存在
            if (newFile.exists()) {
                System.out.println("跳过: " + originalName + " → " + newFileName + 
                                 " (目标文件已存在)");
                continue;
            }
            
            // 执行重命名
            if (originalFile.renameTo(newFile)) {
                System.out.println("重命名: " + originalName + " → " + newFileName);
                renamedCount++;
            } else {
                System.out.println("失败: " + originalName + " → " + newFileName);
            }
        }
        
        System.out.println("\n操作完成。成功重命名 " + renamedCount + " 个文件");
        
    }
}