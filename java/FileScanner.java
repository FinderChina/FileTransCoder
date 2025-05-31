import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class FileScanner {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean scanAll = false;
        
        // 获取用户输入
        System.out.print("请输入要扫描的目录路径: ");
        String directoryPath = scanner.nextLine().trim();
        
        System.out.print("请输入要查找的文件扩展名(多个用逗号分隔，如 txt,jpg,png): ");
        String extensionsInput = scanner.nextLine().trim();

        System.out.print("请输入要忽略的路径名(多个用逗号分隔，如 01,02): ");
        String ignoresInput = scanner.nextLine().trim();

        // 处理扩展名
        List<String> extensions = Arrays.stream(extensionsInput.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        
        if (extensions.isEmpty() || extensions.indexOf("*")>=0) {
            scanAll = true;
        }
        
        // 处理扩展名
        List<String> ignores = Arrays.stream(ignoresInput.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        // 扫描文件
        List<File> matchedFiles = scanFiles(directoryPath, extensions, scanAll, ignores);
        
        // 显示结果
        if (matchedFiles.isEmpty()) {
            System.out.println("\n没有找到匹配的文件");
        } else {
            System.out.println("\n找到 " + matchedFiles.size() + " 个匹配的文件:");
            int maxDisplay = 1000; // 最多显示前200个结果
            int idxPath = 1;
            String oldPath = "";
            for (int i = 0; i < Math.min(matchedFiles.size(), maxDisplay); i++) {
                String fileName = matchedFiles.get(i).getAbsolutePath();
                String wordsCount = "";
                if(fileName.toLowerCase().endsWith(".pdf")){
                    wordsCount = ", " + countWordsInPDF(fileName);
                }



                //切换文件夹时打印空行
                String path = matchedFiles.get(i).getParent();
                if(!path.equals(oldPath)){
                    // 终止打印，每打印100个文件夹提示是否退出
                    if((idxPath-1) % 100 ==0){
                        System.out.println("");
                        System.out.print("按 Q 键退出打印，其他键继续打印结果: ");
                        String exitInput = scanner.nextLine().trim();
                        if("q".equalsIgnoreCase(exitInput)){
                            break;
                        }
                    }
                    
                    // 打印新的文件夹
                    String sPath = path.replace(directoryPath, "$SCAN_HOME$");
                    System.out.println("");
                    System.out.println(String.valueOf(idxPath++) + ": " + sPath);
                    System.out.println("--------------------------------------------");
                    oldPath = path;
                }

                //打印文件名
                fileName = fileName.replace(directoryPath, "$SCAN_HOME$");
                System.out.println((i + 1) + ". " + fileName + wordsCount);
            }
            
            if (matchedFiles.size() > maxDisplay) {
                System.out.println("\n...已省略 " + (matchedFiles.size() - maxDisplay) + " 个文件...");
            }
            
            // 询问是否保存结果
            System.out.print("\n是否要将结果保存到文件?(y/n): ");
            String saveChoice = scanner.nextLine().trim().toLowerCase();
            
            if (saveChoice.equals("y") || saveChoice.equals("yes")) {
                System.out.print("输入文件名(默认scan_results.txt): ");
                String fileName = scanner.nextLine().trim();
                if (fileName.isEmpty()) {
                    fileName = "scan_results.txt";
                }
                
                saveResults(matchedFiles, fileName);
            }
            scanner.close();
        }
    }
    
    /**
     * 扫描指定目录下匹配扩展名的文件
     * @param directoryPath 目录路径
     * @param extensions 扩展名列表(小写)
     * @return 匹配的文件列表
     */
    public static List<File> scanFiles(String directoryPath, List<String> extensions, boolean scanAll, List<String> ignores) {
        List<File> matchedFiles = new ArrayList<>();
        File directory = new File(directoryPath);
        
        if (!directory.exists()) {
            System.out.println("错误：目录不存在 - " + directoryPath);
            return matchedFiles;
        }
        
        if (!directory.isDirectory()) {
            System.out.println("错误：路径不是目录 - " + directoryPath);
            return matchedFiles;
        }
        
        try {
            Files.walk(Paths.get(directoryPath))
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    String fullName = path.toFile().getAbsolutePath();
                    String fileName = path.getFileName().toString();
                    String fileExtension = getFileExtension(fileName).toLowerCase();
                    boolean ignore = false;
                    for(String i:ignores){
                        if(fullName.indexOf(i)>0){
                            ignore = true;
                        }
                    }
                    if (!ignore && (scanAll || extensions.contains(fileExtension))) {
                        matchedFiles.add(path.toFile());
                    }
                });
        } catch (IOException e) {
            System.out.println("扫描文件时出错: " + e.getMessage());
        }
        
        return matchedFiles;
    }
    
    /**
     * 获取文件扩展名(不带点)
     * @param fileName 文件名
     * @return 扩展名
     */
    private static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }
    
    /**
     * 将结果保存到文件
     * @param files 文件列表
     * @param fileName 输出文件名
     */
    private static void saveResults(List<File> files, String fileName) {
        try {
            List<String> filePaths = files.stream()
                    .map(File::getAbsolutePath)
                    .collect(Collectors.toList());
            
            Files.write(Paths.get(fileName), filePaths);
            System.out.println("结果已保存到 " + new File(fileName).getAbsolutePath());
        } catch (IOException e) {
            System.out.println("保存结果时出错: " + e.getMessage());
        }
    }

    public static int countWordsInPDF(String filePath) {
        int count = 0;
        // 加载PDF文档
        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            // 创建PDFTextStripper来提取文本
            PDFTextStripper stripper = new PDFTextStripper();
            
            // 获取PDF中的所有文本
            String text = stripper.getText(document);
            
            // 统计字数
            count = countWords(text,false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return count;
    }

    public static int countAllWords(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        // 使用正则表达式分割单词
        // 匹配任何空白字符（空格、制表符、换行符等）作为分隔符
        String[] words = text.split("\\s+");
        
        return words.length;
    }

    public static int countWords(String text, boolean charOnly) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        // 移除标点符号
        if(charOnly){
            text = text.replaceAll("[^a-zA-Z0-9\\s]", "");
        }
        
        // 分割单词并过滤空字符串
        String[] words = text.split("\\s+");
        
        // 统计非空单词
        int count = 0;
        for (String word : words) {
            if (!word.trim().isEmpty()) {
                count++;
            }
        }
        
        return count;
    }
}