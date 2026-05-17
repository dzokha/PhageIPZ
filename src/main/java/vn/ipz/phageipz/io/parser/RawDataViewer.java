package vn.ipz.phageipz.io.parser;

import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import vn.ipz.phageipz.config.PhageIPZProperties; 

@Component
public class RawDataViewer {
    
    private final PhageIPZProperties properties;

    // Khởi tạo constructor để Spring Boot tự động inject cấu hình vào đây
    public RawDataViewer(PhageIPZProperties properties) {
        this.properties = properties;
    }

    /**
     * Hàm dùng cho API: Đọc trực tiếp từ InputStream của file được upload lên (MultipartFile)
     */
    public List<String> getRawFastqFromStream(InputStream inputStream, int numberOfRecords) {
        List<String> result = new ArrayList<>();
        
        result.add("--------------------------------------------------");
        result.add("🔍 KẾT QUẢ TRÍCH XUẤT DỮ LIỆU THÔ:");
        result.add("--------------------------------------------------\n");

        try (GZIPInputStream gzipIn = new GZIPInputStream(inputStream);
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzipIn))) {

            String line;
            int recordCount = 0;
            int lineInRecord = 0;

            while ((line = reader.readLine()) != null && recordCount < numberOfRecords) {
                lineInRecord++;
                
                switch (lineInRecord) {
                    case 1: result.add("[DÒNG 1 - HEADER]  : " + line); break;
                    case 2: result.add("[DÒNG 2 - DNA SEQ] : " + line); break;
                    case 3: result.add("[DÒNG 3 - PLUS]    : " + line); break;
                    case 4: 
                        result.add("[DÒNG 4 - QUALITY] : " + line);
                        result.add("--- Kết thúc đoạn " + (recordCount + 1) + " ---\n");
                        lineInRecord = 0; 
                        recordCount++;
                        break;
                }
            }

            if (recordCount == 0) {
                result.add("⚠️ Cảnh báo: File rỗng hoặc không đúng định dạng GZIP.");
            }

        } catch (Exception e) {
            result.add("❌ Lỗi giải mã file (Có thể file không phải là chuẩn .gz): " + e.getMessage());
        }

        return result;
    }

    /**
     * Hàm đọc file thô từ server. 
     * Bây giờ ta chỉ cần truyền tên file, đường dẫn gốc sẽ tự động lấy từ properties!
     */
    public void viewRawFastqFromFile(String fileName, int numberOfRecords) {
        // Tự động ghép nối thư mục gốc từ cấu hình và tên file
        String filePath = properties.getRawDir() + "/" + fileName;
        
        System.out.println("\n--------------------------------------------------");
        System.out.println("🔍 ĐANG TRÍCH XUẤT DỮ LIỆU THÔ TỪ FILE:");
        System.out.println("   " + filePath);
        System.out.println("--------------------------------------------------\n");

        try (GZIPInputStream gzipIn = new GZIPInputStream(new FileInputStream(filePath));
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzipIn))) {

            String line;
            int recordCount = 0;
            int lineInRecord = 0;

            while ((line = reader.readLine()) != null && recordCount < numberOfRecords) {
                lineInRecord++;
                switch (lineInRecord) {
                    case 1: System.out.println("[DÒNG 1 - HEADER]  : " + line); break;
                    case 2: System.out.println("[DÒNG 2 - DNA SEQ] : " + line); break;
                    case 3: System.out.println("[DÒNG 3 - PLUS]    : " + line); break;
                    case 4: 
                        System.out.println("[DÒNG 4 - QUALITY] : " + line);
                        System.out.println("--- Kết thúc đoạn " + (recordCount + 1) + " ---\n");
                        lineInRecord = 0; 
                        recordCount++;
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi đọc file thô: " + e.getMessage());
        }
    }
}