package vn.ipz.phageipz.engine.qc.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.HashMap;


import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import vn.ipz.phageipz.config.PhageIPZProperties;

@Service
public class ModuleConfig {

    private final PhageIPZProperties properties;
    private final ResourceLoader resourceLoader;

    // CHỈNH SỬA: Không dùng 'static' và không khởi tạo bằng 'readParams()' cũ
    private final HashMap<String, Double> parameters = new HashMap<>();

    public ModuleConfig(PhageIPZProperties properties, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() {
        // Spring sẽ gọi hàm này sau khi nạp xong các thuộc tính
        setDefaultParams();
        readParamsFromFile();
    }

    private void setDefaultParams() {
        parameters.put("duplication:warn", 70d);
        parameters.put("duplication:error", 50d);
        parameters.put("kmer:warn", 2d);
        parameters.put("kmer:error", 5d);
        parameters.put("n_content:warn", 5d);
        parameters.put("n_content:error", 20d);
        parameters.put("overrepresented:warn", 0.1);
        parameters.put("overrepresented:error", 1d);
        parameters.put("quality_base_lower:warn", 10d);
        parameters.put("quality_base_lower:error", 5d);
        parameters.put("quality_base_median:warn", 25d);
        parameters.put("quality_base_median:error", 20d);
        parameters.put("sequence:warn", 10d);
        parameters.put("sequence:error", 20d);
        parameters.put("gc_sequence:warn", 15d);
        parameters.put("gc_sequence:error", 30d);
        parameters.put("quality_sequence:warn", 20d);
        parameters.put("quality_sequence:error", 27d);
        parameters.put("tile:warn", 5d);
        parameters.put("tile:error", 10d);
        parameters.put("sequence_length:warn", 1d);
        parameters.put("sequence_length:error", 1d);
        parameters.put("adapter:warn", 5d);
        parameters.put("adapter:error", 10d);

        String[] modules = {"duplication", "kmer", "n_content", "overrepresented", "quality_base", "sequence", "gc_sequence", "quality_sequence", "tile", "sequence_length", "adapter"};
        for (String m : modules) {
            parameters.put(m + ":ignore", 0d);
        }
    }

    private void readParamsFromFile() {
        // Lấy đường dẫn file từ PhageIPZProperties (ánh xạ từ application.yml)
        String limitsPath = properties.getAnalysis().getLimitsFile(); 
        
        try (BufferedReader br = getReader(limitsPath)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") || line.isEmpty()) continue;

                String[] sections = line.split("\\s+");
                if (sections.length != 3) continue;

                String key = sections[0] + ":" + sections[1];
                try {
                    parameters.put(key, Double.parseDouble(sections[2]));
                } catch (NumberFormatException e) {
                    System.err.println("Giá trị không hợp lệ trong file limits: " + sections[2]);
                }
            }
        } catch (Exception e) {
            System.err.println("Không thể nạp file limits, sử dụng giá trị mặc định. Lỗi: " + e.getMessage());
        }
    }

    private BufferedReader getReader(String path) throws Exception {
        // 1. Nếu path được cấu hình cụ thể
        if (path != null && !path.isEmpty()) {
            Resource resource = resourceLoader.getResource(path.startsWith("classpath:") ? path : "file:" + path);
            if (resource.exists()) {
                return new BufferedReader(new InputStreamReader(resource.getInputStream()));
            }
        }

        // 2. Fallback về file mặc định theo đúng cấu trúc thư mục trong ảnh (data/limits.txt)
        Resource defaultResource = resourceLoader.getResource("classpath:data/limits.txt");
        if (!defaultResource.exists()) {
            throw new Exception("Không tìm thấy file limits tại classpath:data/limits.txt");
        }
        return new BufferedReader(new InputStreamReader(defaultResource.getInputStream()));
    }
    
    // private BufferedReader getReader(String path) throws Exception {
    //     // Sử dụng ResourceLoader để xử lý cả file trong jar (classpath) và file ngoài hệ thống
    //     if (path == null || path.isEmpty() || path.startsWith("classpath:")) {
    //         // String resourcePath = (path != null && path.startsWith("classpath:")) ? path : "classpath:data/limits.txt";
    //         // Resource resource = resourceLoader.getResource(resourcePath);
    //         // return new BufferedReader(new InputStreamReader(resource.getInputStream()));
    //         Resource resource = resourceLoader.getResource(path.startsWith("classpath:") ? path : "file:" + path);
    //         if (resource.exists()) {
    //             return new BufferedReader(new InputStreamReader(resource.getInputStream()));
    //         }
    //     } else {
    //         return new BufferedReader(new FileReader(path));
    //     }
    // }

    public Double getParam(String module, String level) {
        if (!(level.equals("warn") || level.equals("error") || level.equals("ignore"))) {
            throw new IllegalArgumentException("Level phải là warn, error hoặc ignore");
        }
        String key = module + ":" + level;
        if (!parameters.containsKey(key)) {
            throw new IllegalArgumentException("Không tìm thấy key: " + key);
        }
        return parameters.get(key);
    }
}