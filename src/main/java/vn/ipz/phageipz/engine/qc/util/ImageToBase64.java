package vn.ipz.phageipz.engine.qc.util;

import java.util.Base64;
import java.nio.file.Files;
import java.io.File;
import java.io.IOException;

public class ImageToBase64 {

	public static String encode(File file) {
        if (file == null || !file.exists()) return "";
        
        try {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            return Base64.getEncoder().encodeToString(fileContent);
        } catch (IOException e) {
            return "";
        }
    }

    // Hàm bổ trợ để tạo header cho thẻ <img> trong HTML
    public static String encodeWithHeader(File file, String contentType) {
        return "data:" + contentType + ";base64," + encode(file);
    }
	
}
