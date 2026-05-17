package vn.ipz.phageipz.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class QCConfigManager {

    private static final Logger log = LoggerFactory.getLogger(QCConfigManager.class);

    // Lưu các ngưỡng Warn / Error
    private final Map<String, Map<String, Double>> limits = new HashMap<>();
    
    // Lưu danh sách Adapters (Tên Adapter -> Chuỗi DNA)
    private final Map<String, String> adapters = new HashMap<>();

    @PostConstruct
    public void init() {
        loadLimits();
        loadAdapters();
    }

    private void loadLimits() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("data/limits.txt").getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    String module = parts[0];
                    String type = parts[1];
                    double value = Double.parseDouble(parts[2]);

                    limits.computeIfAbsent(module, k -> new HashMap<>()).put(type, value);
                }
            }
            log.info("Đã nạp thành công bộ luật QC limits.txt ({} modules)", limits.size());
        } catch (Exception e) {
            log.error("Không thể đọc data/limits.txt: {}", e.getMessage());
        }
    }

    private void loadAdapters() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("data/adapter_list.txt").getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\\t+"); 

                if (parts.length >= 2) {
                    String name = parts[0].trim();
                    String seq = parts[1].trim();
                    
                    // Chốt chặn an toàn: Bỏ qua nếu sequence bị rỗng
                    if (!seq.isEmpty()) {
                        adapters.put(name, seq); // Đã sửa adapterMap thành adapters
                    }
                }
            }
            log.info("Đã nạp thành công thư viện adapter_list.txt ({} adapters)", adapters.size());
        } catch (Exception e) {
            log.error("Không thể đọc data/adapter_list.txt: {}", e.getMessage());
        }
    }

    public Double getLimit(String module, String type) {
        if (limits.containsKey(module) && limits.get(module).containsKey(type)) {
            return limits.get(module).get(type);
        }
        return null;
    }

    public Map<String, String> getAllAdapters() {
        return adapters;
    }

    // ============================================================
    // HÀM MỚI BỔ SUNG: Cho phép cập nhật thông số từ giao diện Web
    // ============================================================
    public void updateLimit(String module, String type, Double value) {
        limits.computeIfAbsent(module, k -> new HashMap<>()).put(type, value);
        log.info("Đã cập nhật cấu hình Limit từ Web: Module [{}], Loại [{}], Giá trị [{}]", module, type, value);
    }
}