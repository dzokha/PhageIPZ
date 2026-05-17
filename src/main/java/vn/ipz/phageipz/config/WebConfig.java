package vn.ipz.phageipz.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import jakarta.annotation.PostConstruct;
import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @PostConstruct
    public void init() {
        // Cần thiết cho xử lý ảnh/biểu đồ trên Linux/Docker
        System.setProperty("java.awt.headless", "true");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. Cấu hình đường dẫn cho tài nguyên báo cáo sinh học xuất ra bên ngoài
        registry.addResourceHandler("/reports/**")
                .addResourceLocations("file:data/reports/");

        // 2. GIẢI PHÁP ĐỊNH TUYẾN REACT SPA TUYỆT ĐỐI AN TOÀN
        // Bắt toàn bộ các request và kiểm tra thông qua bộ lọc PathResourceResolver
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        // Nếu là đường dẫn gọi API của hệ thống, bỏ qua không xử lý 
                        // để Spring trả về mã JSON lỗi 404 chuẩn API thay vì trả về giao diện web
                        if (resourcePath.startsWith("api") || resourcePath.startsWith("/api")) {
                            return null;
                        }
                        
                        // Kiểm tra xem file tĩnh thực tế có tồn tại trong thư mục static không
                        Resource requestedResource = location.createRelative(resourcePath);
                        
                        // Nếu file tồn tại (ví dụ: các file .js, .css, ảnh logo do Vite build ra) thì trả về đúng file đó
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        
                        // Nếu file KHÔNG tồn tại (Nghĩa là người dùng đang F5 hoặc gõ đường dẫn ảo của React Router 
                        // như /settings, /dashboard...), tự động mớm file index.html gốc về để React tự xử lý định tuyến.
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}