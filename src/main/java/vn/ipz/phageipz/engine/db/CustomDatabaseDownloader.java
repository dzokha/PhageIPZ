package vn.ipz.phageipz.engine.db;

import org.springframework.stereotype.Service;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;

@Service
public class CustomDatabaseDownloader {

    // Danh sách các link tải trực tiếp (Ví dụ link từ Zenodo hoặc trang chủ)
    private static final String PHROGS_URL = "https://zenodo.org/record/7066751/files/phrogs_mmseqs_db.tar.gz";
    
    public void downloadManual(String targetDir) throws Exception {
        Path path = Paths.get(targetDir);
        if (!Files.exists(path)) Files.createDirectories(path);

        System.out.println("🚀 Bắt đầu tải dữ liệu thủ công từ các nguồn chính thức...");

        // 1. Tải PHROGs
        downloadFile(PHROGS_URL, path.resolve("phrogs.tar.gz"));

        // 2. Giải nén (Sử dụng lệnh hệ thống để nhanh hơn với file lớn)
        executeCommand("tar -xzf " + path.resolve("phrogs.tar.gz") + " -C " + targetDir);

        System.out.println("✅ Hoàn tất thiết lập CSDL.");
    }

    private void downloadFile(String url, Path targetPath) throws Exception {
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();

        System.out.println("📥 Đang tải: " + url);
        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(targetPath));

        if (response.statusCode() != 200) {
            throw new IOException("Lỗi tải file: HTTP " + response.statusCode());
        }
    }

    private void executeCommand(String command) throws Exception {
        Process process = Runtime.getRuntime().exec(command);
        process.waitFor();
    }
}