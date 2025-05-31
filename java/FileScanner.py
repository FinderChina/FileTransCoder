#!usr/bin/env python
# -*- coding: utf-8 -*-
import os
import PyPDF2
from collections import defaultdict

def count_pdf_words(pdf_path):
    """统计PDF文件的字数"""
    word_count = 0
    try:
        with open(pdf_path, 'rb') as file:
            reader = PyPDF2.PdfReader(file)
            for page in reader.pages:
                text = page.extract_text()
                if text:
                    words = text.split()
                    word_count += len(words)
    except Exception as e:
        print(f"处理PDF文件 {pdf_path} 时出错: {e}")
    return word_count

def get_file_stats(directory, extensions=None):
    """获取目录下的文件统计信息"""
    file_stats = defaultdict(list)
    extension_counts = defaultdict(int)
    
    if extensions:
        extensions = [ext.strip().lower() for ext in extensions.split(',')]
    
    current_root = None  # 用于跟踪当前目录
    
    for root, _, files in os.walk(directory):
        # 如果是新的目录，打印空白行
        if root != current_root:
            print()
            current_root = root
        
        for file in files:
            file_path = os.path.join(root, file)
            file_ext = os.path.splitext(file)[1].lower()[1:]  # 去掉点
            
            # 如果指定了扩展名且当前扩展名不在指定列表中，则跳过
            if extensions and file_ext not in extensions:
                continue
                
            file_stats[file_ext].append(file_path)
            extension_counts[file_ext] += 1
            
            # 替换路径中的扫描目录为$SCAN_HOME$
            display_path = file_path.replace(directory, "$SCAN_HOME$")
            
            # 如果是PDF文件，统计字数
            if file_ext == 'pdf':
                word_count = count_pdf_words(file_path)
                print(f"文件: {display_path}, 字数: {word_count}")
            else:
                print(f"文件: {display_path}")
    
    return file_stats, extension_counts

def main():
    print("文件统计程序")
    directory = input("请输入要统计的目录路径: ").strip()
    extensions = input("请输入要统计的文件扩展名(多个用逗号隔开，留空则统计所有): ").strip()
    
    if not os.path.isdir(directory):
        print("错误: 指定的目录不存在!")
        return
    
    # 确保目录路径格式统一（结尾不带斜杠）
    directory = os.path.normpath(directory)
    
    print("\n开始统计...\n")
    file_stats, extension_counts = get_file_stats(directory, extensions)
    
    print("\n统计结果:")
    for ext, count in sorted(extension_counts.items()):
        print(f"{ext.upper()}文件数量: {count}")

if __name__ == "__main__":
    main()