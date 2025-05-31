#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <iomanip>
#include <sstream>
#include <cstdint>
#include <filesystem>
#include <algorithm>
#include <array>

namespace fs = std::filesystem;

// SHA512实现
class SHA512 {
private:
    typedef std::array<uint64_t, 8> HashType;
    HashType hash;

    static constexpr uint64_t k[80] = {
        0x428a2f98d728ae22, 0x7137449123ef65cd, 0xb5c0fbcfec4d3b2f, 0xe9b5dba58189dbbc,
        0x3956c25bf348b538, 0x59f111f1b605d019, 0x923f82a4af194f9b, 0xab1c5ed5da6d8118,
        0xd807aa98a3030242, 0x12835b0145706fbe, 0x243185be4ee4b28c, 0x550c7dc3d5ffb4e2,
        0x72be5d74f27b896f, 0x80deb1fe3b1696b1, 0x9bdc06a725c71235, 0xc19bf174cf692694,
        0xe49b69c19ef14ad2, 0xefbe4786384f25e3, 0x0fc19dc68b8cd5b5, 0x240ca1cc77ac9c65,
        0x2de92c6f592b0275, 0x4a7484aa6ea6e483, 0x5cb0a9dcbd41fbd4, 0x76f988da831153b5,
        0x983e5152ee66dfab, 0xa831c66d2db43210, 0xb00327c898fb213f, 0xbf597fc7beef0ee4,
        0xc6e00bf33da88fc2, 0xd5a79147930aa725, 0x06ca6351e003826f, 0x142929670a0e6e70,
        0x27b70a8546d22ffc, 0x2e1b21385c26c926, 0x4d2c6dfc5ac42aed, 0x53380d139d95b3df,
        0x650a73548baf63de, 0x766a0abb3c77b2a8, 0x81c2c92e47edaee6, 0x92722c851482353b,
        0xa2bfe8a14cf10364, 0xa81a664bbc423001, 0xc24b8b70d0f89791, 0xc76c51a30654be30,
        0xd192e819d6ef5218, 0xd69906245565a910, 0xf40e35855771202a, 0x106aa07032bbd1b8,
        0x19a4c116b8d2d0c8, 0x1e376c085141ab53, 0x2748774cdf8eeb99, 0x34b0bcb5e19b48a8,
        0x391c0cb3c5c95a63, 0x4ed8aa4ae3418acb, 0x5b9cca4f7763e373, 0x682e6ff3d6b2b8a3,
        0x748f82ee5defb2fc, 0x78a5636f43172f60, 0x84c87814a1f0ab72, 0x8cc702081a6439ec,
        0x90befffa23631e28, 0xa4506cebde82bde9, 0xbef9a3f7b2c67915, 0xc67178f2e372532b,
        0xca273eceea26619c, 0xd186b8c721c0c207, 0xeada7dd6cde0eb1e, 0xf57d4f7fee6ed178,
        0x06f067aa72176fba, 0x0a637dc5a2c898a6, 0x113f9804bef90dae, 0x1b710b35131c471b,
        0x28db77f523047d84, 0x32caab7b40c72493, 0x3c9ebe0a15c9bebc, 0x431d67c49c100d4c,
        0x4cc5d4becb3e42b6, 0x597f299cfc657e2a, 0x5fcb6fab3ad6faec, 0x6c44198c4a475817
    };

    static uint64_t rotr(uint64_t x, uint64_t n) {
        return (x >> n) | (x << (64 - n));
    }

    static uint64_t Ch(uint64_t x, uint64_t y, uint64_t z) {
        return (x & y) ^ (~x & z);
    }

    static uint64_t Maj(uint64_t x, uint64_t y, uint64_t z) {
        return (x & y) ^ (x & z) ^ (y & z);
    }

    static uint64_t Sigma0(uint64_t x) {
        return rotr(x, 28) ^ rotr(x, 34) ^ rotr(x, 39);
    }

    static uint64_t Sigma1(uint64_t x) {
        return rotr(x, 14) ^ rotr(x, 18) ^ rotr(x, 41);
    }

    static uint64_t sigma0(uint64_t x) {
        return rotr(x, 1) ^ rotr(x, 8) ^ (x >> 7);
    }

    static uint64_t sigma1(uint64_t x) {
        return rotr(x, 19) ^ rotr(x, 61) ^ (x >> 6);
    }

    void processBlock(const uint8_t* block) {
        uint64_t w[80];
        for (int i = 0; i < 16; ++i) {
            w[i] = ((uint64_t)block[i * 8] << 56) |
                   ((uint64_t)block[i * 8 + 1] << 48) |
                   ((uint64_t)block[i * 8 + 2] << 40) |
                   ((uint64_t)block[i * 8 + 3] << 32) |
                   ((uint64_t)block[i * 8 + 4] << 24) |
                   ((uint64_t)block[i * 8 + 5] << 16) |
                   ((uint64_t)block[i * 8 + 6] << 8) |
                   ((uint64_t)block[i * 8 + 7]);
        }

        for (int i = 16; i < 80; ++i) {
            w[i] = sigma1(w[i - 2]) + w[i - 7] + sigma0(w[i - 15]) + w[i - 16];
        }

        HashType temp = hash;

        for (int i = 0; i < 80; ++i) {
            uint64_t t1 = temp[7] + Sigma1(temp[4]) + Ch(temp[4], temp[5], temp[6]) + k[i] + w[i];
            uint64_t t2 = Sigma0(temp[0]) + Maj(temp[0], temp[1], temp[2]);
            temp[7] = temp[6];
            temp[6] = temp[5];
            temp[5] = temp[4];
            temp[4] = temp[3] + t1;
            temp[3] = temp[2];
            temp[2] = temp[1];
            temp[1] = temp[0];
            temp[0] = t1 + t2;
        }

        for (int i = 0; i < 8; ++i) {
            hash[i] += temp[i];
        }
    }

public:
    SHA512() {
        reset();
    }

    void reset() {
        hash = {
            0x6a09e667f3bcc908,
            0xbb67ae8584caa73b,
            0x3c6ef372fe94f82b,
            0xa54ff53a5f1d36f1,
            0x510e527fade682d1,
            0x9b05688c2b3e6c1f,
            0x1f83d9abfb41bd6b,
            0x5be0cd19137e2179
        };
    }

    void update(const uint8_t* data, size_t length) {
        static uint8_t buffer[128];
        static size_t bufferSize = 0;
        static uint64_t totalLength = 0;

        totalLength += length;

        if (bufferSize + length < 128) {
            std::copy(data, data + length, buffer + bufferSize);
            bufferSize += length;
            return;
        }

        size_t offset = 0;
        if (bufferSize > 0) {
            size_t toCopy = 128 - bufferSize;
            std::copy(data, data + toCopy, buffer + bufferSize);
            processBlock(buffer);
            offset += toCopy;
            bufferSize = 0;
        }

        while (offset + 128 <= length) {
            processBlock(data + offset);
            offset += 128;
        }

        if (offset < length) {
            bufferSize = length - offset;
            std::copy(data + offset, data + length, buffer);
        }
    }

    std::string final() {
        static uint8_t buffer[128];
        size_t bufferSize = 0;

        uint64_t totalBits = (bufferSize + 8) * 8;
        buffer[bufferSize++] = 0x80;

        if (bufferSize > 112) {
            while (bufferSize < 128) {
                buffer[bufferSize++] = 0;
            }
            processBlock(buffer);
            bufferSize = 0;
        }

        while (bufferSize < 112) {
            buffer[bufferSize++] = 0;
        }

        for (int i = 0; i < 8; ++i) {
            buffer[120 + i] = (totalBits >> (56 - i * 8)) & 0xFF;
        }

        processBlock(buffer);

        std::stringstream ss;
        for (uint64_t h : hash) {
            for (int i = 0; i < 8; ++i) {
                ss << std::hex << std::setw(2) << std::setfill('0') << ((h >> (56 - i * 8)) & 0xFF);
            }
        }

        reset();
        return ss.str();
    }
};

// ZIP文件解析
class ZipReader {
private:
    struct LocalFileHeader {
        uint32_t signature;
        uint16_t versionNeeded;
        uint16_t flags;
        uint16_t compressionMethod;
        uint16_t lastModTime;
        uint16_t lastModDate;
        uint32_t crc32;
        uint32_t compressedSize;
        uint32_t uncompressedSize;
        uint16_t fileNameLength;
        uint16_t extraFieldLength;
        std::string fileName;
        std::vector<uint8_t> extraField;
        std::vector<uint8_t> data;
    };

    std::ifstream file;
    std::vector<LocalFileHeader> entries;

    void readLocalFileHeader() {
        LocalFileHeader header;
        file.read(reinterpret_cast<char*>(&header.signature), 4);
        file.read(reinterpret_cast<char*>(&header.versionNeeded), 2);
        file.read(reinterpret_cast<char*>(&header.flags), 2);
        file.read(reinterpret_cast<char*>(&header.compressionMethod), 2);
        file.read(reinterpret_cast<char*>(&header.lastModTime), 2);
        file.read(reinterpret_cast<char*>(&header.lastModDate), 2);
        file.read(reinterpret_cast<char*>(&header.crc32), 4);
        file.read(reinterpret_cast<char*>(&header.compressedSize), 4);
        file.read(reinterpret_cast<char*>(&header.uncompressedSize), 4);
        file.read(reinterpret_cast<char*>(&header.fileNameLength), 2);
        file.read(reinterpret_cast<char*>(&header.extraFieldLength), 2);

        header.fileName.resize(header.fileNameLength);
        file.read(&header.fileName[0], header.fileNameLength);

        header.extraField.resize(header.extraFieldLength);
        file.read(reinterpret_cast<char*>(header.extraField.data()), header.extraFieldLength);

        //if (header.compressionMethod == 0) { // 未压缩
        header.data.resize(header.uncompressedSize);
        file.read(reinterpret_cast<char*>(header.data.data()), header.uncompressedSize);
        //} else {
        //    throw std::runtime_error("Compressed files not supported");
        //}

        entries.push_back(header);
    }

public:
    // 在ZipReader类的open方法中添加更多检查
    bool open(const std::string& filename) {
        file.open(filename, std::ios::binary);
        if (!file.is_open()) {
            std::cerr << "Error: Could not open file " << filename << std::endl;
            return false;
        }

        // 检查文件是否为空
        file.seekg(0, std::ios::end);
        if (file.tellg() == 0) {
            std::cerr << "Error: File is empty" << std::endl;
            file.close();
            return false;
        }
        file.seekg(0, std::ios::beg);

        try {
            bool foundValidEntry = false;
            while (file) {
                uint32_t signature;
                file.read(reinterpret_cast<char*>(&signature), 4);
                
                if (file.gcount() != 4) break; // 读取失败或EOF

                if (signature == 0x04034b50) { // 本地文件头签名
                    readLocalFileHeader();
                    foundValidEntry = true;
                } 
                else if (signature == 0x02014b50) { // 中央目录记录
                    break; // 跳过中央目录
                }
                else {
                    std::cerr << "Warning: Unknown signature " << std::hex << signature << std::endl;
                    // 尝试寻找下一个可能的头
                    file.seekg(-3, std::ios::cur); // 回退3字节，因为签名是4字节
                }
            }

            if (!foundValidEntry) {
                std::cerr << "Error: No valid ZIP entries found" << std::endl;
                file.close();
                return false;
            }
        } 
        catch (const std::exception& e) {
            std::cerr << "Error reading ZIP file: " << e.what() << std::endl;
            file.close();
            return false;
        }

        return true;
    }

    const std::vector<LocalFileHeader>& getEntries() const {
        return entries;
    }

    ~ZipReader() {
        if (file.is_open()) {
            file.close();
        }
    }
};

// 生成转码字符串向量
std::vector<std::string> generateTranscodeStrings(const std::string& baseEncoding, int count) {
    std::vector<std::string> transcodeStrings;
    for (int i = 0; i < count; i++) {
        std::string encodingWithIndex = baseEncoding + std::to_string(i);
        SHA512 sha;
        sha.update(reinterpret_cast<const uint8_t*>(encodingWithIndex.data()), encodingWithIndex.size());
        transcodeStrings.push_back(sha.final());
    }

    std::cout << transcodeStrings.size() << " : ";
    for (size_t i = 0; i < 32; ++i) {  // key.size()
        std::cout << (transcodeStrings[i]) << " ";  // 转成 int 避免打印 ASCII 字符
    }
    std::cout << std::endl;

    return transcodeStrings;
}

// 对数据进行异或操作
void xorDataWithTranscodeStrings(std::vector<uint8_t>& data, const std::vector<std::string>& transcodeStrings) {
    size_t transcodeIndex = 0;
    size_t stringIndex = 0;
    
    for (size_t i = 0; i < data.size(); i++) {
        // 获取当前转码字符串
        const std::string& currentString = transcodeStrings[transcodeIndex];
        
        // 获取当前字符（两个十六进制字符表示一个字节）
        if (stringIndex + 1 >= currentString.size()) {
            transcodeIndex = (transcodeIndex + 1) % transcodeStrings.size();
            stringIndex = 0;
            continue;
        }
        
        std::string byteStr = currentString.substr(stringIndex, 2);
        uint8_t transcodeByte = static_cast<uint8_t>(std::stoul(byteStr, nullptr, 16));
        
        // 执行异或操作
        data[i] ^= transcodeByte;
        
        // 移动到下一个字符
        stringIndex += 2;
        if (stringIndex >= currentString.size()) {
            transcodeIndex = (transcodeIndex + 1) % transcodeStrings.size();
            stringIndex = 0;
        }
    }
}

int main(int argc, char* argv[]) {
    if (argc != 4) {
        std::cerr << "Usage: " << argv[0] << " <zipFile> <outputDir> <baseEncoding>" << std::endl;
        return 1;
    }

    std::string zipPath = argv[1];
    std::string outputDir = argv[2];
    std::string baseEncoding = argv[3];

    try {
        // 生成转码字符串
        auto transcodeStrings = generateTranscodeStrings(baseEncoding, 100);

        // 读取ZIP文件
        ZipReader zip;
        if (!zip.open(zipPath)) {
            std::cerr << "Failed to open ZIP file: " << zipPath << std::endl;
            return 1;
        }

        // 创建输出目录
        fs::create_directories(outputDir);

        // 处理每个文件
        for (const auto& entry : zip.getEntries()) {
            if (entry.fileName.back() == '/') continue; // 跳过目录

            // 处理文件数据
            auto data = entry.data;
            xorDataWithTranscodeStrings(data, transcodeStrings);

            // 写入输出文件
            fs::path outputPath = fs::path(outputDir) / fs::path(entry.fileName).filename();
            std::ofstream outFile(outputPath, std::ios::binary);
            if (!outFile) {
                std::cerr << "Failed to create output file: " << outputPath << std::endl;
                continue;
            }

            outFile.write(reinterpret_cast<const char*>(data.data()), data.size());
            outFile.close();
        }

        std::cout << "Processing completed successfully." << std::endl;
    } catch (const std::exception& e) {
        std::cerr << "Error: " << e.what() << std::endl;
        return 1;
    }

    return 0;
}