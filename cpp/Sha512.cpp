#include <iostream>
#include <string>
#include <iomanip>
#include <sstream>
#include <windows.h>
#include <openssl/evp.h>
#include <openssl/sha.h>

// 解决中文乱码
void init_console() {
    SetConsoleOutputCP(CP_UTF8);
    SetConsoleCP(CP_UTF8);
}

// 使用OpenSSL 3.0推荐API
std::string sha512(const std::string& input) {
    EVP_MD_CTX* ctx = EVP_MD_CTX_new();
    unsigned char digest[EVP_MAX_MD_SIZE];
    unsigned int len;
    
    EVP_DigestInit_ex(ctx, EVP_sha512(), NULL);
    EVP_DigestUpdate(ctx, input.c_str(), input.size());
    EVP_DigestFinal_ex(ctx, digest, &len);
    EVP_MD_CTX_free(ctx);

    std::stringstream ss;
    for (size_t i = 0; i < len; i++) {
        ss << std::hex << std::setw(2) << std::setfill('0') << (int)digest[i];
    }
    return ss.str();
}

// 计算字符串的 SHA-512 哈希值
std::string sha512old(const std::string& input) {
    unsigned char digest[SHA512_DIGEST_LENGTH];
    
    // 计算哈希值
    SHA512_CTX ctx;
    SHA512_Init(&ctx);
    SHA512_Update(&ctx, input.c_str(), input.size());
    SHA512_Final(digest, &ctx);
    
    // 将二进制哈希值转换为十六进制字符串
    std::stringstream ss;
    for (int i = 0; i < SHA512_DIGEST_LENGTH; ++i) {
        ss << std::hex << std::setw(2) << std::setfill('0') << (int)digest[i];
    }
    
    return ss.str();
}

int main() {
    init_console();
    
    std::string input = "hello world";
    std::string hash = sha512(input);
    
    std::cout << "输入: " << input << std::endl;
    std::cout << "SHA-512: " << hash << std::endl;
    
    input = "hello world";
    hash = sha512old(input);
    
    std::cout << "输入: " << input << std::endl;
    std::cout << "SHA-512 Old: " << hash << std::endl;


    std::cout << "请输入要计算 SHA-512 哈希的字符串: ";
    std::getline(std::cin, input);
    
    hash = sha512(input);
    
    std::cout << "SHA-512 哈希值: " << hash << std::endl;

    return 0;
}


/**
 * 
#include <iostream>
#include <string>
#include <iomanip>
#include <openssl/sha.h>

// 计算字符串的 SHA-512 哈希值
std::string sha512(const std::string& input) {
    unsigned char digest[SHA512_DIGEST_LENGTH];
    
    // 计算哈希值
    SHA512_CTX ctx;
    SHA512_Init(&ctx);
    SHA512_Update(&ctx, input.c_str(), input.size());
    SHA512_Final(digest, &ctx);
    
    // 将二进制哈希值转换为十六进制字符串
    std::stringstream ss;
    for (int i = 0; i < SHA512_DIGEST_LENGTH; ++i) {
        ss << std::hex << std::setw(2) << std::setfill('0') << (int)digest[i];
    }
    
    return ss.str();
}

int main() {
    std::string input;
    
    std::cout << "请输入要计算 SHA-512 哈希的字符串: ";
    std::getline(std::cin, input);
    
    std::string hash = sha512(input);
    
    std::cout << "SHA-512 哈希值: " << hash << std::endl;
    
    return 0;
}
*/