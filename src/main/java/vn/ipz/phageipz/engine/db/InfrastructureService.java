package vn.ipz.phageipz.engine.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Locale;

@Service
public class InfrastructureService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InfrastructureService.class);
    private final String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);

    @Override
    public void run(String... args) {
        log.info("🚀 Hệ điều hành phát hiện: {}", System.getProperty("os.name"));
        log.info("🔍 Đang kiểm tra các phụ thuộc hệ thống tin sinh học...");
        
        // 1. Kiểm tra công cụ lõi Sinh học
        checkAndInstall("mmseqs", "mmseqs2");
        checkAndInstall("prodigal", "prodigal");

        // 2. Tự động kiểm tra và tải model AI (Ollama)
        if (isToolInstalled("ollama")) {
            log.info("🤖 Phát hiện Ollama. Bắt đầu kiểm tra và tải model 'llama3' (Sẽ mất vài phút nếu tải lần đầu)...");
            pullOllamaModel("llama3");
        } else {
            log.warn("⚠️ Không tìm thấy phần mềm Ollama trên máy tính. Tính năng Trợ lý AI sẽ không hoạt động!");
            log.warn("👉 Vui lòng tải và cài đặt Ollama tại: https://ollama.com/download");
        }
    }

    private void checkAndInstall(String cmdName, String packageName) {
        if (!isToolInstalled(cmdName)) {
            log.warn("⚠️ Không tìm thấy lệnh '{}'. Bắt đầu quá trình cài đặt tự động cho gói '{}'...", cmdName, packageName);
            autoInstall(packageName);
        } else {
            log.info("✅ Lệnh '{}' đã sẵn sàng.", cmdName);
        }
    }

    private boolean isToolInstalled(String toolName) {
        String checkCmd = os.contains("win") ? "where" : "which";
        try {
            Process process = new ProcessBuilder(checkCmd, toolName).start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tự động kéo (pull) model LLM về máy không gây treo ứng dụng
     */
    private void pullOllamaModel(String modelName) {
        try {
            // SỬ DỤNG "pull" THAY VÌ "run" ĐỂ TRÁNH BỊ TREO TIẾN TRÌNH JAVA
            executeCommand("ollama", "pull", modelName);
            log.info("✅ Model AI '{}' đã được nạp thành công và sẵn sàng phục vụ!", modelName);
        } catch (Exception e) {
            log.error("❌ Xảy ra lỗi khi cố gắng tải model AI (Vui lòng kiểm tra lại kết nối mạng): {}", e.getMessage());
        }
    }

    /**
     * Logic cài đặt thông minh đa nền tảng
     */
    private void autoInstall(String packageName) {
        if (os.contains("win")) {
            log.error("❌ Windows không hỗ trợ cài đặt Native tự động cho các công cụ này.");
            log.error("👉 Giải pháp: Vui lòng chạy ứng dụng Spring Boot này bên trong môi trường WSL (Ubuntu) hoặc sử dụng Docker/Conda.");
            return;
        }

        try {
            if (os.contains("mac")) {
                String brewPackage = packageName;
                if (packageName.equals("hh-suite") || packageName.contains("mmseqs")) {
                    brewPackage = "brewsci/bio/" + packageName;
                }
                executeCommand("brew", "install", brewPackage);
                
            } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                if (isToolInstalled("conda")) {
                    String condaPackage = packageName.equals("hh-suite") ? "hhsuite" : packageName;
                    executeCommand("conda", "install", "-c", "bioconda", "-y", condaPackage);
                } 
                else if (isToolInstalled("apt-get")) {
                    log.warn("⚠️ Đang dùng apt-get. Hãy đảm bảo user có quyền sudo không cần nhập password (NOPASSWD)!");
                    executeCommand("sudo", "apt-get", "install", "-y", packageName);
                } 
                else if (isToolInstalled("yum")) {
                    log.warn("⚠️ Đang dùng yum. Hãy đảm bảo user có quyền sudo không cần nhập password (NOPASSWD)!");
                    executeCommand("sudo", "yum", "install", "-y", packageName);
                } 
                else if (isToolInstalled("brew")) {
                    executeCommand("brew", "install", packageName);
                } 
                else {
                    log.error("❌ Không tìm thấy trình quản lý gói nào (Conda, Apt, Yum, Brew) trên hệ thống Linux này!");
                }
            }
        } catch (Exception e) {
            log.error("❌ Xảy ra lỗi trong quá trình cài đặt tự động: {}", e.getMessage());
        }
    }

    /**
     * Hàm thực thi lệnh an toàn, gộp luồng output vào SLF4J của Spring Boot
     */
    private void executeCommand(String... command) throws Exception {
        log.info("⚙️ Đang thực thi lệnh hệ thống: {}", String.join(" ", command));
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true); 
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // In log quá trình tải (ví dụ: tiến độ tải 4.7GB của Ollama)
                log.info("  [EXEC] {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode == 0) {
            log.info("✅ Lệnh hệ thống hoàn tất!");
        } else {
            log.error("❌ Lệnh hệ thống thất bại với mã lỗi (Exit code): {}", exitCode);
            throw new RuntimeException("Execution failed with code: " + exitCode);
        }
    }
}