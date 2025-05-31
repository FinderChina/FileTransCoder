#include <iostream>
#include <fstream>
#include <vector>
#include <string>
#include <iomanip>
#include <sstream>
#include <filesystem>
#include <array>
#include <algorithm>

namespace fs = std::filesystem;

// ======================== SHA-512 实现 ========================
class SHA512 {
private:
    static constexpr size_t BlockSize = 128;
    
    std::array<uint64_t, 8> hash;
    std::array<uint8_t, BlockSize> buffer;
    uint64_t totalBytes = 0;
    size_t bufferPos = 0;

    static constexpr std::array<uint64_t, 80> K = {
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

    static uint64_t Ch(uint64_t x, uint64_t y, uint64_t z) { return (x & y) ^ (~x & z); }
    static uint64_t Maj(uint64_t x, uint64_t y, uint64_t z) { return (x & y) ^ (x & z) ^ (y & z); }
    static uint64_t Sigma0(uint64_t x) { return rotr(x, 28) ^ rotr(x, 34) ^ rotr(x, 14); }
    static uint64_t Sigma1(uint64_t x) { return rotr(x, 18) ^ rotr(x, 41) ^ rotr(x, 14); }
    static uint64_t sigma0(uint64_t x) { return rotr(x, 1) ^ rotr(x, 8) ^ (x >> 7); }
    static uint64_t sigma1(uint64_t x) { return rotr(x, 19) ^ rotr(x, 61) ^ (x >> 6); }
    static uint64_t rotr(uint64_t x, uint64_t n) { return (x >> n) | (x << (64 - n)); }

    void processBlock(const uint8_t* data) {
        std::array<uint64_t, 80> W;
        
        for (size_t t = 0; t < 16; ++t) {
            W[t] = static_cast<uint64_t>(data[t*8]) << 56;
            W[t] |= static_cast<uint64_t>(data[t*8+1]) << 48;
            W[t] |= static_cast<uint64_t>(data[t*8+2]) << 40;
            W[t] |= static_cast<uint64_t>(data[t*8+3]) << 32;
            W[t] |= static_cast<uint64_t>(data[t*8+4]) << 24;
            W[t] |= static_cast<uint64_t>(data[t*8+5]) << 16;
            W[t] |= static_cast<uint64_t>(data[t*8+6]) << 8;
            W[t] |= static_cast<uint64_t>(data[t*8+7]);
        }
        
        for (size_t t = 16; t < 80; ++t) {
            W[t] = sigma1(W[t-2]) + W[t-7] + sigma0(W[t-15]) + W[t-16];
        }
        
        auto [a, b, c, d, e, f, g, h] = hash;
        
        for (size_t t = 0; t < 80; ++t) {
            uint64_t T1 = h + Sigma1(e) + Ch(e, f, g) + K[t] + W[t];
            uint64_t T2 = Sigma0(a) + Maj(a, b, c);
            h = g;
            g = f;
            f = e;
            e = d + T1;
            d = c;
            c = b;
            b = a;
            a = T1 + T2;
        }
        
        hash[0] += a;
        hash[1] += b;
        hash[2] += c;
        hash[3] += d;
        hash[4] += e;
        hash[5] += f;
        hash[6] += g;
        hash[7] += h;
    }
    
public:
    SHA512() {
        hash = {
            0x6a09e667f3bcc908, 0xbb67ae8584caa73b, 0x3c6ef372fe94f82b, 0xa54ff53a5f1d36f1,
            0x510e527fade682d1, 0x9b05688c2b3e6c1f, 0x1f83d9abfb41bd6b, 0x5be0cd19137e2179
        };
    }
    
    void update(const uint8_t* data, size_t len) {
        totalBytes += len;
        
        while (len > 0) {
            size_t toCopy = std::min(BlockSize - bufferPos, len);
            std::copy_n(data, toCopy, buffer.begin() + bufferPos);
            bufferPos += toCopy;
            data += toCopy;
            len -= toCopy;
            
            if (bufferPos == BlockSize) {
                processBlock(buffer.data());
                bufferPos = 0;
            }
        }
    }
    
    void update(const std::string& str) {
        update(reinterpret_cast<const uint8_t*>(str.data()), str.size());
    }
    
    std::string final() {
        size_t paddingLen = (bufferPos < 112) ? (112 - bufferPos) : (240 - bufferPos);
        uint8_t padding[128] = {0};
        padding[0] = 0x80;
        
        update(padding, paddingLen);
        
        uint64_t bitLength = totalBytes * 8;
        uint8_t lengthBytes[16];
        for (int i = 0; i < 8; ++i) {
            lengthBytes[i] = (bitLength >> (56 - i*8)) & 0xFF;
            lengthBytes[i+8] = 0;
        }
        
        update(lengthBytes, 16);
        
        std::stringstream ss;
        for (uint64_t h : hash) {
            ss << std::hex << std::setw(16) << std::setfill('0') << h;
        }
        
        return ss.str();
    }
};

// ======================== CRC32 计算 ========================
class CRC32 {
private:
    static constexpr uint32_t crcTable[256] = {
        0x00000000, 0x77073096, 0xEE0E612C, 0x990951BA, 0x076DC419, 0x706AF48F, 0xE963A535, 0x9E6495A3,
        0x0EDB8832, 0x79DCB8A4, 0xE0D5E91E, 0x97D2D988, 0x09B64C2B, 0x7EB17CBD, 0xE7B82D07, 0x90BF1D91,
        0x1DB71064, 0x6AB020F2, 0xF3B97148, 0x84BE41DE, 0x1ADAD47D, 0x6DDDE4EB, 0xF4D4B551, 0x83D385C7,
        0x136C9856, 0x646BA8C0, 0xFD62F97A, 0x8A65C9EC, 0x14015C4F, 0x63066CD9, 0xFA0F3D63, 0x8D080DF5,
        0x3B6E20C8, 0x4C69105E, 0xD56041E4, 0xA2677172, 0x3C03E4D1, 0x4B04D447, 0xD20D85FD, 0xA50AB56B,
        0x35B5A8FA, 0x42B2986C, 0xDBBBC9D6, 0xACBCF940, 0x32D86CE3, 0x45DF5C75, 0xDCD60DCF, 0xABD13D59,
        0x26D930AC, 0x51DE003A, 0xC8D75180, 0xBFD06116, 0x21B4F4B5, 0x56B3C423, 0xCFBA9599, 0xB8BDA50F,
        0x2802B89E, 0x5F058808, 0xC60CD9B2, 0xB10BE924, 0x2F6F7C87, 0x58684C11, 0xC1611DAB, 0xB6662D3D,
        0x76DC4190, 0x01DB7106, 0x98D220BC, 0xEFD5102A, 0x71B18589, 0x06B6B51F, 0x9FBFE4A5, 0xE8B8D433,
        0x7807C9A2, 0x0F00F934, 0x9609A88E, 0xE10E9818, 0x7F6A0DBB, 0x086D3D2D, 0x91646C97, 0xE6635C01,
        0x6B6B51F4, 0x1C6C6162, 0x856530D8, 0xF262004E, 0x6C0695ED, 0x1B01A57B, 0x8208F4C1, 0xF50FC457,
        0x65B0D9C6, 0x12B7E950, 0x8BBEB8EA, 0xFCB9887C, 0x62DD1DDF, 0x15DA2D49, 0x8CD37CF3, 0xFBD44C65,
        0x4DB26158, 0x3AB551CE, 0xA3BC0074, 0xD4BB30E2, 0x4ADFA541, 0x3DD895D7, 0xA4D1C46D, 0xD3D6F4FB,
        0x4369E96A, 0x346ED9FC, 0xAD678846, 0xDA60B8D0, 0x44042D73, 0x33031DE5, 0xAA0A4C5F, 0xDD0D7CC9,
        0x5005713C, 0x270241AA, 0xBE0B1010, 0xC90C2086, 0x5768B525, 0x206F85B3, 0xB966D409, 0xCE61E49F,
        0x5EDEF90E, 0x29D9C998, 0xB0D09822, 0xC7D7A8B4, 0x59B33D17, 0x2EB40D81, 0xB7BD5C3B, 0xC0BA6CAD,
        0xEDB88320, 0x9ABFB3B6, 0x03B6E20C, 0x74B1D29A, 0xEAD54739, 0x9DD277AF, 0x04DB2615, 0x73DC1683,
        0xE3630B12, 0x94643B84, 0x0D6D6A3E, 0x7A6A5AA8, 0xE40ECF0B, 0x9309FF9D, 0x0A00AE27, 0x7D079EB1,
        0xF00F9344, 0x8708A3D2, 0x1E01F268, 0x6906C2FE, 0xF762575D, 0x806567CB, 0x196C3671, 0x6E6B06E7,
        0xFED41B76, 0x89D32BE0, 0x10DA7A5A, 0x67DD4ACC, 0xF9B9DF6F, 0x8EBEEFF9, 0x17B7BE43, 0x60B08ED5,
        0xD6D6A3E8, 0xA1D1937E, 0x38D8C2C4, 0x4FDFF252, 0xD1BB67F1, 0xA6BC5767, 0x3FB506DD, 0x48B2364B,
        0xD80D2BDA, 0xAF0A1B4C, 0x36034AF6, 0x41047A60, 0xDF60EFC3, 0xA867DF55, 0x316E8EEF, 0x4669BE79,
        0xCB61B38C, 0xBC66831A, 0x256FD2A0, 0x5268E236, 0xCC0C7795, 0xBB0B4703, 0x220216B9, 0x5505262F,
        0xC5BA3BBE, 0xB2BD0B28, 0x2BB45A92, 0x5CB36A04, 0xC2D7FFA7, 0xB5D0CF31, 0x2CD99E8B, 0x5BDEAE1D,
        0x9B64C2B0, 0xEC63F226, 0x756AA39C, 0x026D930A, 0x9C0906A9, 0xEB0E363F, 0x72076785, 0x05005713,
        0x95BF4A82, 0xE2B87A14, 0x7BB12BAE, 0x0CB61B38, 0x92D28E9B, 0xE5D5BE0D, 0x7CDCEFB7, 0x0BDBDF21,
        0x86D3D2D4, 0xF1D4E242, 0x68DDB3F8, 0x1FDA836E, 0x81BE16CD, 0xF6B9265B, 0x6FB077E1, 0x18B74777,
        0x88085AE6, 0xFF0F6A70, 0x66063BCA, 0x11010B5C, 0x8F659EFF, 0xF862AE69, 0x616BFFD3, 0x166CCF45,
        0xA00AE278, 0xD70DD2EE, 0x4E048354, 0x3903B3C2, 0xA7672661, 0xD06016F7, 0x4969474D, 0x3E6E77DB,
        0xAED16A4A, 0xD9D65ADC, 0x40DF0B66, 0x37D83BF0, 0xA9BCAE53, 0xDEBB9EC5, 0x47B2CF7F, 0x30B5FFE9,
        0xBDBDF21C, 0xCABAC28A, 0x53B39330, 0x24B4A3A6, 0xBAD03605, 0xCDD70693, 0x54DE5729, 0x23D967BF,
        0xB3667A2E, 0xC4614AB8, 0x5D681B02, 0x2A6F2B94, 0xB40BBE37, 0xC30C8EA1, 0x5A05DF1B, 0x2D02EF8D
    };

public:
    static uint32_t calculate(const uint8_t* data, size_t length) {
        uint32_t crc = 0xFFFFFFFF;
        for (size_t i = 0; i < length; ++i) {
            crc = (crc >> 8) ^ crcTable[(crc ^ data[i]) & 0xFF];
        }
        return crc ^ 0xFFFFFFFF;
    }
};

// ======================== ZIP 打包实现 ========================
class ZipCreator {
private:
    struct FileEntry {
        std::string name;
        std::vector<uint8_t> data;
        uint32_t crc;
        uint16_t modTime;
        uint16_t modDate;
    };
    
    std::vector<FileEntry> files;
    
    static void writeUint32(std::ostream& os, uint32_t value) {
        os.put(value & 0xFF);
        os.put((value >> 8) & 0xFF);
        os.put((value >> 16) & 0xFF);
        os.put((value >> 24) & 0xFF);
    }
    
    static void writeUint16(std::ostream& os, uint16_t value) {
        os.put(value & 0xFF);
        os.put((value >> 8) & 0xFF);
    }
    
    static void getDosTime(uint16_t& time, uint16_t& date) {
        auto now = std::chrono::system_clock::now();
        time_t tt = std::chrono::system_clock::to_time_t(now);
        tm* t = localtime(&tt);
        
        time = ((t->tm_hour & 0x1F) << 11) | 
               ((t->tm_min & 0x3F) << 5) | 
               ((t->tm_sec / 2) & 0x1F);
               
        date = (((t->tm_year - 80) & 0x7F) << 9) |
               (((t->tm_mon + 1) & 0x0F) << 5) |
               ((t->tm_mday & 0x1F));
    }

    static std::string normalizePath(const fs::path& baseDir, const fs::path& filePath) {
        auto relative = fs::relative(filePath, baseDir).string();
        std::replace(relative.begin(), relative.end(), '\\', '/');
        if (fs::is_directory(filePath)) {
            return relative + "/";
        }
        return relative;
    }

public:
    void addFile(const std::string& name, const std::vector<uint8_t>& data) {
        uint16_t time, date;
        getDosTime(time, date);
        uint32_t crc = CRC32::calculate(data.data(), data.size());
        files.push_back({name, data, crc, time, date});
    }
    
    bool saveToFile(const fs::path& path) {
        std::ofstream out(path, std::ios::binary);
        if (!out) return false;
        
        std::vector<uint32_t> offsets;
        
        // 写入每个文件的本地文件头和数据
        for (const auto& file : files) {
            offsets.push_back(static_cast<uint32_t>(out.tellp()));
            
            // 本地文件头
            out << "PK\x03\x04"; // 签名
            writeUint16(out, 20); // 版本
            writeUint16(out, 0); // 通用位标志
            writeUint16(out, 0); // 压缩方法 (0=存储)
            writeUint16(out, file.modTime); // 最后修改时间
            writeUint16(out, file.modDate); // 最后修改日期
            writeUint32(out, file.crc); // CRC-32
            writeUint32(out, file.data.size()); // 压缩大小
            writeUint32(out, file.data.size()); // 未压缩大小
            writeUint16(out, static_cast<uint16_t>(file.name.size())); // 文件名长度
            writeUint16(out, 0); // 额外字段长度
            out << file.name; // 文件名
            
            // 文件数据
            out.write(reinterpret_cast<const char*>(file.data.data()), file.data.size());
        }
        
        // 中央目录记录
        uint32_t centralDirStart = static_cast<uint32_t>(out.tellp());
        
        for (size_t i = 0; i < files.size(); ++i) {
            const auto& file = files[i];
            
            out << "PK\x01\x02"; // 中央文件头签名
            writeUint16(out, 20); // 版本
            writeUint16(out, 20); // 版本需要解压
            writeUint16(out, 0); // 通用位标志
            writeUint16(out, 0); // 压缩方法
            writeUint16(out, file.modTime); // 最后修改时间
            writeUint16(out, file.modDate); // 最后修改日期
            writeUint32(out, file.crc); // CRC-32
            writeUint32(out, file.data.size()); // 压缩大小
            writeUint32(out, file.data.size()); // 未压缩大小
            writeUint16(out, static_cast<uint16_t>(file.name.size())); // 文件名长度
            writeUint16(out, 0); // 额外字段长度
            writeUint16(out, 0); // 文件注释长度
            writeUint16(out, 0); // 磁盘号开始
            writeUint16(out, 0); // 内部文件属性
            writeUint32(out, 0); // 外部文件属性
            writeUint32(out, offsets[i]); // 相对偏移量
            out << file.name; // 文件名
        }
        
        uint32_t centralDirSize = static_cast<uint32_t>(out.tellp()) - centralDirStart;
        
        // 中央目录结束记录
        out << "PK\x05\x06"; // 结束记录签名
        writeUint16(out, 0); // 当前磁盘号
        writeUint16(out, 0); // 中央目录开始磁盘号
        writeUint16(out, static_cast<uint16_t>(files.size())); // 本磁盘记录数
        writeUint16(out, static_cast<uint16_t>(files.size())); // 总记录数
        writeUint32(out, centralDirSize); // 中央目录大小
        writeUint32(out, centralDirStart); // 中央目录偏移量
        writeUint16(out, 0); // ZIP文件注释长度
        
        return true;
    }

    void addDirectory(const fs::path& baseDir, const fs::path& currentDir, 
                     const std::vector<uint8_t>& key) {
        for (const auto& entry : fs::directory_iterator(currentDir)) {
            if (entry.is_directory()) {
                addDirectory(baseDir, entry.path(), key);
            } else if (entry.is_regular_file()) {
                // 读取文件内容
                std::ifstream in(entry.path(), std::ios::binary);
                std::vector<uint8_t> data((std::istreambuf_iterator<char>(in)), 
                                         std::istreambuf_iterator<char>());
                
                // 转码文件内容
                size_t keyPos = 0;
                for (auto& byte : data) {
                    byte ^= key[keyPos];
                    keyPos = (keyPos + 1) % key.size();
                }
                
                // 添加文件到ZIP
                std::string relativePath = normalizePath(baseDir, entry.path());
                addFile(relativePath, data);
            }
        }
    }
};

// ======================== 主程序逻辑 ========================
std::vector<uint8_t> generateKey(const std::string &transcodeStr) {
    std::vector<uint8_t> key;
    SHA512 sha;
    
    // 生成主哈希
    sha.update(transcodeStr);
    std::string hash = sha.final();
    
    // 循环100次扩展密钥
    for (int i = 0; i < 100; ++i) {
        SHA512 temp;
        temp.update(hash + std::to_string(i));
        std::string tempHash = temp.final();
        
        // 将十六进制字符串转换为字节
        for (size_t j = 0; j < tempHash.size(); j += 2) {
            std::string byteStr = tempHash.substr(j, 2);
            uint8_t byte = static_cast<uint8_t>(std::stoul(byteStr, nullptr, 16));
            key.push_back(byte);
        }
    }
    
    // 打印 key  // #include <iterator>   // for std::ostream_iterator
    //std::copy(key.begin(), key.end(), std::ostream_iterator<int>(std::cout, " "));
    //std::cout << std::endl;
    std::cout << key.size() << " : ";
    for (size_t i = 0; i < 32; ++i) {  // key.size()
        std::cout << static_cast<int>(key[i]) << " ";  // 转成 int 避免打印 ASCII 字符
    }
    std::cout << std::endl;

    return key;
}

std::string getTranscodeString(const std::string& input) {
    if (fs::is_regular_file(input)) {
        std::ifstream file(input);
        if (file) {
            std::string content((std::istreambuf_iterator<char>(file)), 
                              std::istreambuf_iterator<char>());
            return content;
        }
    }
    return input;
}

bool processFile(const fs::path& inputPath, const fs::path& outputPath, 
                const std::vector<uint8_t>& key) {
    // 读取文件内容
    std::ifstream in(inputPath, std::ios::binary);
    std::vector<uint8_t> data((std::istreambuf_iterator<char>(in)), 
                             std::istreambuf_iterator<char>());
    
    // 转码文件内容
    size_t keyPos = 0;
    for (auto& byte : data) {
        byte ^= key[keyPos];
        keyPos = (keyPos + 1) % key.size();
    }
    
    // 写入输出文件
    std::ofstream out(outputPath, std::ios::binary);
    out.write(reinterpret_cast<const char*>(data.data()), data.size());
    
    return true;
}

bool processDirectory(const fs::path& inputDir, const fs::path& outputZip, 
                     const std::vector<uint8_t>& key) {
    ZipCreator zipper;
    zipper.addDirectory(inputDir, inputDir, key);
    return zipper.saveToFile(outputZip);
}

int main(int argc, char* argv[]) {
    if (argc != 4) {
        std::cout << "Usage: " << argv[0] << " <input_path> <output_path> <transcode_string_or_file>\n";
        std::cout << "  - If input_path is a file, creates encoded output file\n";
        std::cout << "  - If input_path is a directory, creates encoded ZIP archive\n";
        std::cout << "  - transcode_string_or_file can be a string or a filename containing the key\n";
        return 1;
    }
    
    try {
        fs::path inputPath = argv[1];
        fs::path outputPath = argv[2];
        std::string transcodeInput = argv[3];
        
        if (!fs::exists(inputPath)) {
            std::cerr << "Input path does not exist\n";
            return 1;
        }
        
        // 获取转码字符串
        std::string transcodeStr = getTranscodeString(transcodeInput);
        if (transcodeStr.empty()) {
            std::cerr << "Invalid transcode string or file\n";
            return 1;
        }
        
        // 生成密钥
        auto key = generateKey(transcodeStr);
        if (key.empty()) {
            std::cerr << "Failed to generate key\n";
            return 1;
        }
        
        // 处理输入
        bool success = false;
        if (fs::is_regular_file(inputPath)) {
            success = processFile(inputPath, outputPath, key);
        } else if (fs::is_directory(inputPath)) {
            success = processDirectory(inputPath, outputPath, key);
        } else {
            std::cerr << "Input is neither a file nor a directory\n";
            return 1;
        }
        
        if (success) {
            std::cout << "Operation completed successfully\n";
            return 0;
        }
    } catch (const std::exception& e) {
        std::cerr << "Error: " << e.what() << "\n";
    }
    
    std::cerr << "Operation failed\n";
    return 1;
}